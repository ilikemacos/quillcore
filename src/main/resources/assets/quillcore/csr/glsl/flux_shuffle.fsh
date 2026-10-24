#version 330 core
// CSRAI_Shuffle: pixel-shuffle x2 of the 4 net channels + bilinear residual, at 2x the LR size.
// Reference channel = ((y&1)<<1)|(x&1) with y top-down; with GL bottom-up rows and an
// even HR height the row parity flips.
uniform sampler2D uColor; // LR colour, linear clamp sampler
uniform sampler2D uFeat;  // last layer, 4 channels
uniform vec2 uLrSize;
uniform ivec2 uLrSizeI;
uniform vec3 uLuma;
out vec4 fragColor;

void main() {
	ivec2 hr = ivec2(gl_FragCoord.xy);
	ivec2 lr = min(hr >> 1, uLrSizeI - 1);
	int c = ((1 - (hr.y & 1)) << 1) | (hr.x & 1);
	float yDelta = texelFetch(uFeat, lr, 0)[c];
	vec2 uv = (vec2(hr) + 0.5) * 0.5 / uLrSize;
	vec3 rgb = textureLod(uColor, uv, 0.0).rgb;
	float yUp = dot(rgb, uLuma);
	float yNet = yDelta + yUp;
	fragColor = vec4(clamp(rgb + (yNet - yUp), 0.0, 1.0), 1.0);
}
