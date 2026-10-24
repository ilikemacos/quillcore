#version 330 core
// FG2_Consistency: forward/backward disagreement at the tile the backward vector lands on.
uniform sampler2D uBwd;
uniform sampler2D uFwd;
uniform vec2 uTiles;
out vec4 fragColor;

const float OCCL_THRESH = 1.0;

void main() {
	vec2 bwd = texelFetch(uBwd, ivec2(gl_FragCoord.xy), 0).xy;
	vec2 dest = gl_FragCoord.xy + bwd / 8.0;
	vec2 fwd = texture(uFwd, dest / uTiles).xy;
	float e = length(fwd + bwd);
	float thr = OCCL_THRESH;
	fragColor = vec4(clamp((e - 0.5 * thr) / (0.5 * thr), 0.0, 1.0));
}
