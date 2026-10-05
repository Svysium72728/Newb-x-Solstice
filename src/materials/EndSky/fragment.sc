#ifndef INSTANCING
$input v_texcoord0, v_posTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>

  SAMPLER2D_AUTOREG(s_SkyTexture);

  #ifdef NL_END_CLOUDS
  SAMPLER2D_AUTOREG(s_NoiseTex);
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

  //end ambient glow (soft nebula-style haze, brightest near horizon, sky-only)

  vec3 renderEndAmbientGlow(vec3 viewDir, float t) {
      float elevation = viewDir.y;

      float heightFade = smoothstep(0.5, -0.4, elevation);
      heightFade *= heightFade;

      float a = atan2(viewDir.x, viewDir.z);
      float n1 = sin(a*3.0 + viewDir.y*4.0 + t*0.01);
      float n2 = sin(a*7.0 - viewDir.y*2.0 - t*0.015 + n1);
      float haze = 0.5 + 0.3*n1 + 0.2*n2;
      haze = 0.6 + 0.4*haze;

      vec3 col = mix(NL_ENDGLOW_COLOR2, NL_ENDGLOW_COLOR, 0.5 + 0.5*sin(a*2.0 + 0.3*viewDir.y));

      return col * heightFade * haze * NL_ENDGLOW_STRENGTH;
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

  //end clouds (layered plane-projected cloud sea below the horizon)

  #ifdef NL_END_CLOUDS
  vec4 renderEndClouds(vec3 vdir, float t) {
    float k = -vdir.y;
    float fade = smoothstep(0.02, 0.3, k);
    if (fade <= 0.0) return vec4(0.0, 0.0, 0.0, 0.0);

    vec3 col = vec3(0.0, 0.0, 0.0);
    float trans = 1.0;

    for (int i = 0; i < NL_END_CLOUD_LAYERS; i++) {
      float fi = float(i);
      float h = NL_END_CLOUD_HEIGHT * (1.0 + 0.6*fi);
      vec2 p = vdir.xz / k * h * NL_END_CLOUD_SCALE;
      p += vec2(t * NL_END_CLOUD_SPEED * (1.0 + 0.3*fi), 0.0) + fi*vec2(0.37, 0.61);

      float d = 0.65*texture2D(s_NoiseTex, p).r + 0.35*texture2D(s_NoiseTex, p*2.3 + 0.5).r;
      float dl = texture2D(s_NoiseTex, p + NL_END_CLOUD_LIGHT_OFFSET).r;

      float a = smoothstep(0.45, 0.75, d);
      float shade = clamp(0.6 - (dl - d)*3.0, 0.0, 1.0);

      col += trans * a * mix(NL_END_CLOUD_COL_DARK, NL_END_CLOUD_COL_LIGHT, shade);
      trans *= 1.0 - a;
    }

    return vec4(col, 1.0 - trans) * fade;
  }
  #endif
#endif

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

    // Add the end ambient glow haze (sky-only, not reflected onto ground)
    color += renderEndAmbientGlow(viewDir, t);

    // Add procedural stars
    color += renderEndStars(viewDir, 0.0015, 1.2);

    #ifdef NL_END_CLOUDS
        vec4 cl = renderEndClouds(viewDir, t);
        color = mix(color, cl.rgb / max(cl.a, 0.001), cl.a);
    #endif

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
