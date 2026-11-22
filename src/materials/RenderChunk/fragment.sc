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

#if defined(NL_SKY_BLOCK_CLOUDS_REFL) || defined(NL_SKY_VOLUME_CLOUDS_REFL) || defined(NL_SKY_PLANE_CLOUDS_REFL)

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
  #ifndef NL_BLOCK_CLOUD_SHARP
    #define NL_BLOCK_CLOUD_SHARP 5.0
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

#ifdef NL_SKY_PLANE_CLOUDS_REFL
  #ifndef NL_PLANE_CLOUD_SCALE
    #define NL_PLANE_CLOUD_SCALE 0.35
  #endif
  #ifndef NL_PLANE_CLOUD_STRETCH
    #define NL_PLANE_CLOUD_STRETCH 1.72
  #endif
  #ifndef NL_PLANE_CLOUD_SPEED
    #define NL_PLANE_CLOUD_SPEED 0.07
  #endif
  #ifndef NL_PLANE_CLOUD_OPACITY
    #define NL_PLANE_CLOUD_OPACITY 1.2
  #endif
  #ifndef NL_PLANE_CLOUD_LAYERS
    #define NL_PLANE_CLOUD_LAYERS 10
  #endif
  #ifndef NL_PLANE_CLOUD_REFL_LAYERS
    #define NL_PLANE_CLOUD_REFL_LAYERS NL_PLANE_CLOUD_LAYERS
  #endif
  #ifndef NL_PLANE_CLOUD_NIGHT
    #define NL_PLANE_CLOUD_NIGHT 0.30
  #endif

  SAMPLER2D_AUTOREG(s_PlaneCloudNoise);

  float rcPlaneNoise(highp vec2 p) {
    return texture2DLod(s_PlaneCloudNoise, p*0.00390625, 0.0).r;
  }

  float rcPlaneFbm(highp vec2 x, float amp) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 2; i++) {
      v += a*rcPlaneNoise(x);
      x = x*2.0/amp + vec2(100.0, 100.0);
      a *= 0.5;
    }
    return v;
  }

  float rcPlaneShape(highp vec2 uv, float dither) {
    uv *= 5.0;
    float a = 0.0;
    for (int i = 0; i < NL_PLANE_CLOUD_REFL_LAYERS; i++) {
      uv /= 1.01;
      a = mix(a, 1.0, smoothstep(0.35, 0.45, rcPlaneFbm(uv, dither)));
    }
    float t = smoothstep(0.0, 1.5, rcPlaneFbm(uv, pow(dither, 6.0)) + 0.8);
    return a - pow(t, 1.5)*0.7;
  }
#endif

#ifdef NL_SKY_BLOCK_CLOUDS_REFL
// Same occupancy as the sky pass: one point fetch (cell + +x/+y/+xy neighbours packed in RGBA),
// thresholded per cell, blended only across a narrow strip at the cell border.
float blockOcc(highp vec2 q, float occThr) {
  highp vec2 c = q - 0.5;
  highp vec2 i = floor(c);
  vec2 f = clamp((fract(c) - 0.5)*NL_BLOCK_CLOUD_SHARP + 0.5, 0.0, 1.0);
  vec4 n = texture2D(s_BlockCloudNoise, (i + 0.5)*0.00390625);
  vec4 s = step(vec4_splat(occThr), n);
  return mix(mix(s.x, s.y, f.x), mix(s.z, s.w, f.x), f.y);
}
#endif

vec4 nlSkyCloudReflection(vec3 r, vec3 pos, nl_skycolor skycol, nl_environment env, highp float time, float jitter) {
  float f = smoothstep(0.04, 0.22, r.y);
  vec3 col = vec3(0.0, 0.0, 0.0);
  float T = 1.0;

  if (f > 0.0) {
    float dawn = 1.0 - env.dayFactor*env.dayFactor;
    dawn *= dawn*dawn;
    dawn *= 1.0 - smoothstep(0.0, 0.3, env.dayFactor);
    vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
    vec3 pu = pos/NL_CLOUD_REFL_UNIT;
    float invY = 1.0/r.y;
    vec3 sdr = vec3(-r.x, r.y, -r.z);

    #ifdef NL_SKY_BLOCK_CLOUDS_REFL
    {
      float occThr = clamp(1.0 - NL_BLOCK_CLOUD_COVER, 0.05, 0.95);
      float daylight = smoothstep(-0.2, 0.25, env.dayFactor);
      float bright = mix(NL_BLOCK_CLOUD_NIGHT, 1.0, daylight)*(1.0 - 0.35*env.rainFactor);
      vec3 Lb = mix(env.moonDir, env.sunDir, step(0.0, env.dayFactor));
      vec3 topCol = NL_BLOCK_CLOUD_LIGHT_COL*mix(vec3_splat(1.0), sunCol, mix(NL_BLOCK_CLOUD_SUN_TINT, 0.5, dawn))*bright;
      vec3 shadeCol = NL_BLOCK_CLOUD_SHADE_COL*mix(vec3_splat(1.0), skycol.horizon*1.4, 0.35)*bright;
      // wind is added AFTER the per-layer scale so every slice drifts at the same speed
      highp vec2 wind = vec2(time*NL_BLOCK_CLOUD_SPEED, time*NL_BLOCK_CLOUD_SPEED*0.4);

      // wall lighting: compare occupancy one step toward the sun/moon (fades out when it is overhead)
      float lxz = length(Lb.xz);
      vec2 Lq = Lb.xz/max(lxz, 0.0001)*0.6;
      float sunSide = clamp(lxz*2.5, 0.0, 1.0);

      float acc = 0.0;
      float w0 = 0.0;
      float count = 0.0;
      vec3 wallAcc = vec3(0.0, 0.0, 0.0);
      // no early break on purpose: `count` feeds the underside colour and must cover every layer
      for (int i = 0; i < NL_BLOCK_CLOUD_REFL_STEPS; i++) {
        float h = (float(i) + 0.5)/float(NL_BLOCK_CLOUD_REFL_STEPS);
        float t = max(NL_BLOCK_CLOUD_BASE + NL_BLOCK_CLOUD_THICK*h - pu.y, 0.0)*invY;
        highp vec2 p = (sdr.xz*t - pu.xz)*NL_BLOCK_CLOUD_SCALE + wind;

        float o = blockOcc(p, occThr);
        float m = smoothstep(0.5 - NL_BLOCK_CLOUD_SOFT, 0.5 + NL_BLOCK_CLOUD_SOFT, o);
        count += m;

        float a = m*(1.0 - acc);
        if (a > 0.001) {
          float lit = clamp(0.5 - (blockOcc(p + Lq, occThr) - o)*1.2, 0.0, 1.0);
          lit = mix(0.5, lit, sunSide);
          float wk = clamp(0.25 + 0.65*h + 0.35*(lit - 0.5), 0.0, 1.0);
          float first = (i == 0) ? 1.0 : 0.0;
          w0 += a*first;
          wallAcc += a*(1.0 - first)*mix(shadeCol, topCol, wk);
          acc += a;
        }
      }

      if (acc > 0.001) {
        float c01 = count/float(NL_BLOCK_CLOUD_REFL_STEPS);
        vec3 under = mix(topCol*0.92, shadeCol, pow(clamp((c01 - 0.08)/0.6, 0.0, 1.0), 0.8));
        col += (wallAcc + w0*under)*NL_BLOCK_CLOUD_OPACITY;
        T *= 1.0 - acc*NL_BLOCK_CLOUD_OPACITY;
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
      vec2 toLight = L.xz/max(L.y, 0.25);
      highp vec2 base = -pu.xz*NL_VOL_CLOUD_SCALE + vec2(time*NL_VOL_CLOUD_SPEED, time*NL_VOL_CLOUD_SPEED*0.4);
      highp vec2 stepXZ = -r.xz*invY*NL_VOL_CLOUD_SCALE;
      highp vec2 cp = base + stepXZ*max(1.0 - pu.y, 0.0);
      float dt = texture2DLod(s_VolCloudNoise, cp*3.7 + 0.31, 0.0).g;
      float lb = texture2DLod(s_VolCloudNoise, cp + toLight*0.09, 0.0).r;
      float lshade = clamp((lb - thr)*3.0, 0.0, 1.0);

      float Tv = 1.0;
      vec3 cv = vec3(0.0, 0.0, 0.0);
      for (int i = 0; i < NL_VOL_CLOUD_REFL_STEPS; i++) {
        float h = (float(i) + 0.5 + 0.35*(jitter - 0.5))/float(NL_VOL_CLOUD_REFL_STEPS);
        highp vec2 p = cp*(1.0 - 0.12*h) + toLight*0.04*h;
        float b = texture2DLod(s_VolCloudNoise, p, 0.0).r;
        float prof = smoothstep(0.0, 0.18, h)*(1.0 - smoothstep(0.35, 1.0, h - (b - 0.5)*1.1));
        float tp = thr + 0.25*(1.0 - prof);
        float n = b + (dt - 0.5)*0.22*(1.0 - 0.5*prof);
        float d = smoothstep(tp - 0.02, tp + 0.2, n);
        if (d > 0.002) {
          float lit = exp(-lshade*(1.0 - h)*1.8);
          float rim = smoothstep(0.0, 0.6, d);
          float belly = mix(0.4, 1.0, h);
          vec3 c = mix(shadowCol*0.6, topAmb, belly*rim) + lightCol*(lit*0.9*belly + 2.5*silver*lit)*mix(0.6, 1.0, rim);
          float a = 1.0 - exp(-d*NL_VOL_CLOUD_DENSITY/float(NL_VOL_CLOUD_REFL_STEPS));
          cv += Tv*a*c;
          Tv *= 1.0 - a;
          if (Tv < 0.1) {
            break;
          }
        }
      }
      col += T*cv;
      T *= Tv;
    }
    #endif

    #ifdef NL_SKY_PLANE_CLOUDS_REFL
    if (T > 0.04) {
      highp vec2 pplane = sdr.xz/max(r.y, 0.06)*NL_PLANE_CLOUD_SCALE;
      pplane.y *= NL_PLANE_CLOUD_STRETCH;
      float pfade = smoothstep(0.06, 0.25, r.y)*clamp(1.0 - length(pplane)*0.15, 0.0, 1.0);
      if (pfade > 0.002) {
        highp vec2 puv = pplane;
        puv.x += time*NL_PLANE_CLOUD_SPEED;
        float pdither = pow(rcPlaneNoise(gl_FragCoord.xy) + 0.35, 0.008);
        float pshape = max(rcPlaneShape(puv, pdither)*NL_PLANE_CLOUD_OPACITY, 0.0);
        float pa = clamp(pshape*pfade, 0.0, 1.0);
        float plit = clamp(pshape, 0.0, 1.0);

        float pdayK = smoothstep(-0.1, 0.15, env.dayFactor);
        vec3 pmoon = NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
        vec3 plight = mix(pmoon*NL_PLANE_CLOUD_NIGHT, sunCol, pdayK)*(1.0 - 0.6*env.rainFactor);
        vec3 pshadow = mix(skycol.zenith, skycol.horizon, 0.5)*vec3(0.78, 0.84, 1.0)*(1.0 - 0.4*env.rainFactor);
        vec3 pamb = mix(pshadow, vec3_splat(1.0), 0.45)*mix(NL_PLANE_CLOUD_NIGHT*2.0, 1.0, pdayK);
        vec3 pc = mix(pshadow, pamb, plit) + plight*plit*0.9;

        col += T*pc*pa;
        T *= 1.0 - pa;
      }
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

  float metalMask = 0.0;
  #ifdef NL_METAL_REFLECTION
    float metalA = texture2DLod(s_MatTexture, v_texcoord0, 0.0).a;
    metalMask = step(0.9835, metalA)*step(metalA, 0.987);
  #endif

  float lavaBlue = 0.0;
  #ifdef NL_LAVA_BLUE
    if (env.nether) {
      vec3 fcn = FogColor.rgb;
      float ssvB = smoothstep(0.6, 0.35, fcn.r/max(fcn.g, 0.001))*smoothstep(0.1, 0.18, fcn.g);
      float warpB = smoothstep(2.0, 4.0, fcn.b/max(fcn.g, 0.005))*smoothstep(0.04, 0.08, fcn.b);
      lavaBlue = max(ssvB, warpB)*step(0.5, v_color1.a);
    }
  #endif

  float outdoorMask = smoothstep(0.86, 0.93, v_lightmapUV.y);
  float sideshadow = smoothstep(0.64, 0.62, v_color1.g) * max(v_lightmapUV.x, v_lightmapUV.y) * outdoorMask;
  diffuse.rgb *= 1.0-0.25*sideshadow;

  vec3 glow = nlGlow(s_MatTexture, v_texcoord0, v_extra.a);

  #ifdef NL_TORCH_BLOOM
    float glowAmount = max(glow.r, max(glow.g, glow.b));
    float bloom = max(0.0, glowAmount - NL_TORCH_BLOOM_THRESHOLD) * 2.0;
    vec3 bloomCol = mix(NL_TORCH_BLOOM_COLOR, glow/max(glowAmount, 0.001), NL_TORCH_BLOOM_TINT);
    glow += bloomCol * bloom * NL_TORCH_BLOOM_STRENGTH;
  #endif

  diffuse.rgb *= diffuse.rgb;

  float waterPat = 0.0;
  #if defined(TRANSPARENT) && !(defined(SEASONS) || defined(RENDER_AS_BILLBOARDS))
    if (v_extra.b > 0.9) {
      #ifdef NL_REIMAGINED
        vec3 wsrc = texture2DLod(s_MatTexture, v_texcoord0, 0.0).rgb;
        float wl = dot(wsrc*wsrc, vec3(0.2126, 0.7152, 0.0722));
        float wrel = wl/NL_WATER_TEX_MEAN - 1.0;
        float wf = 1.0 + wrel*NL_WATER_TEX_CONTRAST_R + max(wrel, 0.0)*NL_WATER_HIGHLIGHT;
        waterPat = clamp(wf - 1.0, -0.6, 2.0);
        diffuse.rgb = vec3_splat(clamp(wf, 0.4, 3.0));
        diffuse.a = color.a;
      #else
        float texStretch = NL_WATER_TEX_CENTER + (diffuse.b - NL_WATER_TEX_CENTER)*NL_WATER_TEX_CONTRAST;
        float texPattern = NL_WATER_TEX_OPACITY*(texStretch*1.8 - 1.0);
        texPattern = texPattern/(1.0 + abs(texPattern)/(texPattern < 0.0 ? 0.75 : 1.5));
        waterPat = texPattern;
        diffuse.rgb = vec3_splat(1.0 + texPattern);
        diffuse.a = min(color.a*(1.0 + max(texPattern, 0.0)*NL_WATER_TEX_ALPHA), 1.0);
      #endif
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

  #if defined(NL_SKY_BLOCK_CLOUDS_REFL) || defined(NL_SKY_VOLUME_CLOUDS_REFL) || defined(NL_SKY_PLANE_CLOUDS_REFL)
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
   #ifdef NL_SUN_GLINT
      vec2 sdh = normalize(sd.xz + 0.0001);
      float galong = dot(wps.xz, sdh)*mix(-1.0, 1.0, NL_SUN_REFL_MIRROR);
      float glat = abs(wps.x*sdh.y - wps.z*sdh.x);
      float ghalf = NL_SUN_GLINT_WIDTH*0.5;
      float gmask = (1.0 - smoothstep(ghalf*0.7, ghalf, glat))*smoothstep(0.0, 2.0, galong);
      gmask = clamp(gmask*sunVis*NL_SUN_GLINT, 0.0, 1.0);
      float gl = dot(diffuse.rgb, vec3(0.299, 0.587, 0.114));
      vec3 gtint = NL_SUN_GLINT_COL*(gl*NL_SUN_GLINT_BOOST + NL_SUN_GLINT_BASE);
      diffuse.rgb = mix(diffuse.rgb, gtint, gmask);
   #endif
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

    float foamLight = mix(NL_WATER_FOAM_NIGHT, 1.0, smoothstep(-0.2, 0.25, env.dayFactor));
    foamLight = max(foamLight, smoothstep(0.4, 0.9, v_lightmapUV.x));
    diffuse.rgb = mix(diffuse.rgb, NL_WATER_FOAM_COLOR*foamLight, clamp(foamTex*NL_WATER_FOAM_STRENGTH, 0.0, 1.0));
  }
  #endif

  #ifdef NL_LAVA_FOAM
  float ldy = abs(dFdy(v_extra.g));
  highp float lwy = v_extra.g + CameraPosition.y;
  if ((v_color1.r + v_color1.g + v_color1.b) > 2.999
      && abs(fract(lwy) - 0.89) < 0.012
      && ldy < 0.0002
      && v_lightmapUV.x > 0.81 && v_lightmapUV.x < 0.876
      && v_texcoord0.y > 0.5
      && !env.underwater) {
    vec2 lp = v_causticData.xy;
    float lt = NL_LAVA_FOAM_SPEED*ViewPositionAndTime.w;
    float ln = sin(lp.x*0.9 + 3.0*sin(0.5*lp.y) + lt)*sin(lp.y*0.9 + 3.0*sin(0.5*lp.x) - lt);
    ln = 0.5 + 0.5*sin(lp.x*0.9 + 2.0*ln + 0.2*lt)*sin(lp.y*0.9 - 4.0*ln + 0.1*lt);
    float lpatch = smoothstep(0.42, 0.8, ln);
    float lcell = fract(238.084*sin(dot(floor(NL_LAVA_FOAM_SCALE*lp + vec2(lt*0.4, 0.0)), vec2(1.32, 141.3))));
    float lfoam = lpatch*smoothstep(0.3, 0.9, lcell + 0.45*lpatch);
    vec3 lcol = mix(NL_LAVA_FOAM_COLOR_LOW, NL_LAVA_FOAM_COLOR_HIGH, lcell);
    #ifdef NL_LAVA_FOAM_DEBUG
      diffuse.rgb = vec3(3.0, 0.0, 3.0);
    #else
      diffuse.rgb = mix(diffuse.rgb, lcol, clamp(lfoam*NL_LAVA_FOAM_STRENGTH, 0.0, 1.0));
    #endif
  }
  #endif

  diffuse.rgb = mix(diffuse.rgb, vec3(diffuse.b + 0.1*diffuse.r, diffuse.r*0.95, diffuse.r*0.75 + diffuse.g*1.2), lavaBlue);

  #ifdef NL_METAL_REFLECTION
    highp vec3 mpos = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
    highp vec3 mn = cross(dFdx(mpos)*64.0, dFdy(mpos)*64.0);
    mn = length(mn) > 0.0 ? normalize(mn) : vec3(0.0, 1.0, 0.0);
    mn = dot(mn, mpos) > 0.0 ? -mn : mn;
    if (metalMask > 0.5 && !env.underwater && !env.nether && !env.end) {
      vec3 mlook = normalize(mpos - ViewPositionAndTime.xyz);
      vec3 mR = reflect(mlook, mn);
      nl_skycolor msky = nlSkyColors(env);
      float mt = ViewPositionAndTime.w;
      vec3 mrefl = nlRenderSky(msky, env, mR, mt, true);

      #ifdef NL_CLOUD_AURORA_REFLECTION
        if (mR.y > 0.04) {
          vec4 mc = nlCloudAuroraReflection(msky, env, mR, mpos, CameraPosition.xyz, mt);
          mrefl = mix(mrefl, mc.rgb, mc.a);
        }
      #endif

      #if defined(NL_SKY_BLOCK_CLOUDS_REFL) || defined(NL_SKY_VOLUME_CLOUDS_REFL) || defined(NL_SKY_PLANE_CLOUDS_REFL)
        if (mR.y > 0.04) {
          float mj = fract(52.9829189*fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
          vec4 mkc = nlSkyCloudReflection(mR, mpos - ViewPositionAndTime.xyz, msky, env, mt, mj);
          mrefl = mrefl*(1.0 - mkc.a) + mkc.rgb;
        }
      #endif

      float mcos = clamp(dot(-mlook, mn), 0.0, 1.0);
      float mfres = mix(0.45, 1.0, pow(1.0 - mcos, 4.0));
      float msk = smoothstep(0.4, 0.9, v_lightmapUV.y)*(1.0 - 0.5*env.rainFactor);
      float sunUp = smoothstep(0.0, 0.12, env.sunDir.y)*(1.0 - env.rainFactor);
      vec3 msun = nlSunLightColor(env)*pow(max(dot(mR, env.sunDir), 0.0), 80.0)*sunUp;
      vec3 mtorch = vec3(1.0, 0.55, 0.2)*pow(1.0 - mcos, 2.0)*v_lightmapUV.x*v_lightmapUV.x*0.5;
      vec3 mtint = mix(vec3_splat(1.0), diffuse.rgb/max(max(diffuse.r, max(diffuse.g, diffuse.b)), 0.001), 0.6);
      vec3 mspec = (mrefl + msun)*msk + mtorch;
      float mstr = clamp(mfres*NL_METAL_REFLECTION, 0.0, 1.0);
      diffuse.rgb = diffuse.rgb*(1.0 - 0.5*mstr*msk) + mspec*mtint*mstr;
    }
  #endif

  #ifdef NL_DAY_WHITE
    if (!env.underwater && !env.end && !env.nether && v_extra.b < 0.9) {
      float dwDay = smoothstep(NL_DAY_WHITE_START, NL_DAY_WHITE_FULL, env.dayFactor)*(1.0 - env.rainFactor);
      float dwOut = smoothstep(0.82, 0.95, v_lightmapUV.y);
      float dwLum = luminance(diffuse.rgb);
      float dwHi = smoothstep(NL_DAY_WHITE_THR, NL_DAY_WHITE_THR + 0.5, dwLum);
      float dwK = clamp(dwDay*dwOut*dwHi*NL_DAY_WHITE, 0.0, 1.0);
      vec3 dwTarget = mix(vec3_splat(dwLum), vec3_splat(1.0), NL_DAY_WHITE_PURE)*NL_DAY_WHITE_GAIN;
      diffuse.rgb = mix(diffuse.rgb, dwTarget, dwK);
    }
  #endif

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  #ifdef NL_SUN_SHAFTS
    if (!env.underwater && !env.end && !env.nether) {
      float shm = nlSunShaftMask(env);
      if (shm > 0.01) {
        highp vec3 wpq = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
        float dq = length(wpq);
        vec3 dlq = vec3(-wpq.x, wpq.y, -wpq.z)/max(dq, 0.001);
        float shafts = nlSunShafts(dlq, env.sunDir, ViewPositionAndTime.w);
        float scq = 1.0 - exp(-dq*NL_SUN_SHAFTS_DENSITY);
        diffuse.rgb += NL_SUN_SHAFTS_COL*shafts*scq*smoothstep(0.5, 0.95, v_lightmapUV.y)*shm*NL_SUN_SHAFTS;
      }
    }
  #endif

  #ifdef NL_TERRAIN_SUN_GLOW
    if (!env.underwater && !env.end && !env.nether) {
      highp vec3 grel = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
      vec3 grd = vec3(-grel.x, grel.y, -grel.z)*inversesqrt(max(dot(grel, grel), 0.0001));
      float gsun = smoothstep(-0.05, 0.15, env.sunDir.y);
      float gsky = mix(NL_TERRAIN_SUN_GLOW_INDOOR, 1.0, smoothstep(0.3, 0.8, v_lightmapUV.y));
      float gamt = NL_TERRAIN_SUN_GLOW*gsun*gsky;
      if (gamt > 0.001) {
        float gset = 1.0 - smoothstep(0.0, 0.35, abs(env.sunDir.y));
        diffuse.rgb = nlTerrainSunGlow(grd, env.sunDir, diffuse.rgb, NL_TERRAIN_SUN_GLOW_COL, gset, env.rainFactor, gamt);
      }
    }
  #endif

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
