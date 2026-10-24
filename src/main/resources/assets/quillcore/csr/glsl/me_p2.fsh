#version 330 core
// FG2_P2Final: one 5x5 Lucas-Kanade step on L1 around the doubled L2 vector.
// Out = (vector in full-res pixels, confidence, 0).
#include me_common
uniform sampler2D uMv;
uniform ivec2 uTiles;
uniform vec2 uInSize;
out vec4 fragColor;

const float OCCL_THRESH = 1.0;

void main() {
	ivec2 t = ivec2(gl_FragCoord.xy);
	ivec2 hi = uLSize - 1;
	ivec2 pix = clamp(ivec2(gl_FragCoord.xy * vec2(uLSize) / vec2(uTiles)), ivec2(0), hi);
	vec2 texel = 1.0 / vec2(uLSize);
	vec2 uv = (vec2(pix) + 0.5) * texel;
	vec2 mvUV = texelFetch(uMv, t, 0).xy * 2.0 * texel;

	float cv[25];
	vec2 gr[25];
	vec3 a = vec3(0.0);
	for (int k = 0; k < 25; k++) {
		ivec2 o = ivec2(k % 5 - 2, k / 5 - 2);
		ivec2 q = pix + o;
		float gx = 0.5 * (lB(q + ivec2(1, 0)) - lB(q - ivec2(1, 0)));
		float gy = 0.5 * (lB(q + ivec2(0, 1)) - lB(q - ivec2(0, 1)));
		float wk = exp(-0.25 * float(o.x * o.x + o.y * o.y));
		cv[k] = lB(q);
		gr[k] = vec2(gx, gy) * wk;
		a += vec3(gx * gx, gx * gy, gy * gy) * wk;
	}
	float a11 = a.x + 1e-4, a12 = a.y, a22 = a.z + 1e-4;
	float det = a11 * a22 - a12 * a12;
	float b1 = 0.0, b2 = 0.0;
	for (int k = 0; k < 25; k++) {
		vec2 o = vec2(k % 5 - 2, k / 5 - 2);
		float e = texture(uA, uv + o * texel + mvUV).r - cv[k];
		b1 += gr[k].x * e;
		b2 += gr[k].y * e;
	}
	vec2 st = -vec2(a22 * b1 - a12 * b2, a11 * b2 - a12 * b1) / det;
	vec2 d = clamp(clamp(st, -0.75, 0.75), -1.5, 1.5);
	mvUV += d * texel;

	float cost = matchCost(pix, mvUV / texel);
	float conf = clamp(1.0 - cost / (OCCL_THRESH * 9.0), 0.0, 1.0);
	fragColor = vec4(mvUV * uInSize, conf, 0.0);
}
