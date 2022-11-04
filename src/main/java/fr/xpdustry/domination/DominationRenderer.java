package fr.xpdustry.domination;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import cloud.commandframework.meta.*;
import fr.xpdustry.distributor.api.*;
import fr.xpdustry.distributor.api.event.*;
import java.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public final class DominationRenderer implements Runnable, EventBusListener {

  private static final Seq<Effect> EFFECTS = Seq.with(Fx.mine, Fx.mineBig, Fx.mineHuge);

  private final DominationPlugin plugin;
  private final Map<Zone, WorldLabel> labels = new HashMap<>();
  private final Set<Player> viewers = new HashSet<>();

  public DominationRenderer(final DominationPlugin plugin) {
    this.plugin = plugin;
    EventBus.mindustry().register(this);
    DistributorProvider.get().getPluginScheduler().syncRepeatingDelayedTask(this.plugin, this, 10, 10);
    plugin.getClientCommandManager().command(plugin.getClientCommandManager()
      .commandBuilder("domination")
      .literal("zone")
      .literal("view")
      .meta(CommandMeta.DESCRIPTION, "Enable/Disable domination zone view mode.")
      .permission("fr.xpdustry.domination.map.zone.view")
      .handler(ctx -> {
        final var player = ctx.getSender().getPlayer();
        if (!this.viewers.add(player)) {
          this.viewers.remove(player);
        }
        ctx.getSender().sendMessage(
          Strings.format("You @ zone viewing.", viewers.contains(player) ? "enabled" : "disabled")
        );
      }));
  }

  @EventHandler
  public void onPlayEvent(final EventType.PlayEvent event) {
    labels.clear();
    if (plugin.isEnabled()) {
      for (final var zone : plugin.getState().getZones()) {
        final var label = WorldLabel.create();
        label.text("???%");
        label.z(Layer.flyingUnit);
        label.flags((byte) (WorldLabel.flagOutline | WorldLabel.flagBackground));
        label.fontSize(2F);
        label.set(zone);
        label.add();
        labels.put(zone, label);
      }
    }
  }

  @Override
  public void run() {
    if (Vars.state.isPlaying()) {
      if (plugin.isEnabled()) {
        // Graphics
        Groups.player.forEach(this::drawZoneCircles);
        labels.forEach((zone, label) -> {
          label.text(Strings.format("[#@]@%", zone.getTeam().color, zone.getCapture()));
        });

        // HUD text
        final var builder = new StringBuilder(100)
          .append("Time remaining > ").append(Strings.formatMillis(plugin.getState().getRemainingTime().toMillis()));

        // Leaderboard
        plugin.getState().getLeaderboard().entrySet()
          .stream()
          .sorted(Comparator.comparingDouble(Map.Entry::getValue))
          .forEach(e -> {
            builder
                .append("\n[#").append(e.getKey().color).append(']')
                .append(e.getKey() == Team.derelict ? "Unclaimed" : Strings.capitalize(e.getKey().name))
                .append("[] > ").append(e.getValue() / plugin.getState().getZones().size()).append('%');
            }
          );

        Call.setHudText(builder.toString());
      } else {
        for (final var viewer : viewers) {
          drawZoneCircles(viewer);
          for (final var zone : this.plugin.getState().getZones()) {
            Call.label(viewer.con(), "[#" + zone.getTeam().color + "]" + Iconc.box, Time.toSeconds / 6, zone.getX(), zone.getY());
          }
        }
      }
    }
  }

  private void drawZoneCircles(final Player player) {
    for (final var zone : this.plugin.getState().getZones()) {
      final var circle = Geometry.regPoly((int) (Mathf.pi * (zone.getRadius() / Vars.tilesize)), zone.getRadius());
      Geometry.iteratePolygon((px, py) -> {
        Call.effect(player.con(), EFFECTS.random(), px + zone.getX(), py + zone.getY(), 0, zone.getTeam().color);
      }, circle);
    }
  }
}
