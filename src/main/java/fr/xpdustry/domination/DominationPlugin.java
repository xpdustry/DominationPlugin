package fr.xpdustry.domination;

import arc.*;
import arc.struct.*;
import arc.util.*;
import cloud.commandframework.arguments.*;
import cloud.commandframework.arguments.standard.*;
import cloud.commandframework.arguments.standard.StringArgument.*;
import cloud.commandframework.types.tuples.*;
import com.google.gson.*;
import fr.xpdustry.distributor.*;
import fr.xpdustry.distributor.command.*;
import fr.xpdustry.distributor.command.sender.*;
import fr.xpdustry.distributor.message.*;
import fr.xpdustry.distributor.plugin.*;
import fr.xpdustry.domination.Zone.*;
import fr.xpdustry.domination.graphics.*;
import java.util.*;
import java.util.function.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import net.mindustry_ddns.store.*;
import org.checkerframework.checker.nullness.qual.*;
import org.jetbrains.annotations.*;

@SuppressWarnings("unused")
public class DominationPlugin extends AbstractPlugin {

  private static final String DOMINATION_ACTIVE_KEY = "xpdustry:domination";

  private static final Gson gson = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(Zone.class, new ZoneAdapter())
    .create();

  private static final FileStore<DominationMapConfig> store =
    new JsonFileStore<>("./unknown.json", DominationMapConfig.class, DominationMapConfig::new, gson);

  private static final ObjectSet<Player> editors = new ObjectSet<>();
  private static final ObjectFloatMap<Team> leaderboard = new ObjectFloatMap<>();
  private static final Map<Zone, MapLabel> labels = new HashMap<>();

  private static final Interval timers = new Interval(2);
  private static final int
    UPDATE_TIMER = 0,
    COUNTDOWN_TIMER = 1;

  private static boolean showdown = false;

  public static DominationMapConfig getConf() {
    return store.get();
  }

  public static void triggerShowdown(final @NotNull List<Team> teams) {
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

  public static List<Team> getWinners() {
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
  public void init() {
    Events.on(PlayerLeave.class, e -> editors.remove(e.player));

    Events.on(PlayEvent.class, e -> {
      final var file = getDirectory().child(Vars.state.map.name() + ".json");
      store.setFile(file.path());
      store.load();

      if (isActive()) {
        showdown = false;
        timers.reset(COUNTDOWN_TIMER, 0);
        labels.clear();
        getConf().forEach(zone -> labels.put(zone, createLabel(zone)));
      }
    });

    Events.on(TapEvent.class, e -> {
      if (editors.contains(e.player)) {
        final var zone = new Zone(e.tile.x, e.tile.y);

        if (getConf().hasZone(zone)) {
          getConf().removeZone(zone);
          if (isActive()) labels.remove(zone).remove();
        } else {
          getConf().addZone(zone);
          if (isActive()) labels.put(zone, createLabel(zone));
        }

        store.save();
      }
    });

    Events.run(Trigger.update, () -> {
      if (Vars.state.isPlaying() && timers.get(UPDATE_TIMER, Time.toSeconds / 6)) {
        editors.each(p -> {
          getConf().drawZoneCenters(p.con());
          if (!isActive()) getConf().drawZoneCircles(p.con());
        });

        if (isActive()) {
          getConf().forEach(z -> z.update(getConf()));
          leaderboard.clear();
          getConf().forEach(z -> leaderboard.increment(z.getTeam(), 0F, z.getPercent()));

          // Graphics
          getConf().drawZoneCircles();
          labels.forEach((z, l) -> l.setText(Strings.format("[#@]@%", z.getTeam().color, z.getPercent())));

          // HUD text
          final var builder = new StringBuilder(100);
          final var gameDuration = (showdown ? getConf().getShowdownDuration() : getConf().getGameDuration()) * Time.toMinutes;

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
            .append("[] > ").append(Strings.fixed(e.getSecond() / getConf().getZones().size(), 2)).append('%')
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
  public void registerClientCommands(final @NonNull ArcCommandManager manager) {
    manager.command(manager.commandBuilder("domination").literal("edit")
      .meta(ArcMeta.PLUGIN, asLoadedMod().name)
      .meta(ArcMeta.DESCRIPTION, "Enable/Disable domination edit mode.")
      .permission(ArcPermission.ADMIN)
      .handler(ctx -> {
        final var player = ctx.getSender().getPlayer();
        if (!editors.add(player)) editors.remove(player);
        final var formatter = Distributor.getClientMessageFormatter();
        ctx.getSender().sendMessage(
          formatter.format(MessageIntent.SUCCESS, "You @ the editor mode of domination.", editors.contains(player) ? "enabled" : "disabled"
        ));
      })
    );

    createSettingsCommand(
      IntegerArgument.<ArcCommandSender>newBuilder("zone-radius").withMin(1).asOptional().build(),
      () -> getConf().getZoneRadius(),
      v -> getConf().setZoneRadius(v)
    );

    createSettingsCommand(
      FloatArgument.<ArcCommandSender>newBuilder("capture-rate").withMin(1).withMax(100).asOptional().build(),
      () -> getConf().getCaptureRate(),
      v -> getConf().setCaptureRate(v)
    );

    createSettingsCommand(
      FloatArgument.<ArcCommandSender>newBuilder("game-duration").withMin(1).asOptional().build(),
      () -> getConf().getGameDuration(),
      v -> getConf().setGameDuration(v)
    );

    createSettingsCommand(
      FloatArgument.<ArcCommandSender>newBuilder("showdown-duration").withMin(1).asOptional().build(),
      () -> getConf().getShowdownDuration(),
      v -> getConf().setShowdownDuration(v)
    );
  }

  @Override
  public void registerSharedCommands(final @NonNull ArcCommandManager manager) {
    manager.command(manager.commandBuilder("domination").literal("start")
      .meta(ArcMeta.PLUGIN, asLoadedMod().name)
      .meta(ArcMeta.DESCRIPTION, "Start a domination game.")
      .permission(ArcPermission.ADMIN)
      .argument(StringArgument.of("map", StringMode.GREEDY))
      .handler(ctx -> {
        Core.app.post(() -> {
          final var map = Vars.maps.all().find(m -> Strings.stripColors(m.name().replace('_', ' ')).equalsIgnoreCase(Strings.stripColors(ctx.get("map")).replace('_', ' ')));
          final var hotLoading = Vars.state.isPlaying();
          final var reloader = new WorldReloader();

          if (map == null) {
            final var formatter = Distributor.getMessageFormatter(ctx.getSender());
            ctx.getSender().sendMessage(formatter.format(MessageIntent.ERROR, "Failed to load '@' map.", ctx.<String>get("map")));
            return;
          }

          if (hotLoading) reloader.begin();

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

  private <T> void createSettingsCommand(
    final @NotNull CommandArgument<ArcCommandSender, T> argument,
    final @NotNull Supplier<T> getter,
    final @NotNull Consumer<T> setter
  ) {
    final var manager = Distributor.getClientCommandManager();
    manager.command(manager.commandBuilder("domination").literal("settings").literal(argument.getName())
      .meta(ArcMeta.PLUGIN, asLoadedMod().name)
      .meta(ArcMeta.DESCRIPTION, "Change the " + argument.getName() + " map setting.")
      .permission(ArcPermission.ADMIN)
      .argument(argument)
      .handler(ctx -> {
        final var formatter = Distributor.getClientMessageFormatter();

        ctx.<T>getOptional(argument.getName()).ifPresentOrElse(value -> {
          setter.accept(value);
          store.save();
          ctx.getSender().sendMessage(formatter.format(MessageIntent.SUCCESS, "The @ has been set to @.", argument.getName(), value));
        }, () -> {
          ctx.getSender().sendMessage(formatter.format(MessageIntent.INFO, "The current @ is @.", argument.getName(), getter.get()));
        });
      })
    );
  }
}
