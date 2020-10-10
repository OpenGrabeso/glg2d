package net.opengrabeso.glg2d.impl.gl2;

import com.github.opengrabeso.jaagl.GL2;
import net.opengrabeso.ogltext.util.awt.TextRenderer;

import java.awt.*;

public class GL2StringDrawerImpl extends GL2StringDrawer{

    private final GL2 gl;

    public GL2StringDrawerImpl(GL2 gl) {
        this.gl = gl;
    }

    @Override
    protected TextRenderer createTextRenderer(Font font, boolean antialias) {
        return new TextRenderer(font, antialias, false, gl);
    }

    @Override
    protected float[] getTransform(TextRenderer renderer) {
        throw new UnsupportedOperationException("GL2StringDrawerImpl.getTransform");
    }

    @Override
    protected boolean useVerticalFlip() {return false;}

    @Override
    protected void cleanupMatrix(TextRenderer renderer) {
        gl.glPopMatrix();
    }

}
