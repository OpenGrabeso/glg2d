package net.opengrabeso.glg2d.examples;

import javax.swing.*;

public interface AnExample {
    String getTitle();
    default void startAnimation() {}
    default void stopAnimation() {}
}
