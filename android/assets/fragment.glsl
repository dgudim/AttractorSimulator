
varying vec4 v_color;
varying vec2 v_texCoord0;

uniform sampler2D u_sampler2D_x;
uniform sampler2D u_sampler2D_y;

const float PI = 3.141592;

uniform float a;
uniform float b;
uniform float c;
uniform float d;

uniform int attractorType;

const float Precision = 100000000;

uniform int processY;

void main() {

    vec4 pixelColor_x = texture2D(u_sampler2D_x, v_texCoord0);
    vec4 pixelColor_y = texture2D(u_sampler2D_y, v_texCoord0);

    int valueFromPixel_x = ((int(pixelColor_x.r * 255) << 24) | (int(pixelColor_x.g * 255) << 16) | (int(pixelColor_x.b * 255) << 8) | (int(pixelColor_x.a * 255)));
    int valueFromPixel_y = ((int(pixelColor_y.r * 255) << 24) | (int(pixelColor_y.g * 255) << 16) | (int(pixelColor_y.b * 255) << 8) | (int(pixelColor_y.a * 255)));

    float x = valueFromPixel_x / Precision;
    float y = valueFromPixel_y / Precision;

    float newX;
    float newY;

    int outputPixel;

    if(processY == 1){
        switch (attractorType){
            case (0):
            newY = sin(c*x) - cos(d*y);
            break;
            case (1):
            newY = c * cos(a*x) + cos(b*y);
            break;
            case (2):
            case (8):
            newY = sin(b*x) + d*cos(b*y);
            break;
            case (3):
            newY = sin(c*x) + sin(d*y);
            break;
            case (4):
            newY = sin(c*x) - cos(d*y);
            break;
            case (5):
            case (6):
            newY = PI * sin(c*x) * cos(d*y);
            break;
            case (7):
            newY = a * sin(x + tan(b * x));
            break;
        }
        outputPixel = int(newY * Precision);
    }else{
        switch (attractorType){
            case (0):
            newX = sin(a*y) - cos(b*x);
            break;
            case (1):
            newX = d * sin(a*x) - sin(b*y);
            break;
            case (2):
            case (8):
            newX = sin(a*y) + c*cos(a*x);
            break;
            case (3):
            newX = cos(a*y) + cos(b*x);
            break;
            case (4):
            newX = cos(a*y) - sin(b*x);
            break;
            case (5):
            case (6):
            newX = PI * sin(a*y) * cos(b*x);
            break;
            case (7):
            newX = a * sin(y + tan(b * y));
            break;
        }
        outputPixel = int(newX * Precision);
    }

    gl_FragColor = vec4(
    (((outputPixel >> 24) & 0xff))/ 255.0,
    ((outputPixel >> 16) & 0xff) / 255.0,
    ((outputPixel >> 8) & 0xff) / 255.0,
    (((outputPixel >> 0) & 0xff))/ 255.0);
}