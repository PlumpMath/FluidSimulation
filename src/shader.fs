uniform sampler2D texture1;
uniform float speed;

void main() {
	vec4 color = texture2D(texture1, gl_TexCoord[0].st);
	float threshold = 0.2f;
	//(0.15, 0.25, 0.7, 1)
		
	color.r = (color.a >= threshold) ? 0.15f + speed : 0.0f;
	color.g = (color.a >= threshold) ? 0.25f + speed : 0.0f;
	color.b = (color.a >= threshold) ? 0.7f + speed : 0.0f;
	color.a = (color.a >= threshold) ? 1.0f : 0.0f;
	
    gl_FragColor = color;
}