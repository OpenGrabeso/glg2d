package net.opengrabeso.glg2d.examples;

import javax.swing.*;
import java.awt.*;

public class G2DExampleTexts  extends JComponent implements AnExample {
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

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paintComponent(g);
        g2d.setFont(new Font("Serif", Font.PLAIN, 20));
        g2d.drawString("Very long string ... bla bla ... ble ble ... glo glo ... xyz XYZ ... ABC DEF ... more needed 0123456789 9876543210", 10, 40);
        g2d.drawString("Do you have any work for me? bla bla bla --- bla bla bla --- bla bla bla --- bla bla bla --- bla bla XYZ 9876543210", 10, 90);
    }

}
