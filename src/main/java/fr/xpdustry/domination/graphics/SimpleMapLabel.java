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
