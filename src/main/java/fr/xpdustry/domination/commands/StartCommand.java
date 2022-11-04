package fr.xpdustry.domination.commands;

import arc.*;
import arc.util.*;
import cloud.commandframework.annotations.*;
import cloud.commandframework.annotations.specifier.*;
import fr.xpdustry.distributor.api.command.sender.*;
import fr.xpdustry.domination.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.net.*;

public class StartCommand {

  private final DominationPlugin domination;

  public StartCommand(final DominationPlugin domination) {
    this.domination = domination;
  }

  @CommandMethod("domination start <name>")
  @CommandDescription("Start a domination game.")
  public void onDominationStart(
    final CommandSender sender,
    final @Argument("name") @Greedy String name
  ) {
    Core.app.post(() -> {
      final var map = Vars.maps.all().find(m -> Strings.stripColors(m.name().replace('_', ' ')).equalsIgnoreCase(name.replace('_', ' ')));
      final var hotLoading = Vars.state.isPlaying();
      final var reloader = new WorldReloader();

      if (map == null) {
        sender.sendMessage(Strings.format("Failed to load '@' map.", name));
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
