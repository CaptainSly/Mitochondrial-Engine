#version 460 core

uniform usampler2D vram;
uniform float opacity;

flat in ivec2 fragPageClut;
in vec2 texelPos;
in vec3 vertColor;

out vec4 color;

void main() {
    if (fragPageClut.x == -1) {
        color = vec4(vertColor, opacity);
        return;
    }
    ivec2 texel = ivec2(texelPos);
    int pageX = (fragPageClut.x & 15) * 64;
    int pageY = ((fragPageClut.x >> 4) & 1) * 256;
    int clutX = (fragPageClut.y & 63) * 16;
    int clutY = fragPageClut.y >> 6;
    int mode = (fragPageClut.x >> 7) & 3;

    int pixelsPerHalfword = 4;
    int bitsPerPixel = 4;
    int mask = 15;

    if (mode == 1) {
        pixelsPerHalfword = 2;
        bitsPerPixel = 8;
        mask = 255;
    }

    ivec2 word = ivec2(pageX + texel.x / pixelsPerHalfword, pageY + texel.y);
    uint value = texelFetch(vram, word, 0).r;
    uint index = (value >> (bitsPerPixel * (texel.x % pixelsPerHalfword))) & uint(mask);
    uint paletteWord = texelFetch(vram, ivec2(clutX + int(index), clutY), 0).r;

    float alpha = ((paletteWord & 0x8000u) != 0u) ? opacity : 1.0;

    if (paletteWord == 0u) {
        color = vec4(1.0, 0.0, 1.0, 1.0);
        return;
    }

    float r = float(paletteWord & 31u) / 31.0;
    float g = float((paletteWord >> 5) & 31u) / 31.0;
    float b = float((paletteWord >> 10) & 31u) / 31.0;

    color = vec4(r, g, b, alpha);
}