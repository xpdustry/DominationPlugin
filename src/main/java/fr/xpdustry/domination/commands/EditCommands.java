/*
 * Domination, a "capture the zone" like gamemode plugin.
 *
 * Copyright (C) 2024  Xpdustry
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

import com.xpdustry.distributor.api.command.CommandSender;
import fr.xpdustry.domination.DominationPlugin;
import fr.xpdustry.domination.Zone;
import mindustry.Vars;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;

public final class EditCommands {

    private final DominationPlugin domination;

    public EditCommands(final DominationPlugin domination) {
        this.domination = domination;
    }

    @Command("domination zone radius <x> <y> <radius>")
    @Permission("com.xpdustry.domination.zone.edit")
    public void setZoneSize(
            final CommandSender sender,
            final @Argument("x") @Range(min = "0") int x,
            final @Argument("y") @Range(min = "0") int y,
            final @Argument("radius") @Range(min = "1") int radius,
            final @Flag(value = "precise", aliases = "p") boolean precise) {
        final int tx = precise ? x : x * Vars.tilesize;
        final int ty = precise ? y : y * Vars.tilesize;
        domination.getState().getZones().stream()
                .filter(zone -> zone.getX() == tx && zone.getY() == ty)
                .findFirst()
                .ifPresentOrElse(
                        zone -> {
                            zone.setRadius(radius);
                            domination.getState().save();
                            sender.reply(
                                    "The radius of the zone (%d, %d) has been set to %d".formatted(tx, ty, radius));
                        },
                        () -> {
                            sender.reply("There is no zone at (%d, %d).".formatted(tx, ty));
                        });
    }

    @Command("domination zone add <x> <y>")
    @Permission("com.xpdustry.domination.zone.edit")
    public void addZone(
            final CommandSender sender,
            final @Argument("x") @Range(min = "0") int x,
            final @Argument("y") @Range(min = "0") int y,
            final @Flag(value = "precise", aliases = "p") boolean precise) {
        final int tx = precise ? x : x * Vars.tilesize;
        final int ty = precise ? y : y * Vars.tilesize;
        if (domination.getState().getZones().stream().anyMatch(zone -> zone.getX() == tx && zone.getY() == ty)) {
            sender.reply("A zone is already present at this location.");
        } else {
            domination.getState().getZones().add(new Zone(tx, ty, 5));
            domination.getState().save();
            sender.reply("A zone has been added at (%d, %d).".formatted(tx, ty));
        }
    }

    @Command("domination zone remove <x> <y>")
    @Permission("com.xpdustry.domination.zone.edit")
    public void removeZone(
            final CommandSender sender,
            final @Argument("x") @Range(min = "0") int x,
            final @Argument("y") @Range(min = "0") int y,
            final @Flag(value = "precise", aliases = "p") boolean precise) {
        final int tx = precise ? x : x * Vars.tilesize;
        final int ty = precise ? y : y * Vars.tilesize;
        if (domination.getState().getZones().removeIf(zone -> zone.getX() == tx && zone.getY() == ty)) {
            domination.getState().save();
            sender.reply("The zone at (%d, %d) has been removed.".formatted(tx, ty));
        } else {
            sender.reply("No zones are present at this location.");
        }
    }
}
