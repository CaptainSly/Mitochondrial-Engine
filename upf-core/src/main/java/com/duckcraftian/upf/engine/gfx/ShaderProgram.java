package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.utils.FileUtils;
import com.duckcraftian.upf.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram implements IDisposable {

    private final Map<String, Integer> shaderUniforms;

    private final int shaderProgram;

    public ShaderProgram(String shaderName) {
        shaderUniforms = new HashMap<>();

        String vertexSource = null;
        String fragmentSource = null;
        try {
            vertexSource = Files.readString(FileUtils.resolveAssetFile(shaderName + ".vs").toPath());
            fragmentSource = Files.readString(FileUtils.resolveAssetFile(shaderName + ".fs").toPath());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try {
            vertexSource = resolveIncludes(vertexSource, new ArrayList<>());
            fragmentSource = resolveIncludes(fragmentSource, new ArrayList<>());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        int vertexShader = GL30.glCreateShader(GL30.GL_VERTEX_SHADER);
        int fragmentShader = GL30.glCreateShader(GL30.GL_FRAGMENT_SHADER);

        GL30.glShaderSource(vertexShader, vertexSource);
        GL30.glShaderSource(fragmentShader, fragmentSource);

        GL30.glCompileShader(vertexShader);
        GL30.glCompileShader(fragmentShader);

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling Shader code: " + glGetShaderInfoLog(vertexShader, 1024));
        }

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling Shader code: " + glGetShaderInfoLog(fragmentShader, 1024));
        }

        shaderProgram = GL30.glCreateProgram();
        GL30.glAttachShader(shaderProgram, vertexShader);
        GL30.glAttachShader(shaderProgram, fragmentShader);
        GL30.glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking Shader code: " + glGetProgramInfoLog(shaderProgram, 1024));
        }

        GL30.glDeleteShader(vertexShader);
        GL30.glDeleteShader(fragmentShader);

    }

    private String resolveIncludes(String vertexSource, List<String> stringList) throws IOException, URISyntaxException {
        String[] lines = vertexSource.split("\n");
        String preprocessor = "// #include ";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (!line.contains(preprocessor))
                continue;
            else {

                String filename = line.substring(preprocessor.length(), line.length());
                filename = filename.replace("\"", "").strip();

                if (stringList.contains(filename))
                    continue;

                String fileContents = Files.readString(FileUtils.resolveAssetFile(filename).toPath());
                stringList.add(filename);

                fileContents = resolveIncludes(fileContents, stringList);
                lines[i] = fileContents;
            }

        }

        StringBuilder sb = new StringBuilder();
        for (String line : lines)
            sb.append(line + "\n");

        return sb.toString();
    }

    public void begin() {
        GL30.glUseProgram(shaderProgram);
    }

    public void end() {
        GL30.glUseProgram(0);
    }

    public void createUniform(String uniformName) {
        int uniformLocation = glGetUniformLocation(shaderProgram, uniformName);
        if (uniformLocation < 0)
            throw new RuntimeException("Could not find uniform: " + uniformName + " in shader");

        shaderUniforms.put(uniformName, uniformLocation);
    }

    public void set(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int location = getUniform(uniformName);
            glUniformMatrix4fv(location, false, value.get(stack.mallocFloat(16)));
        }
    }

    public void set(String uniformName, int value) {
        int location = getUniform(uniformName);
        glUniform1i(location, value);
    }

    public void set(String uniformName, boolean value) {
        int location = getUniform(uniformName);
        glUniform1i(location, value ? 1 : 0);
    }

    public void set(String uniformName, float value) {
        int location = getUniform(uniformName);
        glUniform1f(location, value);
    }

    public void set(String uniformName, Vector3f value) {
        int location = getUniform(uniformName);
        glUniform3f(location, value.x, value.y, value.z);
    }

    public void set(String uniformName, Vector2f value) {
        int location = getUniform(uniformName);
        glUniform2f(location, value.x, value.y);
    }

    public void set(String uniformName, float x, float y) {
        int location = getUniform(uniformName);
        glUniform2f(location, x, y);
    }

    public void set(String uniformName, Vector2f[] value) {
        int location = getUniform(uniformName);
        glUniform2fv(location, Utils.convertVec2ArrayToFloatArray(value));
    }

    public void set(String uniformName, Vector3f[] value) {
        int location = getUniform(uniformName);
        glUniform3fv(location, Utils.convertVec3ArrayToFloatArray(value));
    }

    public void set(String uniformName, float[] value) {
        int location = getUniform(uniformName);
        glUniform1fv(location, value);
    }

    private int getUniform(String uniformName) {
        Integer location = shaderUniforms.get(uniformName);
        if (location == null)
            throw new RuntimeException("Could not find uniform: " + uniformName);

        return location.intValue();
    }

    public Map<String, Integer> getShaderUniforms() {
        return shaderUniforms;
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    @Override
    public void onDispose() {
        GL30.glDeleteProgram(shaderProgram);
    }
}