#version 460 core
layout (location = 0) in ivec4 posUV;
layout (location = 1) in ivec2 pageClut;
layout (location = 2) in int slot;
layout (location = 3) in int rgb;

uniform ivec2 scroll;

out vec2 texelPos;
out vec3 vertColor;

flat out ivec2 fragPageClut;

void main() {
    vec2 pixel = vec2(posUV.xy + scroll);
    gl_Position = vec4(pixel.x / 160 - 1.0, 1.0 - pixel.y / 112.0, float(slot) / 2048.0 - 1.0, 1.0);
    texelPos = vec2(posUV.zw);
    fragPageClut = pageClut;
    vertColor = vec3(rgb & 255, (rgb >> 8) & 255, (rgb >> 16) & 255) / 255.0;
}