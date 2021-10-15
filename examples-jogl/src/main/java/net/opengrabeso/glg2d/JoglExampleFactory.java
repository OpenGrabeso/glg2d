package net.opengrabeso.glg2d;

import net.opengrabeso.glg2d.examples.AFactory;

import javax.swing.*;

public class JoglExampleFactory implements AFactory {
    @Override
    public JComponent createPanel(JComponent drawableComponent) {
        return new GLG2DPanel(drawableComponent);
    }
}
