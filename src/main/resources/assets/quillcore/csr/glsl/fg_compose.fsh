#version 330 core
// CSRAIFG_Compose at full res: warp both frames by the predicted flow, blend with the
// netB mask logit, add the rgb residual, then mix towards the plain blend by sigmoid(lp).
#include fg_common
#include fg_flow
uniform sampler2D uPrev;
uniform sampler2D uCurr;
uniform sampler2D uNetB0;
uniform sampler2D uNetB1;
uniform sampler2D uPyr16;
uniform float uCutLod;
uniform float uCutThresh;
uniform float uT;
uniform float uMaskA;
uniform float uMaskB;
uniform int uLpSigmoid;
out vec4 fragColor;

void main() {
	vec2 uv = gl_FragCoord.xy / uFullSizeF;
	float t = uT;
	vec3 a0 = texture(uPrev, uv).rgb;
	vec3 b0 = texture(uCurr, uv).rgb;
	// Scene cut: mean |dLuma| over the frame from the top mip -> show the nearest real frame.
	if (textureLod(uPyr16, vec2(0.5), uCutLod).b > uCutThresh) {
		fragColor = vec4(t < 0.5 ? a0 : b0, 1.0);
		return;
	}
	if (hudAt(ivec2(gl_FragCoord.xy)) > 0.5) {
		fragColor = vec4(b0, 1.0);
		return;
	}
	vec2 v = fgV(uv);
	vec2 pA = uv + t * v;
	vec2 pB = uv - (1.0 - t) * v;
	vec3 a = texture(uPrev, pA).rgb;
	vec3 b = texture(uCurr, pB).rgb;
	float occ = texture(uNetA0, uv).z;
	vec4 nb0 = texture(uNetB0, uv);
	vec4 nb1 = texture(uNetB1, uv);
	float md = nb0.x;
	float lpRaw = nb0.y;
	vec3 resAdd = vec3(nb0.z, nb0.w, nb1.x);
	float tc = clamp(t, 1e-6, 1.0 - 1e-6);
	float logit = log(tc / (1.0 - tc)) + occ * uMaskA + uMaskB + md;
	float w = 1.0 / (1.0 + exp(-logit));
	bool okA = inside(pA), okB = inside(pB);
	if (!okA && okB) w = 1.0;
	if (!okB && okA) w = 0.0;
	vec3 warped = (1.0 - w) * a + w * b + resAdd;
	float lp = uLpSigmoid == 1 ? 1.0 / (1.0 + exp(-lpRaw)) : clamp(lpRaw, 0.0, 1.0);
	vec3 avgUv = mix(a0, b0, t);
	fragColor = vec4(clamp(mix(warped, avgUv, lp), 0.0, 1.0), 1.0);
}
