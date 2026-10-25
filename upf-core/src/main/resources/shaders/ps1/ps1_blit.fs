#version 460 core

in vec2 fragUV;

uniform sampler2D screen;

out vec4 color;

void main() {
    color = texture(screen, fragUV);
}