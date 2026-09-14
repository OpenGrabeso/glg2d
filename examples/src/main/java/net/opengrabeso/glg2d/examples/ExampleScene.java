package net.opengrabeso.glg2d.examples;

import java.awt.*;
import javax.swing.JComponent;

/** An opaque demonstration scene with identical Java2D and OpenGL defaults. */
public abstract class ExampleScene extends JComponent implements AnExample {
    protected ExampleScene() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }

    @Override
    public final void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setColor(getBackground());
            copy.fillRect(0, 0, getWidth(), getHeight());
            copy.setColor(getForeground());
            paintScene(copy);
        } finally {
            copy.dispose();
        }
    }

    protected abstract void paintScene(Graphics2D graphics);
}
