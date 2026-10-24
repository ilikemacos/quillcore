#version 330 core
// Generic conv3x3, stride 1, clamp-to-edge (port of CSRAI_Conv / CSRAIFG_Conv).
// Channels are packed 4 per RGBA16F attachment: up to 5 input and 5 output slabs (20 channels).
// Weights: w[(oc*inC+ic)*9 + ky*3+kx], b[oc] in an R32F texture 1024 texels wide.
// The reference indexes rows top-down; GL rows run bottom-up, so kernel row ky maps to dy = 1 - ky.
uniform sampler2D uIn0;
uniform sampler2D uIn1;
uniform sampler2D uIn2;
uniform sampler2D uIn3;
uniform sampler2D uIn4;
uniform sampler2D uW;
uniform int uInC;
uniform int uOutC;
uniform int uWOff;
uniform int uBOff;
uniform int uLeaky;
uniform ivec2 uSize;

layout(location = 0) out vec4 o0;
layout(location = 1) out vec4 o1;
layout(location = 2) out vec4 o2;
layout(location = 3) out vec4 o3;
layout(location = 4) out vec4 o4;

float wt(int i) {
	return texelFetch(uW, ivec2(i & 1023, i >> 10), 0).r;
}

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	int slabs = (uInC + 3) / 4;
	vec4 taps[45];
	for (int k = 0; k < 9; k++) {
		ivec2 q = clamp(p + ivec2(k % 3 - 1, 1 - k / 3), ivec2(0), uSize - 1);
		taps[k] = texelFetch(uIn0, q, 0);
		taps[9 + k] = slabs > 1 ? texelFetch(uIn1, q, 0) : vec4(0.0);
		taps[18 + k] = slabs > 2 ? texelFetch(uIn2, q, 0) : vec4(0.0);
		taps[27 + k] = slabs > 3 ? texelFetch(uIn3, q, 0) : vec4(0.0);
		taps[36 + k] = slabs > 4 ? texelFetch(uIn4, q, 0) : vec4(0.0);
	}
	float o[20];
	for (int oc = 0; oc < 20; oc++) {
		o[oc] = 0.0;
	}
	for (int oc = 0; oc < uOutC; oc++) {
		float acc = wt(uBOff + oc);
		for (int ic = 0; ic < uInC; ic++) {
			int base = uWOff + (oc * uInC + ic) * 9;
			int s = (ic >> 2) * 9;
			int c = ic & 3;
			for (int k = 0; k < 9; k++) {
				acc += wt(base + k) * taps[s + k][c];
			}
		}
		if (uLeaky == 1 && acc < 0.0) {
			acc *= 0.1;
		}
		o[oc] = acc;
	}
	o0 = vec4(o[0], o[1], o[2], o[3]);
	o1 = vec4(o[4], o[5], o[6], o[7]);
	o2 = vec4(o[8], o[9], o[10], o[11]);
	o3 = vec4(o[12], o[13], o[14], o[15]);
	o4 = vec4(o[16], o[17], o[18], o[19]);
}
