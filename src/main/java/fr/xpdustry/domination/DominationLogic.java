package fr.xpdustry.domination;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import cloud.commandframework.types.tuples.*;
import com.google.gson.*;
import fr.xpdustry.distributor.api.event.*;
import fr.xpdustry.distributor.api.plugin.*;
import fr.xpdustry.distributor.api.util.*;
import io.leangen.geantyref.*;
import java.time.*;
import java.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import net.mindustry_ddns.filestore.*;
import net.mindustry_ddns.filestore.serial.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DominationLogic implements ApplicationListener {

  private static final String DOMINATION_STATE_KEY = "xpdustry-domination:state";
  private static final Seq<Effect> EFFECTS = Seq.with(Fx.mine, Fx.mineBig, Fx.mineHuge);

  private final ExtendedPlugin plugin;
  private final FileStore<DominationMap> loader;
  private final ObjectFloatMap<Team> leaderboard = new ObjectFloatMap<>();
  private final Map<DominationZone, WorldLabel> labels = new HashMap<>();
  private final Gson gson = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(DominationZone.class, new DominationZone.Adapter())
    .create();

  private @Nullable DominationState state = null;

  public DominationLogic(final DominationPlugin plugin) {
    this.plugin = plugin;
    this.loader = FileStore.of(
      plugin.getDirectory().resolve("maps").resolve("unknown.json").toFile(),
      Serializers.gson(gson),
      TypeToken.get(DominationMap.class),
      new DominationMap()
    );
  }

  @EventHandler
  private void onPlayEvent(final EventType.PlayEvent event) {
    final var file = plugin.getDirectory().resolve("maps").resolve(Vars.state.map.name() + ".json").toFile();
    loader.setFile(file);
    loader.load();

    if (Vars.state.rules.tags.getBool(DominationPlugin.DOMINATION_ACTIVE_KEY)) {
      final var old = Vars.state.map.tags.get(DOMINATION_STATE_KEY);
      if (old != null) {
        state = gson.fromJson(old, DominationState.class);
      } else {
        state = new DominationState(loader.get());
      }

      labels.clear();
      state.map.getZones().forEach(zone -> {
        final var label = WorldLabel.create();
        label.text("???%");
        label.z(Layer.flyingUnit);
        label.flags((byte) (WorldLabel.flagOutline | WorldLabel.flagBackground));
        label.fontSize(2F);
        label.set(zone.getWorldX(), zone.getWorldY());
        label.add();
        labels.put(zone, label);
      });
    }
  }

  @Override
  public void update() {
    if (state != null) {
      state.map.getZones().forEach(zone -> zone.update(state.map.getCaptureRate()));
      leaderboard.clear();
      state.map.getZones().forEach(zone -> leaderboard.increment(zone.getTeam(), 0F, zone.getPercent()));

      // Graphics
      Groups.player.forEach(this::drawZoneCircles);
      labels.forEach((zone, label) -> {
        label.text(Strings.format("[#@]@%", zone.getTeam().color, zone.getPercent()));
      });

      // HUD text
      final var builder = new StringBuilder(100);
      final Duration duration;
      final Duration remaining;
      if (state.isShowdown()) {
        duration = state.map.getShowdownDuration();
        remaining = Duration.between(state.lastShowdown, Instant.now(Clock.systemUTC()));
      } else {
        duration = state.map.getGameDuration();
        remaining = Duration.between(state.start, Instant.now(Clock.systemUTC()));
      }

      builder.append(state.isShowdown() ? "[red]" : "");
      builder.append("Time remaining > ").append(Strings.formatMillis(Math.max(duration.toMillis() - remaining.toMillis(), 0L)));
      builder.append(state.isShowdown() ? "[]" : "");

      // Leaderboard
      final var sorted = new Seq<Pair<Team, Float>>();
      leaderboard.forEach(e -> sorted.add(Pair.of(e.key, e.value)));
      sorted.sort(Comparator.comparingDouble(Pair::getSecond));
      sorted.each(e -> builder
        .append("\n[#").append(e.getFirst().color).append(']')
        .append(e.getFirst() == Team.derelict ? "Unclaimed" : Strings.capitalize(e.getFirst().name))
        .append("[] > ").append(Strings.fixed(e.getSecond() / state.map.getZones().size(), 2)).append('%')
      );
      Call.setHudText(builder.toString());

      if (remaining.toMillis() > duration.toMillis()) {
        float maxPercent = 0F;
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

        if (winners.size() == 1) {
          Events.fire(new GameOverEvent(winners.get(0)));
        } else {
          if (winners.size() != 0) {
            winners.clear();
            winners.addAll(new ArcList<>(Vars.state.teams.getActive().map(d -> d.team)));
          }
          Call.warningToast((char) 9888, "[red]SHOWDOWN ![]");
          state.lastShowdown = Instant.now(Clock.systemUTC());

          for (final var data : Vars.state.teams.getActive()) {
            if (winners.contains(data.team)) {
              continue;
            }

            data.cores.each(CoreBuild::kill);
            Groups.player.each(p -> {
              if (p.team() == data.team) {
                p.team(Team.derelict);
                p.unit().kill();
              }
            });

            Call.sendMessage(Strings.format("Team [#@]@[] has been reduced to ashes...", data.team.color, data.team.name));
          }
        }
      }
    }
  }

  private void drawZoneCircles(final Player player) {
    if (state != null) {
      state.map.getZones().forEach(zone -> {
        final var circle = Geometry.regPoly((int) (Mathf.pi * zone.getRadius()), zone.getRadius() * Vars.tilesize);
        Geometry.iteratePolygon((px, py) -> {
          Call.effect(player.con(), EFFECTS.random(), px + zone.getWorldX(), py + zone.getWorldY(), 0, zone.getTeam().color);
        }, circle);
      });
    }
  }

  private static final class DominationState {

    private final DominationMap map;
    private final Instant start = Instant.now(Clock.systemUTC());
    private @MonotonicNonNull Instant lastShowdown = null;

    private DominationState(final DominationMap map) {
      this.map = map;
    }

    private boolean isShowdown() {
      return lastShowdown != null;
    }
  }
}
