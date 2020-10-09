package net.opengrabeso.opengl.text;

/**
 * Demonstrates corruption with older versions of TextRenderer. Two
 * problems: errors when punting from glyph-based renderer to
 * string-by-string renderer, and failure of glyph-based renderer when
 * backing store was NPOT using GL_ARB_texture_rectangle.
 *
 * @author emzic
 */

public class Issue326Test1 extends Issue344Base {

    protected String getText() {
        // test 1 - weird artifacts appear with a large font & long string
        return "die Marktwirtschaft. Da regelt sich � angeblich";
    }

    public static void main(final String[] args) {
        new Issue326Test1().run(args);
    }

}
