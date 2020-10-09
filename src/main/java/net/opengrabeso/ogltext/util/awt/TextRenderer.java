/*
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package net.opengrabeso.ogltext.util.awt;

import com.github.opengrabeso.jaagl.GL2;
import com.github.opengrabeso.jaagl.GL2GL3;
import net.opengrabeso.ogltext.util.awt.text.*;
import net.opengrabeso.opengl.util.texture.TextureCoords;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Utility for rendering bitmapped Java 2D text into an OpenGL window.
 *
 * <p>
 * {@code TextRenderer} has high performance, full Unicode support, and a simple API.  It performs
 * appropriate caching of text rendering results in an OpenGL texture internally to avoid repeated
 * font rasterization.  The caching is completely automatic, does not require any user
 * intervention, and has no visible controls in the public API.
 *
 * <p>
 * Using {@code TextRenderer} is simple.  Add a {@code TextRenderer} field to your {@code
 * GLEventListener} and in your {@code init} method, add:
 *
 * <pre>
 * renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
 * </pre>
 *
 * <p>
 * In the {@code display} method of your {@code GLEventListener}, add:
 *
 * <pre>
 * renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
 * // optionally set the color
 * renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
 * renderer.draw("Text to draw", xPosition, yPosition);
 * // ... more draw commands, color changes, etc.
 * renderer.endRendering();
 * </pre>
 *
 * <p>
 * Unless you are sharing textures between OpenGL contexts, you do not need to call the {@link
 * #dispose dispose} method of the {@code TextRenderer}; the OpenGL resources it uses internally
 * will be cleaned up automatically when the OpenGL context is destroyed.
 *
 * <p>
 * Note that a {@code TextRenderer} will cause the Vertex Array Object binding to change, or to be
 * unbound.
 *
 * <p>
 * Internally, the renderer uses a rectangle packing algorithm to pack both glyphs and full
 * strings' rendering results (which are variable size) onto a larger OpenGL texture.
 * A least recently used (LRU) algorithm is used to discard previously rendered
 * strings; the specific algorithm is undefined, but is currently implemented by flushing unused
 * strings' rendering results every few hundred rendering cycles, where a rendering cycle is
 * defined as a pair of calls to {@link #beginRendering beginRendering} / {@link #endRendering
 * endRendering}.
 *
 * @author John Burkey
 * @author Kenneth Russell
 */
/*@NotThreadSafe*/
public final class TextRenderer {

    /**
     * Common instance of the default render delegate.
     */
    /*@Nonnull*/
    private static final RenderDelegate DEFAULT_RENDER_DELEGATE = new DefaultRenderDelegate();

    /**
     * Face, style, and size of text to render with.
     */
    /*@Nonnull*/
    private final Font font;

    /**
     * Delegate to store glyphs.
     */
    /*@Nonnull*/
    private final GlyphCache glyphCache;

    /**
     * Delegate to create glyphs.
     */
    /*@Nonnull*/
    private final GlyphProducer glyphProducer;

    /**
     * Delegate to draw glyphs.
     */
    /*@Nonnull*/
    private final GlyphRenderer glyphRenderer = new GlyphRendererProxy();

    /**
     * Mediator coordinating components.
     */
    /*@Nonnull*/
    private final Mediator mediator = new Mediator();

    /**
     * GL interface
     * */
    private GL2GL3 gl;

    private boolean gl3;

    /**
     * True if this text renderer is ready to be used.
     */
    private boolean ready = false;


    /**
     * Constructs a {@link TextRenderer}.
     *
     * <p>
     * The resulting {@code TextRenderer} will use no antialiasing or fractional metrics and the
     * default render delegate.  It will not attempt to use OpenGL's automatic mipmap generation
     * for better scaling.  All Unicode characters will be available.
     *
     * @param font Font to render text with
     */
    public TextRenderer(/*@Nonnull*/ final Font font, GL2 gl) {
        this(font, false, false, null, false, null, gl, false);
    }

    public TextRenderer(/*@Nonnull*/ final Font font, GL2GL3 gl) {
        this(font, false, false, null, false, null, gl, true);
    }

    public TextRenderer(/*@Nonnull*/ final Font font, final boolean antialias, final boolean subpixel, GL2 gl) {
        this(font, antialias, subpixel, null, false, null, gl, false);
    }

    public TextRenderer(/*@Nonnull*/ final Font font, final boolean antialias, final boolean subpixel, GL2GL3 gl) {
        this(font, antialias, subpixel, null, false, null, gl, true);
    }

    /**
     * Constructs a {@link TextRenderer} with optional text properties, a render delegate,
     * mipmapping, and a range of characters.
     *
     * <p>
     * The resulting {@code TextRenderer} will use antialiasing and fractional metrics if
     * requested.  The optional render delegate provides more control over the text rendered.  If
     * mipmapping is requested, the text renderer will attempt to use OpenGL's automatic mipmap
     * generation for better scaling.  If a character range is specified, the text renderer will
     * limit itself to those characters to try to achieve better performance.  Otherwise all
     * Unicode characters will be available.
     *
     * @param font Font to render text with
     * @param antialias True to smooth edges of text
     * @param subpixel True to use subpixel accuracy
     * @param rd Controller of rendering details, or null to use the default
     * @param mipmap Whether to generate mipmaps to make the text scale better
     * @param ub Range of unicode characters, or null to use the default
     */
    public TextRenderer(/*@Nonnull*/ final Font font,
                        final boolean antialias,
                        final boolean subpixel,
                        /*@CheckForNull*/ RenderDelegate rd,
                        final boolean mipmap,
                        /*@CheckForNull*/ final UnicodeBlock ub,
                        final GL2GL3 gl, boolean gl3) {

        Check.notNull(font, "Font cannot be null");
        if (rd == null) {
            rd = DEFAULT_RENDER_DELEGATE;
        }

        this.gl = gl;
        this.gl3 = gl3;
        this.font = font;
        this.glyphCache = GlyphCache.newInstance(font, rd, antialias, subpixel, mipmap, gl3);
        this.glyphProducer = GlyphProducers.get(font, rd, glyphCache.getFontRenderContext(), ub);
    }

    /**
     * Starts a 3D render cycle.
     *
     * <p>
     * Assumes the end user is responsible for setting up the modelview and projection matrices,
     * and will render text using the {@link #draw3D} method.
     */
    public void begin3DRendering() {
        beginRendering(false, 0, 0, false);
    }

    /**
     * Starts an orthographic render cycle.
     *
     * <p>
     * Sets up a two-dimensional orthographic projection with (0,0) as the lower-left coordinate
     * and (width, height) as the upper-right coordinate.  Binds and enables the internal OpenGL
     * texture object, sets the texture environment mode to GL_MODULATE, and changes the current
     * color to the last color set with this text drawer via {@link #setColor}.
     *
     * <p>
     * This method disables the depth test and is equivalent to beginRendering(width, height,
     * true).
     *
     * @param width Width of the current on-screen OpenGL drawable
     * @param height Height of the current on-screen OpenGL drawable
     */
    public void beginRendering(/*@Nonnegative*/ final int width,
                               /*@Nonnegative*/ final int height) {
        beginRendering(true, width, height, true);
    }

    /**
     * Starts an orthographic render cycle.
     *
     * <p>
     * Sets up a two-dimensional orthographic projection with (0,0) as the lower-left coordinate
     * and (width, height) as the upper-right coordinate.  Binds and enables the internal OpenGL
     * texture object, sets the texture environment mode to GL_MODULATE, and changes the current
     * color to the last color set with this text drawer via {@link #setColor}.
     *
     * <p>
     * Disables the depth test if requested.
     *
     * @param width Width of the current on-screen OpenGL drawable
     * @param height Height of the current on-screen OpenGL drawable
     * @param disableDepthTest True to disable the depth test
     */
    public void beginRendering(/*@Nonnegative*/ final int width,
                               /*@Nonnegative*/ final int height,
                               final boolean disableDepthTest) {
        beginRendering(true, width, height, disableDepthTest);
    }

    /**
     * Starts a render cycle.
     *
     * @param ortho True to use orthographic projection
     * @param width Width of the current OpenGL viewport
     * @param height Height of the current OpenGL viewport
     * @param disableDepthTest True to ignore depth values
     */
    private void beginRendering(final boolean ortho,
                                /*@Nonnegative*/ final int width,
                                /*@Nonnegative*/ final int height,
                                final boolean disableDepthTest) {

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        // Make sure components are set up properly
        if (!ready) {
            glyphCache.addListener(mediator);
            glyphRenderer.addListener(mediator);
            ready = true;
        }

        // Delegate to components
        glyphCache.beginRendering(gl);
        glyphRenderer.beginRendering(gl, ortho, width, height, disableDepthTest, gl3);
    }

    /**
     * Destroys resources used by the text renderer.
     */
    public void dispose() {

        // Destroy the glyph cache
        glyphCache.dispose(gl);

        // Destroy the glyph renderer
        glyphRenderer.dispose(gl);
    }

    /**
     * Draws a character sequence at a location.
     *
     * <p>
     * The baseline of the leftmost character is at position (x, y) specified in OpenGL
     * coordinates, where the origin is at the lower-left of the drawable and the Y coordinate
     * increases in the upward direction.
     *
     * @param text Text to draw
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     */
    public void draw(/*@Nonnull*/ final CharSequence text,
                     /*@CheckForSigned*/ final int x,
                     /*@CheckForSigned*/ final int y) {
        draw3D(text, x, y, 0, 1, false);
    }

    /**
     * Draws a string at a location.
     *
     * <p>
     * The baseline of the leftmost character is at position (x, y) specified in OpenGL
     * coordinates, where the origin is at the lower-left of the drawable and the Y coordinate
     * increases in the upward direction.
     *
     * @param text Text to draw
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     */
    public void draw(/*@Nonnull*/ final String text,
                     /*@CheckForSigned*/ final int x,
                     /*@CheckForSigned*/ final int y) {
        draw3D(text, x, y, 0, 1, false);
    }

    /**
     * Draws a character sequence at a location in 3D space.
     *
     * <p>
     * The baseline of the leftmost character is placed at position (x, y, z) in the current
     * coordinate system.
     *
     * @param text Text to draw
     * @param x X coordinate at which to draw
     * @param y Y coordinate at which to draw
     * @param z Z coordinate at which to draw
     * @param scale Uniform scale applied to width and height of text
     */
    public void draw3D(/*@Nonnull*/ final CharSequence text,
                       /*@CheckForSigned*/ final float x,
                       /*@CheckForSigned*/ final float y,
                       /*@CheckForSigned*/ final float z,
                       /*@CheckForSigned*/ final float scale,
                                    final boolean verticalFlip) {
        draw3D(text.toString(), x, y, z, scale, verticalFlip);
    }

    /**
     * Draws text at a location in 3D space.
     *
     * <p>
     * Uses the renderer's current color.  The baseline of the leftmost character is placed at
     * position (x, y, z) in the current coordinate system.
     *
     * @param text Text to draw
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     * @param z Position to draw on Z axis
     * @param scale Uniform scale applied to width and height of text
     */
    public void draw3D(/*@Nonnull*/ final String text,
                       /*@CheckForSigned*/ float x,
                       /*@CheckForSigned*/ final float y,
                       /*@CheckForSigned*/ final float z,
                       /*@CheckForSigned*/ final float scale,
                                    final boolean verticalFlip
                                    ) {

        Check.notNull(text, "Text cannot be null");

        // Get all the glyphs for the string
        final List<Glyph> glyphs = glyphProducer.createGlyphs(text);

        // Render each glyph
        for (final Glyph glyph : glyphs) {
            if (glyph.location == null) {
                glyphCache.upload(glyph);
            }
            final TextureCoords coords = glyphCache.find(glyph);
            final float advance = glyphRenderer.drawGlyph(gl, glyph, x, y, z, scale, verticalFlip, coords);
            x += advance * scale;
        }
    }

    /**
     * Finishes a 3D render cycle.
     */
    public void end3DRendering() {
        endRendering();
    }

    /**
     * Finishes a render cycle.
     */
    public void endRendering() {

        // Tear down components
        glyphCache.endRendering(gl);
        glyphRenderer.endRendering(gl);
    }

    /**
     * Forces all stored text to be rendered.
     *
     * <p>
     * This should be called after each call to {@code draw} if you are setting OpenGL state such
     * as the modelview matrix between calls to {@code draw}.
     */
    public void flush() {

        // Make sure glyph cache is up to date
        glyphCache.update(gl);

        // Render outstanding glyphs
        glyphRenderer.flush(gl);
    }

    /**
     * Determines the bounding box of a character sequence.
     *
     * <p>
     * Assumes it was rendered at the origin.
     *
     * <p>
     * The coordinate system of the returned rectangle is Java 2D's, with increasing Y coordinates
     * in the downward direction.  The relative coordinate (0,0) in the returned rectangle
     * corresponds to the baseline of the leftmost character of the rendered string, in similar
     * fashion to the results returned by, for example, {@link GlyphVector#getVisualBounds
     * getVisualBounds}.
     *
     * <p>
     * Most applications will use only the width and height of the returned Rectangle for the
     * purposes of centering or justifying the String.  It is not specified which Java 2D bounds
     * ({@link GlyphVector#getVisualBounds getVisualBounds}, {@link GlyphVector#getPixelBounds
     * getPixelBounds}, etc.) the returned bounds correspond to, although every effort is made to
     * ensure an accurate bound.
     *
     * @param text Text to get bounding box for
     * @return Rectangle surrounding the given text, not null
     */
    /*@Nonnull*/
    public Rectangle2D getBounds(/*@Nonnull*/ final CharSequence text) {
        Check.notNull(text, "Text cannot be null");
        return getBounds(text.toString());
    }

    /**
     * Determines the bounding box of a string.
     *
     * @param text Text to get bounding box for
     * @return Rectangle surrounding the given text, not null
     */
    /*@Nonnull*/
    public Rectangle2D getBounds(/*@Nonnull*/ final String text) {
        Check.notNull(text, "Text cannot be null");
        return glyphProducer.findBounds(text);
    }

    /**
     * Determines the pixel width of a character.
     *
     * @param c Character to get pixel width of
     * @return Number of pixels required to advance past the character
     */
    public float getCharWidth(final char c) {
        return glyphProducer.findAdvance(c);
    }

    /**
     * Determines the font this {@link TextRenderer} is using.
     *
     * @return Font used by this text renderer, not null
     */
    /*@Nonnull*/
    public Font getFont() {
        return font;
    }

    /**
     * Checks if the backing texture is using linear interpolation.
     *
     * @return True if the backing texture is using linear interpolation.
     */
    public boolean getSmoothing() {
        return glyphCache.getUseSmoothing();
    }

    /**
     * Checks if vertex arrays are in-use.
     *
     * <p>
     * Indicates whether vertex arrays are being used internally for rendering, or whether text is
     * rendered using the OpenGL immediate mode commands.  Defaults to true.
     */
    public boolean getUseVertexArrays() {
        return glyphRenderer.getUseVertexArrays();
    }

    /**
     * Specifies the current color of this {@link TextRenderer} using a {@link Color}.
     *
     * @param color Color to use for rendering text
     */
    public void setColor(/*@Nonnull*/ final Color color) {

        Check.notNull(color, "Color cannot be null");

        final float r = ((float) color.getRed()) / 255f;
        final float g = ((float) color.getGreen()) / 255f;
        final float b = ((float) color.getBlue()) / 255f;
        final float a = ((float) color.getAlpha()) / 255f;
        setColor(r, g, b, a);
    }

    /**
     * Specifies the current color of this {@link TextRenderer} using individual components.
     *
     * <p>
     * Each component ranges from 0.0f to 1.0f.  The alpha component, if used, does not need to be
     * premultiplied into the color channels as described in the documentation for {@link
     * com.jogamp.opengl.util.texture.Texture Texture} (although premultiplied colors are used
     * internally).  The default color is opaque white.
     *
     * @param r Red component of the new color
     * @param g Green component of the new color
     * @param b Blue component of the new color
     * @param a Alpha component of the new color
     */
    public void setColor(/*@CheckForSigned*/ final float r,
                         /*@CheckForSigned*/ final float g,
                         /*@CheckForSigned*/ final float b,
                         /*@CheckForSigned*/ final float a) {
        glyphRenderer.setColor(gl, r, g, b, a);
    }

    /**
     * Specifies whether the backing texture will use linear interpolation.
     *
     * <p>
     * If smoothing is enabled, {@code GL_LINEAR} will be used.  Otherwise it uses {@code
     * GL_NEAREST}.
     *
     * <p>
     * Defaults to true.
     *
     * <p>
     * A few graphics cards do not behave well when this is enabled, resulting in fuzzy text.
     */
    public void setSmoothing(final boolean smoothing) {
        glyphCache.setUseSmoothing(smoothing);
    }

    /**
     * Changes the transformation matrix used for drawing text in 3D.
     *
     * @param matrix Transformation matrix in column-major order
     */
    public void setTransform(final GL2GL3 gl, /*@Nonnull*/ final float matrix[]) {
        Check.notNull(matrix, "Matrix cannot be null");
        glyphRenderer.setTransform(gl, matrix, false);
    }

    /**
     * Changes whether vertex arrays are in use.
     *
     * <p>
     * This is provided as a concession for certain graphics cards which have poor vertex array
     * performance.  If passed true, the text renderer will use vertex arrays or a vertex buffer
     * internally for rendering.  Otherwise it will use immediate mode commands.  Defaults to
     * true.
     *
     * @param useVertexArrays True to render with vertex arrays
     */
    public void setUseVertexArrays(final boolean useVertexArrays) {
        glyphRenderer.setUseVertexArrays(useVertexArrays);
    }

    /**
     * Utility supporting more full control over rendering the bitmapped text.
     *
     * <p>
     * Allows customization of whether the backing store text bitmap is full-color or intensity
     * only, the size of each individual rendered text rectangle, and the contents of each
     * individual rendered text string.
     */
    public interface RenderDelegate {

        /**
         * Renders text into a graphics instance at a specific location.
         *
         * <p>
         * The surrounding region will already have been cleared to the RGB color (0, 0, 0) with
         * zero alpha.  The initial drawing context of the passed Graphics2D will be set to use
         * AlphaComposite.Src, the color white, the Font specified in the TextRenderer's
         * constructor, and the rendering hints specified in the TextRenderer constructor.
         *
         * <p>
         * Changes made by the end user may be visible in successive calls to this method, but are
         * not guaranteed to be preserved.  Implementations should reset the Graphics2D's state to
         * that desired each time this method is called, in particular those states which are not
         * the defaults.
         *
         * @param g2d Graphics to render into
         * @param str Text to render
         * @param x Location on X axis to render at
         * @param y Location on Y axis to render at
         */
        void draw(/*@Nonnull*/ Graphics2D g2d,
                  /*@Nonnull*/ String str,
                  /*@CheckForSigned*/ int x,
                  /*@CheckForSigned*/ int y);

        /**
         * Renders a glyph into a graphics instance at a specific location.
         *
         * <p>
         * The surrounding region will already have been cleared to the RGB color (0, 0, 0) with
         * zero alpha.  The initial drawing context of the passed Graphics2D will be set to use
         * AlphaComposite.Src, the color white, the Font specified in the TextRenderer's
         * constructor, and the rendering hints specified in the TextRenderer constructor.
         *
         * <p>
         * Changes made by the end user may be visible in successive calls to this method, but are
         * not guaranteed to be preserved.  Implementations should reset the Graphics2D's state to
         * that desired each time this method is called, in particular those states which are not
         * the defaults.
         *
         * @param g2d Graphics to render into
         * @param gv Glyph to render
         * @param x Location on X axis to render at
         * @param y Location on Y axis to render at
         */
        void drawGlyphVector(/*@Nonnull*/ Graphics2D g2d,
                             /*@Nonnull*/ GlyphVector gv,
                             /*@CheckForSigned*/ int x,
                             /*@CheckForSigned*/ int y);

        /**
         * Computes the bounds of a character sequence relative to the origin.
         *
         * @param text Text to compute bounds of
         * @param font Font text renderer is using
         * @param frc Device-dependent details of how text should be rendered
         * @return Rectangle surrounding the text, not null
         */
        /*@Nonnull*/
        Rectangle2D getBounds(/*@Nonnull*/ CharSequence text,
                              /*@Nonnull*/ Font font,
                              /*@Nonnull*/ FontRenderContext frc);

        /**
         * Computes the bounds of a glyph relative to the origin.
         *
         * @param gv Glyph to compute bounds of (non-null)
         * @param frc Device-dependent details of how text should be rendered (non-null)
         * @return Rectangle surrounding the text (non-null)
         */
        Rectangle2D getBounds(/*@Nonnull*/ GlyphVector gv, /*@Nonnull*/ FontRenderContext frc);

        /**
         * Computes the bounds of a string relative to the origin.
         *
         * @param text Text to compute bounds of
         * @param font Font text renderer is using
         * @param frc Device-dependent details of how text should be rendered (non-null)
         * @return Rectangle surrounding the text, not null
         */
        Rectangle2D getBounds(/*@Nonnull*/ String text,
                              /*@Nonnull*/ Font font,
                              /*@Nonnull*/ FontRenderContext frc);

        /**
         * Indicates whether the backing store should be intensity-only or full-color.
         *
         * <p>
         * Note that currently the text renderer does not support full-color.  It will throw an
         * {@link UnsupportedOperationException} if the render delegate requests full-color.
         *
         * @return True if the backing store should be intensity-only
         */
        boolean intensityOnly();
    }

    /**
     * Simple render delegate if one is not specified by the user.
     */
    /*@Immmutable*/
    public static class DefaultRenderDelegate implements RenderDelegate {

        @Override
        public void draw(/*@Nonnull*/ final Graphics2D g2d,
                         /*@Nonnull*/ final String str,
                         /*@CheckForSigned*/ final int x,
                         /*@CheckForSigned*/ final int y) {

            Check.notNull(g2d, "Graphics cannot be null");
            Check.notNull(str, "String cannot be null");

            g2d.drawString(str, x, y);
        }

        @Override
        public void drawGlyphVector(/*@Nonnull*/ final Graphics2D g2d,
                                    /*@Nonnull*/ final GlyphVector gv,
                                    /*@CheckForSigned*/ final int x,
                                    /*@CheckForSigned*/ final int y) {

            Check.notNull(g2d, "Graphics cannot be null");
            Check.notNull(gv, "Glyph vector cannot be null");

            g2d.drawGlyphVector(gv, x, y);
        }

        /*@Nonnull*/
        @Override
        public Rectangle2D getBounds(/*@Nonnull*/ final CharSequence text,
                                     /*@Nonnull*/ final Font font,
                                     /*@Nonnull*/ final FontRenderContext frc) {

            Check.notNull(text, "Text cannot be null");
            Check.notNull(font, "Font cannot be null");
            Check.notNull(frc, "Font render context cannot be null");

            return getBounds(text.toString(), font, frc);
        }

        /*@Nonnull*/
        @Override
        public Rectangle2D getBounds(/*@Nonnull*/ final GlyphVector gv,
                                     /*@Nonnull*/ final FontRenderContext frc) {

            Check.notNull(gv, "Glyph vector cannot be null");
            Check.notNull(frc, "Font render context cannot be null");

            return gv.getVisualBounds();
        }

        /*@Nonnull*/
        @Override
        public Rectangle2D getBounds(/*@Nonnull*/ final String text,
                                     /*@Nonnull*/ final Font font,
                                     /*@Nonnull*/ final FontRenderContext frc) {

            Check.notNull(text, "Text cannot be null");
            Check.notNull(font, "Font cannot be null");
            Check.notNull(frc, "Font render context cannot be null");

            return getBounds(font.createGlyphVector(frc, text), frc);
        }

        @Override
        public boolean intensityOnly() {
            return true;
        }
    }

    /**
     * Utility for coordinating text renderer components.
     */
    private final class Mediator implements GlyphCache.EventListener, GlyphRenderer.EventListener {

        @Override
        public void onGlyphCacheEvent(/*@Nonnull*/ final GlyphCache.EventType type,
                                      /*@Nonnull*/ final Object data) {

            Check.notNull(type, "Event type cannot be null");
            Check.notNull(data, "Data cannot be null");

            switch (type) {
            case REALLOCATE:
                flush();
                break;
            case CLEAR:
                glyphProducer.clearGlyphs();
                break;
            case CLEAN:
                glyphProducer.removeGlyph((Glyph) data);
                break;
            }
        }

        @Override
        public void onGlyphRendererEvent(/*@Nonnull*/ final GlyphRenderer.EventType type) {

            Check.notNull(type, "Event type cannot be null");

            switch (type) {
            case AUTOMATIC_FLUSH:
                glyphCache.update(gl);
                break;
            }
        }
    }

    /**
     * <em>Proxy</em> for a {@link GlyphRenderer}.
     */
    /*@NotThreadSafe*/
    private static final class GlyphRendererProxy implements GlyphRenderer {

        /**
         * Delegate to actually render.
         */
        /*@CheckForNull*/
        private GlyphRenderer delegate;

        /**
         * Listeners added before a delegate is chosen.
         */
        /*@Nonnull*/
        private final List<EventListener> listeners = new ArrayList<EventListener>();

        /**
         * Red component of color.
         */
        /*@CheckForSigned*/
        private Float r;

        /**
         * Green component of color.
         */
        /*@CheckForSigned*/
        private Float g;

        /**
         * Blue component of color.
         */
        /*@CheckForSigned*/
        private Float b;

        /**
         * Alpha component of color.
         */
        /*@CheckForSigned*/
        private Float a;

        /**
         * Transform matrix.
         */
        /*@CheckForNull*/
        private float[] transform;

        /**
         * True if transform is transposed.
         */
        /*@CheckForNull*/
        private Boolean transposed;

        /**
         * True to use vertex arrays.
         */
        private boolean useVertexArrays = true;

        GlyphRendererProxy() {
            // empty
        }

        @Override
        public void addListener(/*@Nonnull*/ final EventListener listener) {

            Check.notNull(listener, "Listener cannot be null");

            if (delegate == null) {
                listeners.add(listener);
            } else {
                delegate.addListener(listener);
            }
        }

        @Override
        public void beginRendering(/*@Nonnull*/ final GL2GL3 gl,
                                   final boolean ortho,
                                   /*@Nonnegative*/ final int width,
                                   /*@Nonnegative*/ final int height,
                                   final boolean disableDepthTest, final boolean gl3) {

            Check.notNull(gl, "GL cannot be null");
            Check.argument(width >= 0, "Width cannot be negative");
            Check.argument(height >= 0, "Height cannot be negative");

            if (delegate == null) {

                // Create the glyph renderer
                delegate = GlyphRenderers.get(gl, gl3);

                // Add the event listeners
                for (EventListener listener : listeners) {
                    delegate.addListener(listener);
                }

                // Specify the color
                if ((r != null) && (g != null) && (b != null) && (a != null)) {
                    delegate.setColor(gl, r, g, b, a);
                }

                // Specify the transform
                if ((transform != null) && (transposed != null)) {
                    delegate.setTransform(gl, transform, transposed);
                }

                // Specify whether to use vertex arrays or not
                delegate.setUseVertexArrays(useVertexArrays);
            }
            delegate.beginRendering(gl, ortho, width, height, disableDepthTest, gl3);
        }

        @Override
        public void dispose(/*@Nonnull*/ final GL2GL3 gl) {

            Check.notNull(gl, "GL cannot be null");

            if (delegate != null) {
                delegate.dispose(gl);
            }
        }

        @Override
        public float drawGlyph(/*@Nonnull*/ final GL2GL3 gl,
                               /*@Nonnull*/ final Glyph glyph,
                               /*@CheckForSigned*/ final float x,
                               /*@CheckForSigned*/ final float y,
                               /*@CheckForSigned*/ final float z,
                               /*@CheckForSigned*/ final float scale,
                               final boolean verticalFlip,
                               /*@Nonnull*/ final TextureCoords coords) {

            Check.notNull(gl, "GL cannot be null");
            Check.notNull(glyph, "Glyph cannot be null");
            Check.notNull(coords, "Texture coordinates cannot be null");

            if (delegate == null) {
                throw new IllegalStateException("Must be in render cycle!");
            } else {
                return delegate.drawGlyph(gl, glyph, x, y, z, scale, verticalFlip, coords);
            }
        }

        @Override
        public void endRendering(/*@Nonnull*/ final GL2GL3 gl) {

            Check.notNull(gl, "GL cannot be null");

            if (delegate == null) {
                throw new IllegalStateException("Must be in render cycle!");
            } else {
                delegate.endRendering(gl);
            }
        }

        @Override
        public void flush(/*@Nonnull*/ final GL2GL3 gl) {

            Check.notNull(gl, "GL cannot be null");

            if (delegate == null) {
                throw new IllegalStateException("Must be in render cycle!");
            } else {
                delegate.flush(gl);
            }
        }

        @Override
        public boolean getUseVertexArrays() {
            if (delegate == null) {
                return useVertexArrays;
            } else {
                return delegate.getUseVertexArrays();
            }
        }

        @Override
        public void setColor(final GL2GL3 gl,
                             /*@CheckForSigned*/ final float r,
                             /*@CheckForSigned*/ final float g,
                             /*@CheckForSigned*/ final float b,
                             /*@CheckForSigned*/ final float a) {
            if (delegate == null) {
                this.r = r;
                this.g = g;
                this.b = b;
                this.a = a;
            } else {
                delegate.setColor(gl, r, g, b, a);
            }
        }

        @Override
        public void setTransform(final GL2GL3 gl, /*@Nonnull*/ final float[] value, final boolean transpose) {

            Check.notNull(value, "Value cannot be null");

            if (delegate == null) {
                this.transform = Arrays.copyOf(value, value.length);
                this.transposed = transpose;
            } else {
                delegate.setTransform(gl, value, transpose);
            }
        }

        @Override
        public void setUseVertexArrays(final boolean useVertexArrays) {
            if (delegate == null) {
                this.useVertexArrays = useVertexArrays;
            } else {
                delegate.setUseVertexArrays(useVertexArrays);
            }
        }
    }
}
