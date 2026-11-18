#version 460 core

// Fullscreen triangle generated from gl_VertexID, no vertex buffer needed.
// IDs 0, 1, 2 -> clip-space corners (-1,-1), (3,-1), (-1,3), which covers the whole screen.
void main() {
    vec2 position = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(position * 2.0 - 1.0, 0.0, 1.0);
}
