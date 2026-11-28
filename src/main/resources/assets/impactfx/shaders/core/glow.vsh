#version 330

in vec3 Position;
in vec4 Color;

layout(std140) uniform GlowData {
    mat4 Proj;
};

out vec4 vColor;

void main() {
    gl_Position = Proj * vec4(Position, 1.0);
    vColor = Color;
}
