$input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra, v_causticData

#include <bgfx_shader.sh>
#include <newb/main.sh>

uniform vec4 ViewPositionAndTime;
uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 TimeOfDay;
uniform vec4 CameraPosition;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);
SAMPLER2D_AUTOREG(s_CausticTex);
SAMPLER2D_AUTOREG(s_SunRefl);

#define DROP_NUMBER 5

float stickyRaindrop(vec2 uv, vec2 center, float baseSize, float stretch) {
    vec2 p = (uv - center);
    p.y /= baseSize * stretch;
    p.x /= baseSize;

    float d = length(p * vec2(1.0, 0.6));
    float y = p.y;

    float shape = smoothstep(0.5, 0.45, d) *
                  smoothstep(0.0, 1.0, y + 0.5) *
                  smoothstep(1.0, -0.3, y);

    return shape;
}

vec3 screenDrops(vec3 baseColor, float time, vec2 uv, bool falling) {
    vec3 kol = baseColor;
    const int drops = DROP_NUMBER;

    for (int i = 0; i < drops; i++) {
        float fi = float(i);

        vec2 dropPos = vec2(
            fract(sin(fi * 12.9898) * 43758.5453),
            fract(sin(fi * 78.233) * 12345.678 + (falling ? time * 0.1 : 0.0))
        );

        float stretch = 1.0;
        if (falling) {
            dropPos.y = mod(dropPos.y - time * 0.18, 1.0);
            stretch = mix(1.0, 2.5, smoothstep(0.1, 0.9, dropPos.y));
        }

        float baseSize = 0.05 + 0.02 * fract(sin(fi * 5.21) * 1000.0);
        float dropMask = stickyRaindrop(uv, dropPos, baseSize, stretch);

        vec3 dropColor = vec3(0.5, 0.7, 0.85);
        kol = mix(kol, dropColor, dropMask * 0.5);
    }
    return kol;
}

#ifndef NL_CAUSTIC_FPS
  #define NL_CAUSTIC_FPS 10.0
#endif
#ifndef NL_CAUSTIC_BLEND
  #define NL_CAUSTIC_BLEND 1
#endif

#ifdef NL_CAUSTIC_ATLAS
  #ifndef NL_CAUSTIC_LO
    #define NL_CAUSTIC_LO 0.68
  #endif
  #ifndef NL_CAUSTIC_HI
    #define NL_CAUSTIC_HI 1.0
  #endif
  #ifndef NL_CAUSTIC_GAIN
    #define NL_CAUSTIC_GAIN 0.6
  #endif

  #define NL_CAUSTIC_FRAME_PX 128.0
  #define NL_CAUSTIC_PAD 2.0
  #define NL_CAUSTIC_CELL_H 132.0
  #define NL_CAUSTIC_TOTAL_H 4224.0

  float nlCausticAtlasTap(vec2 uv, float f) {
    highp float y = (f*NL_CAUSTIC_CELL_H + NL_CAUSTIC_PAD + uv.y*NL_CAUSTIC_FRAME_PX)/NL_CAUSTIC_TOTAL_H;
    return texture2D(s_CausticTex, vec2(uv.x, y)).r;
  }
#endif

float nlCausticFlip(vec2 uv, highp float t) {
  const float frames = 32.0;
  float ft = t*NL_CAUSTIC_FPS;
  float f0 = mod(floor(ft), frames);
  float f1 = mod(f0 + 1.0, frames);

  #ifdef NL_CAUSTIC_ATLAS
    float a = nlCausticAtlasTap(uv, f0);
    #if NL_CAUSTIC_BLEND
      a = mix(a, nlCausticAtlasTap(uv, f1), fract(ft));
    #endif
    return smoothstep(NL_CAUSTIC_LO, NL_CAUSTIC_HI, a)*NL_CAUSTIC_GAIN;
  #else
    vec2 u = vec2(uv.x, uv.y/frames);
    float a = texture2D(s_CausticTex, u + vec2(0.0, f0/frames)).r;
    #if NL_CAUSTIC_BLEND
      float b = texture2D(s_CausticTex, u + vec2(0.0, f1/frames)).r;
      a = mix(a, b, fract(ft));
    #endif
    return a;
  #endif
}

// ---------- sky cloud reflection (water) ----------

#if defined(NL_SKY_BLOCK_CLOUDS_REFL) || defined(NL_SKY_VOLUME_CLOUDS_REFL)

#ifndef NL_CLOUD_REFL_UNIT
  #define NL_CLOUD_REFL_UNIT 120.0
#endif
#ifndef NL_CLOUD_REFL_STRENGTH
  #define NL_CLOUD_REFL_STRENGTH 0.85
#endif

#ifdef NL_SKY_BLOCK_CLOUDS_REFL
  #ifndef NL_BLOCK_CLOUD_SCALE
    #define NL_BLOCK_CLOUD_SCALE 3.0
  #endif
  #ifndef NL_BLOCK_CLOUD_COVER
    #define NL_BLOCK_CLOUD_COVER 0.35
  #endif
  #ifndef NL_BLOCK_CLOUD_BASE
    #define NL_BLOCK_CLOUD_BASE 1.0
  #endif
  #ifndef NL_BLOCK_CLOUD_THICK
    #define NL_BLOCK_CLOUD_THICK 0.22
  #endif
  #ifndef NL_BLOCK_CLOUD_SQUARE
    #define NL_BLOCK_CLOUD_SQUARE 0.7
  #endif
  #ifndef NL_BLOCK_CLOUD_SOFT
    #define NL_BLOCK_CLOUD_SOFT 0.08
  #endif
  #ifndef NL_BLOCK_CLOUD_OPACITY
    #define NL_BLOCK_CLOUD_OPACITY 0.95
  #endif
  #ifndef NL_BLOCK_CLOUD_SPEED
    #define NL_BLOCK_CLOUD_SPEED 0.01
  #endif
  #ifndef NL_BLOCK_CLOUD_NIGHT
    #define NL_BLOCK_CLOUD_NIGHT 0.12
  #endif
  #ifndef NL_BLOCK_CLOUD_LIGHT_COL
    #define NL_BLOCK_CLOUD_LIGHT_COL vec3(1.0, 0.98, 0.98)
  #endif
  #ifndef NL_BLOCK_CLOUD_SHADE_COL
    #define NL_BLOCK_CLOUD_SHADE_COL vec3(0.66, 0.68, 0.86)
  #endif

  float nlRBlockHash(highp vec2 p) {
    highp vec3 p3 = fract(vec3(p.x, p.y, p.x)*0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y)*p3.z);
  }

  float nlRBlockField(highp vec2 p, float occThr, float lo, float hi) {
    highp vec2 i = floor(p);
    vec2 f = fract(p);
    float a = step(occThr, nlRBlockHash(i));
    float b = step(occThr, nlRBlockHash(i + vec2(1.0, 0.0)));
    float c = step(occThr, nlRBlockHash(i + vec2(0.0, 1.0)));
    float d = step(occThr, nlRBlockHash(i + vec2(1.0, 1.0)));
    vec2 tt = clamp((f - lo)/(hi - lo), 0.0, 1.0);
    vec2 u = tt*tt*(3.0 - 2.0*tt);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }
#endif

#ifdef NL_SKY_VOLUME_CLOUDS_REFL
  #ifndef NL_VOL_CLOUD_COVER
    #define NL_VOL_CLOUD_COVER 0.5
  #endif
  #ifndef NL_VOL_CLOUD_SCALE
    #define NL_VOL_CLOUD_SCALE 0.15
  #endif
  #ifndef NL_VOL_CLOUD_SPEED
    #define NL_VOL_CLOUD_SPEED 0.004
  #endif
  #ifndef NL_VOL_CLOUD_DENSITY
    #define NL_VOL_CLOUD_DENSITY 5.0
  #endif
  #ifndef NL_VOL_CLOUD_NIGHT
    #define NL_VOL_CLOUD_NIGHT 0.12
  #endif
  #ifndef NL_VOL_CLOUD_SHADOW_TINT
    #define NL_VOL_CLOUD_SHADOW_TINT vec3(0.78, 0.84, 1.0)
  #endif

  SAMPLER2D_AUTOREG(s_VolCloudNoise);
#endif

// r: reflected direction (y up), pos: surface position relative to camera (blocks)
vec4 nlSkyCloudReflection(vec3 r, vec3 pos, nl_skycolor skycol, nl_environment env, highp float time) {
  float f = smoothstep(0.04, 0.22, r.y);
  vec3 col = vec3(0.0, 0.0, 0.0);
  float T = 1.0;

  if (f > 0.0) {
    float dawn = 1.0 - env.dayFactor*env.dayFactor;
    dawn *= dawn*dawn;
    vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
    float daylight = smoothstep(-0.2, 0.25, env.dayFactor);

    #ifdef NL_SKY_BLOCK_CLOUDS_REFL
    {
      float H = (NL_BLOCK_CLOUD_BASE + 0.5*NL_BLOCK_CLOUD_THICK)*NL_CLOUD_REFL_UNIT;
      vec2 q = (pos.xz + r.xz*(max(H - pos.y, 0.0)/r.y))/NL_CLOUD_REFL_UNIT;
      highp vec2 p = q*NL_BLOCK_CLOUD_SCALE + vec2(time*NL_BLOCK_CLOUD_SPEED, time*NL_BLOCK_CLOUD_SPEED*0.4);
      float occThr = clamp(1.0 - NL_BLOCK_CLOUD_COVER - 0.25*env.rainFactor, 0.05, 0.95);
      float w = mix(0.5, 0.12, NL_BLOCK_CLOUD_SQUARE);
      float d = smoothstep(0.5 - NL_BLOCK_CLOUD_SOFT, 0.5 + NL_BLOCK_CLOUD_SOFT, nlRBlockField(p, occThr, 0.5 - w, 0.5 + w));
      float bright = mix(NL_BLOCK_CLOUD_NIGHT, 1.0, daylight)*(1.0 - 0.35*env.rainFactor);
      vec3 topCol = NL_BLOCK_CLOUD_LIGHT_COL*mix(vec3_splat(1.0), sunCol, 0.5)*bright;
      vec3 shadeCol = NL_BLOCK_CLOUD_SHADE_COL*mix(vec3_splat(1.0), skycol.horizon*1.4, 0.35)*bright;
      float a = d*NL_BLOCK_CLOUD_OPACITY;
      col += T*a*mix(shadeCol, topCol, 0.25);
      T *= 1.0 - a;
    }
    #endif

    #ifdef NL_SKY_VOLUME_CLOUDS_REFL
    {
      float H = 1.3*NL_CLOUD_REFL_UNIT;
      vec2 q = (pos.xz + r.xz*(max(H - pos.y, 0.0)/r.y))/NL_CLOUD_REFL_UNIT;
      vec2 p = q*NL_VOL_CLOUD_SCALE + vec2(time*NL_VOL_CLOUD_SPEED, time*NL_VOL_CLOUD_SPEED*0.4);
      float cover = clamp(NL_VOL_CLOUD_COVER + 0.35*env.rainFactor, 0.0, 1.0);
      float thr = mix(0.62, 0.40, cover);
      float b = texture2D(s_VolCloudNoise, p).r;
      float dt = texture2D(s_VolCloudNoise, p*3.7 + 0.31).g;
      float prof = smoothstep(0.0, 0.15, 0.5)*(1.0 - smoothstep(0.35, 1.0, 0.5 - (b - 0.5)*0.9));
      float tp = thr + 0.25*(1.0 - prof);
      float n = b + (dt - 0.5)*(0.18 + 0.25*(1.0 - prof));
      float d = smoothstep(0.0, 1.0, clamp((n - tp)*3.0, 0.0, 1.0));
      float a = 1.0 - exp(-d*NL_VOL_CLOUD_DENSITY*0.7);

      vec3 moonCol = NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
      vec3 lightCol = mix(moonCol*NL_VOL_CLOUD_NIGHT, sunCol, smoothstep(-0.1, 0.15, env.dayFactor))*(1.0 - 0.6*env.rainFactor);
      vec3 shadowCol = mix(skycol.zenith, skycol.horizon, 0.5)*NL_VOL_CLOUD_SHADOW_TINT*(1.0 - 0.4*env.rainFactor);
      col += T*a*(shadowCol + lightCol*0.35);
      T *= 1.0 - a;
    }
    #endif
  }

  return vec4(col*f*NL_CLOUD_REFL_STRENGTH, (1.0 - T)*f*NL_CLOUD_REFL_STRENGTH);
}

#endif

void main() {
  #if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    return;
  #endif

  nl_environment env = nlDetectEnvironment(TimeOfDay.x, FogColor.rgb, FogAndDistanceControl.xyz);

  vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
  vec4 color = v_color0;

  #ifdef ALPHA_TEST
    if (diffuse.a < 0.6) {
      discard;
    }
  #endif

  #if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
    diffuse.rgb *= mix(vec3(1.0,1.0,1.0), texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0, v_color1.z);
  #endif

  float sideshadow = smoothstep(0.64, 0.62, v_color1.g) * max(v_lightmapUV.x, v_lightmapUV.y);
  diffuse.rgb *= 1.0-0.25*sideshadow;

    vec3 glow = nlGlow(s_MatTexture, v_texcoord0, v_extra.a);

  // === FAKE BLOOM EFFECT FOR TORCHES ===
  #ifdef NL_TORCH_BLOOM
    float glowAmount = max(glow.r, max(glow.g, glow.b));
    float bloom = max(0.0, glowAmount - NL_TORCH_BLOOM_THRESHOLD) * 2.0;
    
    // Add warm bloom glow to the existing glow
    glow += NL_TORCH_BLOOM_COLOR * bloom * NL_TORCH_BLOOM_STRENGTH;
  #endif

  diffuse.rgb *= diffuse.rgb;

  #if defined(TRANSPARENT) && !(defined(SEASONS) || defined(RENDER_AS_BILLBOARDS))
    if (v_extra.b > 0.9) {
      float texPattern = NL_WATER_TEX_OPACITY*(diffuse.b*1.8 - 1.0);
      texPattern = texPattern/(1.0 + abs(texPattern)/(texPattern < 0.0 ? 0.75 : 1.5));
      diffuse.rgb = vec3_splat(1.0 + texPattern);
      diffuse.a = color.a;
    }
  #else
    diffuse.a = 1.0;
  #endif

  diffuse.rgb *= color.rgb;
  diffuse.rgb += glow;

  if (v_extra.b > 0.9) {
    diffuse.rgb += v_refl.rgb*v_refl.a;

  #ifdef NL_CLOUD_AURORA_REFLECTION
    if (!env.underwater && abs(dFdy(v_extra.g)) < 0.0002) {
      highp vec3 wpc = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
      vec3 vdc = normalize(ViewPositionAndTime.xyz - wpc);
      vec3 rc = vec3(-vdc.x, vdc.y, -vdc.z);
      nl_skycolor skyc = nlSkyColors(env);
      vec4 cr = nlCloudAuroraReflection(skyc, env, rc, wpc, CameraPosition.xyz, ViewPositionAndTime.w);
      diffuse.rgb = mix(diffuse.rgb, cr.rgb, cr.a*smoothstep(0.7, 0.95, v_lightmapUV.y));
    }
  #endif

  #if defined(NL_SKY_BLOCK_CLOUDS_REFL) || defined(NL_SKY_VOLUME_CLOUDS_REFL)
    if (!env.underwater && !env.end && !env.nether && abs(dFdy(v_extra.g)) < 0.0002) {
      highp vec3 wpk = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
      vec3 vdk = normalize(ViewPositionAndTime.xyz - wpk);
      vec3 rk = vec3(-vdk.x, vdk.y, -vdk.z);
      if (rk.y > 0.04) {
        nl_skycolor skyk = nlSkyColors(env);
        vec4 ck = nlSkyCloudReflection(rk, wpk - ViewPositionAndTime.xyz, skyk, env, ViewPositionAndTime.w);
        float mk = smoothstep(0.7, 0.95, v_lightmapUV.y);
        diffuse.rgb = diffuse.rgb*(1.0 - ck.a*mk) + ck.rgb*mk;
      }
    }
  #endif

    if (!env.underwater && abs(dFdy(v_extra.g)) < 0.01) {
      highp vec3 wps = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
      vec3 vds = normalize(ViewPositionAndTime.xyz - wps);
      vec3 rds = vec3(mix(vds.x, -vds.x, NL_SUN_REFL_MIRROR), vds.y, mix(vds.z, -vds.z, NL_SUN_REFL_MIRROR));
      vec3 sd = env.sunDir;
      vec3 sr = normalize(vec3(0.0, 0.0, 1.0) - sd*sd.z);
      vec3 su = cross(sd, sr);
      float facing = dot(rds, sd);
      vec2 suv = vec2(dot(rds, sr), dot(rds, su))/max(facing, 0.001)*NL_SUN_REFL_SCALE + 0.5;
      float inside = step(0.0, suv.x)*step(suv.x, 1.0)*step(0.0, suv.y)*step(suv.y, 1.0)*step(0.0, facing);
      float sunVis = smoothstep(0.0, 0.12, sd.y)*(1.0 - env.rainFactor)*smoothstep(0.7, 0.95, v_lightmapUV.y);
      vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, pow(1.0 - env.dayFactor*env.dayFactor, 3.0));
      vec3 sunRefl = texture2D(s_SunRefl, suv).rgb*sunCol*inside*sunVis*NL_SUN_REFL_STRENGTH;
      diffuse.rgb += sunRefl;
    }

  } else if (v_refl.a > 0.0) {
    // reflective effect - only on xz plane
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb *= 1.0 - 0.6*mask;
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  #if defined(NL_CAUSTIC_INTENSITY) && !(defined(RENDER_AS_BILLBOARDS) || defined(SEASONS))
    if (v_causticData.z > 0.5 && v_extra.b < 0.9) {
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
        vec2 causticUV = fract(v_causticData.xy);
        float caustic = nlCausticFlip(causticUV, ViewPositionAndTime.w);
        diffuse.rgb += caustic * NL_CAUSTIC_INTENSITY * 0.3;
    }
}
  #endif

  #ifdef NL_WATER_FOAM
  highp float wcY = v_extra.g + CameraPosition.y;
  float dyW = abs(dFdy(v_extra.g));
  highp vec3 fwp = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
  vec3 fvd = normalize(ViewPositionAndTime.xyz - fwp);

  bool isOneBlockUnderwater = ((wcY <= 62.89 && wcY > 62.0) || (wcY <= -60.11 && wcY > -61.0))
    && v_extra.b < 0.9
    && dyW >= 0.0002
    && fvd.y > 0.02
    && step(0.85, v_lightmapUV.y) * step(v_lightmapUV.y, 0.927) == 1.0;

  if (isOneBlockUnderwater) {
    float d = 0.865;
    float bpy = fract(wcY);
    vec2 offset = (d - bpy) * fvd.xz / fvd.y;
    vec2 watpos = v_causticData.xy + offset;

    float fade = 1.0 - clamp((d - bpy) / abs(fvd.y), 0.0, 1.0);

    float ft = 0.3 * ViewPositionAndTime.w;
    vec2 st = watpos;
    float n = sin(st.x + 3.0*sin(0.5*st.y) + ft) * sin(st.y + 3.0*sin(0.5*st.x) - ft);
    n = 0.5 + 0.5 * sin(st.x + 2.0*n + 0.2*ft) * sin(st.y - 4.0*n + 0.1*ft);
    fade = pow(fade, 1.0 + 4.0 * n);

    float foamTex = fract(238.084 * sin(dot(floor(16.0 * watpos), vec2(1.32, 141.3))));
    foamTex *= fade;
    foamTex = mix(foamTex, 1.0, fade * fade * fade);

    diffuse.rgb = mix(diffuse.rgb, NL_WATER_FOAM_COLOR, clamp(foamTex*NL_WATER_FOAM_STRENGTH, 0.0, 1.0));
  }
  #endif

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  float rain = mix(smoothstep(0.66, 0.3, FogAndDistanceControl.x), 0.0, step(FogAndDistanceControl.x, 0.0));
  float dropStrength = (env.nether || env.end) ? 0.0 : (env.underwater ? 1.0 : rain);

  if (dropStrength > 0.0) {
    vec2 screenUV = gl_FragCoord.xy / vec2(1280.0, 720.0); // match your actual target res
    diffuse.rgb = screenDrops(diffuse.rgb, ViewPositionAndTime.w, screenUV, !env.underwater);
  }

  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}
