#version 330 core
// CSRAIFG_PackA at 1/4 res: [lumaPrev, lumaCurr, bwdX/32, bwdY/32, conf, occl, |dY|, hud].
// Vectors are handed to the net in the reference's top-down orientation (y flipped) and full-res pixels.
#include fg_common
uniform sampler2D uPyr4;
uniform sampler2D uBwd;
uniform sampler2D uFwd;
uniform ivec2 uQSize;
uniform float uFlowScale;

layout(location = 0) out vec4 o0;
layout(location = 1) out vec4 o1;

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	vec4 L = texelFetch(uPyr4, p, 0);
	vec3 bw = texelFetch(uBwd, p, 0).xyz;
	vec2 v = bw.xy;
	ivec2 fp = clamp(p + ivec2(v), ivec2(0), uQSize - 1);
	vec2 f = texelFetch(uFwd, fp, 0).xy;
	// Forward/backward consistency error in full-res pixels -> occlusion estimate.
	float occl = clamp(length(v + f) * 4.0 / 8.0, 0.0, 1.0);
	float conf = clamp(1.0 - bw.z / 0.9, 0.0, 1.0);
	vec2 vbN = vec2(v.x, -v.y) * 4.0 / uFlowScale;
	ivec2 c = ivec2((vec2(p) + 0.5) / vec2(uQSize) * uFullSizeF);
	o0 = vec4(L.r, L.g, vbN.x, vbN.y);
	o1 = vec4(conf, occl, L.b, hudAt(c));
}
