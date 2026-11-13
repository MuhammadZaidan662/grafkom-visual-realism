package com.grafkom.viewing3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import com.grafkom.camera.Camera3D;
import com.grafkom.projection.PerspectiveProjection;
import com.grafkom.projection.ProjectionTaxonomyManager;
import com.grafkom.projection.ProjectionTaxonomyManager.ProjectionType;
import com.grafkom.render.GLMatrix;
import com.grafkom.render.RealismScene;
import com.grafkom.render.Scene;
import com.grafkom.render.StereoAnaglyphRenderer;
import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

/**
 * 3D Viewing Interactive Visualizer (Minggu 10).
 *
 * Kontrol:
 *   Mouse drag kiri / Panah  : orbit kamera        Scroll / W,S : zoom (dolly)
 *   1..6                     : moda proyeksi (taksonomi)
 *   SPASI                    : anaglyph stereo ON/OFF (khusus moda perspektif)
 *   [ ]                      : IPD -/+              , .          : konvergensi -/+
 *   - =                      : FOV -/+              R            : reset      ESC : keluar
 */
public class ThreeDViewing {
    private static final float DEFAULT_RADIUS = 10.0f;

    private long window;
    private int fbWidth = 1100;
    private int fbHeight = 720;

    private final Camera3D camera =
            new Camera3D(new Vector3f(0, 0, DEFAULT_RADIUS), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
    private final PerspectiveProjection perspective = new PerspectiveProjection(45.0f, 1100f / 720f, 0.1f, 100.0f);
    private final ProjectionTaxonomyManager taxonomy = new ProjectionTaxonomyManager(perspective);
    private final StereoAnaglyphRenderer stereo = new StereoAnaglyphRenderer();
    private final Scene scene = new Scene();
    private final RealismScene realism = new RealismScene();   // Minggu 11
    private boolean realismMode = true;
    private boolean vsync = true;
    private int fpsFrames = 0;
    private double fpsStart = 0;
    private double fps = 0;

    private boolean stereoEnabled = false;
    private boolean dragging = false;
    private double lastX, lastY;
    private String notice = "";
    private double noticeUntil = 0;
    private double nextTitleUpdate = 0;

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }

    private void run() {
        init();
        loop();
        cleanup();
    }

    // ------------------------------------------------------------------
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(fbWidth, fbHeight, "3D Viewing", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Gagal membuat window GLFW");
        }

        registerCallbacks();

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();
        System.out.println("OpenGL  : " + glGetString(GL_VERSION));     // BARU
        System.out.println("Renderer: " + glGetString(GL_RENDERER));    // BARU

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        fbWidth = w[0];
        fbHeight = h[0];

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.06f, 0.07f, 0.10f, 1.0f);
        
        realism.init();
        resetView();
    }

    private void registerCallbacks() {
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            fbWidth = w;
            fbHeight = h;
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) {
                    double[] x = new double[1], y = new double[1];
                    glfwGetCursorPos(win, x, y);
                    lastX = x[0];
                    lastY = y[0];
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) {
                float dx = (float) (x - lastX);
                float dy = (float) (y - lastY);
                camera.orbit(-dx * 0.3f, dy * 0.3f);
            }
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (win, xo, yo) -> camera.zoom(-(float) yo * 0.8f));

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_RELEASE) return;
            boolean press = (action == GLFW_PRESS);

            switch (key) {
                // Taksonomi proyeksi (tombol 1-6)
                case GLFW_KEY_1: if (press) selectProjection(ProjectionType.ORTHOGRAPHIC); break;
                case GLFW_KEY_2: if (press) selectProjection(ProjectionType.ISOMETRIC); break;
                case GLFW_KEY_3: if (press) selectProjection(ProjectionType.OBLIQUE_CAVALIER); break;
                case GLFW_KEY_4: if (press) selectProjection(ProjectionType.PERSPECTIVE_1POINT); break;
                case GLFW_KEY_5: if (press) selectProjection(ProjectionType.PERSPECTIVE_2POINT); break;
                case GLFW_KEY_6: if (press) selectProjection(ProjectionType.PERSPECTIVE_3POINT); break;

                case GLFW_KEY_SPACE: if (press) toggleStereo(); break;
                case GLFW_KEY_R:     if (press) resetView(); break;
                case GLFW_KEY_ESCAPE: glfwSetWindowShouldClose(window, true); break;
                case GLFW_KEY_TAB:
                   if (press) {
                       realismMode = !realismMode;
                       showNotice(realismMode ? "Mode REALISM (Minggu 11)" : "Mode VIEWER (Minggu 10)");
                       }
                       break;
                case GLFW_KEY_B: if (press) realism.toggleBlinnPhong(); break;
                case GLFW_KEY_F: if (press) realism.toggleSmooth(); break;
                case GLFW_KEY_M: if (press) realism.toggleWireframe(); break;
                case GLFW_KEY_Z: if (press) realism.toggleDepthTest(); break;
                case GLFW_KEY_C: if (press) realism.toggleCullFace(); break;
                case GLFW_KEY_T: if (press) realism.toggleTexture(); break;
                case GLFW_KEY_N: if (press) realism.toggleFilter(); break;
                case GLFW_KEY_H: if (press) realism.toggleShadow(); break;
                case GLFW_KEY_O: if (press) realism.toggleAnimate(); break;
                case GLFW_KEY_K: realism.adjustBias(-0.0002f); break;    // boleh ditahan (auto-repeat)
                case GLFW_KEY_L: realism.adjustBias(+0.0002f); break;
                case GLFW_KEY_V:
                   if (press) {
                       vsync = !vsync;
                       glfwSwapInterval(vsync ? 1 : 0);
                       showNotice("VSync " + (vsync ? "ON" : "OFF"));
                       }
                       break;

                // Penalaan interaktif (boleh ditahan / auto-repeat)
                case GLFW_KEY_LEFT_BRACKET:  stereo.adjustIpd(-0.02f); break;
                case GLFW_KEY_RIGHT_BRACKET: stereo.adjustIpd(+0.02f); break;
                case GLFW_KEY_COMMA:         stereo.adjustFocalLength(-0.5f); break;
                case GLFW_KEY_PERIOD:        stereo.adjustFocalLength(+0.5f); break;
                case GLFW_KEY_MINUS:         perspective.adjustFov(-2.0f); break;
                case GLFW_KEY_EQUAL:         perspective.adjustFov(+2.0f); break;
                default: break;
            }
        });
    }

    // ------------------------------------------------------------------
    private void selectProjection(ProjectionType type) {
        taxonomy.setCurrent(type);
        camera.setOrbit(type.getPresetAzimuth(), type.getPresetElevation());
        if (stereoEnabled && !type.isPerspective()) {
            stereoEnabled = false;
            showNotice("Stereo dimatikan (hanya untuk moda perspektif)");
        }
    }

    private void toggleStereo() {
        if (!taxonomy.getCurrent().isPerspective()) {
            showNotice("Stereo hanya untuk moda perspektif (tekan 4/5/6)");
            return;
        }
        stereoEnabled = !stereoEnabled;
    }

    private void resetView() {
        stereoEnabled = false;
        perspective.setFovDegree(45.0f);
        stereo.setFocalLength(DEFAULT_RADIUS);
        taxonomy.setCurrent(ProjectionType.PERSPECTIVE_3POINT);
        camera.jumpTo(DEFAULT_RADIUS,
                ProjectionType.PERSPECTIVE_3POINT.getPresetAzimuth(),
                ProjectionType.PERSPECTIVE_3POINT.getPresetElevation());
    }

    private void showNotice(String text) {
        notice = text;
        noticeUntil = glfwGetTime() + 3.0;
    }

    // ------------------------------------------------------------------
    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) Math.min(now - last, 0.1);
            last = now;

            glfwPollEvents();
            handleContinuousInput(dt);
            camera.update(dt);
            realism.update(dt); 
            render();
            updateTitle(now);
            glfwSwapBuffers(window);
            fpsFrames++;                                      // BARU
            if (now - fpsStart >= 0.5) {                      // BARU: rata-rata tiap 0,5 detik
            fps = fpsFrames / (now - fpsStart);
            fpsFrames = 0;
            fpsStart = now;
            }
        }
    }

    private void handleContinuousInput(float dt) {
        float rot = 90.0f * dt;     // derajat/detik
        float zoom = 6.0f * dt;     // satuan/detik
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)  camera.orbit(+rot, 0);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.orbit(-rot, 0);
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS)    camera.orbit(0, -rot);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS)  camera.orbit(0, +rot);
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)     camera.zoom(-zoom);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)     camera.zoom(+zoom);
    }

    private void render() {
        if (fbWidth <= 0 || fbHeight <= 0) return;   // jendela di-minimize
        float aspect = (float) fbWidth / (float) fbHeight;
        perspective.setAspectRatio(aspect);
        
        if (realismMode) realism.renderShadowPass(fbWidth, fbHeight); 

        glViewport(0, 0, fbWidth, fbHeight);
        glColorMask(true, true, true, true);
        glEnable(GL_DEPTH_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        boolean useStereo = stereoEnabled && taxonomy.getCurrent().isPerspective();
        scene.setMonochrome(useStereo);

       scene.setMonochrome(useStereo);

Runnable drawCall = realismMode ? realism::draw : scene::draw;      // BARU

if (useStereo) {
    stereo.renderAnaglyph(camera, perspective, drawCall);           // scene::draw -> drawCall
} else {
    Matrix4f projection = taxonomy.getProjectionMatrix(aspect, camera.getRadius());
    GLMatrix.loadProjection(projection);
    GLMatrix.loadModelView(camera.calculateLookAtMatrix());
    drawCall.run();                                                 // scene.draw() -> drawCall.run()
}
    }

    private void updateTitle(double now) {
        if (now < nextTitleUpdate) return;
        nextTitleUpdate = now + 0.2;

        StringBuilder sb = new StringBuilder("3D Viewing | ");
        sb.append(String.format(Locale.ROOT, "%.0f FPS | ", fps));
        if (realismMode) sb.append(realism.status()).append(" | ");
        sb.append(taxonomy.getCurrent().getLabel());
        sb.append(String.format(Locale.ROOT, " | FOV %.0f deg", perspective.getFovDegree()));
        sb.append(String.format(Locale.ROOT, " | Az %.0f El %.0f R %.1f",
                camera.getAzimuthDeg(), camera.getElevationDeg(), camera.getRadius()));
        if (stereoEnabled && taxonomy.getCurrent().isPerspective()) {
            sb.append(String.format(Locale.ROOT, " | ANAGLYPH IPD %.2f Konv %.1f",
                    stereo.getIpd(), stereo.getFocalLength()));
        } else {
            sb.append(" | Stereo OFF (SPASI)");
        }
        if (now < noticeUntil) sb.append(" | ").append(notice);
        glfwSetWindowTitle(window, sb.toString());
    }

    private void cleanup() {
        realism.cleanup();
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
