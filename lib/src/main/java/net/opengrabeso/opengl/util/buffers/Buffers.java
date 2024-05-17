package net.opengrabeso.opengl.util.buffers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Buffers {
    static public FloatBuffer newDirectFloatBuffer(int size) {
        return ByteBuffer.allocateDirect(size * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }
}
