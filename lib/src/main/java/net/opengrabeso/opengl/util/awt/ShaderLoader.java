/*
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package net.opengrabeso.opengl.util.awt;

import com.github.opengrabeso.jaagl.GL2GL3;


/**
 * Utility to load shaders from files, URLs, and strings.
 *
 * <p>
 * {@code ShaderLoader} is a simple utility for loading shaders.  It takes shaders directly as
 * strings.  It will create and compile the shaders, and link them together into a program.  Both
 * compiling and linking are verified.
 *
 * <p>
 * Note it is highly recommended that if the developer passes the strings directly to {@code
 * ShaderLoader} that they contain newlines.  That way if any errors do occur their line numbers
 * will be reported correctly.  This means that if the shader is to be embedded in Java code, a
 * "\n" should be appended to every line.
 */
/*@VisibleForTesting*/
/*@ThreadSafe*/
public final class ShaderLoader {

    /**
     * Prevents instantiation.
     */
    private ShaderLoader() {
        // empty
    }

    static private final String[] prefixVertex = new String[]{
            "#version 330",
            "#define attribute in",
            "#define varying out",
            "#define texture2D texture"
    };

    static private final String[] prefixFragment = new String[]{
            "#version 330",
            "#define varying in",
            "#define gl_FragDepthEXT gl_FragDepth",
            "#define texture2D texture",
            "#define textureCube texture",
            "#define texture2DProj textureProj",
            "#define texture2DLodEXT textureLod",
            "#define texture2DProjLodEXT textureProjLod",
            "#define textureCubeLodEXT textureLod",
            "#define texture2DGradEXT textureGrad",
            "#define texture2DProjGradEXT textureProjGrad",
            "#define textureCubeGradEXT textureGrad",
            "out highp vec4 pc_fragColor;",
            "#define gl_FragColor pc_fragColor"
    };

    /** adjust shader source to avoid deprecated versions */
    static public String adjustShader(final GL2GL3 gl, int type, String source) {
        String prefixedSource = source;
        // OpenGL 3.3 has GLSL version 330 (see https://en.wikipedia.org/wiki/OpenGL_Shading_Language)
        if (gl.versionAtLeast(3, 3) && source.startsWith("#version 1")) {
            String strippedSource = source.replaceAll("#version 1..", "");
            if (type == gl.GL_FRAGMENT_SHADER()) {
                prefixedSource = String.join("\n", prefixFragment) + strippedSource;
            } else if (type == gl.GL_VERTEX_SHADER()) {
                prefixedSource = String.join("\n", prefixVertex) + strippedSource;
            }
        }
        return gl.adjustShader(prefixedSource);
    }
    /**
     * Checks that a shader was compiled correctly.
     *
     * @param gl     OpenGL context, assumed not null
     * @param shader OpenGL handle to a shader
     * @return True if shader was compiled without errors
     */
    private static boolean isShaderCompiled(/*@Nonnull*/ final GL2GL3 gl, final int shader) {
        return ShaderUtil.isShaderStatusValid(gl, shader, gl.GL_COMPILE_STATUS(), null);
    }

    /**
     * Checks that a shader program was linked successfully.
     *
     * @param gl      OpenGL context, assumed not null
     * @param program OpenGL handle to a shader program
     * @return True if program was linked successfully
     */
    private static boolean isProgramLinked(/*@Nonnull*/ final GL2GL3 gl, final int program) {
        return ShaderUtil.isProgramStatusValid(gl, program, gl.GL_LINK_STATUS());
    }

    /**
     * Checks that a shader program was validated successfully.
     *
     * @param gl      OpenGL context, assumed not null
     * @param program OpenGL handle to a shader program
     * @return True if program was validated successfully
     */
    private static boolean isProgramValidated(/*@Nonnull*/ final GL2GL3 gl, final int program) {
        return ShaderUtil.isProgramStatusValid(gl, program, gl.GL_VALIDATE_STATUS());
    }

    /**
     * Loads a shader program from a pair of strings.
     *
     * @param gl  Current OpenGL context
     * @param vss Vertex shader source
     * @param fss Fragment shader source
     * @return OpenGL handle to the shader program, not negative
     * @throws NullPointerException     if context or either source is null
     * @throws IllegalArgumentException if either source is empty
     */
    /*@Nonnegative*/
    public static int loadProgram(/*@Nonnull*/ final GL2GL3 gl,
            /*@Nonnull*/ final String vss,
            /*@Nonnull*/ final String fss) {

        // Create the shaders
        final int vs = loadShader(gl, vss, gl.GL_VERTEX_SHADER());
        final int fs = loadShader(gl, fss, gl.GL_FRAGMENT_SHADER());

        // Create a program and attach the shaders
        final int program = gl.glCreateProgram();
        gl.glAttachShader(program, vs);
        gl.glAttachShader(program, fs);

        // Link and validate the program
        gl.glLinkProgram(program);
        if (!isProgramLinked(gl, program)) {
            final String log = ShaderUtil.getProgramInfoLog(gl, program);
            throw gl.newGLException(log);
        }

        // Clean up the shaders
        gl.glDeleteShader(vs);
        gl.glDeleteShader(fs);

        return program;
    }

    public static void validateProgram(GL2GL3 gl, int program) {
        gl.glValidateProgram(program);
        if (!isProgramValidated(gl, program)) {
            final String log = ShaderUtil.getProgramInfoLog(gl, program);
            throw gl.newGLException(log);
        }

    }

    /**
     * Loads a shader from a string.
     *
     * @param gl     Current OpenGL context, assumed not null
     * @param source Source code of the shader as one long string, assumed not null or empty
     * @param type   Type of shader, assumed valid
     * @return OpenGL handle to the shader, not negative
     */
    /*@Nonnegative*/
    private static int loadShader(/*@Nonnull*/ final GL2GL3 gl,
            /*@Nonnull*/ final String source,
                                               final int type) {

        // Create and read source
        final int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, adjustShader(gl, type, source));

        // Compile
        gl.glCompileShader(shader);
        if (!isShaderCompiled(gl, shader)) {
            final String log = ShaderUtil.getShaderInfoLog(gl, shader);
            throw gl.newGLException(log);
        }

        return shader;
    }
}
