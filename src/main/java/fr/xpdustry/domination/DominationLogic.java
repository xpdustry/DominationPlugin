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
import fr.xpdustry.distributor.api.*;
import fr.xpdustry.distributor.api.event.*;
import java.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

public final class DominationLogic implements Runnable {

  private final DominationPlugin plugin;

  public DominationLogic(final DominationPlugin plugin) {
    this.plugin = plugin;
    DistributorProvider.get().getPluginScheduler().syncRepeatingDelayedTask(this.plugin, this, 10, 10);
  }

  @Override
  public void run() {
    if (Vars.state.isPlaying() && plugin.isEnabled()) {
      for (final var zone : plugin.getState().getZones()) {
        // Reset the team if the team got beaten
        if (zone.getTeam() != Team.derelict && !zone.getTeam().active()) {
          zone.setTeam(Team.derelict);
          zone.setCapture(100);
        }

        // Count the number of units in the zone, per team
        final var units = new ObjectIntMap<Team>();
        Groups.unit.each(
                unit -> {
                  if (!unit.spawnedByCore) {
                    unit.within(zone.getX(), zone.getY(), zone.getRadius());
                    units.increment(unit.team());
                  }
                }
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

      final var leaderboard = plugin.getState().getLeaderboard();
      if (leaderboard.size() == 1) {
        final var entry = leaderboard.entrySet().iterator().next();
        if (!entry.getKey().equals(Team.derelict) && entry.getValue() == plugin.getState().getZones().size() * 100) {
          EventBus.mindustry().post(new GameOverEvent(entry.getKey()));
          return;
        }
      }

      if (plugin.getState().getRemainingTime().isZero()) {
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
          EventBus.mindustry().post(new GameOverEvent(winners.get(0)));
        } else {
          EventBus.mindustry().post(new GameOverEvent(Team.derelict));
        }
      }
    }
  }
}
