package net.opengrabeso.opengl;

import com.jogamp.opengl.awt.GLCanvas;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Optional provider for the shared interactive text scenarios. */
public final class JoglTestBackend implements JaaglTestBackend {
    @Override public String name() { return "jogl"; }

    @Override public void run(Jaagl2EventListener jaaglListener) {
        final Frame frame = new Frame(getClass().getName());
        frame.setLayout(new BorderLayout());

        final GLCanvas canvas = new GLCanvas();
        Jaagl2EventListenerJogl listenerJogl = new Jaagl2EventListenerJogl(jaaglListener);

        canvas.addGLEventListener(listenerJogl);
        frame.add(canvas, BorderLayout.CENTER);

        frame.setSize(512, 512);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(final WindowEvent e) {
                new Thread(new Runnable() {
                    public void run() {
                        System.exit(0);
                    }
                }).start();
            }
        });
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(true);
                }
            });
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
