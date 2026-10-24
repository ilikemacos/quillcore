#version 330 core
// FG2_P8Prop: pick among the 3x3 neighbour vectors by match cost + lambda * mean distance to the others.
#include me_common
uniform sampler2D uMv;
uniform ivec2 uTiles;
out vec4 fragColor;

const float LAMBDA = 0.02;

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	vec2 vs[9];
	float cost[9];
	for (int k = 0; k < 9; k++) {
		ivec2 q = clamp(p + ivec2(k % 3 - 1, k / 3 - 1), ivec2(0), uTiles - 1);
		vs[k] = texelFetch(uMv, q, 0).xy;
		ivec2 d = ivec2(round(vs[k]));
		cost[k] = sad3(p, d) + CENSUS_W * hamming(p, d);
	}
	int bestI = 0;
	float bestC = 1e20;
	for (int i = 0; i < 9; i++) {
		float sm = 0.0;
		for (int j = 0; j < 9; j++) {
			if (j != i) sm += length(vs[i] - vs[j]);
		}
		float c = cost[i] + LAMBDA * sm * 0.125;
		if (c < bestC) {
			bestC = c;
			bestI = i;
		}
	}
	vec4 self = texelFetch(uMv, p, 0);
	fragColor = vec4(vs[bestI], self.z, self.w);
}
