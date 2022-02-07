package fr.xpdustry.domination;

import arc.*;
import arc.struct.*;
import arc.util.*;

import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.net.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import fr.xpdustry.distributor.command.*;
import fr.xpdustry.distributor.plugin.*;
import fr.xpdustry.distributor.string.*;
import fr.xpdustry.domination.Zone.*;

import cloud.commandframework.arguments.standard.*;
import cloud.commandframework.arguments.standard.StringArgument.*;
import cloud.commandframework.types.tuples.*;
import com.google.gson.*;
import net.mindustry_ddns.store.*;
import org.checkerframework.checker.nullness.qual.*;

import java.util.*;


public class DominationPlugin extends AbstractPlugin{
    private static final String DOMINATION_ACTIVE_KEY = "xpdustry:domination";

    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Zone.class, new ZoneAdapter())
        .create();

    private static final FileStore<DominationMapConfig> store =
        new JsonFileStore<>("./unknown.json", DominationMapConfig.class, DominationMapConfig::new, gson);

    private static final ObjectSet<Player> editors = new ObjectSet<>();
    private static final ObjectFloatMap<Team> leaderboard = new ObjectFloatMap<>();
    private static final Map<Zone, WorldLabel> labels = new HashMap<>();

    private static final Interval timers = new Interval(2);
    private static boolean showdown = false;

    private static final int
        UPDATE_TIMER = 0,
        COUNTDOWN_TIMER = 1;

    public static DominationMapConfig config(){
        return store.get();
    }

    @Override public void init(){
        Events.on(PlayerLeave.class, e -> editors.remove(e.player));

        Events.on(PlayEvent.class, e -> {
            final var file = getDirectory().child("maps").child(Vars.state.map.name() + ".json");
            store.setFile(file.path());
            store.load();

            if(isActive()){
                showdown = false;
                timers.reset(COUNTDOWN_TIMER, 0);
                labels.clear();
                config().forEach(zone -> labels.put(zone, createLabel(zone)));
            }
        });

        Events.on(TapEvent.class, e -> {
            if(editors.contains(e.player)){
                final var zone = new Zone(e.tile.x, e.tile.y);

                if(config().hasZone(zone)){
                    config().removeZone(zone);
                    if(isActive()) labels.remove(zone).remove();
                }else{
                    config().addZone(zone);
                    if(isActive()) labels.put(zone, createLabel(zone));
                }

                store.save();
            }
        });

        Events.run(Trigger.update, () -> {
            if(Vars.state.isPlaying() && timers.get(UPDATE_TIMER, Time.toSeconds / 6)){
                editors.each(p -> {
                    config().drawZoneCenters(p.con());
                    if(!isActive()) config().drawZoneCircles(p.con());
                });

                if(isActive()){
                    config().forEach(z -> z.update(config()));
                    leaderboard.clear();
                    config().forEach(z -> leaderboard.increment(z.getTeam(), 0F, z.getPercent()));

                    // Graphics
                    config().drawZoneCircles();
                    labels.forEach((z, l) -> l.text(Strings.format("[#@]@%", z.getTeam().color, z.getPercent())));

                    // HUD text
                    final var builder = new StringBuilder(100);
                    final var gameDuration = showdown ? config().getShowdownDuration() : config().getGameDuration();

                    builder.append(showdown ? "[red]" : "");
                    final var remainingTime = Math.max((long)((gameDuration - timers.getTime(COUNTDOWN_TIMER)) / Time.toSeconds * 1000L), 0L);
                    builder.append("Time remaining > ").append(Strings.formatMillis(remainingTime));
                    builder.append(showdown ? "[]" : "");

                    // Leaderboard
                    final var sorted = new Seq<Pair<Team, Float>>();
                    leaderboard.forEach(e -> sorted.add(Pair.of(e.key, e.value)));
                    sorted.sort(Comparator.comparingDouble(Pair::getSecond));
                    sorted.each(e -> builder
                        .append("\n[#").append(e.getFirst().color).append(']')
                        .append(e.getFirst() == Team.derelict ? "Unclaimed" : Strings.capitalize(e.getFirst().name))
                        .append("[] > ").append(Strings.fixed(e.getSecond() / leaderboard.values().toArray().sum(), 2)).append('%')
                    );

                    Call.setHudText(builder.toString());

                    if(timers.get(COUNTDOWN_TIMER, gameDuration)){
                        final var winners = getWinners();

                        switch(winners.size()){
                            case 0 -> triggerShowdown(Vars.state.teams.getActive().map(d -> d.team).list());
                            case 1 -> Events.fire(new GameOverEvent(winners.get(0)));
                            default -> triggerShowdown(winners);
                        }
                    }
                }
            }
        });
    }

    @Override public void registerClientCommands(@NonNull ArcCommandManager manager){
        manager.command(manager.commandBuilder("domination").literal("edit")
            .meta(ArcMeta.PLUGIN, asLoadedMod().name)
            .meta(ArcMeta.DESCRIPTION, "Enable/Disable domination edit mode.")
            .permission(ArcPermission.ADMIN)
            .handler(ctx -> {
                if(editors.add(ctx.getSender().asPlayer())){
                    ctx.getSender().send("You enabled the editor mode of domination.");
                }else{
                    editors.remove(ctx.getSender().asPlayer());
                    ctx.getSender().send("You disabled the editor mode of domination.");
                }
            })
        );
    }

    @Override public void registerSharedCommands(@NonNull ArcCommandManager manager){
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

                    if(map == null){
                        ctx.getSender().send(MessageIntent.ERROR, "Failed to load '@' map.", ctx.<String>get("map"));
                        return;
                    }

                    if(hotLoading) reloader.begin();

                    Vars.world.loadMap(map);
                    Vars.state.rules = map.applyRules(Gamemode.pvp);
                    Vars.state.rules.modeName = "[red]Domination";
                    Vars.state.rules.tags.put(DOMINATION_ACTIVE_KEY, "true");

                    Vars.logic.play();
                    if(hotLoading){
                        reloader.end();
                    }else{
                        Vars.netServer.openServer();
                    }
                });
            })
        );
    }

    public static void triggerShowdown(List<Team> teams){
        showdown = true;
        Call.warningToast((char)9888, "[red]SHOWDOWN ![]");

        Vars.state.teams.getActive().forEach(data -> {
            if(teams.contains(data.team)) return;

            data.cores.each(CoreBuild::kill);
            Groups.player.each(p -> {
                if(p.team() == data.team){
                    p.team(Team.derelict);
                    p.unit().kill();
                }
            });

            Call.sendMessage(Strings.format("Team [#@]@[] has been reduced to ashes...", data.team.color, data.team.name));
        });
    }

    public static List<Team> getWinners(){
        var maxPercent = 0F;
        final var winners = new ArrayList<Team>();

        for(final var entry : leaderboard.entries()){
            if(entry.key == Team.derelict) continue;

            if(entry.value > maxPercent){
                winners.clear();
                winners.add(entry.key);
                maxPercent = entry.value;
            }else if(entry.value == maxPercent){
                winners.add(entry.key);
            }
        }

        return winners;
    }

    public static boolean isActive(){
        return Vars.state.isPlaying() && Vars.state.rules.tags.getBool(DOMINATION_ACTIVE_KEY) && !Vars.state.gameOver;
    }

    private static WorldLabel createLabel(final @NonNull Zone zone){
        final var label = WorldLabel.create();
        label.set(zone);
        label.z(Layer.flyingUnit);
        label.flags((byte)(WorldLabel.flagOutline | WorldLabel.flagBackground));
        label.fontSize(2F);
        label.text("???%");
        label.add();
        return label;
    }
}
