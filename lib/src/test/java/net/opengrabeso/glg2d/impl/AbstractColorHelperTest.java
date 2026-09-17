package net.opengrabeso.glg2d.impl;

import org.junit.Test;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbstractColorHelperTest {
    @Test
    public void recognizesMultiplyCompositeProtocol() {
        assertTrue(AbstractColorHelper.isMultiplyComposite(new MultiplyComposite()));
        assertFalse(AbstractColorHelper.isMultiplyComposite(new OtherComposite()));
        assertFalse(AbstractColorHelper.isMultiplyComposite(null));
    }

    public static final class MultiplyComposite implements Composite {
        public String getBlendMode() {
            return "multiply";
        }

        @Override
        public CompositeContext createContext(
                ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return null;
        }
    }

    public static final class OtherComposite implements Composite {
        public String getBlendMode() {
            return "other";
        }

        @Override
        public CompositeContext createContext(
                ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return null;
        }
    }
}
