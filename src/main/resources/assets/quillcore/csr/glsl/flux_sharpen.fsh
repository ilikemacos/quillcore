#version 330 core
// CSRAI_Sharpen: CAS-style post sharpen. Not part of the network.
uniform sampler2D uColor;
uniform ivec2 uSize;
uniform float uSharpness;
out vec4 fragColor;

float safeRcp(float x) {
	return x == 0.0 ? 0.0 : 1.0 / x;
}

vec3 at(ivec2 p) {
	return texelFetch(uColor, clamp(p, ivec2(0), uSize - 1), 0).rgb;
}

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	vec3 e = at(p);
	vec3 b = at(p + ivec2(0, 1));
	vec3 d = at(p + ivec2(-1, 0));
	vec3 f = at(p + ivec2(1, 0));
	vec3 h = at(p + ivec2(0, -1));
	float bL = b.g, dL = d.g, eL = e.g, fL = f.g, hL = h.g;
	float nz = 0.25 * (bL + dL + fL + hL) - eL;
	float range = max(max(max(bL, dL), max(eL, fL)), hL) - min(min(min(bL, dL), min(eL, fL)), hL);
	nz = clamp(abs(nz) * safeRcp(range), 0.0, 1.0);
	nz = 1.0 - 0.5 * nz;
	vec3 mn4 = min(min(b, d), min(f, h));
	vec3 mx4 = max(max(b, d), max(f, h));
	vec3 hitMin = mn4 / max(4.0 * mx4, vec3(1e-6));
	vec3 hitMax = (1.0 - mx4) / min(4.0 * mn4 - 4.0, vec3(-1e-6));
	vec3 lobeRGB = max(-hitMin, hitMax);
	const float kLimit = 0.25 - (1.0 / 16.0);
	float lobe = max(-kLimit, min(max(max(lobeRGB.r, lobeRGB.g), lobeRGB.b), 0.0));
	lobe *= exp2(-uSharpness) * nz;
	vec3 o = (lobe * (b + d + f + h) + e) * safeRcp(4.0 * lobe + 1.0);
	fragColor = vec4(clamp(o, 0.0, 1.0), 1.0);
}
