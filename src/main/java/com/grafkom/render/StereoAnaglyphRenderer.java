package com.grafkom.render;

import static org.lwjgl.opengl.GL11.*;

import com.grafkom.camera.Camera3D;
import com.grafkom.projection.PerspectiveProjection;
import org.joml.Matrix4f;

/**
 * Indikator 3 - Stereo View (Red-Cyan Anaglyph).
 *
 * Metode: Off-Axis Projection (asymmetric frustum) + Color Masking.
 *   - Mata kiri  : saluran MERAH saja, dunia digeser +ipd/2, frustum digeser +
 *   - Mata kanan : saluran HIJAU+BIRU (cyan), dunia digeser -ipd/2, frustum digeser -
 * Benda tepat di jarak 'focalLength' berada di bidang layar (zero parallax).
 */
public class StereoAnaglyphRenderer {
    private float ipd = 0.30f;          // Interpupillary Distance (satuan dunia)
    private float focalLength = 10.0f;  // jarak bidang konvergensi (zero parallax)

    public void renderAnaglyph(Camera3D camera, PerspectiveProjection projection,
                               Runnable renderSceneCallback) {
        Matrix4f view = camera.calculateLookAtMatrix();
        float half = ipd / 2.0f;

        // Bersihkan SEMUA saluran dulu. (Tanpa ini, saluran hijau/biru frame lama tertinggal.)
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PASS 1: mata kiri -> hanya saluran merah
        glColorMask(true, false, false, true);
        setupEye(+half, view, projection);
        renderSceneCallback.run();

        // PASS 2: mata kanan -> hanya saluran hijau & biru (cyan)
        glColorMask(false, true, true, true);
        glClear(GL_DEPTH_BUFFER_BIT);   // color buffer dibiarkan; saluran berbeda sehingga tidak saling timpa
        setupEye(-half, view, projection);
        renderSceneCallback.run();

        // Kembalikan ke normal
        glColorMask(true, true, true, true);
    }

    private void setupEye(float viewShiftX, Matrix4f view, PerspectiveProjection projection) {
        Matrix4f proj = projection.calculateOffAxisMatrix(viewShiftX, focalLength);
        Matrix4f modelView = new Matrix4f().translation(viewShiftX, 0.0f, 0.0f).mul(view);
        GLMatrix.loadProjection(proj);
        GLMatrix.loadModelView(modelView);
    }

    public void adjustIpd(float delta)         { ipd = Math.max(0.0f, Math.min(2.0f, ipd + delta)); }
    public void adjustFocalLength(float delta) { focalLength = Math.max(1.0f, Math.min(60.0f, focalLength + delta)); }
    public float getIpd()                      { return ipd; }
    public float getFocalLength()              { return focalLength; }
    public void setFocalLength(float f)        { this.focalLength = f; }
}
