/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package net.opengrabeso.opengl.util.texture;

import java.nio.*;

import com.github.opengrabeso.jaagl.*;

import com.github.opengrabeso.ogltext.util.texture.TextureCoords;

/**
 * Represents an OpenGL texture object. Contains convenience routines
 * for enabling/disabling OpenGL texture state, binding this texture,
 * and computing texture coordinates for both the entire image as well
 * as a sub-image.
 *
 * <a name="textureCallOrder"><h5>Order of Texture Commands</h5></a>
 * <p>
 * Due to many confusions w/ texture usage, following list described the order
 * and semantics of texture unit selection, binding and enabling.
 * <ul>
 *   <li><i>Optional:</i> Set active textureUnit via <code>gl.glActiveTexture(GL.GL_TEXTURE0 + textureUnit)</code>, <code>0</code> is default.</li>
 *   <li>Bind <code>textureId</code> -> active <code>textureUnit</code>'s <code>textureTarget</code> via <code>gl.glBindTexture(textureTarget, textureId)</code></li>
 *   <li><i>Compatible Context Only:</i> Enable active <code>textureUnit</code>'s <code>textureTarget</code> via <code>glEnable(textureTarget)</code>.
 *   <li><i>Optional:</i> Fiddle with the texture parameters and/or environment settings.</li>
 *   <li>GLSL: Use <code>textureUnit</code> in your shader program, enable shader program.</li>
 *   <li>Issue draw commands</li>
 * </ul>
 * </p>
 *
 * <p>One caveat in this approach is that certain texture wrap modes
 * (e.g.  <code>GL_REPEAT</code>) are not legal when the GL_ARB_texture_rectangle
 * extension is in use.  Another issue to be aware of is that in the
 * default pow2 scenario, if the original image does not have pow2
 * dimensions, then wrapping may not work as one might expect since
 * the image does not extend to the edges of the pow2 texture.  If
 * texture wrapping is important, it is recommended to use only
 * pow2-sized images with the Texture class.
 *
 * <p><a name="perftips"><b>Performance Tips</b></a>
 * <br> For best performance, try to avoid calling {@link #enable} /
 * {@link #bind} / {@link #disable} any more than necessary. For
 * example, applications using many Texture objects in the same scene
 * may want to reduce the number of calls to both {@link #enable} and
 * {@link #disable}. To do this it is necessary to call {@link
 * #getTarget} to make sure the OpenGL texture target is the same for
 * all of the Texture objects in use; non-power-of-two textures using
 * the GL_ARB_texture_rectangle extension use a different target than
 * power-of-two textures using the GL_TEXTURE_2D target. Note that
 * when switching between textures it is necessary to call {@link
 * #bind}, but when drawing many triangles all using the same texture,
 * for best performance only one call to {@link #bind} should be made.
 * User may also utilize multiple texture units,
 * see <a href="#textureCallOrder"> order of texture commands above</a>.
 *
 * <p><a name="premult"><b>Alpha premultiplication and blending</b></a>
 * <p>
 * <i>Disclaimer: Consider performing alpha premultiplication in shader code, if really desired! Otherwise use RGBA.</i><br/>
 * </p>
 * <p>
 * The Texture class does not convert RGBA image data into
 * premultiplied data when storing it into an OpenGL texture.
 * </p>
 * <p>
 * The mathematically correct way to perform blending in OpenGL
 * with the SrcOver "source over destination" mode, or any other
 * Porter-Duff rule, is to use <i>premultiplied color components</i>,
 * which means the R/G/ B color components must have been multiplied by
 * the alpha value.  If using <i>premultiplied color components</i>
 * it is important to use the correct blending function; for
 * example, the SrcOver rule is expressed as:
<pre>
    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
</pre>
 * Also, when using a texture function like <code>GL_MODULATE</code> where
 * the current color plays a role, it is important to remember to make
 * sure that the color is specified in a premultiplied form, for
 * example:
<pre>
    float a = ...;
    float r = r * a;
    float g = g * a;
    float b = b * a;
    gl.glColor4f(r, g, b, a);
</pre>
 *
 * For reference, here is a list of the Porter-Duff compositing rules
 * and the associated OpenGL blend functions (source and destination
 * factors) to use in the face of premultiplied alpha:
 *
<CENTER>
<TABLE WIDTH="75%">
<TR> <TD> Rule     <TD> Source                  <TD> Dest
<TR> <TD> Clear    <TD> GL_ZERO                 <TD> GL_ZERO
<TR> <TD> Src      <TD> GL_ONE                  <TD> GL_ZERO
<TR> <TD> SrcOver  <TD> GL_ONE                  <TD> GL_ONE_MINUS_SRC_ALPHA
<TR> <TD> DstOver  <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_ONE
<TR> <TD> SrcIn    <TD> GL_DST_ALPHA            <TD> GL_ZERO
<TR> <TD> DstIn    <TD> GL_ZERO                 <TD> GL_SRC_ALPHA
<TR> <TD> SrcOut   <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_ZERO
<TR> <TD> DstOut   <TD> GL_ZERO                 <TD> GL_ONE_MINUS_SRC_ALPHA
<TR> <TD> Dst      <TD> GL_ZERO                 <TD> GL_ONE
<TR> <TD> SrcAtop  <TD> GL_DST_ALPHA            <TD> GL_ONE_MINUS_SRC_ALPHA
<TR> <TD> DstAtop  <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_SRC_ALPHA
<TR> <TD> AlphaXor <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_ONE_MINUS_SRC_ALPHA
</TABLE>
</CENTER>
 * @author Chris Campbell, Kenneth Russell, et.al.
 */
public class Texture {
    /** The GL target type for this texture. */
    private int target;
    /** The image GL target type for this texture, or its sub-components if cubemap. */
    private int imageTarget;
    /** The GL texture ID. */
    private int texID;
    /** The width of the texture. */
    private int texWidth;
    /** The height of the texture. */
    private int texHeight;
    /** The width of the image. */
    private int imgWidth;
    /** The height of the image. */
    private int imgHeight;
    /** The original aspect ratio of the image, before any rescaling
        that might have occurred due to using the GLU mipmap routines. */
    private float aspectRatio;
    /** Indicates whether the TextureData requires a vertical flip of
        the texture coords. */
    private boolean mustFlipVertically;
    /** Indicates whether we're using automatic mipmap generation
        support (GL_GENERATE_MIPMAP). */
    private boolean usingAutoMipmapGeneration;

    /** The texture coordinates corresponding to the entire image. */
    private TextureCoords coords;

    @Override
    public String toString() {
        final String targetS = target == imageTarget ? Integer.toHexString(target) : Integer.toHexString(target) + " - image "+Integer.toHexString(imageTarget);
        return "Texture[target "+targetS+", name "+texID+", "+
                imgWidth+"/"+texWidth+" x "+imgHeight+"/"+texHeight+", y-flip "+mustFlipVertically+ "]";
    }

    public Texture(final GL gl, final TextureData data) {
        this.texID = 0;
        this.target = 0;
        this.imageTarget = 0;
        updateImage(gl, data);
    }

    /**
     * Constructor for use when creating e.g. cube maps, where there is
     * no initial texture data
     * @param target the OpenGL texture target, eg GL.GL_TEXTURE_2D,
     *               GL2.GL_TEXTURE_RECTANGLE
     */
    public Texture(final int target) {
        this.texID = 0;
        this.target = target;
        this.imageTarget = target;
    }

    /**
     * Constructor to wrap an OpenGL texture ID from an external library and allows
     * some of the base methods from the Texture class, such as
     * binding and querying of texture coordinates, to be used with
     * it. Attempts to update such textures' contents will yield
     * undefined results.
     *
     * @param textureID the OpenGL texture object to wrap
     * @param target the OpenGL texture target, eg GL.GL_TEXTURE_2D,
     *               GL2.GL_TEXTURE_RECTANGLE
     * @param texWidth the width of the texture in pixels
     * @param texHeight the height of the texture in pixels
     * @param imgWidth the width of the image within the texture in
     *          pixels (if the content is a sub-rectangle in the upper
     *          left corner); otherwise, pass in texWidth
     * @param imgHeight the height of the image within the texture in
     *          pixels (if the content is a sub-rectangle in the upper
     *          left corner); otherwise, pass in texHeight
     * @param mustFlipVertically indicates whether the texture
     *                           coordinates must be flipped vertically
     *                           in order to properly display the
     *                           texture
     */
    public Texture(final int textureID, final int target,
                   final int texWidth, final int texHeight,
                   final int imgWidth, final int imgHeight,
                   final boolean mustFlipVertically) {
        this.texID = textureID;
        this.target = target;
        this.imageTarget = target;
        this.mustFlipVertically = mustFlipVertically;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.aspectRatio = (float) imgWidth / (float) imgHeight;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.updateTexCoords();
    }

    /**
     * Enables this texture's target (e.g., GL_TEXTURE_2D) in the
     * given GL context's state. This method is a shorthand equivalent
     * of the following OpenGL code:
     * <pre>
     *   gl.glEnable(texture.getTarget());
     * </pre>
     * <p>
     * See the <a href="#perftips">performance tips</a> above for hints
     * on how to maximize performance when using many Texture objects.
     * </p>
     * @param gl the current GL object
     */
    public void enable(final GL gl) {
        if (!gl.isGL3()) {
            gl.glEnable(target);
        }
    }

    /**
     * Disables this texture's target (e.g., GL_TEXTURE_2D) in the
     * given GL state. This method is a shorthand equivalent
     * of the following OpenGL code:
     * <pre>
     *   gl.glDisable(texture.getTarget());
     * </pre>
     * <p>
     * See the <a href="#perftips">performance tips</a> above for hints
     * on how to maximize performance when using many Texture objects.
     * </p>
     * @param gl the current GL object
     */
    public void disable(final GL gl) {
        if (!gl.isGL3()) {
            gl.glDisable(target);
        }
    }

    /**
     * Binds this texture to the given GL context. This method is a
     * shorthand equivalent of the following OpenGL code:
     <pre>
     gl.glBindTexture(texture.getTarget(), texture.getTextureObject());
     </pre>
     *
     * See the <a href="#perftips">performance tips</a> above for hints
     * on how to maximize performance when using many Texture objects.
     *
     * @param gl the current GL context
     */
    public void bind(final GL gl) {
        validateTexID(gl, true);
        gl.glBindTexture(target, texID);
    }

    /**
     * Destroys the native resources used by this texture object.
     */
    public void destroy(final GL gl) {
        if(0!=texID) {
            gl.glDeleteTextures(new int[] {texID});
            texID = 0;
        }
    }

    /**
     * Returns the OpenGL "target" of this texture.
     * @see GL#GL_TEXTURE_2D
     */
    public int getTarget() {
        return target;
    }

    /**
     * Returns the width of the allocated OpenGL texture in pixels.
     * Note that the texture width will be greater than or equal to the
     * width of the image contained within.
     *
     * @return the width of the texture
     */
    public int getWidth() {
        return texWidth;
    }

    /**
     * Returns the height of the allocated OpenGL texture in pixels.
     * Note that the texture height will be greater than or equal to the
     * height of the image contained within.
     *
     * @return the height of the texture
     */
    public int getHeight() {
        return texHeight;
    }

    /**
     * Returns the width of the image contained within this texture.
     * Note that for non-power-of-two textures in particular this may
     * not be equal to the result of {@link #getWidth}. It is
     * recommended that applications call {@link #getImageTexCoords} and
     * {@link #getSubImageTexCoords} rather than using this API
     * directly.
     *
     * @return the width of the image
     */
    public int getImageWidth() {
        return imgWidth;
    }

    /**
     * Returns the height of the image contained within this texture.
     * Note that for non-power-of-two textures in particular this may
     * not be equal to the result of {@link #getHeight}. It is
     * recommended that applications call {@link #getImageTexCoords} and
     * {@link #getSubImageTexCoords} rather than using this API
     * directly.
     *
     * @return the height of the image
     */
    public int getImageHeight() {
        return imgHeight;
    }

    /**
     * Returns the original aspect ratio of the image, defined as (image
     * width) / (image height), before any scaling that might have
     * occurred as a result of using the GLU mipmap routines.
     */
    public float getAspectRatio() {
        return aspectRatio;
    }

    /**
     * Returns the set of texture coordinates corresponding to the
     * entire image. If the TextureData indicated that the texture
     * coordinates must be flipped vertically, the returned
     * TextureCoords will take that into account.
     *
     * @return the texture coordinates corresponding to the entire image
     */
    public TextureCoords getImageTexCoords() {
        return coords;
    }

    /**
     * Returns the set of texture coordinates corresponding to the
     * specified sub-image. The (x1, y1) and (x2, y2) points are
     * specified in terms of pixels starting from the lower-left of the
     * image. (x1, y1) should specify the lower-left corner of the
     * sub-image and (x2, y2) the upper-right corner of the sub-image.
     * If the TextureData indicated that the texture coordinates must be
     * flipped vertically, the returned TextureCoords will take that
     * into account; this should not be handled by the end user in the
     * specification of the y1 and y2 coordinates.
     *
     * @return the texture coordinates corresponding to the specified sub-image
     */
    public TextureCoords getSubImageTexCoords(final int x1, final int y1, final int x2, final int y2) {
        final float tx1 = (float)x1 / (float)texWidth;
        final float ty1 = (float)y1 / (float)texHeight;
        final float tx2 = (float)x2 / (float)texWidth;
        final float ty2 = (float)y2 / (float)texHeight;
        if (mustFlipVertically) {
            final float yMax = (float) imgHeight / (float) texHeight;
            return new TextureCoords(tx1, yMax - ty1, tx2, yMax - ty2);
        } else {
            return new TextureCoords(tx1, ty1, tx2, ty2);
        }
    }

    /**
     * Updates the entire content area incl. {@link TextureCoords}
     * of this texture using the data in the given image.
     */
    public void updateImage(final GL gl, final TextureData data) {
        updateImage(gl, data, 0);
    }

    /**
     * Indicates whether this texture's texture coordinates must be
     * flipped vertically in order to properly display the texture. This
     * is handled automatically by {@link #getImageTexCoords
     * getImageTexCoords} and {@link #getSubImageTexCoords
     * getSubImageTexCoords}, but applications may generate or otherwise
     * produce texture coordinates which must be corrected.
     */
    public boolean getMustFlipVertically() {
        return mustFlipVertically;
    }

    /**
     * Change whether the TextureData requires a vertical flip of
     * the texture coords.
     * <p>
     * No-op if no change, otherwise generates new {@link TextureCoords}.
     * </p>
     */
    public void setMustFlipVertically(final boolean v) {
        if( v != mustFlipVertically ) {
            mustFlipVertically = v;
            updateTexCoords();
        }
    }

    /**
     * Updates the content area incl. {@link TextureCoords} of the specified target of this texture
     * using the data in the given image. In general this is intended
     * for construction of cube maps.
     */
    public void updateImage(final GL gl, final TextureData data, final int targetOverride) {
        validateTexID(gl, true);

        imgWidth = data.getWidth();
        imgHeight = data.getHeight();
        aspectRatio = (float) imgWidth / (float) imgHeight;
        mustFlipVertically = data.getMustFlipVertically();

        int texTarget = 0;
        int texParamTarget = this.target;

        // See whether we have automatic mipmap generation support

        // Indicate to the TextureData what functionality is available
        data.setHaveEXTABGR(gl.isExtensionAvailable("GL_EXT_abgr"));
        data.setHaveGL12(gl.isExtensionAvailable("GL_VERSION_1_2"));

        // Note that automatic mipmap generation doesn't work for
        // GL_ARB_texture_rectangle

        texWidth = imgWidth;
        texHeight = imgHeight;
        texTarget = gl.GL_TEXTURE_2D();

        texParamTarget = texTarget;
        imageTarget = texTarget;
        updateTexCoords();

        if (targetOverride != 0) {
            // Allow user to override auto detection and skip bind step (for
            // cubemap construction)
            if (this.target == 0) {
                throw gl.newGLException("Override of target failed; no target specified yet");
            }
            texTarget = targetOverride;
            texParamTarget = this.target;
            gl.glBindTexture(texParamTarget, texID);
        } else {
            gl.glBindTexture(texTarget, texID);
        }

        checkCompressedTextureExtensions(gl, data);
        final Buffer[] mipmapData = data.getMipmapData();
        if (mipmapData != null) {
            int width = texWidth;
            int height = texHeight;
            for (int i = 0; i < mipmapData.length; i++) {
                if (data.isDataCompressed()) {
                    throw new UnsupportedOperationException("Compressed textures not supported");
                } else {
                    // Allocate texture image at this level
                    gl.glTexImage2D(texTarget, i, data.getInternalFormat(),
                                    width, height, data.getBorder(),
                                    data.getPixelFormat(), data.getPixelType(), null);
                    updateSubImageImpl(gl, data, texTarget, i, 0, 0, 0, 0, data.getWidth(), data.getHeight());
                }

                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);
            }
        } else {
            if (data.isDataCompressed()) {
                // Need to use glCompressedTexImage2D directly to allocate and fill this image
                // Avoid spurious memory allocation when possible
                throw new UnsupportedOperationException("Compressed textures not supported");
            } else {
                if (data.getMipmap()) {
                    // For now, only use hardware mipmapping for uncompressed 2D
                    // textures where the user hasn't explicitly specified
                    // mipmap data; don't know about interactions between
                    // GL_GENERATE_MIPMAP and glCompressedTexImage2D
                    gl.glTexParameteri(texParamTarget, gl.GL_GENERATE_MIPMAP(), gl.GL_TRUE());
                    usingAutoMipmapGeneration = true;
                }

                gl.glTexImage2D(texTarget, 0, data.getInternalFormat(),
                                texWidth, texHeight, data.getBorder(),
                                data.getPixelFormat(), data.getPixelType(), null);
                updateSubImageImpl(gl, data, texTarget, 0, 0, 0, 0, 0, data.getWidth(), data.getHeight());
            }
        }

        final int minFilter = (data.getMipmap() ? gl.GL_LINEAR_MIPMAP_LINEAR() : gl.GL_LINEAR());
        final int magFilter = gl.GL_LINEAR();
        final int wrapMode = gl.GL_CLAMP_TO_EDGE();

        gl.glTexParameteri(texParamTarget, gl.GL_TEXTURE_MIN_FILTER(), minFilter);
        gl.glTexParameteri(texParamTarget, gl.GL_TEXTURE_MAG_FILTER(), magFilter);
        gl.glTexParameteri(texParamTarget, gl.GL_TEXTURE_WRAP_S(), wrapMode);
        gl.glTexParameteri(texParamTarget, gl.GL_TEXTURE_WRAP_T(), wrapMode);
        if (this.target == gl.GL_TEXTURE_CUBE_MAP()) {
            gl.glTexParameteri(texParamTarget, gl.GL_TEXTURE_WRAP_R(), wrapMode);
        }

        // Don't overwrite target if we're loading e.g. faces of a cube
        // map
        if ((this.target == 0) ||
            (this.target == gl.GL_TEXTURE_2D())) {
            this.target = texTarget;
        }
    }

    /**
     * Updates a subregion of the content area of this texture using the
     * given data. If automatic mipmap generation is in use (see {@link
     * #isUsingAutoMipmapGeneration isUsingAutoMipmapGeneration}),
     * updates to the base (level 0) mipmap will cause the lower-level
     * mipmaps to be regenerated, and updates to other mipmap levels
     * will be ignored. Otherwise, if automatic mipmap generation is not
     * in use, only updates the specified mipmap level and does not
     * re-generate mipmaps if they were originally produced or loaded.
     *
     * @param data the image data to be uploaded to this texture
     * @param mipmapLevel the mipmap level of the texture to set. If
     * this is non-zero and the TextureData contains mipmap data, the
     * appropriate mipmap level will be selected.
     * @param x the x offset (in pixels) relative to the lower-left corner
     * of this texture
     * @param y the y offset (in pixels) relative to the lower-left corner
     * of this texture
     */
    public void updateSubImage(final GL gl, final TextureData data, final int mipmapLevel, final int x, final int y) {
        if (usingAutoMipmapGeneration && mipmapLevel != 0) {
            // When we're using mipmap generation via GL_GENERATE_MIPMAP, we
            // don't need to update other mipmap levels
            return;
        }
        bind(gl);
        updateSubImageImpl(gl, data, target, mipmapLevel, x, y, 0, 0, data.getWidth(), data.getHeight());
    }

    /**
     * Updates a subregion of the content area of this texture using the
     * specified sub-region of the given data.  If automatic mipmap
     * generation is in use (see {@link #isUsingAutoMipmapGeneration
     * isUsingAutoMipmapGeneration}), updates to the base (level 0)
     * mipmap will cause the lower-level mipmaps to be regenerated, and
     * updates to other mipmap levels will be ignored. Otherwise, if
     * automatic mipmap generation is not in use, only updates the
     * specified mipmap level and does not re-generate mipmaps if they
     * were originally produced or loaded. This method is only supported
     * for uncompressed TextureData sources.
     *
     * @param data the image data to be uploaded to this texture
     * @param mipmapLevel the mipmap level of the texture to set. If
     * this is non-zero and the TextureData contains mipmap data, the
     * appropriate mipmap level will be selected.
     * @param dstx the x offset (in pixels) relative to the lower-left corner
     * of this texture where the update will be applied
     * @param dsty the y offset (in pixels) relative to the lower-left corner
     * of this texture where the update will be applied
     * @param srcx the x offset (in pixels) relative to the lower-left corner
     * of the supplied TextureData from which to fetch the update rectangle
     * @param srcy the y offset (in pixels) relative to the lower-left corner
     * of the supplied TextureData from which to fetch the update rectangle
     * @param width the width (in pixels) of the rectangle to be updated
     * @param height the height (in pixels) of the rectangle to be updated
     */
    public void updateSubImage(final GL gl, final TextureData data, final int mipmapLevel,
                               final int dstx, final int dsty,
                               final int srcx, final int srcy,
                               final int width, final int height) {
        if (data.isDataCompressed()) {
            throw gl.newGLException("updateSubImage specifying a sub-rectangle is not supported for compressed TextureData");
        }
        if (usingAutoMipmapGeneration && mipmapLevel != 0) {
            // When we're using mipmap generation via GL_GENERATE_MIPMAP, we
            // don't need to update other mipmap levels
            return;
        }
        bind(gl);
        updateSubImageImpl(gl, data, target, mipmapLevel, dstx, dsty, srcx, srcy, width, height);
    }

    /**
     * Returns the underlying OpenGL texture object for this texture
     * and generates it if not done yet.
     * <p>
     * Most applications will not need to access this, since it is
     * handled automatically by the bind(GL) and destroy(GL) APIs.
     * </p>
     * @param gl required to be valid and current in case the texture object has not been generated yet,
     *           otherwise it may be <code>null</code>.
     * @see #getTextureObject()
     */
    public int getTextureObject(final GL gl) {
        validateTexID(gl, false);
        return texID;
    }

    /**
     * Returns the underlying OpenGL texture object for this texture,
     * maybe <code>0</code> if not yet generated.
     * <p>
     * Most applications will not need to access this, since it is
     * handled automatically by the bind(GL) and destroy(GL) APIs.
     * </p>
     * @see #getTextureObject(GL)
     */
    public int getTextureObject() {
        return texID;
    }

    /** Indicates whether this Texture is using automatic mipmap
        generation (via the OpenGL texture parameter
        GL_GENERATE_MIPMAP). This will automatically be used when
        mipmapping is requested via the TextureData and either OpenGL
        1.4 or the GL_SGIS_generate_mipmap extension is available. If
        so, updates to the base image (mipmap level 0) will
        automatically propagate down to the lower mipmap levels. Manual
        updates of the mipmap data at these lower levels will be
        ignored. */
    public boolean isUsingAutoMipmapGeneration() {
        return usingAutoMipmapGeneration;
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    private void updateTexCoords() {
        if (mustFlipVertically) {
            coords = new TextureCoords(0,                                      // l
                                       (float) imgHeight / (float) texHeight,  // b
                                       (float) imgWidth / (float) texWidth,    // r
                                       0                                       // t
                                      );
        } else {
            coords = new TextureCoords(0,                                      // l
                                       0,                                      // b
                                       (float) imgWidth / (float) texWidth,    // r
                                       (float) imgHeight / (float) texHeight   // t
                                      );
        }
    }

    private void updateSubImageImpl(final GL gl, final TextureData data, final int newTarget, final int mipmapLevel,
                                    int dstx, int dsty,
                                    int srcx, int srcy, int width, int height) {
        data.setHaveEXTABGR(gl.isExtensionAvailable("GL_EXT_abgr"));
        data.setHaveGL12(gl.isExtensionAvailable("GL_VERSION_1_2"));

        ByteBuffer buffer = data.getBuffer();
        if (buffer == null && data.getMipmapData() == null) {
            // Assume user just wanted to get the Texture object allocated
            return;
        }

        int rowlen = data.getRowLength();
        int dataWidth = data.getWidth();
        int dataHeight = data.getHeight();
        if (data.getMipmapData() != null) {
            // Compute the width, height and row length at the specified mipmap level
            // Note we do not support specification of the row length for
            // mipmapped textures at this point
            for (int i = 0; i < mipmapLevel; i++) {
                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);

                dataWidth = Math.max(dataWidth / 2, 1);
                dataHeight = Math.max(dataHeight / 2, 1);
            }
            rowlen = 0;
            buffer = data.getMipmapData()[mipmapLevel];
        }

        // Clip incoming rectangles to what is available both on this
        // texture and in the incoming TextureData
        if (srcx < 0) {
            width += srcx;
            srcx = 0;
        }
        if (srcy < 0) {
            height += srcy;
            srcy = 0;
        }
        // NOTE: not sure whether the following two are the correct thing to do
        if (dstx < 0) {
            width += dstx;
            dstx = 0;
        }
        if (dsty < 0) {
            height += dsty;
            dsty = 0;
        }

        if (srcx + width > dataWidth) {
            width = dataWidth - srcx;
        }
        if (srcy + height > dataHeight) {
            height = dataHeight - srcy;
        }
        if (dstx + width > texWidth) {
            width = texWidth - dstx;
        }
        if (dsty + height > texHeight) {
            height = texHeight - dsty;
        }

        checkCompressedTextureExtensions(gl, data);

        if (data.isDataCompressed()) {
            throw new UnsupportedOperationException("Compressed textures not supported");
        } else {
            final int[] align = { 0 };
            final int[] rowLength = { 0 };
            final int[] skipRows = { 0 };
            final int[] skipPixels = { 0 };
            gl.glGetIntegerv(gl.GL_UNPACK_ALIGNMENT(),   align); // save alignment
            gl.glGetIntegerv(gl.GL_UNPACK_ROW_LENGTH(),  rowLength); // save row length
            gl.glGetIntegerv(gl.GL_UNPACK_SKIP_ROWS(),   skipRows); // save skipped rows
            gl.glGetIntegerv(gl.GL_UNPACK_SKIP_PIXELS(), skipPixels); // save skipped pixels
            gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT(), data.getAlignment());
            gl.glPixelStorei(gl.GL_UNPACK_ROW_LENGTH(), rowlen);
            gl.glPixelStorei(gl.GL_UNPACK_SKIP_ROWS(), srcy);
            gl.glPixelStorei(gl.GL_UNPACK_SKIP_PIXELS(), srcx);

            gl.glTexSubImage2D(newTarget, mipmapLevel,
                               dstx, dsty, width, height,
                               data.getPixelFormat(), data.getPixelType(),
                               buffer);
            gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT(),   align[0]);      // restore alignment
            gl.glPixelStorei(gl.GL_UNPACK_ROW_LENGTH(),  rowLength[0]);  // restore row length
            gl.glPixelStorei(gl.GL_UNPACK_SKIP_ROWS(),   skipRows[0]);   // restore skipped rows
            gl.glPixelStorei(gl.GL_UNPACK_SKIP_PIXELS(), skipPixels[0]); // restore skipped pixels
        }
    }

    private void checkCompressedTextureExtensions(final GL gl, final TextureData data) {
        if (data.isDataCompressed()) {
            throw new UnsupportedOperationException("Compressed textures not supported");
        }
    }

    private boolean validateTexID(final GL gl, final boolean throwException) {
        if( 0 == texID ) {
            if( null != gl ) {
                final int[] tmp = new int[1];
                gl.glGenTextures(tmp);
                texID = tmp[0];
                if ( 0 == texID && throwException ) {
                    throw gl.newGLException("Create texture ID invalid: texID "+texID+", glerr 0x"+Integer.toHexString(gl.glGetError()));
                }
            } else if ( throwException ) {
                throw new UnsupportedOperationException("No GL context given, can't create texture ID");
            }
        }
        return 0 != texID;
    }

}