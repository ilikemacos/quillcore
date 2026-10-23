#version 150

/*
 * AMD FidelityFX Super Resolution 1 — RCAS (Robust Contrast Adaptive Sharpening)
 * Algorithm © 2021 Advanced Micro Devices, Inc. MIT License.
 * Adapted for Minecraft 1.21.8 RenderPipeline fragment path (QuillCore).
 * See third_party/amd-fsr1/NOTICE.
 */

uniform sampler2D InSampler;

layout(std140) uniform FsrConfig {
    vec2 InputViewport;
    vec2 InputSize;
    vec2 OutputSize;
    float Sharpness;
    float _pad0;
};

in vec2 texCoord;
out vec4 fragColor;

#define FSR_RCAS_LIMIT (0.25 - (1.0 / 16.0))

vec3 FsrRcasLoadF(vec2 p) {
    return texture(InSampler, (p + 0.5) / InputSize).rgb;
}

void main() {
    float con = exp2(-Sharpness);
    vec2 sp = floor(texCoord * InputSize);

    vec3 b = FsrRcasLoadF(sp + vec2(0.0, -1.0));
    vec3 d = FsrRcasLoadF(sp + vec2(-1.0, 0.0));
    vec3 e = FsrRcasLoadF(sp);
    vec3 f = FsrRcasLoadF(sp + vec2(1.0, 0.0));
    vec3 h = FsrRcasLoadF(sp + vec2(0.0, 1.0));

    float bL = b.g + 0.5 * (b.r + b.b);
    float dL = d.g + 0.5 * (d.r + d.b);
    float eL = e.g + 0.5 * (e.r + e.b);
    float fL = f.g + 0.5 * (f.r + f.b);
    float hL = h.g + 0.5 * (h.r + h.b);

    vec3 mn4 = min(b, min(d, min(f, h)));
    vec3 mx4 = max(b, max(d, max(f, h)));
    vec2 peakC = vec2(1.0, -4.0);
    vec3 hitMin = mn4 / (4.0 * mx4);
    vec3 hitMax = (peakC.x - mx4) / (4.0 * mn4 + peakC.y);
    vec3 lobeRGB = max(-hitMin, hitMax);
    float lobe = max(-FSR_RCAS_LIMIT, min(max(lobeRGB.r, max(lobeRGB.g, lobeRGB.b)), 0.0)) * con;

    fragColor = vec4((lobe * (b + d + h + f) + e) / (4.0 * lobe + 1.0), 1.0);
}
