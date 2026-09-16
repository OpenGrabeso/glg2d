package net.opengrabeso.glg2d;

import com.github.opengrabeso.jaagl.GL2GL3;
import net.opengrabeso.glg2d.impl.shader.GLShaderGraphics2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Shared bounded rendering check, run explicitly with either native backend. */
public final class RenderingSmokeScene {
    public static final int WIDTH = 360, HEIGHT = 240;

    private RenderingSmokeScene() {}

    public static void render(GL2GL3 gl) {
        gl.glViewport(0, 0, WIDTH, HEIGHT);
        gl.glClearColor(1, 1, 1, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT() | gl.GL_DEPTH_BUFFER_BIT() | gl.GL_STENCIL_BUFFER_BIT());
        GLGraphics2D graphics = new GLShaderGraphics2D(gl);
        try {
            graphics.prePaint(gl);
            paint(graphics);
            graphics.postPaint();
        } finally {
            graphics.glDispose();
        }
        int error = gl.glGetError();
        if (error != 0) throw new AssertionError("Rendering GL error: " + error);
    }

    private static void paint(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Path2D.Double hole = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        hole.append(new Rectangle(15, 15, 95, 95), false);
        hole.append(new Rectangle(40, 40, 45, 45), false);
        g.setColor(Color.RED);
        g.fill(hole);
        Path2D.Double crossing = new Path2D.Double();
        crossing.moveTo(130, 15);
        crossing.lineTo(225, 110);
        crossing.lineTo(130, 110);
        crossing.lineTo(225, 15);
        crossing.closePath();
        g.setColor(Color.GREEN);
        g.fill(crossing);
        g.setClip(new Ellipse2D.Double(245, 15, 95, 95));
        g.setColor(Color.BLUE);
        g.fillRect(235, 5, 115, 115);
        g.setClip(null);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        g.drawString("GLG2D smoke", 15, 160);
        BufferedImage image = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig = image.createGraphics();
        ig.setColor(Color.MAGENTA);
        ig.fillRect(0, 0, 48, 48);
        ig.setColor(Color.ORANGE);
        ig.fillRect(0, 0, 24, 24);
        ig.dispose();
        g.drawImage(image, 275, 135, null);

        g.setPaint(new LinearGradientPaint(
                15, 190, 165, 190,
                new float[]{0, 0.5f, 1},
                new Color[]{Color.RED, Color.GREEN, Color.BLUE}));
        g.fillRoundRect(15, 190, 150, 35, 14, 14);

        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(190, 190), new Point2D.Float(340, 225),
                new float[]{0, 0.45f, 1},
                new Color[]{new Color(255, 255, 0, 32), new Color(255, 0, 255, 160), new Color(0, 255, 255, 255)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB,
                AffineTransform.getRotateInstance(Math.toRadians(5), 265, 207.5)));
        g.fillRoundRect(190, 190, 150, 35, 14, 14);
    }

    public static void verify(ByteBuffer pixels, File directory, String backend) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create " + directory);
        BufferedImage actual = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int p = ((HEIGHT - 1 - y) * WIDTH + x) * 4;
                actual.setRGB(x, y, ((pixels.get(p) & 255) << 16)
                        | ((pixels.get(p+1) & 255) << 8) | (pixels.get(p+2) & 255));
            }
        }
        BufferedImage expected = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D reference = expected.createGraphics();
        paint(reference);
        reference.dispose();
        ImageIO.write(actual, "png", new File(directory, backend + ".png"));
        ImageIO.write(expected, "png", new File(directory, "java2d.png"));
        int compared = 0, mismatches = 0, darkTextPixels = 0;
        for (int y = 3; y < HEIGHT-3; y++) {
            for (int x = 3; x < WIDTH-3; x++) {
                int color = expected.getRGB(x, y) & 0xffffff;
                int rendered = actual.getRGB(x, y) & 0xffffff;
                // Font rasterization differs by backend; test its presence separately.
                if (x < 260 && y >= 120 && y <= 175) {
                    if (((rendered >> 16) & 255) < 128 && ((rendered >> 8) & 255) < 128
                            && (rendered & 255) < 128) darkTextPixels++;
                    continue;
                }
                if (y >= 180) continue;
                boolean interior = true;
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        if ((expected.getRGB(x+dx, y+dy) & 0xffffff) != color) interior = false;
                    }
                }
                if (!interior) continue;
                compared++;
                if (color != rendered) mismatches++;
            }
        }
        System.out.println(backend + ": compared=" + compared + ", mismatches=" + mismatches
                + ", textPixels=" + darkTextPixels);
        assertCloseToReference(actual, expected, 45, 207, 8);
        assertCloseToReference(actual, expected, 90, 207, 8);
        assertCloseToReference(actual, expected, 145, 207, 8);
        assertCloseToReference(actual, expected, 215, 207, 10);
        assertCloseToReference(actual, expected, 265, 207, 10);
        assertCloseToReference(actual, expected, 320, 207, 10);
        if (compared < 35000 || mismatches != 0 || darkTextPixels < 100 || darkTextPixels > 2500) {
            throw new AssertionError("Rendering differs from Java2D; inspect " + directory);
        }
    }

    private static void assertCloseToReference(BufferedImage actual, BufferedImage expected,
                                               int x, int y, int tolerance) {
        int actualRgb = actual.getRGB(x, y);
        int expectedRgb = expected.getRGB(x, y);
        for (int shift : new int[]{16, 8, 0}) {
            int actualChannel = (actualRgb >> shift) & 255;
            int expectedChannel = (expectedRgb >> shift) & 255;
            if (Math.abs(actualChannel - expectedChannel) > tolerance) {
                throw new AssertionError("Gradient pixel differs at " + x + "," + y
                        + ": expected=" + Integer.toHexString(expectedRgb)
                        + ", actual=" + Integer.toHexString(actualRgb));
            }
        }
    }
}
