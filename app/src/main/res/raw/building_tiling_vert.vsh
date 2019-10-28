#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

precision mediump float;
layout(location = 0) in vec4 a_position;
layout(location = 1) in vec2 a_texcoord;
@MATRIX_UNIFORMS

layout(location = 0) out vec2 coord;


void main()
{
    coord = a_texcoord;
	gl_Position = u_mvp * a_position;
}