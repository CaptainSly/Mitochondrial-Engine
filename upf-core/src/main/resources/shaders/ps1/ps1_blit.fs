#version 460 core

in vec2 fragUV;

uniform sampler2D screen;
uniform vec3 fade;
uniform int fadeBlend;

out vec4 color;

void main() {
    vec3 rgb = texture(screen, fragUV).rgb;
    vec3 result;

    if (fadeBlend == 0) {
        result = (rgb + fade) * 0.5;
    } else if (fadeBlend == 1) {
        result = min(rgb + fade, 1.0);
    } else if (fadeBlend == 3) {
        result = min(rgb + fade * 0.25, 1.0);
    } else {
        result = max(rgb - fade, 0.0);
    }

    color = vec4(result, 1.0);
}