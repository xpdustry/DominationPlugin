package fr.xpdustry.domination;

import arc.*;
import arc.struct.*;
import arc.util.*;

import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;

import java.util.*;
import java.util.Map.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;


@SuppressWarnings("unused")  // <- Only used for this template so IntelliJ stop screaming at me...
public class DominationPlugin extends Plugin{
    private static int zoneRadius = 5;

    private static float captureRate = 10F;
    private static float updateTicks = Time.toSeconds;
    private static float renderTicks = Time.toSeconds / 6;
    private static float gameDuration = Time.toMinutes * 1;

    private static final Seq<Zone> zones = new Seq<>();
    private static final Interval interval = new Interval(3);
    private static final ObjectSet<Player> editors = new ObjectSet<>();

    @Override
    public void init(){
        Events.run(Trigger.update, () -> {
            if(state.rules.pvp){
                if(interval.get(0, updateTicks)) zones.forEach(z -> z.update(captureRate));
                if(interval.get(1, renderTicks)){
                    zones.forEach(z -> z.render(updateTicks));
                    // Shows the zone center to the editors
                    editors.each(p -> {
                        zones.each(z -> Call.effect(p.con, Fx.heal, z.x * tilesize, z.y * tilesize, 0, z.getTeam().color));
                    });
                }

                if(interval.get(2, gameDuration)){
                    Map<Team,Float> teams = new HashMap<>(state.teams.getActive().size);
                    zones.each(zone -> teams.compute(zone.getTeam(), (t, i) -> zone.getPercent() + (i != null ? i : 0)));

                    float max = 0F;
                    Team winner = Team.derelict;

                    for(Entry<Team,Float> entry : teams.entrySet()){
                        if(entry.getValue() > max){
                            max = entry.getValue();
                            winner = entry.getKey();
                        }
                    }

                    Events.fire(new GameOverEvent(winner));
                    Call.sendMessage(Strings.format("Congrats, @ team win!", winner));
                }
            }
        });

        Events.on(TapEvent.class, event -> {
            if(editors.contains(event.player)){
                Zone zone = zones.find(z -> z.x == event.tile.x && z.y == event.tile.y);
                if(zone == null){
                    zones.add(new Zone(event.tile.x, event.tile.y, zoneRadius));
                }else{
                    zones.remove(zone);
                }
            }
        });
    }

    /**
     * This method is called when the game register the server-side commands.
     * Make sure your plugin don't load the commands twice by adding a simple boolean check.
     */
    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    /**
     * This method is called when the game register the client-side commands.
     * Make sure your plugin don't load the commands twice by adding a simple boolean check.
     */
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("edit", "<on/off>", "edit the zones.", (args, player) -> {
            if(player.admin()){
                switch(args[0].toLowerCase()){
                    case "on":
                        editors.add(player);
                        player.sendMessage("You enabled editor mode, now every click will create/delete a Zone.");
                        break;
                    case "off":
                        editors.remove(player);
                        player.sendMessage("You disabled editor mode, how unfortunate...");
                        break;
                    default:
                        player.sendMessage(Strings.format("'@' is not a valid option, idiot!", args[0]));
                }
            }
        });
    }
}
