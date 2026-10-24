#version 330 core
// FG2_Luma (mode 0, rec709 of the colour frame) and FG2_Halve (mode 1, one bilinear tap at the
// destination texel centre of the level above).
uniform int uMode;
uniform sampler2D uSrc;
uniform vec2 uDstSize;
uniform vec3 uLuma;
out vec4 fragColor;

void main() {
	if (uMode == 0) {
		fragColor = vec4(dot(texelFetch(uSrc, ivec2(gl_FragCoord.xy), 0).rgb, uLuma));
	} else {
		fragColor = vec4(texture(uSrc, gl_FragCoord.xy / uDstSize).r);
	}
}
