#version 110
uniform mat4 u_transform;

attribute vec2 a_vertCoord;
varying vec2 v_userCoord;

void main() {
  gl_Position = u_transform * vec4(a_vertCoord, 0, 1);
  v_userCoord = a_vertCoord;
}
