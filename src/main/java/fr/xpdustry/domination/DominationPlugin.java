/*
 * DominationPlugin, a "capture the zone" like gamemode plugin.
 *
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.domination;

import arc.*;
import arc.struct.*;
import arc.util.*;
import cloud.commandframework.arguments.standard.*;
import cloud.commandframework.arguments.standard.StringArgument.*;
import cloud.commandframework.meta.*;
import cloud.commandframework.types.tuples.*;
import com.google.gson.*;
import fr.xpdustry.distributor.api.command.*;
import fr.xpdustry.distributor.api.command.sender.*;
import fr.xpdustry.distributor.api.plugin.*;
import fr.xpdustry.domination.Zone.*;
import fr.xpdustry.domination.graphics.*;
import io.leangen.geantyref.*;
import java.util.*;
import java.util.function.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import net.mindustry_ddns.filestore.*;
import net.mindustry_ddns.filestore.serial.*;
import org.checkerframework.checker.nullness.qual.*;
import org.jetbrains.annotations.*;

@SuppressWarnings("unused")
public class DominationPlugin extends ExtendedPlugin {

  private static final String DOMINATION_ACTIVE_KEY = "xpdustry:domination";

  private static final Gson gson = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(Zone.class, new ZoneAdapter())
    .create();

  private static final int
    UPDATE_TIMER = 0,
    COUNTDOWN_TIMER = 1;

  private final FileStore<DominationMapConfig> store = FileStore.of(
    "./unknown.json",
    Serializers.gson(gson),
    TypeToken.get(DominationMapConfig.class),
    new DominationMapConfig()
  );

  private final ObjectSet<Player> editors = new ObjectSet<>();
  private final ObjectFloatMap<Team> leaderboard = new ObjectFloatMap<>();
  private final Map<Zone, MapLabel> labels = new HashMap<>();
  private final Interval timers = new Interval(2);

  private final ArcCommandManager<CommandSender> clientCommands = ArcCommandManager.standard(this);
  private final ArcCommandManager<CommandSender> serverCommands = ArcCommandManager.standard(this);

  private boolean showdown = false;

  public void triggerShowdown(final @NotNull List<Team> teams) {
    showdown = true;
    Call.warningToast((char) 9888, "[red]SHOWDOWN ![]");

    Vars.state.teams.getActive().forEach(data -> {
      if (teams.contains(data.team)) return;

      data.cores.each(CoreBuild::kill);
      Groups.player.each(p -> {
        if (p.team() == data.team) {
          p.team(Team.derelict);
          p.unit().kill();
        }
      });

      Call.sendMessage(Strings.format("Team [#@]@[] has been reduced to ashes...", data.team.color, data.team.name));
    });
  }

  public List<Team> getWinners() {
    var maxPercent = 0F;
    final var winners = new ArrayList<Team>();

    for (final var entry : leaderboard.entries()) {
      if (entry.key == Team.derelict) continue;

      if (entry.value > maxPercent) {
        winners.clear();
        winners.add(entry.key);
        maxPercent = entry.value;
      } else if (entry.value == maxPercent) {
        winners.add(entry.key);
      }
    }

    return winners;
  }

  public static boolean isActive() {
    return Vars.state.isPlaying() && Vars.state.rules.tags.getBool(DOMINATION_ACTIVE_KEY) && !Vars.state.gameOver;
  }

  private static MapLabel createLabel(final @NonNull Zone zone) {
    final var label = MapLabel.create();
    label.setPosition(zone);
    label.setText("???%");
    label.add();
    return label;
  }

  @Override
  public void onInit() {
    Events.on(PlayerLeave.class, e -> editors.remove(e.player));

    Events.on(PlayEvent.class, e -> {
      final var file = getDirectory().resolve(Vars.state.map.name() + ".json");
      store.setFile(file.toFile());
      store.load();

      if (isActive()) {
        showdown = false;
        timers.reset(COUNTDOWN_TIMER, 0);
        labels.clear();
        store.get().forEach(zone -> labels.put(zone, createLabel(zone)));
      }
    });

    Events.on(TapEvent.class, e -> {
      if (editors.contains(e.player)) {
        final var zone = new Zone(e.tile.x, e.tile.y);

        if (store.get().hasZone(zone)) {
          store.get().removeZone(zone);
          if (isActive()) labels.remove(zone).remove();
        } else {
          store.get().addZone(zone);
          if (isActive()) labels.put(zone, createLabel(zone));
        }

        store.save();
      }
    });

    Events.run(Trigger.update, () -> {
      if (Vars.state.isPlaying() && timers.get(UPDATE_TIMER, Time.toSeconds / 6)) {
        editors.each(p -> {
          store.get().drawZoneCenters(p.con());
          if (!isActive()) store.get().drawZoneCircles(p.con());
        });

        if (isActive()) {
          store.get().forEach(z -> z.update(store.get()));
          leaderboard.clear();
          store.get().forEach(z -> leaderboard.increment(z.getTeam(), 0F, z.getPercent()));

          // Graphics
          store.get().drawZoneCircles();
          labels.forEach((z, l) -> l.setText(Strings.format("[#@]@%", z.getTeam().color, z.getPercent())));

          // HUD text
          final var builder = new StringBuilder(100);
          final var gameDuration = (showdown ? store.get().getShowdownDuration() : store.get().getGameDuration()) * Time.toMinutes;

          builder.append(showdown ? "[red]" : "");
          final var remainingTime = Math.max((long) ((gameDuration - timers.getTime(COUNTDOWN_TIMER)) / Time.toSeconds * 1000L), 0L);
          builder.append("Time remaining > ").append(Strings.formatMillis(remainingTime));
          builder.append(showdown ? "[]" : "");

          // Leaderboard
          final var sorted = new Seq<Pair<Team, Float>>();
          leaderboard.forEach(e -> sorted.add(Pair.of(e.key, e.value)));
          sorted.sort(Comparator.comparingDouble(Pair::getSecond));
          sorted.each(e -> builder
            .append("\n[#").append(e.getFirst().color).append(']')
            .append(e.getFirst() == Team.derelict ? "Unclaimed" : Strings.capitalize(e.getFirst().name))
            .append("[] > ").append(Strings.fixed(e.getSecond() / store.get().getZones().size(), 2)).append('%')
          );

          Call.setHudText(builder.toString());

          if (timers.get(COUNTDOWN_TIMER, gameDuration)) {
            final var winners = getWinners();

            switch (winners.size()) {
              case 0 -> triggerShowdown(Vars.state.teams.getActive().map(d -> d.team).list());
              case 1 -> Events.fire(new GameOverEvent(winners.get(0)));
              default -> triggerShowdown(winners);
            }
          }
        }
      }
    });
  }

  @Override
  public void onServerCommandsRegistration(CommandHandler handler) {
    serverCommands.initialize(handler);
    onSharedCommandsRegistration(serverCommands);
  }

  @Override
  public void onClientCommandsRegistration(final CommandHandler handler) {
    clientCommands.initialize(handler);
    clientCommands.command(clientCommands.commandBuilder("domination").literal("edit")
      .meta(CommandMeta.DESCRIPTION, "Enable/Disable domination edit mode.")
      .permission("fr.xpdustry.domination.edit")
      .handler(ctx -> {
        final var player = ctx.getSender().getPlayer();
        if (!editors.add(player)) editors.remove(player);
        ctx.getSender().sendMessage(
          Strings.format("You @ the editor mode of domination.", editors.contains(player) ? "enabled" : "disabled")
        );
      })
    );
    onSharedCommandsRegistration(clientCommands);
  }

  public void onSharedCommandsRegistration(final @NonNull ArcCommandManager<CommandSender> manager) {
    manager.command(manager.commandBuilder("domination").literal("start")
      .meta(CommandMeta.DESCRIPTION, "Start a domination game.")
      .permission("fr.xpdustry.domination.start")
      .argument(StringArgument.of("map", StringMode.GREEDY))
      .handler(ctx -> {
        Core.app.post(() -> {
          final var map = Vars.maps.all()
            .find(m -> Strings.stripColors(m.name().replace('_', ' '))
              .equalsIgnoreCase(Strings.stripColors(ctx.get("map")).replace('_', ' ')));
          final var hotLoading = Vars.state.isPlaying();
          final var reloader = new WorldReloader();

          if (map == null) {
            ctx.getSender().sendMessage(Strings.format("Failed to load '@' map.", ctx.<String>get("map")));
            return;
          }

          if (hotLoading) {
            reloader.begin();
          }

          Vars.world.loadMap(map);
          Vars.state.rules = map.applyRules(Gamemode.pvp);
          Vars.state.rules.modeName = "[red]Domination";
          Vars.state.rules.tags.put(DOMINATION_ACTIVE_KEY, "true");

          Vars.logic.play();
          if (hotLoading) {
            reloader.end();
          } else {
            Vars.netServer.openServer();
          }
        });
      })
    );
  }
}
