package com.grafkom.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Indikator 1 - Kamera 3D (Synthetic Camera Model).
 *
 * Mengenkapsulasi eye / target / up, menghitung View Matrix (LookAt) SECARA MANUAL,
 * dan menyediakan navigasi orbital (turntable) yang dihaluskan (smoothing).
 */
public class Camera3D {
    public static final float MIN_RADIUS = 2.0f;
    public static final float MAX_RADIUS = 40.0f;
    private static final float MAX_ELEVATION = 89.0f; // hindari up-vector sejajar arah pandang
    private static final float SMOOTHNESS = 12.0f;    // makin besar makin responsif

    private final Vector3f eye;
    private final Vector3f target;
    private final Vector3f up;

    // Keadaan orbit yang sedang dirender
    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    // Keadaan orbit yang dituju (input langsung mengubah ini, lalu di-smooth)
    private float goalRadius;
    private float goalAzimuthDeg;
    private float goalElevationDeg;

    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);

        // Turunkan parameter orbit (radius, azimuth, elevation) dari posisi awal
        Vector3f d = new Vector3f(eye).sub(target);
        this.radius = d.length();
        this.elevationDeg = (float) Math.toDegrees(Math.asin(d.y / radius));
        this.azimuthDeg = (float) Math.toDegrees(Math.atan2(d.x, d.z));
        this.goalRadius = radius;
        this.goalAzimuthDeg = azimuthDeg;
        this.goalElevationDeg = elevationDeg;
    }

    // ------------------------------------------------------------------
    // Matriks LookAt (View Matrix) - perhitungan manual
    // ------------------------------------------------------------------
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();      // n = (E - At) / |E - At|  (View Plane Normal)

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();          // u = (Up x n) / |Up x n|  (Right)

        Vector3f v = new Vector3f();
        n.cross(u, v);                       // v = n x u               (True Up)

        // JOML: mKOLOMBARIS -> m30 = kolom 3 baris 0 (translasi x)
        Matrix4f view = new Matrix4f();
        view.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        view.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        view.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        view.m03(0.0f).m13(0.0f).m23(0.0f).m33(1.0f);
        return view;
    }

    // ------------------------------------------------------------------
    // Navigasi orbital (koordinat sferis di sekeliling target)
    // ------------------------------------------------------------------
    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        float az = (float) Math.toRadians(azimuthDeg);
        float el = (float) Math.toRadians(elevationDeg);
        eye.x = target.x + radius * (float) (Math.cos(el) * Math.sin(az));
        eye.y = target.y + radius * (float) Math.sin(el);
        eye.z = target.z + radius * (float) (Math.cos(el) * Math.cos(az));
    }

    /** Dipanggil tiap frame: menghaluskan gerakan menuju nilai tujuan. */
    public void update(float dt) {
        float k = 1.0f - (float) Math.exp(-SMOOTHNESS * dt);
        radius += (goalRadius - radius) * k;
        azimuthDeg += (goalAzimuthDeg - azimuthDeg) * k;
        elevationDeg += (goalElevationDeg - elevationDeg) * k;
        updateOrbitalPosition(radius, azimuthDeg, elevationDeg);
    }

    /** Putar kamera relatif (derajat). */
    public void orbit(float dAzimuthDeg, float dElevationDeg) {
        goalAzimuthDeg += dAzimuthDeg;
        goalElevationDeg = clamp(goalElevationDeg + dElevationDeg, -MAX_ELEVATION, MAX_ELEVATION);
    }

    /** Dolly in/out (satuan dunia). Nilai positif = menjauh. */
    public void zoom(float dRadius) {
        goalRadius = clamp(goalRadius + dRadius, MIN_RADIUS, MAX_RADIUS);
    }

    /** Pindah ke sudut tertentu secara animasi, lewat jalur terpendek. */
    public void setOrbit(float azimuth, float elevation) {
        float shift = 360.0f * Math.round(azimuthDeg / 360.0f);
        azimuthDeg -= shift;
        goalAzimuthDeg = azimuth;
        while (goalAzimuthDeg - azimuthDeg > 180.0f) goalAzimuthDeg -= 360.0f;
        while (goalAzimuthDeg - azimuthDeg < -180.0f) goalAzimuthDeg += 360.0f;
        goalElevationDeg = clamp(elevation, -MAX_ELEVATION, MAX_ELEVATION);
    }

    /** Pindah seketika (tanpa animasi). */
    public void jumpTo(float radius, float azimuth, float elevation) {
        this.radius = this.goalRadius = clamp(radius, MIN_RADIUS, MAX_RADIUS);
        this.azimuthDeg = this.goalAzimuthDeg = azimuth;
        this.elevationDeg = this.goalElevationDeg = clamp(elevation, -MAX_ELEVATION, MAX_ELEVATION);
        updateOrbitalPosition(this.radius, this.azimuthDeg, this.elevationDeg);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Getter
    public Vector3f getEye()        { return new Vector3f(eye); }
    public Vector3f getTarget()     { return new Vector3f(target); }
    public float getRadius()        { return radius; }
    public float getAzimuthDeg()    { return azimuthDeg; }
    public float getElevationDeg()  { return elevationDeg; }
}
