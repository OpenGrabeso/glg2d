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


import java.nio.FloatBuffer;

import com.github.opengrabeso.jaagl.GL;
import com.github.opengrabeso.jaagl.GL2GL3;

import net.opengrabeso.glg2d.GLGraphics2D;
import net.opengrabeso.glg2d.impl.AbstractTesselatorVisitor;

public class GL2ES2TesselatingVisitor extends AbstractTesselatorVisitor implements ShaderPathVisitor {
    protected GL2GL3 gl;
    protected GLGraphics2D glg2d;
    protected UniformBufferObject uniforms;

    protected AnyModePipeline pipeline;

    public GL2ES2TesselatingVisitor(String shaderDirectory) {
        this(new AnyModePipeline(shaderDirectory));
    }

    public GL2ES2TesselatingVisitor(AnyModePipeline pipeline) {
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
    public void beginPoly(int windingRule) {
        super.beginPoly(windingRule);

        pipeline.setPaint(gl, glg2d.getPaint(), uniforms.colorHook.getRGBA(), uniforms.colorHook.getAlpha());
        pipeline.setTransform(gl, uniforms.transformHook.getGLMatrixData());
    }

    @Override
    public void endPoly() {
        super.endPoly();

        pipeline.finishPaint(gl);
    }

    @Override
    protected void endTess() {
        FloatBuffer buf = vBuffer.getBuffer();
        buf.flip();

        pipeline.draw(gl, drawMode, buf);
    }

    @Override
    public void dispose() {
        if (gl != null) {
            pipeline.delete(gl);
            gl = null;
        }
    }
}
