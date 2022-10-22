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
