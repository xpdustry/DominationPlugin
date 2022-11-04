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
package fr.xpdustry.domination.commands;

import cloud.commandframework.annotations.*;
import cloud.commandframework.annotations.specifier.*;
import fr.xpdustry.distributor.api.command.sender.*;
import fr.xpdustry.domination.*;

public final class EditCommands {

  private final DominationPlugin domination;

  public EditCommands(final DominationPlugin domination) {
    this.domination = domination;
  }

  @CommandPermission("fr.xpdustry.domination.zone.edit")
  @CommandMethod("domination zone radius <x> <y> <radius>")
  public void setZoneSize(
    final CommandSender sender,
    final @Argument("x") @Range(min = "0") int x,
    final @Argument("y") @Range(min = "0") int y,
    final @Argument("radius") @Range(min = "1") int radius
  ) {
    domination.getState().getZones()
      .stream()
      .filter(zone -> zone.getX() == x && zone.getY() == y)
      .findFirst()
      .ifPresentOrElse(
        zone -> {
          zone.setRadius(radius);
          domination.getState().save();
          sender.sendMessage("The radius of the zone (%d, %d) has been set to %d".formatted(x, y, radius));
        },
        () -> {
          sender.sendMessage("There is no zone at (%d, %d).".formatted(x, y));
        }
    );
  }

  @CommandPermission("fr.xpdustry.domination.zone.edit")
  @CommandMethod("domination zone add <x> <y>")
  public void addZone(
    final CommandSender sender,
    final @Argument("x") @Range(min = "0") int x,
    final @Argument("y") @Range(min = "0") int y
  ) {
    if (domination.getState().getZones().stream().anyMatch(zone -> zone.getX() == x && zone.getY() == y)) {
      sender.sendMessage("A zone is already present at this location.");
    } else {
      domination.getState().getZones().add(new Zone(x, y, 5));
      domination.getState().save();
      sender.sendMessage("A zone has been added at (%d, %d).".formatted(x, y));
    }
  }

  @CommandPermission("fr.xpdustry.domination.zone.edit")
  @CommandMethod("domination zone remove <x> <y>")
  public void removeZone(
    final CommandSender sender,
    final @Argument("x") @Range(min = "0") int x,
    final @Argument("y") @Range(min = "0") int y
  ) {
    if (domination.getState().getZones().removeIf(zone -> zone.getX() == x && zone.getY() == y)) {
      domination.getState().save();
      sender.sendMessage("The zone at (%d, %d) has been removed.".formatted(x, y));
    } else {
      sender.sendMessage("No zones are present at this location.");
    }
  }
}
