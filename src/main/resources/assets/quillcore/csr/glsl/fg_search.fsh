#version 330 core
// Simplified hierarchical block match (stands in for the reference's FgV2 flow, which is not ported).
// One level: candidates = 2x the coarse vectors of the 3x3 coarse neighbourhood (or zero at the top),
// then a +-uRadius integer refine around the best. Cost = 3x3 SAD on luma + a small length penalty.
// oB: backward vector (curr pixel -> prev position), oF: forward (prev pixel -> curr), in this level's
// pixels, GL orientation; .z = SAD of the winner.
uniform sampler2D uPyr;
uniform ivec2 uSize;
uniform sampler2D uCoarseB;
uniform sampler2D uCoarseF;
uniform ivec2 uCoarseSize;
uniform int uHasCoarse;
uniform int uRadius;

layout(location = 0) out vec4 oB;
layout(location = 1) out vec4 oF;

vec2 L(ivec2 q) {
	return texelFetch(uPyr, clamp(q, ivec2(0), uSize - 1), 0).rg;
}

float cost(ivec2 p, ivec2 d, bool bwd) {
	float s = 0.0;
	for (int k = 0; k < 9; k++) {
		ivec2 o = ivec2(k % 3 - 1, k / 3 - 1);
		vec2 here = L(p + o);
		vec2 there = L(p + o + d);
		s += bwd ? abs(here.g - there.r) : abs(here.r - there.g);
	}
	return s + 0.004 * float(abs(d.x) + abs(d.y));
}

vec4 search(ivec2 p, bool bwd) {
	ivec2 best = ivec2(0);
	float bc = cost(p, best, bwd);
	if (uHasCoarse == 1) {
		for (int n = 0; n < 9; n++) {
			ivec2 cp = clamp((p >> 1) + ivec2(n % 3 - 1, n / 3 - 1), ivec2(0), uCoarseSize - 1);
			vec2 cv = bwd ? texelFetch(uCoarseB, cp, 0).xy : texelFetch(uCoarseF, cp, 0).xy;
			ivec2 c = ivec2(round(cv * 2.0));
			float cc = cost(p, c, bwd);
			if (cc < bc) {
				bc = cc;
				best = c;
			}
		}
	}
	ivec2 center = best;
	for (int dy = -uRadius; dy <= uRadius; dy++) {
		for (int dx = -uRadius; dx <= uRadius; dx++) {
			ivec2 d = center + ivec2(dx, dy);
			float cc = cost(p, d, bwd);
			if (cc < bc) {
				bc = cc;
				best = d;
			}
		}
	}
	return vec4(vec2(best), bc, 0.0);
}

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	oB = search(p, true);
	oF = search(p, false);
}
