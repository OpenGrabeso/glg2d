package net.opengrabeso.glg2d;

import com.github.opengrabeso.jaagl.GL2GL3;
import net.opengrabeso.glg2d.impl.shader.GLShaderGraphics2D;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import javax.swing.*;
import java.awt.*;

/**
 * LWJGL-backed OpenGL canvas that renders a Swing JComponent using GLG2D.
 * This replaces the previous GLFW window approach so it can be embedded in Swing.
 */
public class GLG2DPanelLWJGL extends AWTGLCanvas {
    private final JComponent component;

    private GL2GL3 gl;
    private GLShaderGraphics2D graphics2D;

    public GLG2DPanelLWJGL(JComponent component) {
        super(createGLData());
        this.component = component;
        Dimension pref = component != null ? component.getPreferredSize() : null;
        if (pref != null) setPreferredSize(pref);
        setBackground(component != null && component.getBackground() != null ? component.getBackground() : Color.WHITE);
    }

    // Backwards-compatible constructors for examples expecting previous API
    public GLG2DPanelLWJGL() {
        super(createGLData());
        this.component = null;
        setBackground(Color.WHITE);
    }
    public GLG2DPanelLWJGL(JComponent component, String title) {
        this(component);
        // title is not used in AWTGLCanvas mode, kept for API compatibility
    }

    private static GLData createGLData() {
        GLData data = new GLData();
        data.majorVersion = 3;
        data.minorVersion = 2;
        data.profile = GLData.Profile.CORE;
        data.doubleBuffer = true;
        data.samples = 0;
        data.swapInterval = 1; // vsync
        return data;
    }

    @Override
    public void initGL() {
        GLCapabilities caps = GL.createCapabilities();
        if (caps.OpenGL30) {
            gl = com.github.opengrabeso.jaagl.lwjgl.LWGL.createGL3();
        } else {
            gl = com.github.opengrabeso.jaagl.lwjgl.LWGL.createGL2();
        }
        graphics2D = new GLShaderGraphics2D(gl);
        Color bg = component != null && component.getBackground() != null ? component.getBackground() : Color.WHITE;
        GL11.glClearColor(bg.getRed()/255f, bg.getGreen()/255f, bg.getBlue()/255f, 1f);
    }

    public void run() {
        showInFrame();
    }

    public void showInFrame() {
        JFrame frame = new JFrame("GLG2D");
        frame.getContentPane().add(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void paintGL() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());

        GL11.glViewport(0, 0, w, h);
        Color bg = component != null && component.getBackground() != null ? component.getBackground() : Color.WHITE;
        GL11.glClearColor(bg.getRed()/255f, bg.getGreen()/255f, bg.getBlue()/255f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

        if (component != null) {
            if (component.getWidth() != w || component.getHeight() != h) {
                component.setSize(w, h);
            }

            // Prepare GLG2D and render Swing component
            graphics2D.prePaint(gl);
            try {
                Graphics2D g2 = graphics2D;
                g2.setClip(new Rectangle(0, 0, w, h));
                component.paint(g2);
            } finally {
                graphics2D.postPaint();
            }
        }

        swapBuffers();
    }

    private void doPaint() {
        assert SwingUtilities.isEventDispatchThread();
        try {
            Graphics g = getGraphics(); // this call is necessary, it causes swing to flush its rendering - calls WWindowPeer.getStateLock
            render();
            if (g != null) g.dispose();
        } catch (Exception e) { // we want to print possible hidden asserts too
            e.printStackTrace(); 
        }
    }

    @Override
    public void paint(Graphics g) {
        if (SwingUtilities.isEventDispatchThread()) {
            doPaint();
        } else {
            SwingUtilities.invokeLater(this::doPaint);
        }
    }
}