#version 460 core

// Draws the terminal grid into the terminal framebuffer, which is (cols * glyphWidth) x (rows * glyphHeight).
//
// cellTex   : RGBA32UI, one texel per terminal cell, (glyph, fg, bg, flags). Must be GL_NEAREST.
// glyphSheet: the 16x16 glyph atlas, mask in the red channel. Row 0 of its data is the TOP row of glyphs.
//
// Colors are read with unpackUnorm4x8, where the LOWEST byte becomes .x (red). Pack colors on the CPU as
// 0xAABBGGRR (red in the low byte), or red and blue will swap.
//
// No uniforms besides the two samplers: grid size and glyph size are derived from the textures.

uniform usampler2D cellTex;
uniform sampler2D glyphSheet;

const int GLYPHS_PER_ROW = 16;

out vec4 fragColor;

void main() {
    ivec2 gridSize = textureSize(cellTex, 0);                       // (cols, rows)
    ivec2 glyphSize = textureSize(glyphSheet, 0) / GLYPHS_PER_ROW;  // (8, 8) for the Unscii-8 atlas

    // gl_FragCoord counts up from the bottom. Flip once here so row 0 is the top row, like the cell data.
    // The result is a normal upright image in GL terms, so the present pass needs no flip.
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    pixel.y = gridSize.y * glyphSize.y - 1 - pixel.y;

    ivec2 cell = pixel / glyphSize;     // which terminal cell
    ivec2 local = pixel % glyphSize;    // which pixel inside that cell

    uvec4 data = texelFetch(cellTex, cell, 0);   // (glyph, fg, bg, flags)

    int glyph = int(data.x);
    ivec2 tile = ivec2(glyph % GLYPHS_PER_ROW, glyph / GLYPHS_PER_ROW);

    // Atlas rows are already top-down, and so is `local`, so no second flip.
    float mask = texelFetch(glyphSheet, tile * glyphSize + local, 0).r;

    vec4 fg = unpackUnorm4x8(data.y);
    vec4 bg = unpackUnorm4x8(data.z);

    fragColor = vec4(mix(bg.rgb, fg.rgb, mask), 1.0);
}
