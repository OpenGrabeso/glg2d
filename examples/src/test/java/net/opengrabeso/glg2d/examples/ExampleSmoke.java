package net.opengrabeso.glg2d.examples;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Runs one real example in its own JVM and checks screen pixels against Java2D. */
public final class ExampleSmoke {
    private static final AtomicReference<Throwable> failure = new AtomicReference<>();
    private static JFrame frame;
    private static JComponent scene;
    private static Component surface;

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            failure.compareAndSet(null, error);
            error.printStackTrace();
        });
        try {
            File directory = new File(args[1]);
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Cannot create " + directory);
            Class.forName("net.opengrabeso.glg2d.examples." + args[0])
                    .getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            SwingUtilities.invokeAndWait(() -> {
                for (Frame candidate : Frame.getFrames()) {
                    if (candidate.isShowing()) {
                        if (frame != null) throw new AssertionError("Multiple example windows");
                        frame = (JFrame) candidate;
                    }
                }
                if (frame == null) throw new AssertionError("No example window");
                locate(frame.getContentPane());
                if (surface == scene) frame.setAlwaysOnTop(true);
                frame.toFront();
            });
            Robot robot = new Robot();
            robot.mouseMove(0, 0);
            robot.delay(900);
            BufferedImage first = capture(robot, directory, "initial");
            if (scene instanceof UIDemo || scene instanceof AWTExample) {
                robot.delay(650);
                BufferedImage next = capture(robot, directory, "animated");
                if (differentPixels(first, next) < 10) throw new AssertionError("Animation did not repaint");
            }
            SwingUtilities.invokeAndWait(() -> frame.setSize(frame.getWidth() + 100, frame.getHeight() + 80));
            robot.delay(500);
            BufferedImage resized = capture(robot, directory, "resized");
            if (resized.getWidth() <= first.getWidth() || resized.getHeight() <= first.getHeight())
                throw new AssertionError("Surface did not resize");
            if (surface.getClass().getName().equals("net.opengrabeso.glg2d.GLG2DPanelLWJGL")) {
                SwingUtilities.invokeAndWait(() -> {
                    Container parent = surface.getParent();
                    parent.remove(surface);
                    parent.add(surface);
                    frame.validate();
                });
                robot.delay(500);
                capture(robot, directory, "reattached");
            }
        } catch (Throwable error) {
            failure.compareAndSet(null, error);
            error.printStackTrace();
        } finally {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    for (Frame window : Frame.getFrames()) window.dispose();
                });
            } catch (Throwable error) {
                failure.compareAndSet(null, error);
                error.printStackTrace();
            }
        }
        System.out.println(args[0] + ": " + (failure.get() == null ? "PASS" : "FAIL"));
        System.exit(failure.get() == null ? 0 : 1);
    }

    private static void locate(Component candidate) {
        try {
            if (candidate.getClass().getName().equals("net.opengrabeso.glg2d.GLG2DPanelLWJGL")) {
                Field field = candidate.getClass().getDeclaredField("component");
                field.setAccessible(true);
                scene = (JComponent) field.get(candidate);
                surface = candidate;
            } else if (candidate.getClass().getName().equals("net.opengrabeso.glg2d.GLG2DPanel")) {
                scene = (JComponent) candidate.getClass().getMethod("getDrawableComponent").invoke(candidate);
                surface = (Component) candidate.getClass().getMethod("getGLDrawable").invoke(candidate);
            } else if (candidate instanceof AnExample) {
                scene = (JComponent) candidate;
                surface = candidate;
            } else if (candidate instanceof Container) {
                for (Component child : ((Container) candidate).getComponents()) locate(child);
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static BufferedImage capture(Robot robot, File directory, String phase) throws Exception {
        robot.waitForIdle();
        if (failure.get() != null) throw new AssertionError("Asynchronous rendering failure", failure.get());
        if (scene == null || surface == null) throw new AssertionError("No rendered scene");
        AtomicReference<Rectangle> bounds = new AtomicReference<>();
        AtomicReference<BufferedImage> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            bounds.set(new Rectangle(surface.getLocationOnScreen(), surface.getSize()));
            if (scene instanceof UIDemo || scene instanceof AWTExample) checkLayout(scene);
            BufferedImage image = new BufferedImage(scene.getWidth(), scene.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try { scene.printAll(graphics); } finally { graphics.dispose(); }
            reference.set(image);
        });
        BufferedImage actual = nativeCapture();
        if (actual == null) actual = robot.createScreenCapture(bounds.get());
        ImageIO.write(actual, "png", new File(directory, phase + ".png"));
        ImageIO.write(reference.get(), "png", new File(directory, phase + "-java2d.png"));
        verify(reference.get(), actual, scene instanceof ExampleScene);
        return actual;
    }

    private static void checkLayout(Container parent) {
        for (Component child : parent.getComponents()) {
            // CellRendererPane and hidden tabs intentionally have empty bounds.
            if (child.isVisible() && !(child instanceof CellRendererPane)) {
                boolean content = child instanceof JTree || child instanceof JTable || child instanceof JList
                        || child instanceof JScrollPane || child instanceof JSplitPane
                        || child instanceof javax.swing.text.JTextComponent || child instanceof JProgressBar;
                if (content && (child.getWidth() <= 0 || child.getHeight() <= 0))
                    throw new AssertionError("Unlaid-out " + child.getClass().getName());
                if (child instanceof Container) checkLayout((Container) child);
            }
        }
    }

    private static int distance(int a, int b) {
        return Math.max(Math.abs((a & 255) - (b & 255)), Math.max(
                Math.abs((a >> 8 & 255) - (b >> 8 & 255)), Math.abs((a >> 16 & 255) - (b >> 16 & 255))));
    }

    private static void verify(BufferedImage expected, BufferedImage actual, boolean staticScene) {
        if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight())
            throw new AssertionError("Reference dimensions differ");
        int foreground = 0, found = 0, checked = 0, mismatched = 0;
        for (int y = 2; y < expected.getHeight() - 2; y++) {
            for (int x = 2; x < expected.getWidth() - 2; x++) {
                int color = expected.getRGB(x, y);
                boolean match = false;
                for (int dy = -2; dy <= 2 && !match; dy++)
                    for (int dx = -2; dx <= 2 && !match; dx++)
                        match = distance(color, actual.getRGB(x + dx, y + dy)) < 65;
                checked++;
                if (!match) mismatched++;
                boolean foregroundPixel = staticScene ? distance(color, Color.WHITE.getRGB()) > 90
                        : Math.max(color & 255, Math.max(color >> 8 & 255, color >> 16 & 255)) < 140;
                if (foregroundPixel) {
                    foreground++;
                    if (match) found++;
                }
            }
        }
        double error = mismatched / (double) Math.max(1, checked);
        double content = found / (double) Math.max(1, foreground);
        System.out.printf("Pixels: mismatch=%.4f foreground-recall=%.4f (%d pixels)%n", error, content, foreground);
        if (foreground < 10 || content < 0.70 || error > (staticScene ? 0.04 : 0.18))
            throw new AssertionError("Rendered image differs from Java2D");
    }

    private static BufferedImage nativeCapture() throws Exception {
        AtomicReference<BufferedImage> result = new AtomicReference<>();
        if (surface.getClass().getName().equals("net.opengrabeso.glg2d.GLG2DPanelLWJGL")) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    surface.getClass().getMethod("runInContext", Runnable.class).invoke(surface, (Runnable) () -> {
                        try {
                            Class<?> gl = Class.forName("org.lwjgl.opengl.GL11");
                            int w = (int) Math.round(surface.getWidth() * surface.getGraphicsConfiguration().getDefaultTransform().getScaleX());
                            int h = (int) Math.round(surface.getHeight() * surface.getGraphicsConfiguration().getDefaultTransform().getScaleY());
                            ByteBuffer bytes = ByteBuffer.allocateDirect(w * h * 4);
                            gl.getMethod("glReadBuffer", int.class).invoke(null, 0x0404); // front after swap
                            gl.getMethod("glReadPixels", int.class, int.class, int.class, int.class, int.class, int.class, ByteBuffer.class)
                                    .invoke(null, 0, 0, w, h, 0x1908, 0x1401, bytes);
                            int error = (Integer) gl.getMethod("glGetError").invoke(null);
                            if (error != 0) throw new AssertionError("GL error: " + error);
                            result.set(fromPixels(bytes, w, h));
                        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                    });
                } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            });
        } else if (surface.getClass().getName().equals("com.jogamp.opengl.awt.GLCanvas")) {
            Class<?> listener = Class.forName("com.jogamp.opengl.GLEventListener");
            Class<?> drawableType = Class.forName("com.jogamp.opengl.GLAutoDrawable");
            CountDownLatch rendered = new CountDownLatch(1);
            Object callback = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener}, (proxy, method, args) -> {
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                if (method.getName().equals("toString")) return "Framebuffer capture";
                if (method.getName().equals("display")) {
                    Object drawable = args[0];
                    int w = (Integer) drawableType.getMethod("getSurfaceWidth").invoke(drawable);
                    int h = (Integer) drawableType.getMethod("getSurfaceHeight").invoke(drawable);
                    Object gl = drawableType.getMethod("getGL").invoke(drawable);
                    Class<?> glType = Class.forName("com.jogamp.opengl.GL");
                    ByteBuffer bytes = ByteBuffer.allocateDirect(w * h * 4);
                    glType.getMethod("glReadPixels", int.class, int.class, int.class, int.class, int.class, int.class, Buffer.class)
                            .invoke(gl, 0, 0, w, h, 0x1908, 0x1401, bytes);
                    int error = (Integer) glType.getMethod("glGetError").invoke(gl);
                    if (error != 0) throw new AssertionError("GL error: " + error);
                    result.set(fromPixels(bytes, w, h));
                    rendered.countDown();
                }
                return null;
            });
            SwingUtilities.invokeAndWait(() -> {
                try {
                    drawableType.getMethod("addGLEventListener", listener).invoke(surface, callback);
                } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            });
            try {
                if (!rendered.await(3, TimeUnit.SECONDS)) throw new AssertionError("JOGL window did not repaint");
            } finally {
                SwingUtilities.invokeAndWait(() -> {
                    try { drawableType.getMethod("removeGLEventListener", listener).invoke(surface, callback); }
                    catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                });
            }
        }
        return result.get();
    }

    private static BufferedImage fromPixels(ByteBuffer bytes, int width, int height) {
        System.out.printf("Framebuffer: %dx%d; logical scene: %dx%d; device scale: %s%n",
                width, height, scene.getWidth(), scene.getHeight(), surface.getGraphicsConfiguration().getDefaultTransform());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int offset = (y * width + x) * 4;
            int color = (bytes.get(offset) & 255) << 16 | (bytes.get(offset + 1) & 255) << 8 | bytes.get(offset + 2) & 255;
            image.setRGB(x, height - 1 - y, color);
        }
        if (width == scene.getWidth() && height == scene.getHeight()) return image;
        BufferedImage logical = new BufferedImage(scene.getWidth(), scene.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = logical.createGraphics();
        try { graphics.drawImage(image, 0, 0, logical.getWidth(), logical.getHeight(), null); }
        finally { graphics.dispose(); }
        return logical;
    }

    private static int differentPixels(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++) if (a.getRGB(x, y) != b.getRGB(x, y)) count++;
        return count;
    }
}
