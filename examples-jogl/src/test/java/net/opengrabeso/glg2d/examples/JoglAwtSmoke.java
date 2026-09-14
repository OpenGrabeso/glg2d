package net.opengrabeso.glg2d.examples;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import net.opengrabeso.glg2d.GLG2DCanvas;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Explicit native smoke test of a real example's AWT pack/display/dispose path.
 * Run in a separate JVM; it briefly shows the example and exits after checking it.
 * On Windows with JDK 9+, JOGL 2.3.2 requires
 * {@code --add-exports=java.desktop/sun.awt=ALL-UNNAMED}.
 */
public final class JoglAwtSmoke {
    private static final AtomicReference<Throwable> failure = new AtomicReference<>();
    private static int rendered;

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            failure.compareAndSet(null, error);
            error.printStackTrace();
        });
        String example = args.length == 0 ? "JoglRoundCorners" : args[0];
        try {
            Class.forName("net.opengrabeso.glg2d.examples." + example)
                    .getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            SwingUtilities.invokeAndWait(() -> {
                for (Frame frame : Frame.getFrames()) {
                    if (frame.isDisplayable()) checkDisplay(frame);
                }
            });
            if (rendered != 1) throw new AssertionError("Expected one rendered panel, got " + rendered);
        } catch (Throwable error) {
            failure.compareAndSet(null, error);
            error.printStackTrace();
        } finally {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    for (Frame frame : Frame.getFrames()) frame.dispose();
                });
            } catch (Throwable error) {
                failure.compareAndSet(null, error);
                error.printStackTrace();
            }
        }
        if (failure.get() == null) {
            System.out.println(example + ": AWT pack, GL display and dispose passed");
        }
        System.exit(failure.get() == null ? 0 : 1);
    }

    private static void checkDisplay(Component component) {
        if (component instanceof GLG2DCanvas) {
            GLAutoDrawable drawable = ((GLG2DCanvas) component).getGLDrawable();
            drawable.addGLEventListener(new GLEventListener() {
                @Override public void init(GLAutoDrawable drawable) {}
                @Override public void dispose(GLAutoDrawable drawable) {}
                @Override public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
                @Override public void display(GLAutoDrawable drawable) {
                    int error = drawable.getGL().glGetError();
                    if (error != 0) throw new AssertionError("GL error after example rendering: " + error);
                    rendered++;
                }
            });
            drawable.display();
            if (!drawable.getContext().isCreated()) throw new AssertionError("No GL context");
        } else if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) checkDisplay(child);
        }
    }
}
