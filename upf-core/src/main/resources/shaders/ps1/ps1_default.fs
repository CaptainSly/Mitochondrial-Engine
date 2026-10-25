#version 460 core

in vec2 fragUV;

out vec4 color;

uniform usampler2D vram;
uniform vec2 pageOrigin;
uniform vec2 pageSize;

void main() {
    // Which pixel this fragment is on
    ivec2 pixel = ivec2(fragUV * pageSize);

    // Quad has v = 1 at the top
    // Flip to make row 0 top row
    pixel.y = int(pageSize.y) - 1 - pixel.y;

    ivec2 word = ivec2(pageOrigin) + ivec2(pixel.x / 2, pixel.y);
    uint value = texelFetch(vram, word, 0).r;
    uint byteValue = (pixel.x % 2 == 0) ? (value & 0xFFu) : (value >> 8);

    float gray = float(byteValue) / 255.0;
    color = vec4(gray, gray, gray, 1.0);
}