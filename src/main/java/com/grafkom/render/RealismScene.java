package com.grafkom.render;

   import static org.lwjgl.opengl.GL30.*;

import com.grafkom.viewing3d.HSRManager;
import com.grafkom.viewing3d.ShadowMapFBO;  
import com.grafkom.viewing3d.Texture2D;
import java.util.Locale;  
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Minggu 11 - adegan Visual Realism berbasis shader GLSL. */
public class RealismScene {
    private static final float GROUND_Y = -1.6f;

    private boolean blinnPhong = true;
    private boolean smooth = true;        // BARU: true = Smooth, false = Flat
    private boolean wireframe = false;    // BARU
    private boolean depthTest = true;    // BARU: Z-Buffer
    private boolean cullFace = true;     // BARU: Back-Face Culling
    private boolean useTexture = true;    // BARU
    private boolean mipmapped = true;     // BARU: true = Bilinear+Mipmap, false = Nearest
    private boolean shadowOn = true;       // BARU: H
    private boolean animate = false;       // BARU: O (cahaya mengorbit, bola berputar)
    private float shadowBias = 0.002f;     // BARU: K / L

    private float lightAngle = (float) Math.atan2(3.0, 4.0);   // BARU: posisi awal cahaya (4, 7, 3)
    private float spinDeg = 0f;

    private ShaderProgram depthShader;     // BARU
    private ShadowMapFBO shadowFbo;        // BARU
    private final Matrix4f lightSpace = new Matrix4f();   // BARU: P_light * V_light
    
    private final Vector3f lightPos = new Vector3f(4f, 7f, 3f);

    private ShaderProgram lit;
    private GLMesh sphereSmooth, sphereFlat, cube, ground;   // BARU
    private Texture2D brick, checker;     // BARU

    private final Matrix4f sphereModel = new Matrix4f();
    private final Matrix4f cubeModel = new Matrix4f()        // BARU: kubus kecil di atas lantai
            .translate(3.0f, GROUND_Y + 0.5f, -1.5f).rotateY((float) Math.toRadians(30));
    private final Matrix4f groundModel = new Matrix4f();

    public void init() {
        lit = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");

        MeshData sphere = MeshData.sphere(1.0f, 24, 36);
        sphereSmooth = sphere.buildSmooth();   // normal dirata-rata per vertex
        sphereFlat = sphere.buildFlat();       // normal per face
        cube = MeshData.cube(0.5f).buildFlat();   // kubus selalu flat (tepi tegas)
        ground = MeshData.ground(8f, GROUND_Y, 8f).buildSmooth();
        brick = new Texture2D("/textures/brick.png");
        checker = new Texture2D("/textures/checker.png");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowFbo = new ShadowMapFBO();
        update(0f);
    }
    
    /** Dipanggil tiap frame: animasi (opsional) + hitung ulang cahaya. */
    public void update(float dt) {
    if (animate) {
        lightAngle += 0.6f * dt;
        spinDeg += 25.0f * dt;
    }
    lightPos.set(5f * (float) Math.cos(lightAngle), 7f, 5f * (float) Math.sin(lightAngle));

    // Ruang cahaya: proyeksi ortografik (directional light) + view dari cahaya ke pusat adegan
    Matrix4f lightProj = new Matrix4f().ortho(-14f, 14f, -14f, 14f, 1.0f, 25.0f);
    Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
    lightSpace.set(lightProj).mul(lightView);

    sphereModel.identity().rotateY((float) Math.toRadians(spinDeg));
}

    public void toggleBlinnPhong() { blinnPhong = !blinnPhong; }
    public void toggleSmooth()     { smooth = !smooth; }          // BARU
    public void toggleWireframe()  { wireframe = !wireframe; }    // BARU
    public void toggleDepthTest()  { depthTest = !depthTest; }
    public void toggleCullFace()   { cullFace = !cullFace; }
    public void toggleTexture() { useTexture = !useTexture; }

    public void toggleFilter() {
        mipmapped = !mipmapped;
        brick.setMipmapped(mipmapped);
        checker.setMipmapped(mipmapped);
    }
    
    public void toggleShadow()  { shadowOn = !shadowOn; }
    public void toggleAnimate() { animate = !animate; }
    public void adjustBias(float delta) {
        shadowBias = Math.max(0.0f, Math.min(0.02f, shadowBias + delta));
    }

/** Ringkasan state untuk judul jendela. */
    public String status() {
        return (blinnPhong ? "Blinn-Phong" : "Phong")
             + " | " + (smooth ? "Smooth" : "Flat")
             + " | Z " + (depthTest ? "ON" : "OFF")
             + " | C " + (cullFace ? "ON" : "OFF")
             + " | Tex " + (useTexture ? (mipmapped ? "Mipmap" : "Nearest") : "OFF")   
             + " | Shadow " + (shadowOn ? String.format(Locale.ROOT, "ON b=%.4f", shadowBias) : "OFF") 
             + (wireframe ? " | Wire" : "");
    }
    
    /** PASS 1 - Depth Pass: gambar adegan dari sudut pandang CAHAYA ke Shadow Map. */
    public void renderShadowPass(int screenW, int screenH) {
    if (!shadowOn) return;
    shadowFbo.bindForWriting();
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);

    depthShader.use();
    depthShader.setMat4("lightSpaceMatrix", lightSpace);
    depthShader.setMat4("model", sphereModel);
    (smooth ? sphereSmooth : sphereFlat).draw();
    depthShader.setMat4("model", cubeModel);
    cube.draw();                    // lantai tidak digambar: ia hanya PENERIMA bayangan
    depthShader.stop();

    shadowFbo.unbind(screenW, screenH);
}

    public void draw() {
        HSRManager.setupDepthTest(depthTest);          // BARU
        HSRManager.setupBackFaceCulling(cullFace);     // BARU
        glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

        Matrix4f view = GLMatrix.currentModelView();
        Matrix4f proj = GLMatrix.currentProjection();
        Vector3f eye = new Matrix4f(view).invert().getTranslation(new Vector3f());

        lit.use();
        lit.setMat4("view", view);
        lit.setMat4("projection", proj);
        lit.setVec3("lightPos", lightPos);
        lit.setVec3("viewPos", eye);
        lit.setVec3("lightColor", new Vector3f(1f, 1f, 1f));
        lit.setBool("useBlinnPhong", blinnPhong);
        lit.setInt("textureSampler", 0);          // BARU: sampler membaca texture unit 0
        lit.setBool("useTexture", useTexture);    // BARU
        lit.setMat4("lightSpaceMatrix", lightSpace);     // BARU
        lit.setBool("useShadow", shadowOn);              // BARU
        lit.setFloat("shadowBias", shadowBias);          // BARU
        lit.setInt("shadowMap", 1);                      // BARU: Shadow Map dibaca dari texture unit 1
        glActiveTexture(GL_TEXTURE1);                    // BARU
        glBindTexture(GL_TEXTURE_2D, shadowFbo.getDepthTextureID());   // BARU

        brick.bind(0);
        lit.setMat4("model", sphereModel);
        lit.setVec3("objectColor", new Vector3f(0.90f, 0.35f, 0.30f));
        (smooth ? sphereSmooth : sphereFlat).draw();       // BARU: pilih mesh sesuai mode

        lit.setMat4("model", cubeModel);
        lit.setVec3("objectColor", new Vector3f(0.30f, 0.60f, 0.90f));
        cube.draw();

        checker.bind(0);
        lit.setMat4("model", groundModel);
        lit.setVec3("objectColor", new Vector3f(0.55f, 0.60f, 0.65f));
        ground.draw();

        lit.stop();
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);   // kembalikan agar mode Viewer tidak terpengaruh
        HSRManager.setupBackFaceCulling(false);        // BARU: kembalikan untuk mode Viewer
        HSRManager.setupDepthTest(true);               // BARU
    }

    public void cleanup() {
        lit.cleanup();
        sphereSmooth.cleanup();
        sphereFlat.cleanup();
        cube.cleanup();
        ground.cleanup();
        brick.cleanup();
        checker.cleanup();
        depthShader.cleanup();
        shadowFbo.cleanup();
    }
}