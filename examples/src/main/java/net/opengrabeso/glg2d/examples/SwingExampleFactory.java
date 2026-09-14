package net.opengrabeso.glg2d.examples;

import javax.swing.*;
import java.util.function.Supplier;

public class SwingExampleFactory {
    /** Constructs the entire Swing hierarchy on the event dispatch thread. */
    public static void display(Supplier<? extends JComponent> factory) {
        SwingUtilities.invokeLater(() -> display(factory.get()));
    }

    public static void display(JComponent component) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> display(component));
            return;
        }
        String title = ((AnExample) component).getTitle();
        JFrame frame = new JFrame("Swing - " + title);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        frame.setContentPane(component);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        AnExample example = (AnExample) component;
        example.startAnimation();
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) { example.stopAnimation(); }
        });
    }
}