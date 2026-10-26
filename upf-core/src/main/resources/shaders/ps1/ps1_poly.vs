#version 460 core
layout (location = 0) in ivec4 posUV;
layout (location = 1) in ivec2 pageClut;
layout (location = 2) in int slot;

uniform ivec2 scroll;

out vec2 texelPos;

flat out ivec2 fragPageClut;

void main() {
    vec2 pixel = vec2(posUV.xy + scroll);
    gl_Position = vec4(pixel.x / 160 - 1.0, 1.0 - pixel.y / 112.0, float(slot) / 2048.0 - 1.0, 1.0);
    texelPos = vec2(posUV.zw);
    fragPageClut = pageClut;
}