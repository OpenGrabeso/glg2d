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
 * The component is a rendering tree, not an interactive Swing child: input and
 * focus are not forwarded. Create and use the canvas and its component on the EDT.
 * Nested components receive peer lifecycle notifications and layout before painting.
 */
public class GLG2DPanelLWJGL extends AWTGLCanvas {
    private final JComponent component;

    private GL2GL3 gl;
    private GLShaderGraphics2D graphics2D;
    private GLCapabilities capabilities;
    private String title = "GLG2D";
    private final Timer repaintTimer = new Timer(33, event -> repaint());

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
        this.title = title;
    }

    private static GLData createGLData() {
        GLData data = new GLData();
        data.majorVersion = 3;
        data.minorVersion = 2;
        data.profile = GLData.Profile.CORE;
        data.doubleBuffer = true;
        data.samples = 0;
        data.stencilSize = 8;
        data.swapInterval = 1; // vsync
        return data;
    }

    @Override
    public void initGL() {
        GLCapabilities caps = capabilities = GL.createCapabilities();
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
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::showInFrame);
            return;
        }
        JFrame frame = new JFrame(title);
        frame.getContentPane().add(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (component == null) setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void paintGL() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());

        GL.setCapabilities(capabilities);
        java.awt.geom.AffineTransform scale = getGraphicsConfiguration().getDefaultTransform();
        GL11.glViewport(0, 0, (int) Math.round(w * scale.getScaleX()), (int) Math.round(h * scale.getScaleY()));
        Color bg = component != null && component.getBackground() != null ? component.getBackground() : Color.DARK_GRAY;
        GL11.glClearColor(bg.getRed()/255f, bg.getGreen()/255f, bg.getBlue()/255f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

        if (component != null) {
            if (component.getWidth() != w || component.getHeight() != h) {
                component.setSize(w, h);
            }
            layoutTree(component);

            // Prepare GLG2D and render Swing component
            graphics2D.prePaint(gl);
            RepaintManager manager = RepaintManager.currentManager(component);
            boolean buffered = manager.isDoubleBufferingEnabled();
            try {
                manager.setDoubleBufferingEnabled(false);
                Graphics2D g2 = graphics2D;
                g2.scale(scale.getScaleX(), scale.getScaleY());
                g2.setClip(new Rectangle(0, 0, w, h));
                component.paint(g2);
            } finally {
                manager.setDoubleBufferingEnabled(buffered);
                graphics2D.postPaint();
            }
        }

        swapBuffers();
    }

    private void doPaint() {
        assert SwingUtilities.isEventDispatchThread();
        if (!isShowing()) return;
        Graphics g = getGraphics();
        try {
            render();
        } finally {
            if (g != null) g.dispose();
        }
    }

    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) layoutTree((Container) child);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (component != null) component.addNotify();
        repaintTimer.start();
    }

    @Override
    public void removeNotify() {
        repaintTimer.stop();
        try {
            if (graphics2D != null) runInContext(() -> {
                GL.setCapabilities(capabilities);
                graphics2D.glDispose();
            });
        } finally {
            try {
                // lwjgl3-awt 0.1.8 clears the handle in removeNotify but only
                // releases the AWT drawing surface; it does not delete the GL context.
                if (context != 0 && !platformCanvas.deleteContext(context)) {
                    throw new IllegalStateException("Could not delete LWJGL OpenGL context");
                }
            } finally {
                context = 0;
                gl = null;
                graphics2D = null;
                capabilities = null;
                GL.setCapabilities(null);
                try {
                    if (component != null) component.removeNotify();
                } finally {
                    super.removeNotify();
                }
            }
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
