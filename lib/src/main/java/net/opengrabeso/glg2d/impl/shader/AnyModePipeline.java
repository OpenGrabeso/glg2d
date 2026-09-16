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

import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.nio.FloatBuffer;

import com.github.opengrabeso.jaagl.GL2GL3;

/** Selects one shader program for the complete shape draw call. */
public class AnyModePipeline {
    private final SolidColorPipeline solidPipeline;
    private final LinearGradientPipeline gradientPipeline;
    private ShapePipeline selectedPipeline;

    public AnyModePipeline(String shaderDirectory) {
        this(shaderDirectory, "FixedFuncShader.v", "FixedFuncShader.f");
    }

    public AnyModePipeline(String directory, String vertexShaderFileName, String fragmentShaderFileName) {
        solidPipeline = new SolidColorPipeline(directory, vertexShaderFileName, fragmentShaderFileName);
        gradientPipeline = new LinearGradientPipeline(directory);
        selectedPipeline = solidPipeline;
    }

    public void setup(GL2GL3 gl) {
        solidPipeline.setup(gl);
        gradientPipeline.setup(gl);
    }

    public boolean isSetup() {
        return solidPipeline.isSetup() && gradientPipeline.isSetup();
    }

    public void setPaint(GL2GL3 gl, Paint paint, float[] solidColor, float compositeAlpha) {
        if (paint instanceof LinearGradientPaint
                && ((LinearGradientPaint) paint).getColorSpace() == MultipleGradientPaint.ColorSpaceType.SRGB) {
            selectedPipeline = gradientPipeline;
            selectedPipeline.use(gl, true);
            gradientPipeline.setGradient(gl, (LinearGradientPaint) paint, compositeAlpha);
        } else {
            selectedPipeline = solidPipeline;
            selectedPipeline.use(gl, true);
            solidPipeline.setColor(gl, solidColor);
        }
    }

    public void setTransform(GL2GL3 gl, float[] glMatrixData) {
        selectedPipeline.setTransform(gl, glMatrixData);
    }

    public void draw(GL2GL3 gl, int mode, FloatBuffer vertexBuffer) {
        selectedPipeline.draw(gl, mode, vertexBuffer);
    }

    public void finishPaint(GL2GL3 gl) {
        if (selectedPipeline == gradientPipeline) {
            gradientPipeline.finishPaint(gl);
        }
        selectedPipeline.use(gl, false);
    }

    public void delete(GL2GL3 gl) {
        solidPipeline.delete(gl);
        gradientPipeline.delete(gl);
        selectedPipeline = solidPipeline;
    }
}
