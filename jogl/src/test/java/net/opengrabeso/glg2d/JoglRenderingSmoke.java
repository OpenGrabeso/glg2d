package net.opengrabeso.glg2d;

import com.github.opengrabeso.jaagl.jogl.JoGL;
import com.jogamp.opengl.*;

import java.io.File;
import java.nio.ByteBuffer;

/** Offscreen counterpart of the LWJGL smoke test, using the optional JOGL adapter. */
public final class JoglRenderingSmoke {
    public static void main(String[] args) throws Exception {
        GLProfile profile = GLProfile.get(GLProfile.GL3);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setOnscreen(false);
        caps.setFBO(true);
        caps.setDoubleBuffered(false);
        caps.setStencilBits(8);
        GLOffscreenAutoDrawable drawable = GLDrawableFactory.getFactory(profile)
                .createOffscreenAutoDrawable(null, caps, null, RenderingSmokeScene.WIDTH, RenderingSmokeScene.HEIGHT);
        try {
            drawable.display();
            if (drawable.getContext().makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
                throw new IllegalStateException("JOGL context is not current");
            }
            try {
                GL3 gl = drawable.getGL().getGL3();
                System.out.println("OpenGL: " + gl.glGetString(GL.GL_VERSION));
                RenderingSmokeScene.render(JoGL.wrap(gl));
                ByteBuffer pixels = ByteBuffer.allocateDirect(RenderingSmokeScene.WIDTH * RenderingSmokeScene.HEIGHT * 4);
                gl.glReadPixels(0, 0, RenderingSmokeScene.WIDTH, RenderingSmokeScene.HEIGHT,
                        GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixels);
                if (gl.glGetError() != 0) throw new AssertionError("Readback GL error");
                RenderingSmokeScene.verify(pixels, new File(args.length == 0 ? "target/render-smoke" : args[0]), "jogl");
            } finally {
                drawable.getContext().release();
            }
        } finally {
            drawable.destroy();
        }
    }
}
