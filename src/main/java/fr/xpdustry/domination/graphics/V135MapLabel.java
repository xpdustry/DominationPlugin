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
