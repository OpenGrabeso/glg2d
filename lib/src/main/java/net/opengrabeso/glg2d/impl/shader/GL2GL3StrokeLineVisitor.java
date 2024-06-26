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
import net.opengrabeso.glg2d.VertexBuffer;
import net.opengrabeso.glg2d.impl.SimplePathVisitor;

public class GL2GL3StrokeLineVisitor extends SimplePathVisitor implements ShaderPathVisitor {
    protected VertexBuffer buffer = new VertexBuffer(1024);

    protected BasicStroke stroke;

    protected float[] lastV = new float[2];

    protected GL2GL3 gl;
    protected GLGraphics2D glg2d;
    protected UniformBufferObject uniforms;

    protected GeometryShaderStrokePipeline pipeline;

    public GL2GL3StrokeLineVisitor(String shaderDirectory) {
        this(new GeometryShaderStrokePipeline(shaderDirectory));
    }

    public GL2GL3StrokeLineVisitor(GeometryShaderStrokePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public GLGraphics2D getGLG2D() {
        return glg2d;
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
    public void setGLContext(GL glContext, GLGraphics2D g2D, UniformBufferObject uniforms) {
        setGLContext(glContext, g2D);
        this.uniforms = uniforms;
    }

    @Override
    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
    }

    @Override
    public void moveTo(float[] vertex) {
        draw(false);

        lastV[0] = vertex[0];
        lastV[1] = vertex[1];
        buffer.addVertex(vertex[0], vertex[1]);
    }

    @Override
    public void lineTo(float[] vertex) {
        // no 0-length lines
        if (vertex[0] == lastV[0] && vertex[1] == lastV[1]) {
            return;
        }

        buffer.addVertex(vertex, 0, 1);
        lastV[0] = vertex[0];
        lastV[1] = vertex[1];
    }

    @Override
    public void closeLine() {
        /*
         * Sometimes shapes will set the last point to be the same as the first and
         * then close the line. That can be confusing. So we discard the last point
         * if it's the same as the first. Now no 2 consecutive points are the same.
         */
        FloatBuffer buf = buffer.getBuffer();
        if (buf.get(0) == lastV[0] && buf.get(1) == lastV[1]) {
            buf.position(buf.position() - 2);
        }

        draw(true);
    }

    @Override
    public void beginPoly(int windingRule) {
        pipeline.use(gl, true);

        pipeline.setColor(gl, uniforms.colorHook.getRGBA());
        pipeline.setTransform(gl, uniforms.transformHook.getGLMatrixData());
        pipeline.setStroke(gl, stroke);

        buffer.clear();
    }

    @Override
    public void endPoly() {
        draw(false);
        pipeline.use(gl, false);
    }

    protected void draw(boolean close) {
        FloatBuffer buf = buffer.getBuffer();
        if (buf.position() == 0) {
            return;
        }

        buf.flip();
        pipeline.draw(gl, buf, close);

        buffer.clear();
    }
}
