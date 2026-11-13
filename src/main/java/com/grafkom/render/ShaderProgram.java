package com.grafkom.render;

import static org.lwjgl.opengl.GL20.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexResource));
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentResource));
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Link error: " + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Resource tidak ditemukan: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use()  { glUseProgram(programId); }
    public void stop() { glUseProgram(0); }

    private int loc(String name) { return glGetUniformLocation(programId, name); }

    public void setInt(String n, int v)       { glUniform1i(loc(n), v); }
    public void setBool(String n, boolean v)  { glUniform1i(loc(n), v ? 1 : 0); }
    public void setFloat(String n, float v)   { glUniform1f(loc(n), v); }
    public void setVec3(String n, Vector3f v) { glUniform3f(loc(n), v.x, v.y, v.z); }

    public void setMat4(String n, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(loc(n), false, m.get(stack.mallocFloat(16)));
        }
    }

    public void cleanup() { glDeleteProgram(programId); }
}