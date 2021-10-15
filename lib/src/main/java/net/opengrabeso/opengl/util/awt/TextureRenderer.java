/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package net.opengrabeso.opengl.util.awt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.github.opengrabeso.jaagl.GL2GL3;
import com.jogamp.common.nio.Buffers;
import net.opengrabeso.glg2d.impl.shader.AbstractShaderPipeline;
import net.opengrabeso.glg2d.impl.shader.AnyModePipeline;
import net.opengrabeso.opengl.util.texture.*;
import net.opengrabeso.opengl.util.texture.awt.*;

/**
 * Provides the ability to render into an OpenGL {@link
 * com.jogamp.opengl.util.texture.Texture Texture} using the Java 2D
 * APIs. This renderer class uses an internal Java 2D image (of
 * unspecified type) for its backing store and flushes portions of
 * that image to an OpenGL texture on demand. The resulting OpenGL
 * texture can then be mapped on to a polygon for display.
 */

public class TextureRenderer {
    // For now, we supply only a BufferedImage back-end for this
    // renderer. In theory we could use the Java 2D/JOGL bridge to fully
    // accelerate the rendering paths, but there are restrictions on
    // what work can be done where; for example, Graphics2D-related work
    // must not be done on the Queue Flusher Thread, but JOGL's
    // OpenGL-related work must be. This implies that the user's code
    // would need to be split up into multiple callbacks run from the
    // appropriate threads, which would be somewhat unfortunate.

    private final GL2GL3 gl;

    // Whether smoothing is enabled for the OpenGL texture (switching
    // between GL_LINEAR and GL_NEAREST filtering)
    private boolean smoothing = true;
    private boolean smoothingChanged;

    // The backing store itself
    private BufferedImage image;

    private Texture texture;
    private AWTTextureData textureData;
    private boolean mustReallocateTexture;
    private Rectangle dirtyRegion;

    private int program;

    private int transformUniform = -1;
    private int colorUniform = -1;
    private int vertCoordAttrib = -1;
    private int texCoordAttrib = -1;

    private boolean useVAO = false; // avoid using VAO on older OpenGL
    private int vao = -1;
    private boolean vaoSetupDone = false;


    /**
     * Creates a new renderer with backing store of the specified width
     * and height. If <CODE>alpha</CODE> is true, allocates an alpha channel in the
     * backing store image. If <CODE>mipmap</CODE> is true, attempts to use OpenGL's
     * automatic mipmap generation for better smoothing when rendering
     * the TextureRenderer's contents at a distance.
     *
     * @param width  the width of the texture to render into
     * @param height the height of the texture to render into
     * @param alpha  whether to allocate an alpha channel for the texture
     * @param mipmap whether to attempt use of automatic mipmap generation
     */
    public TextureRenderer(final GL2GL3 gl, final int width, final int height, final boolean alpha, final boolean mipmap) {
        this(gl, width, height, alpha, false, mipmap);
    }

    // Internal constructor to avoid confusion since alpha only makes
    // sense when intensity is not set
    private TextureRenderer(final GL2GL3 gl, final int width, final int height, final boolean alpha, final boolean intensity, final boolean mipmap) {
        this.gl = gl;
        init(width, height);
        setup();
    }

    static private String readResource(Class<?> context, String path) {
        InputStream stream = null;
        if (context != null) {
            stream = context.getResourceAsStream(path);
        }

        if (stream == null) {
            stream = TextureRenderer.class.getResourceAsStream(path);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch(IOException ignored) {
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }

        return sb.toString();
    }

    // create shaders and similar OpenGL objects
    private void setup() {
        gl.glGetError(); // flush any pending errors

        String directory = gl.isGL3() ? "gl3/" : "gl2/";

        String vsSource = readResource(AnyModePipeline.class, directory + "TextShader.v");
        String fsSource = readResource(AnyModePipeline.class, directory + "TextShader.f");

        program = ShaderLoader.loadProgram(gl, vsSource, fsSource);

        transformUniform = gl.glGetUniformLocation(program, "MVPMatrix");
        colorUniform = gl.glGetUniformLocation(program, "Color");

        vertCoordAttrib = gl.glGetAttribLocation(program, "MCVertex");
        texCoordAttrib = gl.glGetAttribLocation(program, "TexCoord0");

        // try VAO support
        // it seems when there is no VAO glGenVertexArrays fails gracefully by returning -1,
        // but as it is hard to test this on older implementations, using an additional try should do no harm
        try {
            int[] vao = new int[]{0};
            gl.glGenVertexArrays(vao);
            this.vao = vao[0];
            this.useVAO = this.vao > 0;

        } catch (Exception ex) {
            this.useVAO = false;
        }

        if (false) {
            // crashes on MacOS - VAO not provided
            // validating has no sense - we have no fallback anyway
            ShaderLoader.validateProgram(gl, program);
        }
    }

    private void setupVertexAttributesImpl() {
        gl.glEnableVertexAttribArray(vertCoordAttrib);
        gl.glEnableVertexAttribArray(texCoordAttrib);

        gl.glVertexAttribPointer(vertCoordAttrib, 3, gl.GL_FLOAT(), false, 5 * Buffers.SIZEOF_FLOAT, 0);
        gl.glVertexAttribPointer(texCoordAttrib, 2, gl.GL_FLOAT(), false, 5 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
    }

    private void cleanupVertexAttributesImpl() {
        gl.glDisableVertexAttribArray(vertCoordAttrib);
        gl.glDisableVertexAttribArray(texCoordAttrib);
    }

    public void setupVertexAttributes(float[] transform, float[] color) {
        // TODO: VBA
        if (useVAO) {
            gl.glBindVertexArray(vao);
            if (!vaoSetupDone) {
                setupVertexAttributesImpl();
                vaoSetupDone = true;
            }
        } else {
            setupVertexAttributesImpl();
        }

        gl.glUseProgram(program);
        gl.glUniformMatrix4fv(transformUniform, 1, false, transform, 0);
        gl.glUniform4fv(colorUniform, 1, color, 0);
    }

    public void cleanupVertexAttributes() {
        gl.glUseProgram(0);

        if (useVAO) {
            gl.glBindVertexArray(0);
        } else {
            cleanupVertexAttributesImpl();
        }
    }


    private void cleanup() {
        gl.glDeleteProgram(program);
    }

    /**
     * Creates a new renderer with a special kind of backing store
     * which acts only as an alpha channel. No mipmap support is
     * requested. Internally, this associates a GL_INTENSITY OpenGL
     * texture with the backing store.
     */
    public static TextureRenderer createAlphaOnlyRenderer(final GL2GL3 gl, final int width, final int height) {
        return createAlphaOnlyRenderer(gl, width, height, false);
    }

    /**
     * Creates a new renderer with a special kind of backing store
     * which acts only as an alpha channel. If <CODE>mipmap</CODE> is
     * true, attempts to use OpenGL's automatic mipmap generation for
     * better smoothing when rendering the TextureRenderer's contents
     * at a distance. Internally, this associates a GL_INTENSITY OpenGL
     * texture with the backing store.
     */
    public static TextureRenderer createAlphaOnlyRenderer(final GL2GL3 gl, final int width, final int height, final boolean mipmap) {
        return new TextureRenderer(gl, width, height, false, true, mipmap);
    }

    /**
     * Returns the width of the backing store of this renderer.
     *
     * @return the width of the backing store of this renderer
     */
    public int getWidth() {
        return image.getWidth();
    }

    /**
     * Returns the height of the backing store of this renderer.
     *
     * @return the height of the backing store of this renderer
     */
    public int getHeight() {
        return image.getHeight();
    }

    /**
     * Returns the size of the backing store of this renderer in a
     * newly-allocated {@link Dimension Dimension} object.
     *
     * @return the size of the backing store of this renderer
     */
    public Dimension getSize() {
        return getSize(null);
    }

    /**
     * Returns the size of the backing store of this renderer. Uses the
     * {@link Dimension Dimension} object if one is supplied,
     * or allocates a new one if null is passed.
     *
     * @param d a {@link Dimension Dimension} object in which
     *          to store the results, or null to allocate a new one
     * @return the size of the backing store of this renderer
     */
    public Dimension getSize(Dimension d) {
        if (d == null)
            d = new Dimension();
        d.setSize(image.getWidth(), image.getHeight());
        return d;
    }

    /**
     * Sets the size of the backing store of this renderer. This may
     * cause the OpenGL texture object associated with this renderer to
     * be invalidated; it is not recommended to cache this texture
     * object outside this class but to instead call {@link #getTexture
     * getTexture} when it is needed.
     *
     * @param width  the new width of the backing store of this renderer
     * @param height the new height of the backing store of this renderer
     */
    public void setSize(final int width, final int height) {
        init(width, height);
    }

    /**
     * Sets the size of the backing store of this renderer. This may
     * cause the OpenGL texture object associated with this renderer to
     * be invalidated.
     *
     * @param d the new size of the backing store of this renderer
     */
    public void setSize(final Dimension d) {
        setSize(d.width, d.height);
    }

    /**
     * Sets whether smoothing is enabled for the OpenGL texture; if so,
     * uses GL_LINEAR interpolation for the minification and
     * magnification filters. Defaults to true. Changes to this setting
     * will not take effect until the next call to {@link
     * #beginRendering beginRendering}.
     *
     * @param smoothing whether smoothing is enabled for the OpenGL texture
     */
    public void setSmoothing(final boolean smoothing) {
        this.smoothing = smoothing;
        smoothingChanged = true;
    }

    /**
     * Returns whether smoothing is enabled for the OpenGL texture; see
     * {@link #setSmoothing setSmoothing}. Defaults to true.
     *
     * @return whether smoothing is enabled for the OpenGL texture
     */
    public boolean getSmoothing() {
        return smoothing;
    }

    /**
     * Creates a {@link Graphics2D Graphics2D} instance for
     * rendering to the backing store of this renderer. The returned
     * object should be disposed of using the normal {@link
     * java.awt.Graphics#dispose() Graphics.dispose()} method once it
     * is no longer being used.
     *
     * @return a new {@link Graphics2D Graphics2D} object for
     * rendering into the backing store of this renderer
     */
    public Graphics2D createGraphics() {
        return image.createGraphics();
    }

    /**
     * Returns the underlying Java 2D {@link Image Image}
     * being rendered into.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Marks the given region of the TextureRenderer as dirty. This
     * region, and any previously set dirty regions, will be
     * automatically synchronized with the underlying Texture during
     * the next {@link #getTexture getTexture} operation, at which
     * point the dirty region will be cleared. It is not necessary for
     * an OpenGL context to be current when this method is called.
     *
     * @param x      the x coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param y      the y coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param width  the width of the region to update
     * @param height the height of the region to update
     */
    public void markDirty(final int x, final int y, final int width, final int height) {
        final Rectangle curRegion = new Rectangle(x, y, width, height);
        if (dirtyRegion == null) {
            dirtyRegion = curRegion;
        } else {
            dirtyRegion.add(curRegion);
        }
    }

    /**
     * Returns the underlying OpenGL Texture object associated with
     * this renderer, synchronizing any dirty regions of the
     * TextureRenderer with the underlying OpenGL texture.
     */
    public Texture getTexture() {
        if (dirtyRegion != null) {
            sync(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
            dirtyRegion = null;
        }

        ensureTexture();
        return texture;
    }

    /**
     * Disposes all resources associated with this renderer. It is not
     * valid to use this renderer after calling this method.
     */
    public void dispose() {
        cleanup();
        if (texture != null) {
            texture.destroy(gl);
            texture = null;
        }
        if (image != null) {
            image.flush();
            image = null;
        }
    }

    /**
     * Convenience method which assists in rendering portions of the
     * OpenGL texture to the screen as 2D quads in 3D space.
     */
    public void begin3DRendering() {
        beginRendering();
    }

    /**
     * Convenience method which assists in rendering portions of the
     * OpenGL texture to the screen as 2D quads in 3D space. Must be
     * used if {@link #begin3DRendering} is used to set up the
     * rendering stage for this overlay.
     */
    public void end3DRendering() {
        endRendering();
    }

    /**
     * Indicates whether automatic mipmap generation is in use for this
     * TextureRenderer. The result of this method may change from true
     * to false if it is discovered during allocation of the
     * TextureRenderer's backing store that automatic mipmap generation
     * is not supported at the OpenGL level.
     */
    public boolean isUsingAutoMipmapGeneration() {
        return false;
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    private void beginRendering() {
        gl.glEnable(gl.GL_BLEND());
        gl.glBlendFunc(gl.GL_ONE(), gl.GL_ONE_MINUS_SRC_ALPHA());
        assert(gl.glGetString(gl.GL_VERSION()) != null);
        final Texture texture = getTexture();
        texture.enable(gl);
        texture.bind(gl);
        // Change polygon color to last saved
        if (smoothingChanged) {
            smoothingChanged = false;
            if (smoothing) {
                texture.setTexParameteri(gl, gl.GL_TEXTURE_MAG_FILTER(), gl.GL_LINEAR());
                texture.setTexParameteri(gl, gl.GL_TEXTURE_MIN_FILTER(), gl.GL_LINEAR());
            } else {
                texture.setTexParameteri(gl, gl.GL_TEXTURE_MIN_FILTER(), gl.GL_NEAREST());
                texture.setTexParameteri(gl, gl.GL_TEXTURE_MAG_FILTER(), gl.GL_NEAREST());
            }
        }

    }

    private void endRendering() {

        final Texture texture = getTexture();
        texture.disable(gl);
    }

    private void init(final int width, final int height) {
        // Discard previous BufferedImage if any
        if (image != null) {
            image.flush();
            image = null;
        }

        // Infer the internal format if not an intensity texture
        final int internalFormat = gl.isGL3() ? gl.getGL3().GL_RED() : gl.getGL2().GL_LUMINANCE();
        final int imageType = BufferedImage.TYPE_BYTE_GRAY;
        image = new BufferedImage(width, height, imageType);
        // Always realllocate the TextureData associated with this
        // BufferedImage; it's just a reference to the contents but we
        // need it in order to update sub-regions of the underlying
        // texture
        textureData = new AWTTextureData(gl, internalFormat, image);
        // For now, always reallocate the underlying OpenGL texture when
        // the backing store size changes
        mustReallocateTexture = true;
    }

    /**
     * Synchronizes the specified region of the backing store down to
     * the underlying OpenGL texture. If {@link #markDirty markDirty}
     * is used instead to indicate the regions that are out of sync,
     * this method does not need to be called.
     *
     * @param x      the x coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param y      the y coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param width  the width of the region to update
     * @param height the height of the region to update
     */
    private void sync(final int x, final int y, final int width, final int height) {
        // Force allocation if necessary
        final boolean canSkipUpdate = ensureTexture();

        if (!canSkipUpdate) {
            // Update specified region.
            // NOTE that because BufferedImage-based TextureDatas now don't
            // do anything to their contents, the coordinate systems for
            // OpenGL and Java 2D actually line up correctly for
            // updateSubImage calls, so we don't need to do any argument
            // conversion here (i.e., flipping the Y coordinate).
            texture.updateSubImage(gl, textureData, 0, x, y, x, y, width, height);
        }
    }

    // Returns true if the texture was newly allocated, false if not
    private boolean ensureTexture() {
        if (mustReallocateTexture) {
            if (texture != null) {
                texture.destroy(gl);
                texture = null;
            }
            mustReallocateTexture = false;
        }

        if (texture == null) {
            texture = new Texture(gl, textureData);

            if (!smoothing) {
                // The TextureIO classes default to GL_LINEAR filtering
                texture.setTexParameteri(gl, gl.GL_TEXTURE_MIN_FILTER(), gl.GL_NEAREST());
                texture.setTexParameteri(gl, gl.GL_TEXTURE_MAG_FILTER(), gl.GL_NEAREST());
            }
            return true;
        }

        return false;
    }
}
