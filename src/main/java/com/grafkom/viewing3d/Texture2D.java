package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;   // GL30 mewarisi GL11..GL20 (glActiveTexture, glGenerateMipmap, dst.)
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryStack;

/** Indikator 4 - Texture Mapping: memuat PNG/JPG dari classpath dengan STBImage ke GPU. */
public class Texture2D {
    private final int textureID;

    /** @param resourcePath contoh: "/textures/brick.png" */
    public Texture2D(String resourcePath) {
        byte[] bytes;
        try (InputStream in = Texture2D.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new RuntimeException("Tekstur tidak ditemukan: " + resourcePath);
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ByteBuffer raw = memAlloc(bytes.length);
        raw.put(bytes).flip();

        stbi_set_flip_vertically_on_load(true);   // baris pertama gambar = v=0 (bawah), sesuai konvensi UV
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer data = stbi_load_from_memory(raw, width, height, channels, 4);
            memFree(raw);
            if (data == null) {
                throw new RuntimeException("Gagal memuat tekstur " + resourcePath + ": " + stbi_failure_reason());
            }

            textureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureID);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, data);
            glGenerateMipmap(GL_TEXTURE_2D);       // buat tingkat mipmap 1/2, 1/4, 1/8, ...
            stbi_image_free(data);
        }
        setMipmapped(true);
    }

    /** true = Bilinear + Mipmap, false = Nearest Neighbor. */
    public void setMipmapped(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() { glDeleteTextures(textureID); }
}