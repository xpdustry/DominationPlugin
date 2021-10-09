package fr.xpdustry.domination;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;

import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;

import fr.xpdustry.domination.Zone.*;

import com.google.gson.*;
import com.google.gson.reflect.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;


@SuppressWarnings("unused")
public class DominationPlugin extends Plugin{
    private static boolean showdown = false;
    private static final Interval interval = new Interval(3);
    private static final ObjectSet<Playerc> editors = new ObjectSet<>();

    private static final ObjectFloatMap<Team> leaderboard = new ObjectFloatMap<>();

    private static final Gson gson;
    private static final TreeMap<String, DominationMap> dominationMaps = new TreeMap<>();
    private static final Fi configFile = new Fi(Core.files.external("domination-config.json").absolutePath());

    private static final int
        LOGIC_TIMER_SLOT = 0,
        COUNTDOWN_TIMER_SLOT = 1,
        GRAPHICS_TIMER_SLOT = 2;


    static{
        gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Zone.class, new ZoneAdapter().nullSafe())
        .create();
    }

    @Override
    public void init(){
        loadDominationMaps();

        Events.run(PlayEvent.class, () -> {
            if(isActive()) Vars.state.rules.modeName = "[red]Domination";
        });

        Events.run(WorldLoadEvent.class, DominationPlugin::resetGameCountdown);

        // Main
        Events.run(Trigger.update, () -> {
            if(isActive()){
                if(interval.get(LOGIC_TIMER_SLOT, getCurrentMap().getUpdateTicks())){
                    // Updates the zone internal data
                    getCurrentMap().forEach(z -> z.update(getCurrentMap()));
                    // Updates the leaderboard [team -> percent_captured]
                    leaderboard.clear(Vars.state.teams.active.size + 1);
                    getCurrentMap().forEach(z -> leaderboard.increment(z.getTeam(), 0, z.getPercent()));
                }

                if(interval.get(COUNTDOWN_TIMER_SLOT, (showdown ? getCurrentMap().getShowdownDuration() : getCurrentMap().getGameDuration()) * Time.toMinutes)){
                    var winners = getWinners();
                    if(winners.size() == 0){
                        Events.fire(new GameOverEvent(Team.derelict));
                    }else if(winners.size() == 1){
                        Events.fire(new GameOverEvent(winners.get(0)));
                    }else{
                        triggerShowdown(winners);
                    }
                }
            }

            if(interval.get(GRAPHICS_TIMER_SLOT, Time.toSeconds / 6)){
                if(isActive()){
                    // HUD text
                    StringBuilder builder = new StringBuilder(100);

                    getCurrentMap().drawZoneCircles();
                    getCurrentMap().drawZoneTexts();

                    // Time remaining
                    int time = (int)(((showdown ? Time.toMinutes : getCurrentMap().getGameDuration() * Time.toMinutes) - interval.getTime(1)) / Time.toSeconds);
                    builder.append(Strings.format("@Time remaining > @@", (showdown ? "[red]" : ""), Strings.formatMillis(time * 1000L), (showdown ? "[]" : "")));

                    // Unclaimed zones
                    if(leaderboard.containsKey(Team.derelict)){
                        builder.append(Strings.format("\n[#@]unclaimed[] > @%", Team.derelict.color, (int)leaderboard.get(Team.derelict, 0F)));
                    }

                    // Leaderboard
                    var orderedLeaderboard = Seq.with(leaderboard.entries()).sort(Comparator.comparing(a -> a.key));
                    orderedLeaderboard.each(entry -> {
                        if(entry.key != Team.derelict) builder.append(Strings.format("\n[#@]@[] > @%", entry.key.color, entry.key.name, (int)entry.value));
                    });

                    Call.setHudText(builder.toString());
                }

                // Rendering for editors
                editors.each(p -> {
                    getCurrentMap().drawZoneCenters(p.con());
                    if(!Vars.state.rules.pvp && Vars.state.isGame()){
                        getCurrentMap().drawZoneCircles(p.con());
                    }
                });
            }
        });

        // Reset the zones to their original states and save the settings
        Events.run(GameOverEvent.class, () -> {
            getCurrentMap().forEach(Zone::reset);
            showdown = false;
            saveDominationMaps();
        });

        Events.on(PlayerLeave.class, event -> {
            if(editors.contains(event.player)){
                editors.remove(event.player);
            }
        });

        Events.on(TapEvent.class, event -> {
            if(editors.contains(event.player)){
                Zone zone = getCurrentMap().getZone(event.tile.x, event.tile.y);
                if(zone == null){
                    getCurrentMap().addZone(new Zone(event.tile.x, event.tile.y));
                }else{
                    getCurrentMap().removeZone(zone);
                }
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("domination-config", "<save/load>", "Settings for the Domination plugin...", args -> {
            switch(args[0].toLowerCase()){
                case "save" -> saveDominationMaps();
                case "load" -> loadDominationMaps();
                default -> Log.info("The option '@' is invalid.", args[0].toLowerCase());
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Playerc>register("domination-edit", "<on/off>", "edit the zones.", (args, player) -> {
            if(!player.admin()){
                player.sendMessage("[red]You need to be a chosen one to use this command.");
                return;
            }

            switch(args[0].toLowerCase()){
                case "on" -> {
                    editors.add(player);
                    player.sendMessage("You enabled editor mode, now every click will create/delete a Zone.");
                }
                case "off" -> {
                    editors.remove(player);
                    player.sendMessage("You disabled editor mode, how unfortunate...");
                }
                default -> player.sendMessage(Strings.format("'@' is not a valid option.", args[0]));
            }
        });
    }

    public static void loadDominationMaps(){
        if(configFile.exists()){
            dominationMaps.putAll(gson.fromJson(configFile.reader(), new TypeToken<TreeMap<String, DominationMap>>(){}.getType()));
        }else{
            configFile.writeString(gson.toJson(dominationMaps));
        }

        Log.info("Domination maps have been loaded.");
    }

    public static void saveDominationMaps(){
        configFile.writeString(gson.toJson(dominationMaps));
        Log.info("Domination maps have been saved.");
    }

    public static boolean isActive(){
        return Vars.state.rules.pvp && !Vars.state.isMenu() && !Vars.state.gameOver;
    }

    @Nullable
    public static DominationMap getMap(String name){
        return dominationMaps.get(name);
    }

    public static DominationMap getCurrentMap(){
        return dominationMaps.computeIfAbsent(Vars.state.map.name(), k -> new DominationMap());
    }

    public static List<Team> getWinners(){
        float maxPercent = 0F;
        List<Team> winners = new ArrayList<>();

        for(var entry : leaderboard.entries()){
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

    public static void resetGameCountdown(){
        interval.reset(COUNTDOWN_TIMER_SLOT, 0);
    }

    public static void triggerShowdown(List<Team> teams){
        showdown = true;
        Call.sendMessage("[red]SHOWDOWN[].");
        resetGameCountdown();

        for(var data : Vars.state.teams.getActive()){
            if(!teams.contains(data.team)){
                data.cores.each(CoreBuild::kill);
                Groups.player.each(p -> {
                    if(p.team() == data.team){
                        p.team(Team.derelict);
                        p.unit().kill();
                    }
                });

                Call.sendMessage(Strings.format("Team [#@]@[] has been reduced to ashes...", data.team.color, data.team.name));
            }
        }
    }
}
