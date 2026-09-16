package net.opengrabeso.glg2d.impl.shader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Scanner;

import org.junit.Test;

public class ShaderResourceSeparationTest {
    @Test
    public void solidShaderHasNoGradientBranch() {
        for (String directory : new String[]{"gl2/", "gl3/"}) {
            String vertex = resource(directory + "FixedFuncShader.v");
            String fragment = resource(directory + "FixedFuncShader.f");

            assertFalse(vertex.contains("v_userCoord"));
            assertFalse(fragment.contains("u_paintMode"));
            assertFalse(fragment.contains("sampler2D"));
            assertTrue(fragment.contains("u_color"));
        }
    }

    @Test
    public void gradientShaderUsesDedicatedResources() {
        for (String directory : new String[]{"gl2/", "gl3/"}) {
            String vertex = resource(directory + "LinearGradientShader.v");
            String fragment = resource(directory + "LinearGradientShader.f");

            assertTrue(vertex.contains("v_userCoord"));
            assertTrue(fragment.contains("sampler2D u_gradient"));
            assertTrue(fragment.contains("u_gradientCycle"));
            assertFalse(fragment.contains("u_paintMode"));
        }
    }

    private String resource(String name) {
        InputStream stream = AnyModePipeline.class.getResourceAsStream(name);
        assertNotNull(name, stream);
        Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        try {
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            scanner.close();
        }
    }
}
