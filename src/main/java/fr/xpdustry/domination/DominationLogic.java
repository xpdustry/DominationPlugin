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

import arc.struct.*;
import arc.util.*;
import cloud.commandframework.meta.*;
import fr.xpdustry.distributor.api.plugin.*;
import fr.xpdustry.distributor.api.util.*;
import java.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

public final class DominationLogic implements PluginListener {

  private static final String DOMINATION_RULES = """
    Welcome to [cyan]Domination PVP[].
    The rules are simple. [red]Your team must capture all the zones.[]
    To do so, a team sends their units in a zone until it reaches 100%.
    You can obtain a list of the zones and their status with the command [orange]/domination zones[].
    To see this message again, do [orange]/domination rules[].
    """;

  private final DominationPlugin domination;
  private final Interval interval = new Interval();

  public DominationLogic(final DominationPlugin domination) {
    this.domination = domination;
  }

  @Override
  public void onPluginInit() {
    MoreEvents.subscribe(EventType.PlayerJoin.class, event -> {
      if (this.domination.isEnabled()) {
        Call.infoMessage(event.player.con(), DOMINATION_RULES);
      }
    });
  }

  @Override
  public void onPluginClientCommandsRegistration(final CommandHandler handler) {
    this.domination.getClientCommandManager().command(this.domination.getClientCommandManager()
      .commandBuilder("domination")
      .literal("rules")
      .meta(CommandMeta.DESCRIPTION, "Display the domination rules")
      .handler(context -> {
        Call.infoMessage(context.getSender().getPlayer().con(), DOMINATION_RULES);
      })
    );
  }

  @Override
  public void onPluginUpdate() {
    if (interval.get(Time.toSeconds / 6) && Vars.state.isPlaying() && domination.isEnabled()) {
      for (final var zone : domination.getState().getZones()) {
        // Reset the team if the team got beaten
        if (zone.getTeam() != Team.derelict && !zone.getTeam().active()) {
          zone.setTeam(Team.derelict);
          zone.setCapture(100);
        }

        // Count the number of units in the zone, per team
        final var units = new ObjectIntMap<Team>();
        Groups.unit.each(
          unit -> unit.within(zone.getX(), zone.getY(), zone.getRadius()) && !unit.spawnedByCore(),
          unit -> units.increment(unit.team())
        );

        // Search for the team with the most units
        var winner = Team.derelict;
        int max = 0;

        for (final var entry : units) {
          if (entry.value > max) {
            winner = entry.key;
            max = entry.value;
          } else if (entry.value == max) {
            // If 2 teams have the same number of units, don't update so set back to derelict.
            winner = Team.derelict;
          }
        }

        // Updates the zone values
        if (winner != Team.derelict) {
          final int rate = 1;
          if (zone.getTeam() == winner) {
            zone.setCapture(Math.min(zone.getCapture() + rate, 100));
          } else {
            zone.setCapture(Math.max(zone.getCapture() - rate, 0));
            if (zone.getCapture() == 0) {
              Call.warningToast(
                Iconc.warning,
                String.format("[#%s]%s[] captured a zone at (%d, %d).", winner.color, winner.name, zone.getX() / 8, zone.getY() / 8)
              );
              zone.setTeam(winner);
            }
          }
        }
      }

      final var leaderboard = domination.getState().getLeaderboard();
      if (leaderboard.size() == 1) {
        final var entry = leaderboard.entrySet().iterator().next();
        if (!entry.getKey().equals(Team.derelict) && entry.getValue() == domination.getState().getZones().size() * 100) {
          MoreEvents.post(new GameOverEvent(entry.getKey()));
          return;
        }
      }

      if (domination.getState().getRemainingTime().isZero()) {
        int max = 0;
        final var winners = new ArrayList<Team>();

        for (final var entry : leaderboard.entrySet()) {
          if (entry.getKey() == Team.derelict) {
            continue;
          }

          if (entry.getValue() > max) {
            winners.clear();
            winners.add(entry.getKey());
            max = entry.getValue();
          } else if (entry.getValue() == max) {
            winners.add(entry.getKey());
          }
        }

        if (winners.size() == 1) {
          MoreEvents.post(new GameOverEvent(winners.get(0)));
        } else {
          MoreEvents.post(new GameOverEvent(Team.derelict));
        }
      }
    }
  }
}
