package fr.xpdustry.domination.graphics;

import arc.math.geom.*;
import mindustry.core.*;
import org.jetbrains.annotations.*;

public interface MapLabel {

  static MapLabel create() {
    return Version.isAtLeast("135") ? new V135MapLabel() : new SimpleMapLabel();
  }

  int getTileX();

  int getTileY();

  void setTileX(final int x);

  void setTileY(final int y);

  @NotNull String getText();

  void setText(final @NotNull String text);

  void add();

  void remove();

  boolean isAdded();

  default void setPosition(final @NotNull Position pos) {
    setTileX(World.toTile(pos.getX()));
    setTileY(World.toTile(pos.getY()));
  }
}
