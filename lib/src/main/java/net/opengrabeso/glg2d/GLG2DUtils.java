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
package net.opengrabeso.glg2d;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.opengrabeso.jaagl.GL;
import com.github.opengrabeso.jaagl.GL2GL3;

public class GLG2DUtils {
    private static final Logger LOGGER = Logger.getLogger(GLG2DUtils.class.getName());

    public static int getViewportHeight(GL gl) {
        int[] viewportDimensions = new int[4];
        gl.glGetIntegerv(gl.GL_VIEWPORT(), viewportDimensions);
        return viewportDimensions[3];
    }

    public static int getViewportWidth(GL gl) {
        int[] viewportDimensions = new int[4];
        gl.glGetIntegerv(gl.GL_VIEWPORT(), viewportDimensions);
        return viewportDimensions[2];
    }

    public static void logGLError(GL gl) {
        int error = gl.glGetError();
        if (error != gl.GL_NO_ERROR()) {
            LOGGER.log(Level.SEVERE, "GL Error: code " + error);
        }
    }

    public static int ensureIsGLBuffer(GL2GL3 gl, int bufferId) {
        if (gl.glIsBuffer(bufferId)) {
            return bufferId;
        } else {
            return genBufferId(gl);
        }
    }

    public static int genBufferId(GL2GL3 gl) {
        int[] ids = new int[1];
        gl.glGenBuffers(ids);
        return ids[0];
    }

}
