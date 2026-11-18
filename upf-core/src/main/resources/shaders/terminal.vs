#version 460 core

// Fullscreen triangle from gl_VertexID, no vertex buffer needed (draw 3 vertices with an empty VAO bound).
void main() {
    vec2 position = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(position * 2.0 - 1.0, 0.0, 1.0);
}
