#version 460 core

// Draws the terminal framebuffer's texture into the window.
// The quad's UVs are already upright for the FBO (the cell shader flips Y once), so no flip here.
// The texture must be GL_NEAREST. With an integer viewport scale, every source pixel becomes an exact block.

uniform sampler2D screenTex;

in vec2 uv;
out vec4 fragColor;

void main() {
    fragColor = vec4(texture(screenTex, uv).rgb, 1.0);
}
