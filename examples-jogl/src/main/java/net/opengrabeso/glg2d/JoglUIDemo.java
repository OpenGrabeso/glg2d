package net.opengrabeso.glg2d;

import net.opengrabeso.glg2d.examples.UIDemoFrame;

public class JoglUIDemo {
    public static void main(String[] args) {
        UIDemoFrame main = new UIDemoFrame(new JoglExampleFactory());
        try {
            main.display();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
