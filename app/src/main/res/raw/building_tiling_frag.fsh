#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

precision mediump float;

layout ( location = 0 ) in vec2  coord;
layout(set = 0, binding = 4) uniform sampler2D u_texture;

@MATERIAL_UNIFORMS

layout (location = 0) out vec4 outColor;

void main()
{
    vec2 uv = coord * scale.z * vec2(scale.y, scale.x);
    outColor = texture(u_texture, uv);
}