#version 460 core

in vec2 fragUV;

uniform sampler2D screen;
uniform float fade;

out vec4 color;

void main() {
    vec3 rgb = texture(screen, fragUV).rgb;
    color = vec4(max(rgb - vec3(fade), 0.0), 1.0);
}