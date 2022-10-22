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

import arc.util.*;
import arc.util.Timer.*;
import mindustry.*;
import mindustry.gen.*;
import org.jetbrains.annotations.*;

final class SimpleMapLabel implements MapLabel {
  private int x;
  private int y;
  private String text = "sample text";
  private final Timer.Task task = new Task() {
    @Override
    public void run() {
      Call.label(SimpleMapLabel.this.text, 1, getTileX() * Vars.tilesize, getTileY() * Vars.tilesize);
    }
  };

  @Override
  public int getTileX() {
    return x;
  }

  @Override
  public int getTileY() {
    return y;
  }

  @Override
  public void setTileX(final int x) {
    this.x = x;
  }

  @Override
  public void setTileY(final int y) {
    this.y = y;
  }

  @Override
  public @NotNull String getText() {
    return text;
  }

  @Override
  public void setText(final @NotNull String text) {
    this.text = text;
  }

  @Override
  public void add() {
    if(!task.isScheduled()) Timer.schedule(task, 1, 1);
  }

  @Override
  public void remove() {
    if(task.isScheduled()) task.cancel();
  }

  @Override
  public boolean isAdded() {
    return task.isScheduled();
  }
}
