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
package com.xpdustry.domination;

import arc.util.CommandHandler;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.xpdustry.distributor.api.Distributor;
import com.xpdustry.distributor.api.command.CommandSender;
import com.xpdustry.distributor.api.command.cloud.MindustryCommandManager;
import com.xpdustry.distributor.api.plugin.AbstractMindustryPlugin;
import com.xpdustry.domination.Zone.Adapter;
import com.xpdustry.domination.commands.EditCommands;
import com.xpdustry.domination.commands.StartCommand;
import com.xpdustry.domination.commands.ZoneListCommand;
import io.leangen.geantyref.TypeToken;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.game.EventType;
import net.mindustry_ddns.filestore.FileStore;
import net.mindustry_ddns.filestore.serial.Serializers;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.meta.CommandMeta;

public final class DominationPlugin extends AbstractMindustryPlugin {

    private static final String DOMINATION_ENABLED_KEY = "xpdustry-domination:enabled";

    // TODO Make the DominationState object itself loadable to allow more options like game duration and stuff...
    private final FileStore<List<Zone>> loader = FileStore.of(
            getDirectory().resolve("maps").resolve("unknown.json").toFile(),
            Serializers.gson(new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                    .registerTypeAdapter(Zone.class, new Adapter())
                    .registerTypeAdapter(Duration.class, new DurationAdapter())
                    .setPrettyPrinting()
                    .create()),
            new TypeToken<>() {},
            new ArrayList<>());

    private @MonotonicNonNull MindustryCommandManager<CommandSender> clientCommands;
    private @MonotonicNonNull MindustryCommandManager<CommandSender> serverCommands;

    private @MonotonicNonNull DominationState state = null;

    @Override
    public void onInit() {
        Distributor.get().getEventBus().subscribe(EventType.PlayEvent.class, this, event -> {
            this.loader.setFile(getDirectory()
                    .resolve("maps")
                    .resolve(Vars.state.map.name() + ".json")
                    .toFile());
            this.loader.set(new ArrayList<>());
            this.loader.load();
            this.state = new DominationState(this.loader);
        });

        this.addListener(new DominationLogic(this));
        this.addListener(new DominationRenderer(this));
    }

    @Override
    public void onServerCommandsRegistration(final CommandHandler handler) {
        serverCommands = new MindustryCommandManager<>(
                this, handler, ExecutionCoordinator.simpleCoordinator(), SenderMapper.identity());
        final var annotations = new AnnotationParser<>(serverCommands, CommandSender.class);
        annotations.parse(new StartCommand(this));
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        clientCommands = new MindustryCommandManager<>(
                this, handler, ExecutionCoordinator.simpleCoordinator(), SenderMapper.identity());
        final var annotations =
                new AnnotationParser<>(clientCommands, CommandSender.class, params -> CommandMeta.empty());
        annotations.parse(new StartCommand(this));
        annotations.parse(new EditCommands(this));
        annotations.parse(new ZoneListCommand(this));
    }

    public boolean isEnabled() {
        return Vars.state.rules.tags.getBool(DOMINATION_ENABLED_KEY);
    }

    public void setEnabled(final boolean enabled) {
        Vars.state.rules.tags.put(DOMINATION_ENABLED_KEY, Boolean.toString(enabled));
    }

    public DominationState getState() {
        return state;
    }

    public MindustryCommandManager<CommandSender> getServerCommandManager() {
        return serverCommands;
    }

    public MindustryCommandManager<CommandSender> getClientCommandManager() {
        return clientCommands;
    }
}
