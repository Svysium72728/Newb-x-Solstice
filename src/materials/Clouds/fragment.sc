$input v_color0, v_color1, v_color2, v_dayFactor
#include <newb/config.h>

#include <bgfx_shader.sh>
#include <newb/main.sh>

uniform vec4 CameraPosition;

#define saturate(x) clamp(x, 0.0, 1.0)

void main() {
  vec4 color = v_color0; 

  #if NL_CLOUD_TYPE == 2
    vec3 vDir = normalize(v_color0.xyz);
    vec3 cloudPos = v_color0.xyz;
    cloudPos.xz += CameraPosition.xz;

    #define NL_CLOUD_PARAMS(x) NL_CLOUD2##x##STEPS, NL_CLOUD2##x##THICKNESS, NL_CLOUD2##x##RAIN_THICKNESS, NL_CLOUD2##x##VELOCITY, NL_CLOUD2##x##SCALE, NL_CLOUD2##x##DENSITY, NL_CLOUD2##x##SHAPE
    color = vec4(0.0, 0.0, 0.0, 0.0);
    if (vDir.y > 0.05) {
    color = renderCloudsRounded(vDir, cloudPos, v_color1.w, v_color2.w, v_color2.rgb, v_color1.rgb, NL_CLOUD_PARAMS(_));
}

    #ifdef NL_CLOUD2_LAYER2
        vec2 parallax = vDir.xz / abs(vDir.y) * NL_CLOUD2_LAYER2_OFFSET;
        vec3 offsetPos = cloudPos;
        offsetPos.xz += parallax;
        vec4 color2 = renderCloudsRounded(vDir, offsetPos, v_color1.a, v_color2.a * 2.0, v_color2.rgb, v_color1.rgb, NL_CLOUD_PARAMS(_LAYER2_));
        color = mix(color2, color, 0.2 + 0.8 * color.a);
    #endif

    #ifdef NL_AURORA
        color += renderAuroraComplementary(vDir, v_color0.xz, v_color2.w, v_dayFactor) * (1.0 - 0.95 * color.a);
    #endif

    color.a *= v_color0.a;
    color.rgb = colorCorrection(color.rgb);

  #elif NL_CLOUD_TYPE == 3
    // Original Newb-X Realistic Clouds
    vec3 vDir3 = normalize(v_color0.xyz);
    vDir3.xz *= 0.3 + v_color0.w;
    vec2 p = (vDir3.xz) / (0.015 + 0.035 * abs(vDir3.y));
    p += 0.035 * CameraPosition.xz;
    vec4 clouds = renderClouds(p, v_color2.w, v_color1.w, v_color2.rgb, v_color1.rgb, NL_CLOUD3_SCALE, NL_CLOUD3_SPEED, NL_CLOUD3_SHADOW);
    color = clouds;

    #ifdef NL_AURORA
        p.xy *= 34.7;
        color += renderAuroraComplementary(vDir3, p, v_color2.w, v_dayFactor) * (1.0 - 0.95 * color.a);
    #endif

    color.a *= smoothstep(0.0, 0.7, vDir3.y);
    color.rgb = colorCorrection(color.rgb);

  #elif NL_CLOUD_TYPE == 4
    // 2.5d cloud
    vec3 worldPos = v_color0.xyz; 
    float fadeFactor = v_color0.w; 
    float timeVal = v_color2.w;

    vec2 cp = worldPos.xz * 0.005; 

    vec3 cloudNoise = pixelatedCloud(cp, timeVal);

    float cloudAlpha = clamp(length(cloudNoise.r), 0.0, 1.0);
    
    vec3 baseHorizon = v_color2.rgb;
    float dayBright = clamp(v_dayFactor*0.5+0.5, 0.0, 1.0);
vec3 cloudcol = baseHorizon * mix(NL_CLOUD4_NIGHT_DARKEN, 1.5, dayBright);

    color = vec4(cloudcol, cloudAlpha * fadeFactor);
    
    #ifdef NL_AURORA
        vec3 vDir4 = normalize(v_color0.xyz);
        color.rgb += renderAuroraComplementary(vDir4, v_color0.xz, timeVal, v_dayFactor).rgb * (1.0 - color.a);
    #endif

    color.rgb = colorCorrection(color.rgb);
  #endif

  gl_FragColor = color;
}
