#version 330 core
// Box-filtered luma for PackA (1/4) and the scene-cut mean (1/16 + mips). r = prev, g = curr, b = |curr - prev|.
// Mode 0: 4x4 box average straight from the two colour frames (as CSRAIFG_PackA does).
// Mode 1: 2x2 average of the previous pyramid level.
uniform int uMode;
uniform sampler2D uPrev;
uniform sampler2D uCurr;
uniform sampler2D uSrc;
uniform ivec2 uSrcSize;
uniform vec3 uLuma;
out vec4 fragColor;

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	ivec2 hi = uSrcSize - 1;
	if (uMode == 0) {
		float y0 = 0.0, y1 = 0.0;
		for (int dy = 0; dy < 4; dy++) {
			for (int dx = 0; dx < 4; dx++) {
				ivec2 q = clamp(p * 4 + ivec2(dx, dy), ivec2(0), hi);
				y0 += dot(texelFetch(uPrev, q, 0).rgb, uLuma);
				y1 += dot(texelFetch(uCurr, q, 0).rgb, uLuma);
			}
		}
		y0 *= 1.0 / 16.0;
		y1 *= 1.0 / 16.0;
		fragColor = vec4(y0, y1, abs(y1 - y0), 0.0);
	} else {
		vec4 s = vec4(0.0);
		for (int k = 0; k < 4; k++) {
			s += texelFetch(uSrc, clamp(p * 2 + ivec2(k & 1, k >> 1), ivec2(0), hi), 0);
		}
		fragColor = s * 0.25;
	}
}
