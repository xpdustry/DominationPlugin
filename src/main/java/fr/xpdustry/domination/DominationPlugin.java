package fr.xpdustry.domination;

import arc.*;
import arc.util.*;
import cloud.commandframework.arguments.standard.*;
import cloud.commandframework.arguments.standard.StringArgument.*;
import cloud.commandframework.meta.*;
import com.google.gson.*;
import fr.xpdustry.distributor.api.command.*;
import fr.xpdustry.distributor.api.command.sender.*;
import fr.xpdustry.distributor.api.plugin.*;
import java.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.*;
import org.checkerframework.checker.nullness.qual.*;

public final class DominationPlugin extends ExtendedPlugin {

  static final String DOMINATION_ACTIVE_KEY = "xpdustry-domination:active";

  private static final Gson gson = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(DominationZone.class, new DominationZone.Adapter())
    .create();

  private final Set<Player> editors = new HashSet<>();
  private final ArcCommandManager<CommandSender> clientCommands = ArcCommandManager.standard(this);
  private final ArcCommandManager<CommandSender> serverCommands = ArcCommandManager.standard(this);

  @Override
  public void onInit() {
    Core.app.addListener(new DominationLogic(this));
  }

  @Override
  public void onServerCommandsRegistration(CommandHandler handler) {
    serverCommands.initialize(handler);
    onSharedCommandsRegistration(serverCommands);
  }

  @Override
  public void onClientCommandsRegistration(final CommandHandler handler) {
    clientCommands.initialize(handler);
    onSharedCommandsRegistration(clientCommands);
    clientCommands.command(clientCommands.commandBuilder("domination").literal("edit")
      .meta(CommandMeta.DESCRIPTION, "Enable/Disable domination edit mode.")
      .permission("fr.xpdustry.domination.edit")
      .handler(ctx -> {
        final var player = ctx.getSender().getPlayer();
        if (!editors.add(player)) editors.remove(player);
        ctx.getSender().sendMessage(
          Strings.format("You @ the editor mode of domination.", editors.contains(player) ? "enabled" : "disabled")
        );
      })
    );
  }

  public void onSharedCommandsRegistration(final @NonNull ArcCommandManager<CommandSender> manager) {
    manager.command(
      manager
        .commandBuilder("domination")
        .literal("start")
        .meta(CommandMeta.DESCRIPTION, "Start a domination game.")
        .permission("fr.xpdustry.domination.start")
        .argument(StringArgument.of("map", StringMode.GREEDY))
        .handler(ctx -> {
          Core.app.post(() -> {
            final var map = Vars.maps.all()
              .find(m -> Strings.stripColors(m.name().replace('_', ' '))
                .equalsIgnoreCase(Strings.stripColors(ctx.get("map")).replace('_', ' ')));
            final var hotLoading = Vars.state.isPlaying();
            final var reloader = new WorldReloader();

            if (map == null) {
              ctx.getSender().sendMessage(Strings.format("Failed to load '@' map.", ctx.<String>get("map")));
              return;
            }

            if (hotLoading) {
              reloader.begin();
            }

            Vars.world.loadMap(map);
            Vars.state.rules = map.applyRules(Gamemode.pvp);
            Vars.state.rules.modeName = "[red]Domination";
            Vars.state.rules.tags.put(DOMINATION_ACTIVE_KEY, "true");

            Vars.logic.play();
            if (hotLoading) {
              reloader.end();
            } else {
              Vars.netServer.openServer();
            }
          });
        })
      );
  }
}
