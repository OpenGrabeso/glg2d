package net.opengrabeso.opengl.text;

import java.awt.Font;
import java.awt.geom.*;
import net.opengrabeso.opengl.Jaagl2EventListener;
import net.opengrabeso.opengl.SelectJaaglEventListener;
import net.opengrabeso.ogltext.util.awt.TextRenderer;
import org.joml.Math;
import org.joml.Matrix4f;


/** Test Code adapted from TextCube.java (in JOGL demos)
 *
 * @author spiraljetty
 * @author kbr
 */

public abstract class Issue344Base implements Jaagl2EventListener
{
    TextRenderer renderer;

    float textScaleFactor;
    Font font;
    boolean antialias = true;
    Matrix4f projection;

    protected Issue344Base() {
        font = new Font("default", Font.PLAIN, 200);
    }

    protected abstract String getText();

    protected void run(final String[] args) {
        new SelectJaaglEventListener(this).run(args);
    }

    @Override
    public void init(final com.github.opengrabeso.jaagl.GL2GL3 gl) {
        gl.glEnable(gl.GL_DEPTH_TEST());

        renderer = new TextRenderer(font, true, false, gl);

        final Rectangle2D bounds = renderer.getBounds(getText());
        final float w = (float) bounds.getWidth();
        // final float h = (float) bounds.getHeight();
        textScaleFactor = 2.0f / (w * 1.1f);
        //gl.setSwapInterval(0);
    }

    @Override
    public void display(final com.github.opengrabeso.jaagl.GL2GL3 gl) {
        gl.glClearColor(0.8f, 0.5f, 0.5f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT() | gl.GL_DEPTH_BUFFER_BIT());


        final Matrix4f translate = new Matrix4f().translate(0, 0, -10);
        final Matrix4f view = new Matrix4f().scale(16, -16, 1);
        final Rectangle2D bounds = renderer.getBounds(getText());
        final float w = (float) bounds.getWidth();
        final float h = (float) bounds.getHeight();
        final Matrix4f mvp = new Matrix4f().set(projection).mul(translate);
        renderer.begin3DRendering(mvp.get(new float[16]));
        renderer.draw3D(getText(),
                        w / -2.0f * textScaleFactor,
                        h / -2.0f * textScaleFactor,
                        3f,
                        textScaleFactor,
                true);

        renderer.end3DRendering();
    }

    @Override
    public void reshape(final com.github.opengrabeso.jaagl.GL2GL3 gl, final int x, final int y, final int width, final int height) {
        float fov = 15;

        float aspect = (float) width / (float) height;
        float znear = 5;
        float zfar = 15;

        float fovRad = (float)(fov * java.lang.Math.PI / 180);
        projection = new Matrix4f().perspective(fovRad, aspect, znear, zfar);
        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(final com.github.opengrabeso.jaagl.GL2GL3 drawable) {}
}
