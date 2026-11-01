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
SAMPLER2D_AUTOREG(s_SunRefl);
SAMPLER2D_AUTOREG(s_BlockCloudNoise);

#ifdef NL_CAUSTIC_ATLAS
  SAMPLER2D_AUTOREG(s_UnboundCausticTex);
#else
  SAMPLER2D_AUTOREG(s_CausticTex);
#endif

#define DROP_NUMBER 0

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

  float nlUnboundCausticTap(vec2 uv, float f) {
    highp float y = (f*132.0 + 2.0 + uv.y*128.0)/4224.0;
    return texture2D(s_UnboundCausticTex, vec2(uv.x, y)).r;
  }

  float nlUnboundCaustic(vec2 uv, highp float t) {
    float ft = t*NL_CAUSTIC_FPS;
    float f0 = mod(floor(ft), 32.0);
    float a = nlUnboundCausticTap(uv, f0);
    #if NL_CAUSTIC_BLEND
      a = mix(a, nlUnboundCausticTap(uv, mod(f0 + 1.0, 32.0)), fract(ft));
    #endif
    return smoothstep(NL_CAUSTIC_LO, NL_CAUSTIC_HI, a)*NL_CAUSTIC_GAIN;
  }
#else
  float nlCausticFlip(vec2 uv, highp float t) {
    const float frames = 32.0;
    float ft = t*NL_CAUSTIC_FPS;
    float f0 = mod(floor(ft), frames);
    vec2 u = vec2(uv.x, uv.y/frames);
    float a = texture2D(s_CausticTex, u + vec2(0.0, f0/frames)).r;
    #if NL_CAUSTIC_BLEND
      float b = texture2D(s_CausticTex, u + vec2(0.0, mod(f0 + 1.0, frames)/frames)).r;
      a = mix(a, b, fract(ft));
    #endif
    return a;
  }
#endif

#if defined(NL_SKY_BLOCK_CLOUDS_REFL) || defined(NL_SKY_VOLUME_CLOUDS_REFL)

#ifndef NL_CLOUD_REFL_UNIT
  #define NL_CLOUD_REFL_UNIT 600.0
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
  #ifndef NL_BLOCK_CLOUD_BEVEL
    #define NL_BLOCK_CLOUD_BEVEL 0.45
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
  #ifndef NL_BLOCK_CLOUD_REFL_STEPS
    #define NL_BLOCK_CLOUD_REFL_STEPS 5
  #endif
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
  #ifndef NL_VOL_CLOUD_REFL_STEPS
    #define NL_VOL_CLOUD_REFL_STEPS 4
  #endif

  SAMPLER2D_AUTOREG(s_VolCloudNoise);
#endif

vec4 nlSkyCloudReflection(vec3 r, vec3 pos, nl_skycolor skycol, nl_environment env, highp float time, float jitter) {
  float f = smoothstep(0.04, 0.22, r.y);
  vec3 col = vec3(0.0, 0.0, 0.0);
  float T = 1.0;

  if (f > 0.0) {
    float dawn = 1.0 - env.dayFactor*env.dayFactor;
    dawn *= dawn*dawn;
    vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
    vec3 pu = pos/NL_CLOUD_REFL_UNIT;
    float invY = 1.0/r.y;
    vec3 sdr = vec3(-r.x, r.y, -r.z);

    #ifdef NL_SKY_BLOCK_CLOUDS_REFL
    {
      float occThr = clamp(1.0 - NL_BLOCK_CLOUD_COVER, 0.05, 0.95);
      float w = mix(0.5, 0.12, NL_BLOCK_CLOUD_SQUARE);
      float lo = 0.5 - w;
      float hi = 0.5 + w;
      float daylight = smoothstep(-0.2, 0.25, env.dayFactor);
      float bright = mix(NL_BLOCK_CLOUD_NIGHT, 1.0, daylight)*(1.0 - 0.35*env.rainFactor);
      vec3 Lb = mix(env.moonDir, env.sunDir, step(0.0, env.dayFactor));
      vec3 topCol = NL_BLOCK_CLOUD_LIGHT_COL*mix(vec3_splat(1.0), sunCol, 0.5)*bright;
      vec3 shadeCol = NL_BLOCK_CLOUD_SHADE_COL*mix(vec3_splat(1.0), skycol.horizon*1.4, 0.35)*bright;
      highp vec2 wind = vec2(time*NL_BLOCK_CLOUD_SPEED, time*NL_BLOCK_CLOUD_SPEED*0.4);

      for (int i = 0; i < NL_BLOCK_CLOUD_REFL_STEPS; i++) {
        float h = (float(i) + jitter)/float(NL_BLOCK_CLOUD_REFL_STEPS);
        float t = max(NL_BLOCK_CLOUD_BASE + NL_BLOCK_CLOUD_THICK*h - pu.y, 0.0)*invY;
        highp vec2 p = (sdr.xz*t - pu.xz)*NL_BLOCK_CLOUD_SCALE + wind;
        vec4 cell = texture2D(s_BlockCloudNoise, (floor(p) + 0.5)*0.00390625);
        vec3 fg = blockField(cell, p, occThr, lo, hi);

        float e = abs(2.0*h - 1.0);
        float e2 = e*e;
        float inset = NL_BLOCK_CLOUD_BEVEL*(1.0 - sqrt(max(1.0 - e2*e2, 0.0)));
        float thr = 0.5 + inset;
        float d = smoothstep(thr - NL_BLOCK_CLOUD_SOFT, thr + NL_BLOCK_CLOUD_SOFT, fg.x);

        if (d > 0.01) {
          float s = step(0.5, h)*2.0 - 1.0;
          float vtl = smoothstep(0.45, 1.0, e);
          float gmag = length(fg.yz);
          vec2 gd = fg.yz/max(gmag, 0.0001);
          float gm = clamp(gmag*3.0, 0.0, 1.0);
          float ny = s*max(vtl, 0.2);
          float nh = sqrt(max(1.0 - ny*ny, 0.0))*gm;
          vec3 nrm = normalize(vec3(-gd.x*nh, ny, -gd.y*nh));
          float diff = clamp(dot(nrm, Lb)*0.5 + 0.5, 0.0, 1.0);
          diff = diff*diff*(3.0 - 2.0*diff);
          float a = d*NL_BLOCK_CLOUD_OPACITY;
          col += T*a*mix(shadeCol, topCol, diff);
          T *= 1.0 - a;
          if (T < 0.05) {
            break;
          }
        }
      }
    }
    #endif

    #ifdef NL_SKY_VOLUME_CLOUDS_REFL
    if (T > 0.04) {
      float cover = clamp(NL_VOL_CLOUD_COVER + 0.35*env.rainFactor, 0.0, 1.0);
      float thr = mix(0.62, 0.40, cover);
      float dayK = smoothstep(-0.1, 0.15, env.dayFactor);
      vec3 L = mix(env.moonDir, env.sunDir, step(0.0, env.dayFactor));
      vec3 moonCol = NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
      vec3 lightCol = mix(moonCol*NL_VOL_CLOUD_NIGHT, sunCol, dayK)*(1.0 - 0.6*env.rainFactor);
      vec3 shadowCol = mix(skycol.zenith, skycol.horizon, 0.5)*NL_VOL_CLOUD_SHADOW_TINT*(1.0 - 0.4*env.rainFactor);
      vec3 topAmb = mix(shadowCol, vec3_splat(1.0), 0.45)*mix(NL_VOL_CLOUD_NIGHT*2.0, 1.0, dayK);
      float silver = pow(max(dot(sdr, L), 0.0), 6.0);
      vec2 base = -pu.xz*NL_VOL_CLOUD_SCALE + vec2(time*NL_VOL_CLOUD_SPEED, time*NL_VOL_CLOUD_SPEED*0.4);
      vec2 stepXZ = -r.xz*invY*NL_VOL_CLOUD_SCALE;

      float Tv = 1.0;
      vec3 cv = vec3(0.0, 0.0, 0.0);
      for (int i = 0; i < NL_VOL_CLOUD_REFL_STEPS; i++) {
        float h = (float(i) + jitter)/float(NL_VOL_CLOUD_REFL_STEPS);
        vec2 p = base + stepXZ*max(1.0 + 0.6*h - pu.y, 0.0);
        float b = texture2DLod(s_VolCloudNoise, p, 0.0).r;
        float prof = smoothstep(0.0, 0.1, h)*(1.0 - smoothstep(0.25, 0.9, h - (b - 0.5)*1.2));
        float tp = thr + 0.28*(1.0 - prof);
        if (b > tp - 0.12) {
          float dt = texture2DLod(s_VolCloudNoise, p*3.7 + 0.31, 0.0).g;
          float dt2 = texture2DLod(s_VolCloudNoise, p*9.1 + 0.67, 0.0).b;
          float billow = (dt - 0.5)*0.2 + (dt2 - 0.5)*0.08;
          float n = b + billow*(0.6 + 0.8*(1.0 - prof));
          float core = clamp((n - tp)*5.0, 0.0, 1.0);
          float d = core*core*(3.0 - 2.0*core);
          if (d > 0.0) {
            float rim = smoothstep(0.0, 0.5, core);
            float belly = mix(0.35, 1.0, smoothstep(0.0, 0.8, h));
            float lit = belly*exp(-d*0.9);
            vec3 c = mix(shadowCol*0.6, topAmb, belly*rim) + lightCol*(lit*0.9 + 2.5*silver)*mix(0.55, 1.0, rim);
            float a = 1.0 - exp(-d*NL_VOL_CLOUD_DENSITY/float(NL_VOL_CLOUD_REFL_STEPS));
            cv += Tv*a*c;
            Tv *= 1.0 - a;
            if (Tv < 0.04) {
              break;
            }
          }
        }
      }
      col += T*cv;
      T *= Tv;
    }
    #endif
  }

  return vec4(col*f*NL_CLOUD_REFL_STRENGTH, (1.0 - T)*f*NL_CLOUD_REFL_STRENGTH);
}

#endif

#if defined(NL_BLACKHOLE) && defined(NL_BLACKHOLE_REFL)
vec4 nlBlackholeReflection(vec3 vdir, highp float time) {
  float t = time*NL_BH_SPEED;
  float cr = cos(NL_BH_AZIMUTH);
  float sr = sin(NL_BH_AZIMUTH);
  vec3 baseDir = normalize(vec3(0.0, NL_BH_HEIGHT, 1.0));
  vec3 center = vec3(baseDir.x*cr - baseDir.z*sr, baseDir.y, baseDir.x*sr + baseDir.z*cr);

  vec3 vd = vdir - center;
  float nl = sin(15.0*vd.x + t)*sin(15.0*vd.y - t)*sin(15.0*vd.z + t);
  float a = atan2(vd.z, vd.x);
  float d = NL_BH_DIST*length(vd + 0.003*nl);
  float d0 = (0.6 - d)/0.6;
  float dm0 = 1.0 - max(d0, 0.0);
  float gl = 1.0 - clamp(-0.3*d0, 0.0, 1.0);
  float gla = pow(1.0 - min(abs(d0), 1.0), 8.0);
  float gl8 = pow(gl, 8.0);
  float hole = 0.9*pow(dm0, 32.0) + 0.1*pow(dm0, 3.0);
  float bh = (gla + 0.8*gl8 + 0.2*gl8*gl8)*hole;
  float df = sin(3.0*a - 4.0*d + 24.0*pow(abs(1.4 - d), 4.0) + t);
  df *= 0.9 + 0.1*sin(8.0*a + d + 4.0*t - 4.0*df);
  bh *= 1.0 + pow(df, 4.0)*hole*max(1.0 - bh, 0.0);
  vec3 col = bh*4.0*mix(NL_BH_COL_LOW, NL_BH_COL_HIGH, min(bh, 1.0));
  return vec4(col, hole);
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

  #ifdef NL_TORCH_BLOOM
    float glowAmount = max(glow.r, max(glow.g, glow.b));
    float bloom = max(0.0, glowAmount - NL_TORCH_BLOOM_THRESHOLD) * 2.0;
    vec3 bloomCol = mix(NL_TORCH_BLOOM_COLOR, glow/max(glowAmount, 0.001), NL_TORCH_BLOOM_TINT);
    glow += bloomCol * bloom * NL_TORCH_BLOOM_STRENGTH;
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
        float cj = fract(52.9829189*fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
        vec4 ck = nlSkyCloudReflection(rk, wpk - ViewPositionAndTime.xyz, skyk, env, ViewPositionAndTime.w, cj);
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
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb *= 1.0 - 0.6*mask;
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  #if defined(NL_BLACKHOLE) && defined(NL_BLACKHOLE_REFL)
    if (env.end && v_extra.b < 0.9 && abs(dFdy(v_extra.g)) < 0.0002) {
      highp vec3 wpb = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
      vec3 vdb = normalize(ViewPositionAndTime.xyz - wpb);
      vec3 rb = vec3(mix(vdb.x, -vdb.x, NL_BH_REFL_MIRROR), vdb.y, mix(vdb.z, -vdb.z, NL_BH_REFL_MIRROR));
      if (rb.y > 0.02) {
        vec4 bhr = nlBlackholeReflection(rb, ViewPositionAndTime.w);
        float fres = pow(1.0 - rb.y, 3.0)*smoothstep(0.02, 0.2, rb.y)*NL_BH_REFL_TERRAIN;
        diffuse.rgb = diffuse.rgb*mix(1.0, bhr.a, fres) + bhr.rgb*fres;
      }
    }
  #endif

  #if defined(NL_CAUSTIC_INTENSITY) && !(defined(RENDER_AS_BILLBOARDS) || defined(SEASONS))
    if (v_causticData.z > 0.5 && v_extra.b < 0.9) {
      float dy = abs(dFdy(v_extra.g));
      if (dy < 0.0002) {
        vec2 causticUV = fract(v_causticData.xy);
        #ifdef NL_CAUSTIC_ATLAS
          float caustic = nlUnboundCaustic(causticUV, ViewPositionAndTime.w);
        #else
          float caustic = nlCausticFlip(causticUV, ViewPositionAndTime.w);
        #endif
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

  #ifdef NL_UNDERWATER_STREAKS
    if (env.underwater) {
      highp vec3 wpu = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
      float du = length(wpu);
      vec3 dl = vec3(-wpu.x, wpu.y, -wpu.z)/max(du, 0.001);
      float rays = nlUnderwaterRays(dl, nlUnderwaterLightDir(env), ViewPositionAndTime.w);
      float sc = 1.0 - exp(-du*NL_UNDERWATER_RAY_DENSITY);
      float skyM = smoothstep(0.3, 0.85, v_lightmapUV.y);
      diffuse.rgb += NL_UNDERWATER_RAY_COL*rays*sc*skyM*nlUnderwaterRayMask(env)*NL_UNDERWATER_STREAKS;
    }
  #endif

  float rain = mix(smoothstep(0.66, 0.3, FogAndDistanceControl.x), 0.0, step(FogAndDistanceControl.x, 0.0));
  float dropStrength = (env.nether || env.end) ? 0.0 : (env.underwater ? 1.0 : rain);

  if (dropStrength > 0.0) {
    vec2 screenUV = gl_FragCoord.xy / vec2(1280.0, 720.0);
    diffuse.rgb = screenDrops(diffuse.rgb, ViewPositionAndTime.w, screenUV, !env.underwater);
  }

  #ifdef NL_OCEAN_DEPTH
    if (env.underwater) {
      float od = clamp((NL_OCEAN_SEA_LEVEL - CameraPosition.y)/NL_OCEAN_DEPTH_MAX, 0.0, 1.0);
      od = od*od*(3.0 - 2.0*od);

      float keep = smoothstep(0.4, 0.9, v_lightmapUV.x)*NL_OCEAN_TORCH_KEEP;
      vec3 darkTint = mix(vec3_splat(1.0), NL_OCEAN_DEEP_COL, od);
      diffuse.rgb *= mix(darkTint, vec3_splat(1.0), keep);

      vec2 screenuv = gl_FragCoord.xy / u_viewRect.zw;
      float dist = distance(screenuv, vec2_splat(0.5));
      float vignette = pow(1.0 - smoothstep(NL_OCEAN_VIG_START, NL_OCEAN_VIG_END, dist), NL_OCEAN_VIG_POWER);
      diffuse.rgb *= mix(1.0, vignette, od*NL_OCEAN_VIG_STRENGTH);
    }
  #endif

  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}
