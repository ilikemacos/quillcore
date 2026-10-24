#version 330 core
// CSRAIFG_PackA at 1/4 res: [lumaPrev, lumaCurr, bwdX/32, bwdY/32, conf, occl, |dY|, hud].
// The vector is pickTileV: of the 9 nearest tile vectors, the one whose half-way colour match is best.
// Vectors go to the net in the reference's top-down orientation (y flipped) and full-res pixels.
#include fg_common
uniform sampler2D uPyr4;
uniform sampler2D uMv;
uniform sampler2D uOccl;
uniform sampler2D uPrev;
uniform ivec2 uQSize;
uniform ivec2 uTiles;
uniform float uFlowScale;

layout(location = 0) out vec4 o0;
layout(location = 1) out vec4 o1;

vec4 tileAt(vec2 uv) {
	return texelFetch(uMv, clamp(ivec2(floor(uv * vec2(uTiles))), ivec2(0), uTiles - 1), 0);
}

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	vec2 uv = gl_FragCoord.xy / vec2(uQSize);
	vec2 os = 1.0 / vec2(uTiles);
	vec4 s0 = tileAt(uv);
	vec2 best = s0.xy / uFullSizeF;
	float conf = s0.z;
	float bc = 1e20;
	for (int k = 0; k < 9; k++) {
		vec4 s = tileAt(uv + vec2(k % 3 - 1, k / 3 - 1) * os);
		vec2 v = s.xy / uFullSizeF;
		vec2 pB = uv - 0.5 * v;
		vec3 d = abs(texture(uPrev, pB + v).rgb - texture(uCurrFinal, pB).rgb);
		float c = d.r + d.g + d.b;
		if (c < bc) {
			bc = c;
			best = v;
			conf = s.z;
		}
	}
	vec2 vbN = vec2(best.x, -best.y) * uFullSizeF / uFlowScale;
	float occl = texelFetch(uOccl, clamp(ivec2(floor(uv * vec2(uTiles))), ivec2(0), uTiles - 1), 0).r;
	vec4 L = texelFetch(uPyr4, p, 0);
	ivec2 c = ivec2(uv * uFullSizeF);
	o0 = vec4(L.r, L.g, vbN.x, vbN.y);
	o1 = vec4(conf, occl, L.b, hudAt(c));
}
