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
import java.awt.Shape;
import java.awt.Stroke;

import com.github.opengrabeso.jaagl.GL;

import net.opengrabeso.glg2d.GLGraphics2D;
import net.opengrabeso.glg2d.PathVisitor;
import net.opengrabeso.glg2d.impl.AbstractShapeHelper;
import net.opengrabeso.glg2d.impl.SimpleOrTesselatingVisitor;

public class GL2ES2ShapeDrawer extends AbstractShapeHelper {
    protected ShaderPathVisitor lineVisitor;
    protected ShaderPathVisitor simpleFillVisitor;
    protected ShaderPathVisitor tesselatingVisitor;
    protected PathVisitor complexFillVisitor;

    public GL2ES2ShapeDrawer(String shaderDirectory) {
        lineVisitor = new GL2ES2StrokeLineVisitor(shaderDirectory);
        simpleFillVisitor = new GL2ES2SimpleConvexFillVisitor(shaderDirectory);
        tesselatingVisitor = new GL2ES2TesselatingVisitor(shaderDirectory);
        complexFillVisitor = new SimpleOrTesselatingVisitor(simpleFillVisitor, tesselatingVisitor);
    }

    @Override
    public void setG2D(GLGraphics2D g2d) {
        super.setG2D(g2d);

        if (g2d instanceof GLShaderGraphics2D) {
            GL gl = g2d.getGL();
            UniformBufferObject uniforms = ((GLShaderGraphics2D) g2d).getUniformsObject();

            lineVisitor.setGLContext(gl, g2d, uniforms);
            simpleFillVisitor.setGLContext(gl, g2d, uniforms);
            tesselatingVisitor.setGLContext(gl, g2d, uniforms);
            complexFillVisitor.setGLContext(gl, g2d);
        } else {
            throw new IllegalArgumentException(GLGraphics2D.class.getName() + " implementation must be instance of "
                    + GLShaderGraphics2D.class.getSimpleName());
        }
    }

    public void draw(Shape shape) {
        Stroke stroke = getStroke();
        if (stroke instanceof BasicStroke) {
            lineVisitor.setStroke((BasicStroke) stroke);
            traceShape(shape, lineVisitor);
        } else {
            fill(stroke.createStrokedShape(shape), false);
        }
    }

    @Override
    protected void fill(Shape shape, boolean isDefinitelySimpleConvex) {
        if (isDefinitelySimpleConvex) {
            traceShape(shape, simpleFillVisitor);
        } else {
            traceShape(shape, complexFillVisitor);
        }
    }
}
