#version 460 core
layout(location = 0) in ivec4 tileA;
layout(location = 1) in ivec4 tileB;

flat out ivec4 fragA;
flat out ivec4 fragB;

uniform ivec2 scroll;

out vec2 texelInTile;

void main() {
    ivec2 corner = ivec2(gl_VertexID & 1, gl_VertexID >> 1);
    vec2 pixelPos = vec2(tileA.xy + scroll) + vec2(corner) * 16.0;

    gl_Position = vec4(pixelPos.x / 160.0 - 1.0, 1.0 - pixelPos.y / 112.0, 0.0, 1.0);
    texelInTile = vec2(corner) * 16.0;
    fragA = tileA;
    fragB = tileB;
}