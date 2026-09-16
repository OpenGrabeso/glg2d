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

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;

import com.github.opengrabeso.jaagl.GL2GL3;

final class LinearGradientPipeline extends ShapePipeline {
    private static final int GRADIENT_TEXTURE_SIZE = 1024;

    private int gradientTextureLocation = -1;
    private int gradientLineLocation = -1;
    private int gradientCycleLocation = -1;
    private int gradientAlphaLocation = -1;
    private int gradientTextureScaleLocation = -1;
    private int gradientTextureOffsetLocation = -1;
    private int gradientTextureId = -1;
    private LinearGradientPaint cachedGradient;

    LinearGradientPipeline(String directory) {
        super(directory, "LinearGradientShader.v", "LinearGradientShader.f");
    }

    void setGradient(GL2GL3 gl, LinearGradientPaint gradient, float compositeAlpha) {
        if (cachedGradient != gradient) {
            uploadGradient(gl, gradient);
            cachedGradient = gradient;
        }

        Point2D start = gradient.getTransform().transform(gradient.getStartPoint(), null);
        Point2D end = gradient.getTransform().transform(gradient.getEndPoint(), null);
        float dx = (float) (end.getX() - start.getX());
        float dy = (float) (end.getY() - start.getY());

        gl.glUniform1i(gradientCycleLocation, cycleMode(gradient.getCycleMethod()));
        gl.glUniform4fv(gradientLineLocation, 1, new float[] {
                (float) start.getX(), (float) start.getY(), dx, dy
        }, 0);
        gl.glUniform1f(gradientAlphaLocation, compositeAlpha);
        gl.glUniform1f(gradientTextureScaleLocation,
                (GRADIENT_TEXTURE_SIZE - 1f) / GRADIENT_TEXTURE_SIZE);
        gl.glUniform1f(gradientTextureOffsetLocation, 0.5f / GRADIENT_TEXTURE_SIZE);
        gl.glActiveTexture(gl.GL_TEXTURE0());
        gl.glBindTexture(gl.GL_TEXTURE_2D(), gradientTextureId);
        gl.glUniform1i(gradientTextureLocation, 0);
    }

    void finishPaint(GL2GL3 gl) {
        gl.glActiveTexture(gl.GL_TEXTURE0());
        gl.glBindTexture(gl.GL_TEXTURE_2D(), 0);
    }

    private int cycleMode(MultipleGradientPaint.CycleMethod cycleMethod) {
        if (cycleMethod == MultipleGradientPaint.CycleMethod.REPEAT) {
            return 1;
        }
        if (cycleMethod == MultipleGradientPaint.CycleMethod.REFLECT) {
            return 2;
        }
        return 0;
    }

    private void uploadGradient(GL2GL3 gl, LinearGradientPaint gradient) {
        if (gradientTextureId < 0) {
            int[] ids = new int[1];
            gl.glGenTextures(ids);
            gradientTextureId = ids[0];
        }

        float[] fractions = gradient.getFractions();
        Color[] colors = gradient.getColors();
        ByteBuffer data = ByteBuffer.allocateDirect(GRADIENT_TEXTURE_SIZE * 4);
        int stop = 0;
        for (int i = 0; i < GRADIENT_TEXTURE_SIZE; i++) {
            float position = i / (GRADIENT_TEXTURE_SIZE - 1f);
            while (stop + 1 < fractions.length - 1 && position > fractions[stop + 1]) {
                stop++;
            }
            int next = Math.min(stop + 1, fractions.length - 1);
            float span = fractions[next] - fractions[stop];
            float amount = span == 0 ? 0 : (position - fractions[stop]) / span;
            amount = Math.max(0, Math.min(1, amount));
            Color a = colors[stop];
            Color b = colors[next];
            data.put(interpolate(a.getRed(), b.getRed(), amount));
            data.put(interpolate(a.getGreen(), b.getGreen(), amount));
            data.put(interpolate(a.getBlue(), b.getBlue(), amount));
            data.put(interpolate(a.getAlpha(), b.getAlpha(), amount));
        }
        data.flip();

        gl.glActiveTexture(gl.GL_TEXTURE0());
        gl.glBindTexture(gl.GL_TEXTURE_2D(), gradientTextureId);
        gl.glTexParameteri(gl.GL_TEXTURE_2D(), gl.GL_TEXTURE_MIN_FILTER(), gl.GL_LINEAR());
        gl.glTexParameteri(gl.GL_TEXTURE_2D(), gl.GL_TEXTURE_MAG_FILTER(), gl.GL_LINEAR());
        gl.glTexParameteri(gl.GL_TEXTURE_2D(), gl.GL_TEXTURE_WRAP_S(), gl.GL_CLAMP_TO_EDGE());
        gl.glTexParameteri(gl.GL_TEXTURE_2D(), gl.GL_TEXTURE_WRAP_T(), gl.GL_CLAMP_TO_EDGE());
        gl.glTexImage2D(gl.GL_TEXTURE_2D(), 0, gl.GL_RGBA(), GRADIENT_TEXTURE_SIZE, 1,
                0, gl.GL_RGBA(), gl.GL_UNSIGNED_BYTE(), data);
    }

    private byte interpolate(int a, int b, float amount) {
        return (byte) Math.round(a + (b - a) * amount);
    }

    @Override
    protected void setupUniformsAndAttributes(GL2GL3 gl) {
        super.setupUniformsAndAttributes(gl);
        gradientTextureLocation = gl.glGetUniformLocation(programId, "u_gradient");
        gradientLineLocation = gl.glGetUniformLocation(programId, "u_gradientLine");
        gradientCycleLocation = gl.glGetUniformLocation(programId, "u_gradientCycle");
        gradientAlphaLocation = gl.glGetUniformLocation(programId, "u_gradientAlpha");
        gradientTextureScaleLocation = gl.glGetUniformLocation(programId, "u_gradientTextureScale");
        gradientTextureOffsetLocation = gl.glGetUniformLocation(programId, "u_gradientTextureOffset");
    }

    @Override
    public void delete(GL2GL3 gl) {
        super.delete(gl);
        if (gradientTextureId >= 0) {
            gl.glDeleteTextures(new int[]{gradientTextureId});
            gradientTextureId = -1;
            cachedGradient = null;
        }
    }
}
