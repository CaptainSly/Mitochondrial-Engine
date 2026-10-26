#version 460 core

uniform usampler2D vram;

flat in ivec2 fragPageClut;
in vec2 texelPos;

out vec4 color;

void main() {
    ivec2 texel = ivec2(texelPos);
    int pageX = (fragPageClut.x & 15) * 64;
    int pageY = ((fragPageClut.x >> 4) & 1) * 256;
    int clutX = (fragPageClut.y & 63) * 16;
    int clutY = fragPageClut.y >> 6;

    ivec2 word = ivec2(pageX + texel.x / 4, pageY + texel.y);
    uint value = texelFetch(vram, word, 0).r;
    uint index = (value >> (4 * (texel.x % 4))) & 15u;
    uint paletteWord = texelFetch(vram, ivec2(clutX + int(index), clutY), 0).r;
    if (paletteWord == 0u) {
        color = vec4(1.0, 0.0, 1.0, 1.0);
        return;
    }

    float r = float(paletteWord & 31u) / 31.0;
    float g = float((paletteWord >> 5) & 31u) / 31.0;
    float b = float((paletteWord >> 10) & 31u) / 31.0;

    color = vec4(r, g, b, 1.0);
}