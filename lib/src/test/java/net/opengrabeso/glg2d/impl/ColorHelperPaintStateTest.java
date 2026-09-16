package net.opengrabeso.glg2d.impl;

import net.opengrabeso.glg2d.GLGraphics2D;
import org.junit.Test;

import java.awt.Color;
import java.awt.Composite;
import java.awt.LinearGradientPaint;
import java.awt.Paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ColorHelperPaintStateTest {
    private final TestColorHelper helper = new TestColorHelper();

    @Test
    public void colorIsAlsoTheCurrentPaintAndNullPaintIsIgnored() {
        helper.setG2D(null);
        helper.setColor(Color.RED);
        assertEquals(Color.RED, helper.getPaint());

        helper.setPaint(null);
        assertEquals(Color.RED, helper.getPaint());
    }

    @Test
    public void gradientPaintSurvivesPushAndPop() {
        helper.setG2D(null);
        LinearGradientPaint gradient = new LinearGradientPaint(
                0, 0, 100, 0,
                new float[] {0, 0.5f, 1},
                new Color[] {Color.RED, Color.GREEN, Color.BLUE});
        helper.setPaint(gradient);

        helper.push(null);
        helper.setColor(Color.BLACK);
        assertEquals(Color.BLACK, helper.getPaint());

        helper.pop(null);
        assertSame(gradient, helper.getPaint());
    }

    private static final class TestColorHelper extends AbstractColorHelper {
        @Override public void setColorNoRespectComposite(Color color) {}
        @Override public void setColorRespectComposite(Color color) {}
        @Override public void setComposite(Composite composite) {}
        @Override public void setPaintMode() {}
        @Override public void setXORMode(Color color) {}
        @Override public void copyArea(int x, int y, int width, int height, int dx, int dy) {}
    }
}
