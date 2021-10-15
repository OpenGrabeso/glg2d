package net.opengrabeso.glg2d;

import net.opengrabeso.glg2d.examples.AWTExample;

public class JoglAWTExample {
    public static void main(String[] args) {
        AWTExample main = new AWTExample(new JoglExampleFactory());
        main.display();
    }
}
