package net.opengrabeso.opengl.text;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.awt.TextRenderer;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Another test case demonstrating corruption with older version of
 * TextRenderer when glyphs were too big for backing store. Font and
 * text courtesy of Patrick Murris. Adapted from Issue326Test1.
 */

public class Issue326Test2 extends Issue344Base {

    protected String getText() {
        return "LA CLAPI\u00c8RE \nAlt: 1100-1700m \nGlissement de terrain majeur";
    }

    public static void main(final String[] args) {
        new Issue326Test2().run(args);
    }
}

