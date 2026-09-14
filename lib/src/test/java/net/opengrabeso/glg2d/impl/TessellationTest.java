package net.opengrabeso.glg2d.impl;

import net.opengrabeso.glg2d.GLGraphics2D;
import net.opengrabeso.glg2d.impl.tessellation.GLU;
import org.junit.Test;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Geometry checks exercise the real visitor without creating a GL context. */
public class TessellationTest {
    @Test public void concavePolygon() {
        verify(path(Path2D.WIND_NON_ZERO, new double[]{0,0, 12,0, 12,4, 4,4, 4,12, 0,12}));
    }

    @Test public void holeWithEitherWindingRule() {
        for (int rule : new int[]{Path2D.WIND_NON_ZERO, Path2D.WIND_EVEN_ODD}) {
            verify(path(rule, outer(), new double[]{3,3, 3,9, 9,9, 9,3}));
        }
    }

    @Test public void nestedContoursWithBothOrientations() {
        for (int rule : new int[]{Path2D.WIND_NON_ZERO, Path2D.WIND_EVEN_ODD}) {
            for (double[] inner : new double[][]{
                    {3,3, 9,3, 9,9, 3,9}, {3,3, 3,9, 9,9, 9,3}}) {
                verify(path(rule, outer(), inner, new double[]{5,5, 7,5, 7,7, 5,7}));
            }
        }
    }

    @Test public void selfIntersectionCreatesCombinedVertex() {
        for (int rule : new int[]{Path2D.WIND_NON_ZERO, Path2D.WIND_EVEN_ODD}) {
            verify(path(rule, new double[]{0,0, 12,12, 0,12, 12,0}));
        }
    }

    @Test public void repeatedAndCollinearVertices() {
        verify(path(Path2D.WIND_NON_ZERO, new double[]{0,0, 6,0, 6,0, 12,0, 12,12, 0,12, 0,0}));
    }

    @Test public void emptyAndDegeneratePaths() {
        verify(new Path2D.Double());
        verify(path(Path2D.WIND_NON_ZERO, new double[]{2,2}));
        verify(path(Path2D.WIND_NON_ZERO, new double[]{0,0, 6,0, 12,0}));
    }

    @Test public void openContoursAndRepeatedVisitorUse() {
        Collector visitor = new Collector();
        Path2D.Double open = path(Path2D.WIND_NON_ZERO, outer());
        Path2D.Double unclosed = new Path2D.Double();
        unclosed.moveTo(0,0);
        unclosed.lineTo(12,0);
        unclosed.lineTo(12,12);
        unclosed.lineTo(0,12);
        verify(unclosed, visitor);
        verify(open, visitor);
        verify(new Path2D.Double(), visitor);
    }

    @Test public void tessellationErrorUsesJavaException() {
        Collector visitor = new Collector();
        visitor.beginPoly(Path2D.WIND_NON_ZERO);
        try {
            visitor.tesselator.gluTessProperty(-1, 0);
            fail("Expected a tessellation error");
        } catch (IllegalStateException error) {
            assertTrue(error.getMessage().contains("invalid enumerant"));
        } finally {
            visitor.endPoly();
        }
        verify(path(Path2D.WIND_NON_ZERO, outer()), visitor);
    }

    private static double[] outer() { return new double[]{0,0, 12,0, 12,12, 0,12}; }

    private static Path2D.Double path(int rule, double[]... contours) {
        Path2D.Double path = new Path2D.Double(rule);
        for (double[] c : contours) {
            path.moveTo(c[0], c[1]);
            for (int i = 2; i < c.length; i += 2) path.lineTo(c[i], c[i + 1]);
            path.closePath();
        }
        return path;
    }

    private static void verify(Path2D.Double path) { verify(path, new Collector()); }

    private static void verify(Path2D.Double path, Collector visitor) {
        visitor.triangles.clear();
        visitor.beginPoly(path.getWindingRule());
        float[] coordinates = new float[6];
        for (PathIterator it = path.getPathIterator(null); !it.isDone(); it.next()) {
            switch (it.currentSegment(coordinates)) {
                case PathIterator.SEG_MOVETO: visitor.moveTo(coordinates); break;
                case PathIterator.SEG_LINETO: visitor.lineTo(coordinates); break;
                case PathIterator.SEG_CLOSE: visitor.closeLine(); break;
                default: fail("These fixtures contain only straight segments");
            }
        }
        visitor.endPoly();
        Area expected = new Area(path);
        Area actual = new Area();
        double totalTriangleArea = 0;
        for (Path2D.Double triangle : visitor.triangles) {
            Area part = new Area(triangle);
            totalTriangleArea += area(part);
            actual.add(part);
        }
        assertEquals("Triangle area, including overlaps", area(expected), totalTriangleArea, 1e-6);
        Area difference = new Area(expected);
        difference.exclusiveOr(actual);
        assertTrue("Triangulation must cover exactly the Java2D area", difference.isEmpty());
        for (double y = -1.29; y < 14; y += .53) {
            for (double x = -1.17; x < 14; x += .47) {
                assertEquals("Coverage at " + x + "," + y, expected.contains(x, y), actual.contains(x, y));
            }
        }
    }

    private static double area(Area area) {
        double[] c = new double[6];
        double startX = 0, startY = 0, x = 0, y = 0, sum = 0;
        for (PathIterator it = area.getPathIterator(null); !it.isDone(); it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO: startX = x = c[0]; startY = y = c[1]; break;
                case PathIterator.SEG_LINETO: sum += x*c[1] - c[0]*y; x = c[0]; y = c[1]; break;
                case PathIterator.SEG_CLOSE: sum += x*startY - startX*y; break;
                default: throw new AssertionError("Unexpected curve");
            }
        }
        return Math.abs(sum) / 2;
    }

    private static class Collector extends AbstractTesselatorVisitor {
        final List<Path2D.Double> triangles = new ArrayList<>();

        @Override public GLGraphics2D getGLG2D() { return null; }
        @Override public void setGLContext(com.github.opengrabeso.jaagl.GL gl, GLGraphics2D graphics) {}

        @Override protected void endTess() {
            FloatBuffer vertices = vBuffer.getBuffer().duplicate();
            vertices.flip();
            int count = vertices.remaining() / 2;
            if (drawMode == GLU.GL_TRIANGLES) {
                for (int i = 0; i < count; i += 3) add(vertices, i, i+1, i+2);
            } else if (drawMode == GLU.GL_TRIANGLE_FAN) {
                for (int i = 2; i < count; i++) add(vertices, 0, i-1, i);
            } else if (drawMode == GLU.GL_TRIANGLE_STRIP) {
                for (int i = 2; i < count; i++) add(vertices, i-2, i-1, i);
            } else {
                fail("Unexpected tessellation primitive " + drawMode);
            }
        }

        private void add(FloatBuffer b, int a, int c, int d) {
            triangles.add(path(Path2D.WIND_NON_ZERO, new double[]{
                    b.get(a*2), b.get(a*2+1), b.get(c*2), b.get(c*2+1), b.get(d*2), b.get(d*2+1)}));
        }
    }
}
