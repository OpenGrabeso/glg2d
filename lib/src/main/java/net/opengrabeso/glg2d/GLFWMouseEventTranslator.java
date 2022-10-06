package net.opengrabeso.glg2d;

import net.opengrabeso.glg2d.event.MouseEventTranslator;

import java.awt.*;
import java.awt.event.MouseEvent;

import static org.lwjgl.glfw.GLFW.*;

public class GLFWMouseEventTranslator extends MouseEventTranslator {
    int mouseX = 0;
    int mouseY = 0;

    public GLFWMouseEventTranslator(Component target) {
        super(target);
    }
    public void onMouseButton(int button, int action, int mods) {
        int b = -1;
        switch (button) {
            //case GLFW_MOUSE_BUTTON_LEFT: b = MouseEvent.BUTTON1;break;
            case GLFW_MOUSE_BUTTON_MIDDLE: b = MouseEvent.BUTTON2;break;
            case GLFW_MOUSE_BUTTON_RIGHT: b = MouseEvent.BUTTON3;break;
        }
        int id = -1;
        switch (action) {
            case GLFW_RELEASE: id = MouseEvent.MOUSE_RELEASED;break;
            case GLFW_PRESS: id = MouseEvent.MOUSE_PRESSED;break;
        }
        if (id >=0 && b >= 0) {
            publishMouseEvent(id, System.currentTimeMillis(), mods, 0, b, new Point(mouseX, mouseY));
        }
    }

    public void onMouseMove(int x, int y) {
        mouseX = x;
        mouseY = y;
        // TODO: track modifiers
        publishMouseEvent(MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 0, 0, new Point(mouseX, mouseY));
    }
}
