package fr.xpdustry.domination;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;

import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import fr.xpdustry.domination.Zone.*;

import com.google.gson.*;
import com.google.gson.reflect.*;

import java.util.*;


@SuppressWarnings("unused")
public class DominationPlugin extends Plugin{
    private static boolean showdown = false;
    private static final Interval timers = new Interval(3);
    private static final ObjectSet<Playerc> editors = new ObjectSet<>();
    private static final OrderedMap<Team, Float> leaderboard = new OrderedMap<>();

    private static final Gson gson;
    private static final TreeMap<String, DominationMap> dominationMaps = new TreeMap<>();
    private static final Fi configFile = new Fi(Core.files.external("domination-config.json").absolutePath());

    private static final int
        LOGIC_TIMER = 0,
        COUNTDOWN_TIMER = 1,
        GRAPHICS_TIMER = 2;

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
                if(timers.get(LOGIC_TIMER, getCurrentMap().getUpdateTicks())){
                    getCurrentMap().update();
                    // Updates the leaderboard [team -> percent_captured]
                    leaderboard.clear(Vars.state.teams.active.size + 1);
                    getCurrentMap().forEach(z -> leaderboard.put(z.getTeam(), leaderboard.get(z.getTeam(), 0F) + z.getPercent()));
                }

                if(timers.get(COUNTDOWN_TIMER, (showdown ? getCurrentMap().getShowdownDuration() : getCurrentMap().getGameDuration()) * Time.toMinutes)){
                    List<Team> winners = getWinners();
                    if(winners.size() == 0){
                        Events.fire(new GameOverEvent(Team.derelict));
                    }else if(winners.size() == 1){
                        Events.fire(new GameOverEvent(winners.get(0)));
                    }else{
                        triggerShowdown(winners);
                    }
                }
            }

            if(timers.get(GRAPHICS_TIMER, Time.toSeconds / 6)){
                if(isActive()){
                    // HUD text
                    StringBuilder builder = new StringBuilder(100);

                    getCurrentMap().drawZoneCircles();
                    getCurrentMap().drawZoneTexts(1F / 6);

                    // Time remaining
                    builder.append(showdown ? "[red]" : "");
                    builder.append(Strings.format("Time remaining > @", Strings.formatMillis(getRemainingTime())));
                    builder.append(showdown ? "[]" : "");

                    // Leaderboard
                    leaderboard.each((team, percent) -> {
                        String percentString = Strings.fixed(percent / getCurrentMap().getZoneNumber(), 2);
                        builder.append(Strings.format("\n[#@]@[] > @%", team.color, team == Team.derelict ? "unclaimed" : team.name, percentString));
                    });

                    Call.setHudText(builder.toString());
                }

                // Rendering for editors
                editors.each(p -> {
                    if(Vars.state.isGame()){
                        getCurrentMap().drawZoneCenters(p.con());
                        if(!isActive()) getCurrentMap().drawZoneCircles(p.con());
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
                case "save": saveDominationMaps(); break;
                case "load": loadDominationMaps(); break;
                default: Log.info("The option '@' is invalid.", args[0].toLowerCase());
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
                case "on":
                    editors.add(player);
                    player.sendMessage("You enabled editor mode, now every click will create/delete a Zone.");
                    break;

                case "off":
                    editors.remove(player);
                    player.sendMessage("You disabled editor mode, how unfortunate...");
                    break;

                default: player.sendMessage(Strings.format("'@' is not a valid option.", args[0]));
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

    public static void resetGameCountdown(){
        timers.reset(COUNTDOWN_TIMER, 0);
    }

    public static void triggerShowdown(List<Team> teams){
        showdown = true;
        Call.sendMessage("[red]SHOWDOWN[].");
        resetGameCountdown();

        for(TeamData data : Vars.state.teams.getActive()){
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

        for(Entry<Team, Float> entry : leaderboard.entries()){
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

    /** Returns the remaining time in milliseconds */
    public static long getRemainingTime(){
        float remainingTicks = ((showdown ? getCurrentMap().getShowdownDuration() : getCurrentMap().getGameDuration()) * Time.toMinutes) - timers.getTime(COUNTDOWN_TIMER);
        return ((long)(remainingTicks / Time.toSeconds)) * 1000L;
    }

    public static boolean isActive(){
        return Vars.state.rules.pvp && !Vars.state.isMenu() && !Vars.state.gameOver;
    }
}
