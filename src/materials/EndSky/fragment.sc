#ifndef INSTANCING
$input v_texcoord0, v_posTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>

  SAMPLER2D_AUTOREG(s_SkyTexture);
#endif

//blackhole

#ifdef NL_BLACKHOLE
vec4 renderBlackhole(vec3 vdir, float t) {
    t *= NL_BH_SPEED;

    float cr = cos(NL_BH_AZIMUTH);
    float sr = sin(NL_BH_AZIMUTH);
    vec3 baseDir = normalize(vec3(0.0, NL_BH_HEIGHT, 1.0));
    vec3 center = vec3(baseDir.x*cr - baseDir.z*sr, baseDir.y, baseDir.x*sr + baseDir.z*cr);

    vec3 vd = vdir - center;
    float nl = sin(15.0*vd.x+t) * sin(15.0*vd.y-t) * sin(15.0*vd.z+t);
    float a = atan2(vd.z, vd.x);

    float d = NL_BH_DIST * length(vd + 0.003*nl);
    float d0 = (0.6 - d) / 0.6;
    float dm0 = 1.0 - max(d0, 0.0);

    float gl = 1.0 - clamp(-0.3*d0, 0.0, 1.0);
    float gla = pow(1.0 - min(abs(d0), 1.0), 8.0);
    float gl8 = pow(gl, 8.0);

    float hole = 0.9*pow(dm0, 32.0) + 0.1*pow(dm0, 3.0);
    float bh = (gla + 0.8*gl8 + 0.2*gl8*gl8) * hole;

    float df = sin(3.0*a - 4.0*d + 24.0*pow(1.4-d, 4.0) + t);
    df *= 0.9 + 0.1*sin(8.0*a + d + 4.0*t - 4.0*df);
    bh *= 1.0 + pow(df, 4.0) * hole * max(1.0 - bh, 0.0);

    vec3 col = bh * 4.0 * mix(NL_BH_COL_LOW, NL_BH_COL_HIGH, min(bh, 1.0));
    return vec4(col, hole);
}
#endif

//end glow curtain (aurora-style, sky-only, not shared with ground reflection)

vec3 renderEndGlowCurtain(vec3 viewDir, float t) {
  float a = atan2(viewDir.x, viewDir.z);
  float elevation = viewDir.y;

  float bandPattern = sin(a*14.0) + 0.5*sin(a*23.0 + 1.3*t*0.02) + 0.3*sin(a*37.0 - 0.7*t*0.02);
  bandPattern = smoothstep(0.7, 1.3, bandPattern);

  float heightFade = smoothstep(-0.3, 0.5, elevation) * (1.0 - smoothstep(0.5, 1.0, elevation));

  vec3 col = mix(NL_ENDGLOW_COLOR, NL_ENDGLOW_COLOR2, 0.5 + 0.5*sin(a*5.0 + t*0.05));

  return col * bandPattern * heightFade * NL_ENDGLOW_STRENGTH;
}

//end soundwave glow (regular radiating bands converging toward zenith)

vec3 renderEndSoundwaveGlow(vec3 viewDir, float t) {
  float a = atan2(viewDir.x, viewDir.z);
  float elevation = viewDir.y;

  float bands = abs(sin(a * 11.0));
  bands = pow(bands, 3.0);

  float radialFade = smoothstep(-0.3, 0.9, elevation);

  vec3 col = mix(NL_ENDGLOW_COLOR2, NL_ENDGLOW_COLOR, radialFade);

  return col * bands * radialFade * NL_ENDGLOW_STRENGTH;
}

//end stars (procedural, layered over vanilla texture stars)

vec3 renderEndStars(vec3 viewDir, float density, float brightness) {
  vec3 p = viewDir * 400.0;
  vec3 cell = floor(p);
  vec3 f = fract(p);

  float h = fract(sin(dot(cell, vec3(12.9898, 78.233, 45.164))) * 43758.5453);
  float star = step(1.0 - density, h);

  float d = length(f - 0.5);
  star *= smoothstep(0.5, 0.1, d);

  float twinkle = 0.7 + 0.3*sin(h*123.4 + dot(cell, vec3(1.0)));

  return vec3(star * brightness * twinkle);
}

void main() {
  #ifndef INSTANCING
    vec4 diffuse = texture2D(s_SkyTexture, v_texcoord0);

    // Extract the raw layout values from the vertex shader packet
    // v_posTime.xyz is the rotated world position of the skybox vertex
    vec3 viewDir = normalize(v_posTime.xyz);
    float t = v_posTime.w;

     // Build the default Newb base sky for The End
    vec3 color = renderEndSky(getEndHorizonCol(), getEndZenithCol(), viewDir, t);

    // Add the native cloud texture overlays over the horizon
    color += 2.8 * diffuse.rgb;

    // Add the endglow soundwave bands (sky-only, not reflected onto ground)
    color += renderEndSoundwaveGlow(viewDir, t);

    // Add procedural stars
    color += renderEndStars(viewDir, 0.0015, 1.2);

    // Blend Blackhole mathematical layer over the base sky box
    #ifdef NL_BLACKHOLE
        vec4 bh = renderBlackhole(viewDir, t);
        color *= bh.a;   // Absorb light in the center (forces black singularity)
        color += bh.rgb; // Add the burning outer accretion disk
    #endif

    // Run final color mapping and tone correction configuration
    color = colorCorrection(color);

    gl_FragColor = vec4(color, 1.0);
  #else
    // Instanced geometry drops draw buffer execution
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
