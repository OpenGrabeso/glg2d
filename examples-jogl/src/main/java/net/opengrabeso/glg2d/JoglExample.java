package net.opengrabeso.glg2d;

import net.opengrabeso.glg2d.examples.G2DExample;

public class JoglExample {
    public static void main(String[] args) {
        G2DExample main = new G2DExample(new JoglExampleFactory());
        main.display();
    }
}
