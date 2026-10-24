#version 330 core
// CSRAIFG_PackB at 1/2 res: 12 channels for netB.
#include fg_common
#include fg_flow
uniform sampler2D uPrev;
uniform sampler2D uCurr;
uniform sampler2D uPackA1;
uniform sampler2D uNetA1;
uniform vec2 uHSize;
uniform float uT;

layout(location = 0) out vec4 o0;
layout(location = 1) out vec4 o1;
layout(location = 2) out vec4 o2;

void main() {
	vec2 uv = gl_FragCoord.xy / uHSize;
	float t = uT;
	vec2 v = fgV(uv);
	vec2 pA = uv + t * v;
	vec2 pB = uv - (1.0 - t) * v;
	float y0 = luma709(texture(uPrev, pA).rgb);
	float y1 = luma709(texture(uCurr, pB).rgb);
	float yu0 = luma709(texture(uPrev, uv).rgb);
	float yu1 = luma709(texture(uCurr, uv).rgb);
	ivec2 texel = clamp(ivec2(floor(uv * uFullSizeF)), ivec2(1), uFullSize - 2);
	float sumY = 0.0, sumY2 = 0.0;
	for (int k = 0; k < 9; k++) {
		float y = luma709(texelFetch(uCurr, texel + ivec2(k % 3 - 1, k / 3 - 1), 0).rgb);
		sumY += y;
		sumY2 += y * y;
	}
	float meanY = sumY * (1.0 / 9.0);
	float localVar = sumY2 * (1.0 / 9.0) - meanY * meanY;
	vec4 na0 = texture(uNetA0, uv);
	vec4 na1 = texture(uNetA1, uv);
	o0 = vec4(y0, y1, abs(y1 - y0), 0.5 * (yu0 + yu1));
	o1 = vec4(localVar, texture(uPackA1, uv).x, na0.z, t);
	o2 = vec4(na0.w, na1.x, na1.y, na1.z);
}
