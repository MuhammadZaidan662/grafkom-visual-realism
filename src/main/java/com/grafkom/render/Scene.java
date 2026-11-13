package com.grafkom.render;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Isi dunia 3D: lantai berpetak, sumbu koordinat, dan beberapa kubus pada
 * kedalaman berbeda (agar foreshortening, titik hilang, dan parallax terlihat).
 */
public class Scene {
    private static final float FLOOR_Y = -1.0f;
    private static final int GRID_HALF = 8;

    private final List<Mesh3D> meshes = new ArrayList<>();
    private boolean monochrome;

    public Scene() {
        // Kubus utama (sisi 2) tepat di tengah -> zero parallax
        meshes.add(Mesh3D.createColorCube(1.0f));
        // Kubus kecil di belakang-kanan (positive parallax)
        meshes.add(Mesh3D.createColorCube(1.0f).setUniformScale(0.5f).setPosition(3.2f, -0.5f, -3.0f));
        // Kubus kecil di depan-kiri (mendekati kamera -> negative parallax)
        meshes.add(Mesh3D.createColorCube(1.0f).setUniformScale(0.5f).setPosition(-3.2f, -0.5f, 3.0f));
    }

    public void setMonochrome(boolean monochrome) { this.monochrome = monochrome; }

    /** Callback yang dipakai renderer biasa maupun stereo (Runnable). */
    public void draw() {
        drawFloorGrid();
        drawAxes();
        for (Mesh3D mesh : meshes) {
            mesh.draw(monochrome);
        }
    }

    private void drawFloorGrid() {
        if (monochrome) glColor3f(0.55f, 0.55f, 0.55f); else glColor3f(0.35f, 0.38f, 0.45f);
        glLineWidth(1.0f);
        glBegin(GL_LINES);
        for (int i = -GRID_HALF; i <= GRID_HALF; i++) {
            glVertex3f(i, FLOOR_Y, -GRID_HALF);  glVertex3f(i, FLOOR_Y, GRID_HALF);
            glVertex3f(-GRID_HALF, FLOOR_Y, i);  glVertex3f(GRID_HALF, FLOOR_Y, i);
        }
        glEnd();
    }

    private void drawAxes() {
        glLineWidth(3.0f);
        glBegin(GL_LINES);
        if (monochrome) glColor3f(0.9f, 0.9f, 0.9f); else glColor3f(1.0f, 0.2f, 0.2f);
        glVertex3f(0, 0, 0); glVertex3f(3.5f, 0, 0);   // X
        if (monochrome) glColor3f(0.9f, 0.9f, 0.9f); else glColor3f(0.2f, 1.0f, 0.2f);
        glVertex3f(0, 0, 0); glVertex3f(0, 3.5f, 0);   // Y
        if (monochrome) glColor3f(0.9f, 0.9f, 0.9f); else glColor3f(0.3f, 0.5f, 1.0f);
        glVertex3f(0, 0, 0); glVertex3f(0, 0, 3.5f);   // Z
        glEnd();
        glLineWidth(1.0f);
    }
}
