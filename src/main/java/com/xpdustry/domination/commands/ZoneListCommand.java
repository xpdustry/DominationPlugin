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
package com.xpdustry.domination.commands;

import com.xpdustry.distributor.api.command.CommandSender;
import com.xpdustry.domination.DominationPlugin;
import com.xpdustry.domination.Zone;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;

public final class ZoneListCommand {

    private final DominationPlugin domination;

    public ZoneListCommand(final DominationPlugin domination) {
        this.domination = domination;
    }

    @Command("domination zones")
    @CommandDescription("Get the data about the zones.")
    public void listZones(final CommandSender sender) {
        final var builder = new StringBuilder();
        final var captured = domination.getState().getZones().stream()
                .map(Zone::getTeam)
                .filter(sender.getPlayer().team()::equals)
                .count();
        builder.append("[orange]");
        if (captured == domination.getState().getZones().size()) {
            builder.append("Your team is about to capture all the zones :\n");
        } else {
            builder.append("Your team has [red]")
                    .append(domination.getState().getZones().size() - captured)
                    .append("[] more zones to capture :\n");
        }
        final var iterator = domination.getState().getZones().iterator();
        while (iterator.hasNext()) {
            final var zone = iterator.next();
            builder.append("[white]- Zone at (")
                    .append(zone.getX() / 8)
                    .append(", ")
                    .append(zone.getY() / 8)
                    .append(") is captured by ")
                    .append("[#")
                    .append(zone.getTeam().color)
                    .append("]")
                    .append(zone.getTeam().name)
                    .append("[] at ")
                    .append(zone.getCapture())
                    .append("% percent.");
            if (iterator.hasNext()) {
                builder.append('\n');
            }
        }
        sender.reply(builder.toString());
    }
}
