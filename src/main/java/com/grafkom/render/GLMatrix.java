package com.grafkom.render;

import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4f;

/** Jembatan JOML -> OpenGL fixed-function (matriks kolom-mayor). */
public final class GLMatrix {
    private static final float[] BUFFER = new float[16];
    private static final Matrix4f CURRENT_PROJECTION = new Matrix4f();   // BARU
    private static final Matrix4f CURRENT_MODELVIEW = new Matrix4f();    // BARU

    private GLMatrix() { }

    public static void loadProjection(Matrix4f m) {
        CURRENT_PROJECTION.set(m);                                       // BARU
        glMatrixMode(GL_PROJECTION);
        m.get(BUFFER);
        glLoadMatrixf(BUFFER);
        glMatrixMode(GL_MODELVIEW);
    }

    public static void loadModelView(Matrix4f m) {
        CURRENT_MODELVIEW.set(m);                                        // BARU
        glMatrixMode(GL_MODELVIEW);
        m.get(BUFFER);
        glLoadMatrixf(BUFFER);
    }

    public static void multModelView(Matrix4f m) {
        m.get(BUFFER);
        glMultMatrixf(BUFFER);
    }

    /** Matriks proyeksi yang terakhir dimuat (untuk uniform shader). */   // BARU
    public static Matrix4f currentProjection() { return CURRENT_PROJECTION; }

    /** Matriks view yang terakhir dimuat (untuk uniform shader). */       // BARU
    public static Matrix4f currentModelView()  { return CURRENT_MODELVIEW; }
}