#version 460 core
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv;

out vec2 fragUV;

void main() {
    fragUV = uv;
    gl_Position = vec4(position * 2.0, 1.0);
}