/*
 * Copyright 2015 Brandon Borkholder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.opengrabeso.glg2d.impl.gl2;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;

import net.opengrabeso.glg2d.impl.AbstractTextDrawer;
import net.opengrabeso.ogltext.util.awt.TextRenderer;

/**
 * Draws text for the {@code GLGraphics2D} class.
 */
public abstract class GL2StringDrawer extends AbstractTextDrawer {
  protected FontRenderCache cache = new FontRenderCache();

  protected abstract TextRenderer createTextRenderer(Font font, boolean antialias);

  @Override
  public void dispose() {
    cache.dispose();
  }

  @Override
  public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    drawString(iterator, (int) x, (int) y);
  }

  @Override
  public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    StringBuilder builder = new StringBuilder(iterator.getEndIndex() - iterator.getBeginIndex());
    while (iterator.next() != AttributedCharacterIterator.DONE) {
      builder.append(iterator.current());
    }

    drawString(builder.toString(), x, y);
  }

  @Override
  public void drawString(String string, float x, float y) {
    drawString(string, (int) x, (int) y);
  }

  @Override
  public void drawString(String string, int x, int y) {
    TextRenderer renderer = getRenderer(getFont());

    begin(renderer);
    if (useVerticalFlip()) {
        renderer.draw3D(string, x, y, 0, 1, true);
    } else {
        renderer.draw3D(string, x, g2d.getCanvasHeight() - y, 0, 1, false);
    }
    end(renderer);
  }

  protected TextRenderer getRenderer(Font font) {
    return cache.getRenderer(font, stack.peek().antiAlias);
  }

  /**
   * Sets the font color, respecting the AlphaComposite if it wants to
   * pre-multiply an alpha.
   */
  protected void setTextColorRespectComposite(TextRenderer renderer) {
    Color color = g2d.getColor();
    if (g2d.getComposite() instanceof AlphaComposite) {
      float alpha = ((AlphaComposite) g2d.getComposite()).getAlpha();
      if (alpha < 1) {
        float[] rgba = color.getRGBComponents(null);
        color = new Color(rgba[0], rgba[1], rgba[2], alpha * rgba[3]);
      }
    }

    renderer.setColor(color);
  }

  protected abstract float[] getTransform(TextRenderer renderer);

  protected abstract boolean useVerticalFlip();

  protected abstract void cleanupMatrix(TextRenderer renderer);

  protected void begin(TextRenderer renderer) {
    setTextColorRespectComposite(renderer);
      float[] transform = getTransform(renderer);
      renderer.setTransform(g2d.getGL(), transform);
    renderer.begin3DRendering();
  }

  protected void end(TextRenderer renderer) {
    renderer.end3DRendering();
    cleanupMatrix(renderer);
  }

  @SuppressWarnings("serial")
  public class FontRenderCache extends HashMap<Font, TextRenderer[]> {
    public TextRenderer getRenderer(Font font, boolean antiAlias) {
      TextRenderer[] renderers = get(font);
      if (renderers == null) {
        renderers = new TextRenderer[2];
        put(font, renderers);
      }

      TextRenderer renderer = renderers[antiAlias ? 1 : 0];

      if (renderer == null) {
        renderer = createTextRenderer(font, antiAlias);
        renderers[antiAlias ? 1 : 0] = renderer;
      }

      return renderer;
    }

    public void dispose() {
      for (TextRenderer[] value : values()) {
        if (value[0] != null) {
          value[0].dispose();
        }
        if (value[1] != null) {
          value[1].dispose();
        }
      }
    }
  }
}
