package com.grafkom.projection;

import org.joml.Matrix4f;

/**
 * Indikator 2 - Proyeksi Perspektif (Viewing Frustum).
 *
 * Parameter frustum: FOV_y, aspect ratio, near, far.
 * Menyediakan matriks simetris (untuk tampilan biasa) dan matriks off-axis /
 * asimetris (untuk stereo).
 */
public class PerspectiveProjection {
    public static final float MIN_FOV = 15.0f;
    public static final float MAX_FOV = 120.0f;

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = fovDegree;
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    /** Matriks perspektif simetris - rumus persis seperti di modul. */
    public Matrix4f calculateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(f / aspectRatio);
        proj.m11(f);
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2.0f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1.0f);
        proj.m33(0.0f);
        return proj;
    }

    /**
     * Matriks off-axis (asymmetric frustum) untuk satu mata.
     *
     * @param viewShiftX  pergeseran dunia pada sumbu x di ruang kamera
     *                    (+ipd/2 untuk mata kiri, -ipd/2 untuk mata kanan)
     * @param convergence jarak bidang layar (zero parallax)
     */
    public Matrix4f calculateOffAxisMatrix(float viewShiftX, float convergence) {
        float top = zNear * (float) Math.tan(Math.toRadians(fovDegree) / 2.0);
        float bottom = -top;
        float halfWidth = top * aspectRatio;
        float shift = viewShiftX * zNear / convergence;
        return frustum(-halfWidth + shift, halfWidth + shift, bottom, top, zNear, zFar);
    }

    /** Matriks frustum umum (setara glFrustum), dihitung manual. */
    public static Matrix4f frustum(float l, float r, float b, float t, float n, float f) {
        Matrix4f m = new Matrix4f();
        m.zero();
        m.m00(2.0f * n / (r - l));
        m.m11(2.0f * n / (t - b));
        m.m20((r + l) / (r - l));
        m.m21((t + b) / (t - b));
        m.m22(-(f + n) / (f - n));
        m.m23(-1.0f);
        m.m32(-(2.0f * f * n) / (f - n));
        m.m33(0.0f);
        return m;
    }

    // Getter / Setter (untuk pengujian interaktif FOV / dolly zoom)
    public void setFovDegree(float fovDegree) {
        this.fovDegree = Math.max(MIN_FOV, Math.min(MAX_FOV, fovDegree));
    }
    public void adjustFov(float deltaDeg) { setFovDegree(fovDegree + deltaDeg); }
    public float getFovDegree()           { return fovDegree; }
    public void setAspectRatio(float a)   { this.aspectRatio = a; }
    public float getAspectRatio()         { return aspectRatio; }
    public float getZNear()               { return zNear; }
    public float getZFar()                { return zFar; }
}
