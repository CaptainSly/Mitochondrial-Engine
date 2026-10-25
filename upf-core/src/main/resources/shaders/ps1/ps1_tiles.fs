#version 460 core

uniform usampler2D vram;

flat in ivec4 fragA;
flat in ivec4 fragB;

in vec2 texelInTile;
out vec4 color;

void main() {
    ivec2 texel = ivec2(texelInTile);
    
    int texelX = fragA.z + texel.x;
    int texelY = fragA.w + texel.y;
    
    ivec2 word = ivec2(fragB.x + texelX / 2, fragB.y + texelY);

    uint value = texelFetch(vram, word, 0).r;
    uint index = (texelX % 2 == 0) ? (value & 0xFFu) : (value >> 8);

    uint paletteWord = texelFetch(vram, ivec2(fragB.z + int(index), fragB.w), 0).r;

    if (paletteWord == 0u) discard;

    float r = float(paletteWord & 31u) / 31.0;
    float g = float((paletteWord >> 5) & 31u) / 31.0;
    float b = float((paletteWord >> 10) & 31u) / 31.0;

    color = vec4(r, g, b, 1.0);
}