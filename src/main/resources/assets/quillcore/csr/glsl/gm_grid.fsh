#version 330 core
// FG2_GlobalMotion step 1: both L3 lumas on a 64x36 grid, four bilinear taps at +-1/4 cell.
uniform sampler2D uPrev;
uniform sampler2D uCurr;
out vec4 fragColor;

void main() {
	vec2 grid = vec2(64.0, 36.0);
	vec2 uv = gl_FragCoord.xy / grid;
	vec2 q = 0.25 / grid;
	float a = 0.0, b = 0.0;
	for (int t = 0; t < 4; t++) {
		vec2 o = vec2((t & 1) != 0 ? q.x : -q.x, (t & 2) != 0 ? q.y : -q.y);
		a += texture(uPrev, uv + o).r;
		b += texture(uCurr, uv + o).r;
	}
	fragColor = vec4(0.25 * a, 0.25 * b, 0.0, 0.0);
}
