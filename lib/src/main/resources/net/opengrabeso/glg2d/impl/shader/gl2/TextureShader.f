#version 110

uniform sampler2D u_tex;
uniform vec4 u_color;

varying vec2 v_texCoord;

void main() {
  vec4 texel;

  texel = texture2D(u_tex, v_texCoord);
  gl_FragColor = u_color * texel;
}