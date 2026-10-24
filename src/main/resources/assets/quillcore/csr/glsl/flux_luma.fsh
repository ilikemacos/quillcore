#version 330 core
// CSRAI_Luma: the only network input.
uniform sampler2D uColor;
uniform vec3 uLuma;
out vec4 fragColor;

void main() {
	vec3 rgb = texelFetch(uColor, ivec2(gl_FragCoord.xy), 0).rgb;
	fragColor = vec4(dot(rgb, uLuma), 0.0, 0.0, 0.0);
}
