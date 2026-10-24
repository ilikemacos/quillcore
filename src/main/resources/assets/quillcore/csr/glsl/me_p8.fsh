#version 330 core
// FG2_P8Search on L3, one texel per 8x8 tile: full +-4 search (SAD + census), block consensus,
// then the history and global-motion candidates. Out = (vector, SAD, census distance).
#include me_common
uniform sampler2D uBlock;
uniform sampler2D uHist;
uniform sampler2D uGmv;
uniform int uHistValid;
uniform vec2 uInSize;
out vec4 fragColor;

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	// 11x11 window of A around p covers every candidate's 3x3 SAD and census.
	float a[121];
	for (int k = 0; k < 121; k++) {
		a[k] = lA(p + ivec2(k % 11 - 5, k / 11 - 5));
	}
	float c[9];
	for (int j = 0; j < 9; j++) {
		c[j] = lB(p + ivec2(j % 3 - 1, j / 3 - 1));
	}
	uint cenC = censusB(p);
	float bestCost = 1e20, bestSad = 0.0, bestCen = 0.0;
	ivec2 best = ivec2(0);
	for (int ci = 0; ci < 81; ci++) {
		int dx = ci % 9 - 4, dy = ci / 9 - 4;
		int ctr = (dy + 5) * 11 + dx + 5;
		float sad = 0.0;
		for (int j = 0; j < 9; j++) {
			sad += abs(a[ctr + (j / 3 - 1) * 11 + j % 3 - 1] - c[j]);
		}
		uint cenP = 0u;
		for (int k = 0; k < 8; k++) {
			ivec2 r = ring8(k);
			if (a[ctr + r.y * 11 + r.x] > a[ctr]) cenP |= 1u << uint(k);
		}
		float ham = float(popc8(cenP ^ cenC));
		float cost = SAD_W * sad + CENSUS_W * ham + 0.004 * float(abs(dx) + abs(dy));
		if (cost < bestCost) {
			bestCost = cost;
			best = ivec2(dx, dy);
			bestSad = sad;
			bestCen = ham;
		}
	}
	ivec2 blockV = ivec2(texelFetch(uBlock, p / 8, 0).xy);
	float cBlock = sad3(p, blockV);
	if (cBlock <= bestSad + 0.06) {
		best = blockV;
		bestSad = cBlock;
		bestCen = hamming(p, best);
		bestCost = SAD_W * bestSad + CENSUS_W * bestCen + 0.004 * float(abs(best.x) + abs(best.y));
	}
	if (uHistValid != 0) {
		vec2 tv = texelFetch(uHist, p, 0).xy / 8.0;
		float cost = matchCost(p, tv);
		if (cost < bestCost) {
			bestCost = cost;
			best = ivec2(round(tv));
			bestSad = sad3(p, best);
			bestCen = hamming(p, best);
		}
	}
	vec2 tv = texelFetch(uGmv, ivec2(0), 0).xy * uInSize / 8.0;
	if (dot(tv, tv) > 1e-8) {
		float cost = matchCost(p, tv);
		if (cost < bestCost) {
			best = ivec2(round(tv));
			bestSad = sad3(p, best);
			bestCen = hamming(p, best);
		}
	}
	fragColor = vec4(vec2(best), bestSad, bestCen);
}
