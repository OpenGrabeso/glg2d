package net.opengrabeso.glg2d;

import com.github.opengrabeso.jaagl.lwjgl.LWGL;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;

/** Creates an invisible GLFW context, renders one frame, saves and checks pixels, then exits. */
public final class LwjglRenderingSmoke {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("com.jogamp.opengl.GL", false, LwjglRenderingSmoke.class.getClassLoader());
            throw new AssertionError("JOGL must not be on the LWJGL smoke-test classpath");
        } catch (ClassNotFoundException expected) {
            // This is the purpose of the isolated test process.
        }
        GLFWErrorCallback callback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(callback);
        long window = 0;
        try {
            if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed");
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_STENCIL_BITS, 8);
            window = glfwCreateWindow(RenderingSmokeScene.WIDTH, RenderingSmokeScene.HEIGHT, "GLG2D smoke", 0, 0);
            if (window == 0) throw new IllegalStateException("GLFW context creation failed");
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("OpenGL: " + GL11.glGetString(GL11.GL_VERSION));
            RenderingSmokeScene.render(LWGL.createGL3());
            ByteBuffer pixels = ByteBuffer.allocateDirect(RenderingSmokeScene.WIDTH * RenderingSmokeScene.HEIGHT * 4);
            GL11.glReadPixels(0, 0, RenderingSmokeScene.WIDTH, RenderingSmokeScene.HEIGHT,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            if (GL11.glGetError() != 0) throw new AssertionError("Readback GL error");
            RenderingSmokeScene.verify(pixels, new File(args.length == 0 ? "target/render-smoke" : args[0]), "lwjgl");
        } finally {
            if (window != 0) glfwDestroyWindow(window);
            GL.setCapabilities(null);
            glfwTerminate();
            glfwSetErrorCallback(null);
            callback.free();
        }
    }
}
