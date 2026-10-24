#version 150

// Quill compact SOLID terrain: decodes the 20 B/vertex layout (render/mesh/QuillCompactVertex).
// Same uniform blocks and samplers as vanilla's SOLID terrain pipeline; only the vertex decode differs.

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in ivec4 Position; // xyz: (p + 8) * 2048 - 32768, section-local; w: face | slot << 3
in vec4 Color;     // RGBA, normalized
in ivec2 UV0;      // unsigned 16-bit in the low bits, uv * 65536
in ivec2 UV2;      // lightmap coords, as vanilla

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

vec4 quill_lightmap(sampler2D lightMap, ivec2 uv) {
    vec2 coord = clamp(vec2(uv) / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0));
    return texture(lightMap, coord);
}

void main() {
    vec3 local = (vec3(Position.xyz) + 32768.0) / 2048.0 - 8.0;
    // ModelOffset is the region origin minus the camera; the slot says which section of the 8x4x8 region this is
    int slot = (Position.w >> 3) & 0xFF;
    vec3 section = vec3(slot & 7, slot >> 6, (slot >> 3) & 7) * 16.0;
    vec3 pos = local + section + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * quill_lightmap(Sampler2, UV2);
    texCoord0 = vec2(UV0 & ivec2(0xFFFF)) / 65536.0;
}
