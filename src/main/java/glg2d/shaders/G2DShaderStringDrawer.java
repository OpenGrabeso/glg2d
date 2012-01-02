/**************************************************************************
   Copyright 2011 Brandon Borkholder

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 ***************************************************************************/

package glg2d.shaders;

import glg2d.G2DGLStringDrawer;
import glg2d.GLGraphics2D;

import java.awt.Color;

import javax.media.opengl.GL;

import com.sun.opengl.util.j2d.TextRenderer;

public class G2DShaderStringDrawer extends G2DGLStringDrawer {
  protected Shader shader;

  public G2DShaderStringDrawer(Shader shader) {
    this.shader = shader;
  }

  @Override
  public void setG2D(GLGraphics2D g2d) {
    super.setG2D(g2d);

    GL gl = g2d.getGLContext().getGL();
    if (!shader.isProgram(gl)) {
      shader.setup(gl);
    }
  }

  @Override
  protected void begin(TextRenderer renderer, Color textColor) {
    super.begin(renderer, textColor);
    shader.use(true);
  }

  @Override
  protected void end(TextRenderer renderer) {
    shader.use(false);
    super.end(renderer);
  }
}