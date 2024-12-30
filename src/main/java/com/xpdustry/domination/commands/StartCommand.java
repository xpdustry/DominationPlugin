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

import arc.Core;
import arc.util.Strings;
import com.xpdustry.distributor.api.command.CommandSender;
import com.xpdustry.domination.DominationPlugin;
import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.net.WorldReloader;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;

public final class StartCommand {

    private final DominationPlugin domination;

    public StartCommand(final DominationPlugin domination) {
        this.domination = domination;
    }

    @Command("domination start <name>")
    @CommandDescription("Start a domination game.")
    public void onDominationStart(final CommandSender sender, final @Argument("name") @Greedy String name) {
        Core.app.post(() -> {
            final var map = Vars.maps.all().find(m -> Strings.stripColors(
                            m.name().replace('_', ' '))
                    .equalsIgnoreCase(name.replace('_', ' ')));
            final var hotLoading = Vars.state.isPlaying();
            final var reloader = new WorldReloader();

            if (map == null) {
                sender.reply(Strings.format("Failed to load '@' map.", name));
                return;
            }

            if (hotLoading) {
                reloader.begin();
            }

            Vars.world.loadMap(map);
            Vars.state.rules = map.applyRules(Gamemode.pvp);
            Vars.state.rules.modeName = "[red]Domination";
            domination.setEnabled(true);

            Vars.logic.play();
            if (hotLoading) {
                reloader.end();
            } else {
                Vars.netServer.openServer();
            }
        });
    }
}
