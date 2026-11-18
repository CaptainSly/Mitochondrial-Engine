#version 460 core

// For FramebufferQuad's Mesh: location 0 = position (vec3), location 1 = uv (vec2).
// The quad's corners are at +-0.5, so scale by 2 to cover the whole viewport.
// Set glViewport to the (integer-scaled, centered) rectangle you want the image to fill.

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aUv;

out vec2 uv;

void main() {
    uv = aUv;
    gl_Position = vec4(aPosition.xy * 2.0, 0.0, 1.0);
}
