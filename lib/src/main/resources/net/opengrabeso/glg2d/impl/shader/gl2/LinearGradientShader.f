#version 110

uniform sampler2D u_gradient;
uniform vec4 u_gradientLine;
uniform int u_gradientCycle;
uniform float u_gradientAlpha;
uniform float u_gradientTextureScale;
uniform float u_gradientTextureOffset;
varying vec2 v_userCoord;

void main() {
  float lengthSquared = dot(u_gradientLine.zw, u_gradientLine.zw);
  float t = lengthSquared == 0.0 ? 0.0 : dot(v_userCoord - u_gradientLine.xy, u_gradientLine.zw) / lengthSquared;
  if (u_gradientCycle == 1) {
    t = fract(t);
  } else if (u_gradientCycle == 2) {
    t = 1.0 - abs(mod(t, 2.0) - 1.0);
  } else {
    t = clamp(t, 0.0, 1.0);
  }
  float lookup = u_gradientTextureOffset + t * u_gradientTextureScale;
  vec4 color = texture2D(u_gradient, vec2(lookup, 0.5));
  gl_FragColor = vec4(color.rgb, color.a * u_gradientAlpha);
}
