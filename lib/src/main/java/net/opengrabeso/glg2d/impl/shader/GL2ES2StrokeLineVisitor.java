/*
 * Copyright 2015 Brandon Borkholder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.opengrabeso.glg2d.impl.shader;


import java.awt.BasicStroke;
import java.nio.FloatBuffer;

import com.github.opengrabeso.jaagl.GL;
import com.github.opengrabeso.jaagl.GL2GL3;

import net.opengrabeso.glg2d.GLGraphics2D;
import net.opengrabeso.glg2d.impl.BasicStrokeLineVisitor;

public class GL2ES2StrokeLineVisitor extends BasicStrokeLineVisitor implements ShaderPathVisitor {
    protected GL2GL3 gl;
    protected GLGraphics2D glg2d;
    protected UniformBufferObject uniforms;

    protected AnyModePipeline pipeline;

    public GL2ES2StrokeLineVisitor(String shaderDirectory) {
        this(new AnyModePipeline(shaderDirectory));
    }

    public GL2ES2StrokeLineVisitor(AnyModePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public GLGraphics2D getGLG2D() {
        return glg2d;
    }

    @Override
    public void setGLContext(GL context, GLGraphics2D g2D, UniformBufferObject uniforms) {
        setGLContext(context, g2D);

        this.uniforms = uniforms;
    }

    @Override
    public void setGLContext(GL context, GLGraphics2D g2d) {
        gl = context.getGL2GL3();
        glg2d = g2d;

        if (!pipeline.isSetup()) {
            pipeline.setup(gl);
        }
    }

    @Override
    public void setStroke(BasicStroke stroke) {
        super.setStroke(stroke);
    }

    @Override
    public void beginPoly(int windingRule) {
        pipeline.use(gl, true);
        pipeline.setTransform(gl, uniforms.transformHook.getGLMatrixData());
        pipeline.setColor(gl, uniforms.colorHook.getRGBA());

        super.beginPoly(windingRule);
    }

    @Override
    public void endPoly() {
        super.endPoly();

        pipeline.use(gl, false);
    }

    @Override
    protected void drawBuffer() {
        FloatBuffer buf = vBuffer.getBuffer();
        if (buf.position() == 0) {
            return;
        }

        buf.flip();

        pipeline.draw(gl, gl.GL_TRIANGLE_STRIP(), buf);

        vBuffer.clear();
    }
}
