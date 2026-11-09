#version 460 core

in vec2 fragUV;

uniform sampler2D screen;
uniform vec3 fade;

out vec4 color;

void main() {
    vec3 rgb = texture(screen, fragUV).rgb;
    color = vec4(max(rgb - fade, 0.0), 1.0);
}