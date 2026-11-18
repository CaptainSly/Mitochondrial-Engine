#version 460 core

// Milestone 1: show the whole glyph atlas in the bottom-left corner of the window.

uniform sampler2D glyphSheet;
uniform int zoom;

out vec4 fragColor;

void main() {
    ivec2 size = textureSize(glyphSheet, 0);

    // gl_FragCoord starts at the bottom-left of the window; divide by zoom to scale up in whole pixels.
    ivec2 pixel = ivec2(gl_FragCoord.xy) / zoom;

    // Outside the sheet: dark blue-gray, so the sheet's own black (off) pixels stay visible.
    if (pixel.x >= size.x || pixel.y >= size.y) {
        fragColor = vec4(0.15, 0.15, 0.20, 1.0);
        return;
    }

    // Row 0 of the uploaded data is the TOP row of glyphs, but pixel.y counts up from the bottom,
    // so flip it. texelFetch takes integer coordinates, so no UV math and no filtering.
    ivec2 texel = ivec2(pixel.x, size.y - 1 - pixel.y);
    float mask = texelFetch(glyphSheet, texel, 0).r;

    fragColor = vec4(mask, mask, mask, 1.0);
}
