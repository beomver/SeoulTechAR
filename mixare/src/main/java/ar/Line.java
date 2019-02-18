package ar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Beom on 2017-04-15.
 */

public class Line {
    private final static int VERTS = 2;

    private FloatBuffer mFVertexBuffer;
    private ShortBuffer mIndexBuffer;

    private float vertices[] = { 0.0f, 0.0f, 0.0f, // 0, Top
            1.0f, 0.0f, 0.0f // Bottom
    };



    public Line(float[] coords) {
        vertices = coords;

        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();

        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());

        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();

        for (int i = 0; i < VERTS; i++) {
            for (int j = 0; j < 3; j++) {
                mFVertexBuffer.put(vertices[i * 3 + j]);
            }
        }
        for (int i = 0; i < VERTS; i++) {
            mIndexBuffer.put((short) i);
        }

        mFVertexBuffer.position(0);
        mIndexBuffer.position(0);
    }

    public void draw(GL10 gl) {
        gl.glFrontFace(GL10.GL_CCW);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
        gl.glEnable(GL10.GL_TEXTURE);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glLineWidth(20.0f);
        gl.glDrawElements(GL10.GL_LINES, VERTS,
                GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
    }
}
