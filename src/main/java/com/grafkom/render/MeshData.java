package com.grafkom.render;

import java.util.HashMap;
import java.util.Map;

/** Data mesh mentah di CPU: posisi, UV, dan indeks segitiga (CCW = sisi depan). */
public class MeshData {
    public final float[] pos;   // x,y,z per vertex
    public final float[] uv;    // s,t per vertex
    public final int[] idx;     // 3 indeks per segitiga

    public MeshData(float[] pos, float[] uv, int[] idx) {
        this.pos = pos; this.uv = uv; this.idx = idx;
    }

    /** Normal face: N = (B-A) x (C-A), dinormalisasi. */
    private float[] faceNormal(int a, int b, int c) {
        float ux = pos[3*b]   - pos[3*a], uy = pos[3*b+1] - pos[3*a+1], uz = pos[3*b+2] - pos[3*a+2];
        float vx = pos[3*c]   - pos[3*a], vy = pos[3*c+1] - pos[3*a+1], vz = pos[3*c+2] - pos[3*a+2];
        float nx = uy*vz - uz*vy, ny = uz*vx - ux*vz, nz = ux*vy - uy*vx;
        float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len < 1e-8f) return new float[]{0, 0, 0};   // segitiga degenerate (di kutub bola)
        return new float[]{nx/len, ny/len, nz/len};
    }
    
       /** FLAT: tiap segitiga punya 3 vertex sendiri + 1 normal face yang sama. */
    public GLMesh buildFlat() {
        int triCount = idx.length / 3;
        float[] v = new float[triCount * 3 * 8];
        int[] ind = new int[triCount * 3];
        int o = 0;
        for (int t = 0; t < triCount; t++) {
            int a = idx[3*t], b = idx[3*t+1], c = idx[3*t+2];
            float[] n = faceNormal(a, b, c);
            for (int k : new int[]{a, b, c}) {
                v[o++] = pos[3*k]; v[o++] = pos[3*k+1]; v[o++] = pos[3*k+2];
                v[o++] = n[0];     v[o++] = n[1];       v[o++] = n[2];
                v[o++] = uv[2*k];  v[o++] = uv[2*k+1];
            }
            ind[3*t] = 3*t; ind[3*t+1] = 3*t+1; ind[3*t+2] = 3*t+2;
        }
        return new GLMesh(v, ind);
    }

    /** SMOOTH: N_vertex = sum(N_face) / ||sum(N_face)||, dirata-rata per POSISI vertex. */
    public GLMesh buildSmooth() {
        int vCount = pos.length / 3;
        Map<String, float[]> sums = new HashMap<>();
        String[] keys = new String[vCount];
        for (int i = 0; i < vCount; i++) {
            // dibulatkan ke grid 1e-4 agar vertex di jahitan UV (posisi sama) terhitung satu
            keys[i] = Math.round(pos[3*i] * 1e4) + "," + Math.round(pos[3*i+1] * 1e4) + "," + Math.round(pos[3*i+2] * 1e4);
            sums.putIfAbsent(keys[i], new float[3]);
        }
        for (int t = 0; t < idx.length / 3; t++) {
            int a = idx[3*t], b = idx[3*t+1], c = idx[3*t+2];
            float[] n = faceNormal(a, b, c);
            for (int k : new int[]{a, b, c}) {
                float[] s = sums.get(keys[k]);
                s[0] += n[0]; s[1] += n[1]; s[2] += n[2];
            }
        }
        float[] v = new float[vCount * 8];
        for (int i = 0; i < vCount; i++) {
            float[] s = sums.get(keys[i]);
            float len = (float) Math.sqrt(s[0]*s[0] + s[1]*s[1] + s[2]*s[2]);
            if (len < 1e-8f) len = 1f;
            v[8*i]   = pos[3*i];  v[8*i+1] = pos[3*i+1]; v[8*i+2] = pos[3*i+2];
            v[8*i+3] = s[0]/len;  v[8*i+4] = s[1]/len;   v[8*i+5] = s[2]/len;
            v[8*i+6] = uv[2*i];   v[8*i+7] = uv[2*i+1];
        }
        return new GLMesh(v, idx);
    }

    // ================= PABRIK BENTUK =================

    /** UV Sphere, winding CCW dilihat dari luar. */
    public static MeshData sphere(float r, int stacks, int slices) {
        int vc = (stacks + 1) * (slices + 1);
        float[] p = new float[vc * 3], t = new float[vc * 2];
        int k = 0;
        for (int i = 0; i <= stacks; i++) {
            double theta = Math.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                double phi = 2.0 * Math.PI * j / slices;
                p[3*k]   = (float) (r * Math.sin(theta) * Math.cos(phi));
                p[3*k+1] = (float) (r * Math.cos(theta));
                p[3*k+2] = (float) (r * Math.sin(theta) * Math.sin(phi));
                t[2*k]   = (float) j / slices;
                t[2*k+1] = 1f - (float) i / stacks;
                k++;
            }
        }
        int[] ind = new int[stacks * slices * 6];
        int m = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * (slices + 1) + j, b = a + slices + 1;
                ind[m++] = a;     ind[m++] = a + 1; ind[m++] = b;
                ind[m++] = a + 1; ind[m++] = b + 1; ind[m++] = b;
            }
        }
        return new MeshData(p, t, ind);
    }
    
        /** Kubus 2*h dengan 24 vertex (4 per sisi) supaya tiap sisi punya UV 0..1 sendiri. */
    public static MeshData cube(float h) {
        float[][][] faces = {
            {{-h,-h, h}, { h,-h, h}, { h, h, h}, {-h, h, h}},   // +Z depan
            {{ h,-h,-h}, {-h,-h,-h}, {-h, h,-h}, { h, h,-h}},   // -Z belakang
            {{ h,-h, h}, { h,-h,-h}, { h, h,-h}, { h, h, h}},   // +X kanan
            {{-h,-h,-h}, {-h,-h, h}, {-h, h, h}, {-h, h,-h}},   // -X kiri
            {{-h, h, h}, { h, h, h}, { h, h,-h}, {-h, h,-h}},   // +Y atas
            {{-h,-h,-h}, { h,-h,-h}, { h,-h, h}, {-h,-h, h}}    // -Y bawah
        };
        float[][] corner = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        float[] p = new float[24 * 3], t = new float[24 * 2];
        int[] ind = new int[36];
        for (int f = 0; f < 6; f++) {
            for (int c = 0; c < 4; c++) {
                int k = f * 4 + c;
                p[3*k] = faces[f][c][0]; p[3*k+1] = faces[f][c][1]; p[3*k+2] = faces[f][c][2];
                t[2*k] = corner[c][0];   t[2*k+1] = corner[c][1];
            }
            int b = f * 4, m = f * 6;
            ind[m] = b; ind[m+1] = b + 1; ind[m+2] = b + 2;
            ind[m+3] = b; ind[m+4] = b + 2; ind[m+5] = b + 3;
        }
        return new MeshData(p, t, ind);
    }

    /** Lantai di y = height, normal menghadap +Y, UV diulang 'tiles' kali. */
    public static MeshData ground(float half, float height, float tiles) {
        float[] p = {
            -half, height, -half,
            -half, height,  half,
             half, height,  half,
             half, height, -half };
        float[] t = { 0, 0,  0, tiles,  tiles, tiles,  tiles, 0 };
        int[] ind = { 0, 1, 2,  0, 2, 3 };
        return new MeshData(p, t, ind);
    }
}