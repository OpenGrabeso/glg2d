package net.opengrabeso.glg2d.examples;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import com.github.opengrabeso.jaagl.GL2GL3;
import net.opengrabeso.glg2d.impl.shader.GLShaderGraphics2D;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

public class GLG2DCanvasPanelLWJGL {

    public GLG2DCanvasPanelLWJGL(JComponent component, String title) {

        GLData data = new GLData();
        data.stencilSize = 8;
        // needed for RenderDoc debugging
        data.majorVersion = 3;
        data.minorVersion = 2;
        data.profile = GLData.Profile.CORE;
        //data.samples = 4 // request MSAA

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        AWTGLCanvas canvas = new AWTGLCanvas(data) {
            private GLCapabilities caps;
            private GL2GL3 gl;

            void resizeComponent(Dimension size) {
                component.setSize(size);
                component.doLayout();
            }

            class ResizeListener extends ComponentAdapter {
                public void componentResized(ComponentEvent e) {
                    resizeComponent(e.getComponent().getSize());
                }
            }

            @Override
            public void initGL() {
                caps = GL.createCapabilities();


                if (caps.OpenGL30) {
                    gl = com.github.opengrabeso.jaagl.lwjgl.LWGL.createGL3();
                } else {
                    gl = com.github.opengrabeso.jaagl.lwjgl.LWGL.createGL2();
                }

                addComponentListener(new ResizeListener());
                resizeComponent(getSize());
            }

            @Override
            public void paintGL() {
                gl.glClearColor(1, 1, 0.5f, 1);
                gl.glClear(gl.GL_COLOR_BUFFER_BIT()|gl.GL_DEPTH_BUFFER_BIT());
                gl.glViewport(0, 0, getWidth(), getHeight());

                GLShaderGraphics2D graphics2D = new GLShaderGraphics2D(gl);
                graphics2D.prePaint(gl);
                component.paint(graphics2D);
                graphics2D.postPaint();
                swapBuffers();
            }

            @Override
            public void paint(Graphics g) {
                render();
            }

        };
        canvas.setPreferredSize(component.getPreferredSize());
        frame.getContentPane().add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void run() {

    }
}
