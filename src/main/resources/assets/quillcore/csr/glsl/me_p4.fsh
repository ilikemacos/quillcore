#version 330 core
// FG2_P4Refine: double the L3 vector and take the best of its 3x3 integer neighbourhood on L2.
#include me_common
uniform sampler2D uMv;
uniform ivec2 uTiles;
out vec4 fragColor;

void main() {
	ivec2 t = ivec2(gl_FragCoord.xy);
	ivec2 p = ivec2(gl_FragCoord.xy * vec2(uLSize) / vec2(uTiles));
	vec2 seed = texelFetch(uMv, t, 0).xy * 2.0;
	float bestC = 1e20;
	vec2 best = seed;
	for (int k = 0; k < 9; k++) {
		vec2 v = seed + vec2(k % 3 - 1, k / 3 - 1);
		float c = matchCost(p, v);
		if (c < bestC) {
			bestC = c;
			best = v;
		}
	}
	fragColor = vec4(best, 0.0, 0.0);
}
