#version 130

uniform sampler2D Texture;
uniform vec4 Color;
varying vec2 Coord0;

void main() {
   float t = texture(Texture,Coord0).r;
   gl_FragColor = Color * t;
}
