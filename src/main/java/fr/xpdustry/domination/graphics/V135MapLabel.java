package fr.xpdustry.domination.graphics;

import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import org.jetbrains.annotations.*;

final class V135MapLabel implements MapLabel {
  private final WorldLabel label = WorldLabel.create();

  public V135MapLabel() {
    this.label.z(Layer.flyingUnit);
    this.label.flags((byte) (WorldLabel.flagOutline | WorldLabel.flagBackground));
    this.label.fontSize(2F);
  }

  @Override
  public int getTileX() {
    return label.tileX();
  }

  @Override
  public int getTileY() {
    return label.tileY();
  }

  @Override
  public void setTileX(final int x) {
    label.x(x * Vars.tilesize);
  }

  @Override
  public void setTileY(final int y) {
    label.y(y * Vars.tilesize);
  }

  @Override
  public @NotNull String getText() {
    return label.text();
  }

  @Override
  public void setText(final @NotNull String text) {
    label.text(text);
  }

  @Override
  public void add() {
    label.add();
  }

  @Override
  public void remove() {
    label.remove();
  }

  @Override
  public boolean isAdded() {
    return label.isAdded();
  }
}
