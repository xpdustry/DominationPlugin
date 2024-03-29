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

import arc.util.*;
import com.google.gson.*;
import fr.xpdustry.distributor.api.command.*;
import fr.xpdustry.distributor.api.command.sender.*;
import fr.xpdustry.distributor.api.plugin.*;
import fr.xpdustry.distributor.api.util.*;
import fr.xpdustry.domination.Zone.*;
import fr.xpdustry.domination.commands.*;
import io.leangen.geantyref.*;
import java.time.*;
import java.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import net.mindustry_ddns.filestore.*;
import net.mindustry_ddns.filestore.serial.*;
import org.checkerframework.checker.nullness.qual.*;

public final class DominationPlugin extends ExtendedPlugin {

  public static final String DOMINATION_RULES = """
    Welcome to [cyan]Domination PVP[].
    The rules are simple. [red]Your team must capture all the zones.[]
    To do so, a team sends their units in a zone until it reaches 100%.
    You can obtain a list of the zones and their status with the command [orange]/domination zones[].
    To see this message again, do [orange]/domination rules[].
    """;
  private static final String DOMINATION_ENABLED_KEY = "xpdustry-domination:enabled";

  // TODO Make the DominationState object itself loadable to allow more options like game duration and stuff...
  private final FileStore<List<Zone>> loader = FileStore.of(
    getDirectory().resolve("maps").resolve("unknown.json").toFile(),
    Serializers.gson(
      new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
        .registerTypeAdapter(Zone.class, new Adapter())
        .registerTypeAdapter(Duration.class, new DurationAdapter())
        .setPrettyPrinting()
        .create()
    ),
    new TypeToken<>() {},
    new ArrayList<>()
  );

  private final ArcCommandManager<CommandSender> clientCommands = ArcCommandManager.standard(this);
  private final ArcCommandManager<CommandSender> serverCommands = ArcCommandManager.standard(this);

  private @MonotonicNonNull DominationState state = null;

  @Override
  public void onInit() {
    MoreEvents.subscribe(EventType.PlayEvent.class, event -> {
      loader.setFile(getDirectory().resolve("maps").resolve(Vars.state.map.name() + ".json").toFile());
      loader.set(new ArrayList<>());
      loader.load();
      state = new DominationState(loader);
    });

    MoreEvents.subscribe(EventType.PlayerJoin.class, event -> {
      if (isEnabled()) {
        Call.infoMessage(event.player.con(), DOMINATION_RULES);
      }
    });

    addListener(new DominationLogic(this));
    addListener(new DominationRenderer(this));
  }

  @Override
  public void onServerCommandsRegistration(final CommandHandler handler) {
    serverCommands.initialize(handler);
    final var annotations = serverCommands.createAnnotationParser(CommandSender.class);
    annotations.parse(new StartCommand(this));
  }

  @Override
  public void onClientCommandsRegistration(final CommandHandler handler) {
    clientCommands.initialize(handler);
    final var annotations = clientCommands.createAnnotationParser(CommandSender.class);
    annotations.parse(new StartCommand(this));
    annotations.parse(new EditCommands(this));
    annotations.parse(new StandardCommands(this));
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

  public ArcCommandManager<CommandSender> getServerCommandManager() {
    return serverCommands;
  }

  public ArcCommandManager<CommandSender> getClientCommandManager() {
    return clientCommands;
  }
}
