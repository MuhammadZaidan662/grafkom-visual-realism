package com.grafkom.projection;

import org.joml.Matrix4f;

/**
 * Indikator 4 - Taksonomi Proyeksi.
 *
 * Mengelola 6 moda proyeksi lewat enum. Karena jumlah titik hilang dan sifat
 * proyeksi aksonometrik bergantung pada ORIENTASI objek terhadap bidang
 * proyeksi, setiap moda juga membawa sudut orbit kamera "preset"-nya.
 *
 *   PARALEL     : Orthographic, Isometric (aksonometrik), Oblique Cavalier
 *   PERSPEKTIF  : 1 titik hilang, 2 titik hilang, 3 titik hilang
 */
public class ProjectionTaxonomyManager {

    public enum Family { PARALLEL, PERSPECTIVE }

    public enum ProjectionType {
        ORTHOGRAPHIC      ("Orthographic (Front Elevation)", Family.PARALLEL,    0.0f,   0.0f),
        ISOMETRIC         ("Isometric (Axonometric)",        Family.PARALLEL,   45.0f, 35.264f),
        OBLIQUE_CAVALIER  ("Oblique Cavalier 45",            Family.PARALLEL,    0.0f,   0.0f),
        PERSPECTIVE_1POINT("Perspective 1-Point",            Family.PERSPECTIVE, 0.0f,   0.0f),
        PERSPECTIVE_2POINT("Perspective 2-Point",            Family.PERSPECTIVE, 35.0f,  0.0f),
        PERSPECTIVE_3POINT("Perspective 3-Point",            Family.PERSPECTIVE, 35.0f, 30.0f);

        private final String label;
        private final Family family;
        private final float presetAzimuth;
        private final float presetElevation;

        ProjectionType(String label, Family family, float az, float el) {
            this.label = label;
            this.family = family;
            this.presetAzimuth = az;
            this.presetElevation = el;
        }

        public String getLabel()           { return label; }
        public Family getFamily()          { return family; }
        public float getPresetAzimuth()    { return presetAzimuth; }
        public float getPresetElevation()  { return presetElevation; }
        public boolean isPerspective()     { return family == Family.PERSPECTIVE; }
    }

    private static final float OBLIQUE_ANGLE_DEG = 45.0f;
    private static final float OBLIQUE_SCALE = 1.0f; // 1.0 = Cavalier, 0.5 = Cabinet

    private final PerspectiveProjection perspective;
    private ProjectionType current = ProjectionType.PERSPECTIVE_3POINT;

    public ProjectionTaxonomyManager(PerspectiveProjection perspective) {
        this.perspective = perspective;
    }

    public ProjectionType getCurrent()           { return current; }
    public void setCurrent(ProjectionType type)  { this.current = type; }

    /**
     * @param aspect          lebar/tinggi viewport
     * @param cameraRadius    jarak kamera ke target; dipakai agar skala ortografik
     *                        sebanding dengan perspektif pada jarak target
     */
    public Matrix4f getProjectionMatrix(float aspect, float cameraRadius) {
        switch (current) {
            case ORTHOGRAPHIC:
            case ISOMETRIC:
                return orthographic(aspect, cameraRadius);

            case OBLIQUE_CAVALIER: {
    float phi = (float) Math.toRadians(OBLIQUE_ANGLE_DEG);
    float kx = -OBLIQUE_SCALE * (float) Math.cos(phi);
    float ky = -OBLIQUE_SCALE * (float) Math.sin(phi);
    Matrix4f shear = new Matrix4f();
    shear.m20(kx).m21(ky);
    shear.m30(kx * cameraRadius).m31(ky * cameraRadius);
    return orthographic(aspect, cameraRadius).mul(shear);
}
            

            case PERSPECTIVE_1POINT:
            case PERSPECTIVE_2POINT:
            case PERSPECTIVE_3POINT:
            default:
                perspective.setAspectRatio(aspect);
                return perspective.calculateProjectionMatrix();
        }
    }

    /** Matriks ortografik manual (setara glOrtho). */
    private Matrix4f orthographic(float aspect, float cameraRadius) {
        float halfH = cameraRadius * (float) Math.tan(Math.toRadians(perspective.getFovDegree()) / 2.0);
        float halfW = halfH * aspect;
        return ortho(-halfW, halfW, -halfH, halfH, perspective.getZNear(), perspective.getZFar());
    }

    public static Matrix4f ortho(float l, float r, float b, float t, float n, float f) {
        Matrix4f m = new Matrix4f();
        m.m00(2.0f / (r - l));
        m.m11(2.0f / (t - b));
        m.m22(-2.0f / (f - n));
        m.m30(-(r + l) / (r - l));
        m.m31(-(t + b) / (t - b));
        m.m32(-(f + n) / (f - n));
        m.m33(1.0f);
        return m;
    }
}
