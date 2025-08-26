package net.opengrabeso.glg2d.examples;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

// adapted from https://stackoverflow.com/a/6263897/16673

public class G2DExampleComplexClip extends JComponent implements AnExample {
    @Override
    public String getTitle() {
        return "G2DExample";
    }

    private static final long serialVersionUID = 1L;

    private final int margin = 10;

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(100, 100);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 300);
    }

    private void drawCircleUsingArc(Graphics2D g2d, int x, int y, int w, int h, int segments) {
        int step = 360 / segments;
        for (int angle = 0; angle < 360; angle += step) {
            g2d.fillArc(x, y, w, h, angle, step);
        }

    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paintComponent(g);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = 140;
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(60, y - 20, 100, 100);
        g2d.setColor(Color.RED);
        g2d.fillOval(10, y + 5, 200, 50);

        int s = 10;
        Shape clipShape = new Ellipse2D.Double(60 + s, y - 20 + s, 100 - s * 2, 100 - s * 2);
        g2d.setClip(clipShape);

        g2d.setColor(Color.BLACK);
        g2d.fillOval(10, y + 5, 200, 50);


    }
}

