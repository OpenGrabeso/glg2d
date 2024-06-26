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

public class GL2ES2SimpleConvexFillVisitor extends SimplePathVisitor implements ShaderPathVisitor {
    protected GL2GL3 gl;
    protected GLGraphics2D glg2d;
    protected UniformBufferObject uniforms;

    protected VertexBuffer vBuffer = new VertexBuffer(1024);

    protected AnyModePipeline pipeline;

    public GL2ES2SimpleConvexFillVisitor(String shaderDirectory) {
        this(new AnyModePipeline(shaderDirectory));
    }

    public GL2ES2SimpleConvexFillVisitor(AnyModePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void setGLContext(GL glContext, GLGraphics2D g2D, UniformBufferObject uniforms) {
        setGLContext(glContext, g2D);

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
    public GLGraphics2D getGLG2D() {
        return glg2d;
    }

    @Override
    public void setStroke(BasicStroke stroke) {
        // nop
    }

    @Override
    public void beginPoly(int windingRule) {
        // do we need to care about winding rule?
        pipeline.use(gl, true);

        pipeline.setColor(gl, uniforms.colorHook.getRGBA());
        pipeline.setTransform(gl, uniforms.transformHook.getGLMatrixData());

        vBuffer.clear();
        vBuffer.addVertex(0, 0);  // reserve space for a future centroid
    }

    @Override
    public void moveTo(float[] vertex) {
        draw();

        vBuffer.addVertex(vertex[0], vertex[1]);
    }

    @Override
    public void lineTo(float[] vertex) {
        vBuffer.addVertex(vertex[0], vertex[1]);
    }

    @Override
    public void closeLine() {
        FloatBuffer buf = vBuffer.getBuffer();
        float x = buf.get(2);
        float y = buf.get(3);
        vBuffer.addVertex(x, y);
    }

    @Override
    public void endPoly() {
        draw();
        pipeline.use(gl, false);
    }

    protected void draw() {
        FloatBuffer buf = vBuffer.getBuffer();
        if (buf.position() <= 2) {
            buf.position(2);
            return;
        }

        buf.flip();

        setupCentroid(buf);

        pipeline.draw(gl, gl.GL_TRIANGLE_FAN(), buf);

        vBuffer.clear();
        vBuffer.addVertex(0, 0); // reserve space for a future centroid
    }

    protected void setupCentroid(FloatBuffer vertexBuffer) {
        float x = 0;
        float y = 0;

        vertexBuffer.position(2);
        int numPts = 0;
        while (vertexBuffer.position() < vertexBuffer.limit()) {
            x += vertexBuffer.get();
            y += vertexBuffer.get();
            numPts++;
        }

        vertexBuffer.rewind();
        vertexBuffer.put(x / numPts);
        vertexBuffer.put(y / numPts);

        vertexBuffer.rewind();
    }
}
