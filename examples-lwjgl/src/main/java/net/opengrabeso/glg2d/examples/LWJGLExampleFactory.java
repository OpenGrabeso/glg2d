package net.opengrabeso.glg2d.examples;

import net.opengrabeso.glg2d.GLG2DPanelLWJGL;

import javax.swing.*;
import java.util.Arrays;

public class LWJGLExampleFactory {
    {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    public static void display(String[] args, JComponent component) {
        String title = ((AnExample) component).getTitle();
        if (!Arrays.asList(args).contains("-glfw")) {
            new GLG2DCanvasPanelLWJGL(component, title).run();
        } else {
            new GLG2DPanelLWJGL(component, title).run();
        }
    }
}

