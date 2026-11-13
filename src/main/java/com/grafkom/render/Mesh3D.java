package com.grafkom.render;

import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4f;

/**
 * Model 3D (Color Cube) yang didefinisikan sebagai kumpulan vertex + face.
 * Posisi & skala disimpan sebagai model matrix.
 */
public class Mesh3D {
    private final float[][] vertices;   // [v][x,y,z]
    private final int[][] faces;        // [f][4 indeks vertex]
    private final float[][] faceColors; // [f][r,g,b]
    private final Matrix4f model = new Matrix4f();

    public Mesh3D(float[][] vertices, int[][] faces, float[][] faceColors) {
        this.vertices = vertices;
        this.faces = faces;
        this.faceColors = faceColors;
    }

    /** Kubus berwarna dengan sisi 2*halfSize, berpusat di titik asal. */
    public static Mesh3D createColorCube(float halfSize) {
        float s = halfSize;
        float[][] v = {
            {-s, -s,  s}, { s, -s,  s}, { s,  s,  s}, {-s,  s,  s},   // 0-3 depan (z+)
            {-s, -s, -s}, { s, -s, -s}, { s,  s, -s}, {-s,  s, -s}    // 4-7 belakang (z-)
        };
        int[][] f = {
            {0, 1, 2, 3},   // depan
            {5, 4, 7, 6},   // belakang
            {4, 0, 3, 7},   // kiri
            {1, 5, 6, 2},   // kanan
            {3, 2, 6, 7},   // atas
            {4, 5, 1, 0}    // bawah
        };
        float[][] c = {
            {0.90f, 0.20f, 0.20f},  // merah
            {0.95f, 0.55f, 0.10f},  // oranye
            {0.20f, 0.70f, 0.30f},  // hijau
            {0.20f, 0.45f, 0.90f},  // biru
            {0.95f, 0.85f, 0.20f},  // kuning
            {0.65f, 0.30f, 0.85f}   // ungu
        };
        return new Mesh3D(v, f, c);
    }

    public Mesh3D setPosition(float x, float y, float z) {
        model.identity().translate(x, y, z).scale(currentScale);
        this.posX = x; this.posY = y; this.posZ = z;
        return this;
    }

    public Mesh3D setUniformScale(float s) {
        this.currentScale = s;
        model.identity().translate(posX, posY, posZ).scale(s);
        return this;
    }

    private float posX, posY, posZ;
    private float currentScale = 1.0f;

    /**
     * @param monochrome true untuk mode anaglyph: warna diganti abu-abu (luminansi)
     *                   supaya terlihat oleh kedua mata (saluran merah & cyan).
     */
    public void draw(boolean monochrome) {
        glPushMatrix();
        GLMatrix.multModelView(model);

        // Sisi berwarna (didorong sedikit ke belakang agar garis tepi tidak "z-fight")
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        glBegin(GL_QUADS);
        for (int i = 0; i < faces.length; i++) {
            float r = faceColors[i][0], g = faceColors[i][1], b = faceColors[i][2];
            if (monochrome) {
                float lum = 0.2f + 0.8f * (0.299f * r + 0.587f * g + 0.114f * b);
                r = g = b = lum;
            }
            glColor3f(r, g, b);
            for (int idx : faces[i]) {
                glVertex3f(vertices[idx][0], vertices[idx][1], vertices[idx][2]);
            }
        }
        glEnd();
        glDisable(GL_POLYGON_OFFSET_FILL);

        // Garis tepi hitam
        glColor3f(0.0f, 0.0f, 0.0f);
        glLineWidth(2.0f);
        for (int[] face : faces) {
            glBegin(GL_LINE_LOOP);
            for (int idx : face) {
                glVertex3f(vertices[idx][0], vertices[idx][1], vertices[idx][2]);
            }
            glEnd();
        }

        glPopMatrix();
    }
}
