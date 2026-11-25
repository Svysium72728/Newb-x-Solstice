#ifndef INSTANCING
$input v_worldPos, v_underwaterRainTimeDay, v_fogColor, v_starRot
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>
  uniform vec4 TimeOfDay;
  uniform vec4 FogColor;
  uniform vec4 FogAndDistanceControl;
  uniform vec4 ViewPositionAndTime;

  #ifdef NL_SKY_BLOCK_CLOUDS
    SAMPLER2D_AUTOREG(s_BlockCloudNoise);
    #ifndef NL_BLOCK_CLOUD_STEPS
      #define NL_BLOCK_CLOUD_STEPS 6
    #endif
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

    // One point fetch returns this cell plus its +x / +y / +xy neighbours (packed in RGBA).
    // Each cell is thresholded first, then blended only across a narrow strip at the cell
    // border (NL_BLOCK_CLOUD_SHARP), so blocks stay square with soft, slightly rounded edges.
    float blockOcc(highp vec2 q, float occThr) {
      highp vec2 c = q - 0.5;
      highp vec2 i = floor(c);
      vec2 f = clamp((fract(c) - 0.5)*NL_BLOCK_CLOUD_SHARP + 0.5, 0.0, 1.0);
      vec4 n = texture2D(s_BlockCloudNoise, (i + 0.5)*0.00390625);
      vec4 s = step(vec4_splat(occThr), n);
      return mix(mix(s.x, s.y, f.x), mix(s.z, s.w, f.x), f.y);
    }

    vec4 BlockClouds(vec3 viewDir, vec3 sd, nl_skycolor skycol, nl_environment env, highp float time, float jitter) {
      if (viewDir.y < 0.05) {
        return vec4(0.0, 0.0, 0.0, 0.0);
      }
      const int steps = NL_BLOCK_CLOUD_STEPS;
      float invY = 1.0/viewDir.y;
      float occThr = clamp(1.0 - NL_BLOCK_CLOUD_COVER, 0.05, 0.95);
      // wind is added AFTER the per-layer scale so every slice drifts at the same speed
      highp vec2 wind = vec2(time*NL_BLOCK_CLOUD_SPEED, time*NL_BLOCK_CLOUD_SPEED*0.4);

      float daylight = smoothstep(-0.2, 0.25, env.dayFactor);
      vec3 L = mix(env.moonDir, env.sunDir, step(0.0, env.dayFactor));
      float dawn = 1.0 - env.dayFactor*env.dayFactor;
      dawn *= dawn*dawn;
      dawn *= 1.0 - smoothstep(0.0, 0.3, env.dayFactor);
      vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
      float bright = mix(NL_BLOCK_CLOUD_NIGHT, 1.0, daylight)*(1.0 - 0.35*env.rainFactor);
      vec3 topCol = NL_BLOCK_CLOUD_LIGHT_COL*mix(vec3_splat(1.0), sunCol, mix(NL_BLOCK_CLOUD_SUN_TINT, 0.5, dawn))*bright;
      vec3 shadeCol = NL_BLOCK_CLOUD_SHADE_COL*mix(vec3_splat(1.0), skycol.horizon*1.4, 0.35)*bright;

      // wall lighting: compare occupancy one step toward the sun/moon (fades out when it is overhead)
      float lxz = length(L.xz);
      vec2 Lq = L.xz/max(lxz, 0.0001)*0.6;
      float sunSide = clamp(lxz*2.5, 0.0, 1.0);

      float acc = 0.0;
      float w0 = 0.0;
      float count = 0.0;
      vec3 wallAcc = vec3(0.0, 0.0, 0.0);
      // no early break on purpose: `count` feeds the underside colour and must cover every layer
      for (int i = 0; i < steps; i++) {
        float h = (float(i) + 0.5)/float(steps);
        highp vec2 q = sd.xz*((NL_BLOCK_CLOUD_BASE + NL_BLOCK_CLOUD_THICK*h)*invY)*NL_BLOCK_CLOUD_SCALE + wind;

        float o = blockOcc(q, occThr);
        float m = smoothstep(0.5 - NL_BLOCK_CLOUD_SOFT, 0.5 + NL_BLOCK_CLOUD_SOFT, o);
        count += m;

        float a = m*(1.0 - acc);
        if (a > 0.001) {
          float lit = clamp(0.5 - (blockOcc(q + Lq, occThr) - o)*1.2, 0.0, 1.0);
          lit = mix(0.5, lit, sunSide);
          float wk = clamp(0.25 + 0.65*h + 0.35*(lit - 0.5), 0.0, 1.0);
          float first = (i == 0) ? 1.0 : 0.0;
          w0 += a*first;
          wallAcc += a*(1.0 - first)*mix(shadeCol, topCol, wk);
          acc += a;
        }
      }

      if (acc < 0.001) {
        return vec4(0.0, 0.0, 0.0, 0.0);
      }

      float c01 = count/float(steps);
      vec3 under = mix(topCol*0.92, shadeCol, pow(clamp((c01 - 0.08)/0.6, 0.0, 1.0), 0.8));
      float k = NL_BLOCK_CLOUD_OPACITY*smoothstep(0.05, 0.25, viewDir.y);
      return vec4((wallAcc + w0*under)*k, acc*k);
    }
  #endif

  #ifdef NL_SKY_VOLUME_CLOUDS
    #ifndef NL_VOL_CLOUD_STEPS
      #define NL_VOL_CLOUD_STEPS 8
    #endif
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
    #ifndef NL_VOL_CLOUD_SHADE
      #define NL_VOL_CLOUD_SHADE 14.0
    #endif
    #ifndef NL_VOL_CLOUD_SHADE_DIST
      #define NL_VOL_CLOUD_SHADE_DIST 0.012
    #endif
    #ifndef NL_VOL_CLOUD_SHADOW_TINT
      #define NL_VOL_CLOUD_SHADOW_TINT vec3(0.78, 0.84, 1.0)
    #endif

    SAMPLER2D_AUTOREG(s_VolCloudNoise);

      vec4 VolumetricClouds(vec3 viewDir, vec3 sd, nl_skycolor skycol, nl_environment env, highp float time, float jitter) {
      if (viewDir.y < 0.04) {
        return vec4(0.0, 0.0, 0.0, 0.0);
      }
      const int layers = NL_VOL_CLOUD_STEPS;
      float cover = clamp(NL_VOL_CLOUD_COVER + 0.35*env.rainFactor, 0.0, 1.0);
      float thr = mix(0.62, 0.40, cover);

      vec3 L = mix(env.moonDir, env.sunDir, step(0.0, env.dayFactor));
      float dawn = 1.0 - env.dayFactor*env.dayFactor;
      dawn *= dawn*dawn;
      vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
      vec3 moonCol = NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
      vec3 lightCol = mix(moonCol*NL_VOL_CLOUD_NIGHT, sunCol, smoothstep(-0.1, 0.15, env.dayFactor))*(1.0 - 0.6*env.rainFactor);
      vec3 shadowCol = mix(skycol.zenith, skycol.horizon, 0.5)*NL_VOL_CLOUD_SHADOW_TINT*(1.0 - 0.4*env.rainFactor);
      vec3 topAmb = mix(shadowCol, vec3_splat(1.0), 0.45)*mix(NL_VOL_CLOUD_NIGHT*2.0, 1.0, smoothstep(-0.1, 0.15, env.dayFactor));
      float cosv = max(dot(sd, L), 0.0);
      highp vec2 wind = vec2(time*NL_VOL_CLOUD_SPEED, time*NL_VOL_CLOUD_SPEED*0.4);
      vec2 toLight = L.xz/max(L.y, 0.25);

      highp vec2 cp = sd.xz/viewDir.y*NL_VOL_CLOUD_SCALE + wind;
      float dt = texture2D(s_VolCloudNoise, cp*3.7 + 0.31).g;
      float lb = texture2D(s_VolCloudNoise, cp + toLight*0.09).r;
      float lshade = clamp((lb - thr)*3.0, 0.0, 1.0);

      float T = 1.0;
      vec3 col = vec3(0.0, 0.0, 0.0);
      for (int i = 0; i < layers; i++) {
        float h = (float(i) + 0.5 + 0.35*(jitter - 0.5))/float(layers);
        highp vec2 p = cp*(1.0 - 0.12*h) + toLight*0.04*h;
        float b = texture2D(s_VolCloudNoise, p).r;
        float prof = smoothstep(0.0, 0.18, h)*(1.0 - smoothstep(0.35, 1.0, h - (b - 0.5)*1.1));
        float tp = thr + 0.25*(1.0 - prof);
        float n = b + (dt - 0.5)*0.22*(1.0 - 0.5*prof);
        float d = smoothstep(tp - 0.02, tp + 0.2, n);
        if (d > 0.002) {
          float lit = exp(-lshade*(1.0 - h)*1.8);
          float rim = smoothstep(0.0, 0.6, d);
          float belly = mix(0.4, 1.0, h);
          float silver = pow(cosv, 6.0)*lit;
          vec3 c = mix(shadowCol*0.6, topAmb, belly*rim) + lightCol*(lit*0.9*belly + 2.5*silver)*mix(0.6, 1.0, rim);
          float a = 1.0 - exp(-d*NL_VOL_CLOUD_DENSITY/float(layers));
          col += T*a*c;
          T *= 1.0 - a;
          if (T < 0.1) {
            break;
          }
        }
      }
      float f = smoothstep(0.04, 0.22, viewDir.y);
      return vec4(col*f, (1.0 - T)*f);
    }
  #endif

  #ifdef NL_SKY_EQUI_CLOUDS
    #ifndef NL_EQUI_CLOUD_COVER
      #define NL_EQUI_CLOUD_COVER 0.50
    #endif
    #ifndef NL_EQUI_CLOUD_SOFT
      #define NL_EQUI_CLOUD_SOFT 0.14
    #endif
    #ifndef NL_EQUI_CLOUD_OPACITY
      #define NL_EQUI_CLOUD_OPACITY 1.0
    #endif
    #ifndef NL_EQUI_CLOUD_SHADE
      #define NL_EQUI_CLOUD_SHADE 1.8
    #endif
    #ifndef NL_EQUI_CLOUD_BUMP
      #define NL_EQUI_CLOUD_BUMP 1.0
    #endif
    #ifndef NL_EQUI_CLOUD_RIM
      #define NL_EQUI_CLOUD_RIM 0.5
    #endif
    #ifndef NL_EQUI_CLOUD_EDGE
      #define NL_EQUI_CLOUD_EDGE 0.25
    #endif
    #ifndef NL_EQUI_CLOUD_SPEED
      #define NL_EQUI_CLOUD_SPEED 0.0004
    #endif
    #ifndef NL_EQUI_CLOUD_NIGHT
      #define NL_EQUI_CLOUD_NIGHT 0.30
    #endif
    #ifndef NL_EQUI_CLOUD_SHADOW_TINT
      #define NL_EQUI_CLOUD_SHADOW_TINT vec3(0.78, 0.84, 1.0)
    #endif

    SAMPLER2D_AUTOREG(s_SkyCloudTex);

    vec4 EquiClouds(vec3 viewDir, vec3 sd, nl_skycolor skycol, nl_environment env, highp float time) {
      if (viewDir.y < 0.02) {
        return vec4(0.0, 0.0, 0.0, 0.0);
      }
      highp float u = atan2(viewDir.x, viewDir.z)*0.15915494 + 0.5 + time*NL_EQUI_CLOUD_SPEED;
      float v = clamp(1.0 - asin(min(viewDir.y, 1.0))*0.63661977, 0.002, 0.998);
      vec3 tx = texture2DLod(s_SkyCloudTex, vec2(u, v), 0.0).rgb;

      float cover = clamp(NL_EQUI_CLOUD_COVER + 0.30*env.rainFactor, 0.0, 1.0);
      float thr = 1.0 - cover;
      float a = smoothstep(thr, thr + NL_EQUI_CLOUD_SOFT, tx.r);
      a = min(a*NL_EQUI_CLOUD_OPACITY, 1.0);
      float edge = 1.0 - smoothstep(thr, thr + NL_EQUI_CLOUD_EDGE, tx.r);

      vec3 L = mix(env.moonDir, env.sunDir, step(0.0, env.dayFactor));
      float dawn = 1.0 - env.dayFactor*env.dayFactor;
      dawn *= dawn*dawn;
      vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
      vec3 moonCol = NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
      float dayK = smoothstep(-0.1, 0.15, env.dayFactor);
      vec3 lightCol = mix(moonCol*NL_EQUI_CLOUD_NIGHT, sunCol, dayK)*(1.0 - 0.6*env.rainFactor);
      vec3 shadowCol = mix(skycol.zenith, skycol.horizon, 0.5)*NL_EQUI_CLOUD_SHADOW_TINT*(1.0 - 0.4*env.rainFactor);
      vec3 topAmb = mix(shadowCol, vec3_splat(1.0), 0.45)*mix(NL_EQUI_CLOUD_NIGHT*2.0, 1.0, dayK);

      float lit = clamp(exp(-tx.g*NL_EQUI_CLOUD_SHADE) + (tx.b - 0.5)*NL_EQUI_CLOUD_BUMP + edge*NL_EQUI_CLOUD_RIM, 0.0, 1.0);
      float silver = pow(max(dot(sd, L), 0.0), 8.0)*lit;
      vec3 c = mix(shadowCol, topAmb, lit) + lightCol*(lit*0.9 + 2.0*silver);

      float f = smoothstep(0.02, 0.2, viewDir.y);
      return vec4(c*a*f, a*f);
    }
  #endif

#ifdef NL_SKY_PLANE_CLOUDS
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
  #ifndef NL_PLANE_CLOUD_NIGHT
    #define NL_PLANE_CLOUD_NIGHT 0.30
  #endif
  #ifndef NL_PLANE_CLOUD_FLAT
    #define NL_PLANE_CLOUD_FLAT 0.5
  #endif

  SAMPLER2D_AUTOREG(s_PlaneCloudNoise);

  float planeNoise(highp vec2 p) {
    highp vec2 q = fract(p*0.00390625);
    return texture2DLod(s_PlaneCloudNoise, q, 0.0).r;
  }

  float planeFbm(highp vec2 x, float detail) {
    float v = 0.5*mix(NL_PLANE_CLOUD_FLAT, planeNoise(x), min(detail*2.0, 1.0));
    x = x*2.0 + vec2(100.0, 100.0);
    v += 0.25*mix(NL_PLANE_CLOUD_FLAT, planeNoise(x), detail*detail);
    return v;
  }

  float planeCloudShape(highp vec2 uv, float detail) {
    uv *= 5.0;
    float a = 0.0;
    for (int i = 0; i < NL_PLANE_CLOUD_LAYERS; i++) {
      uv -= clamp(uv*0.0099, vec2(-0.06, -0.06), vec2(0.06, 0.06));
      a = mix(a, 1.0, smoothstep(0.35, 0.45, planeFbm(uv, detail)));
    }
    return a - pow(smoothstep(0.0, 1.5, planeFbm(uv, detail) + 0.8), 1.5)*0.7;
  }

  vec4 PlaneClouds(vec3 viewDir, vec3 sd, nl_skycolor skycol, nl_environment env, highp float time) {
    if (viewDir.y <= 0.0) {
      return vec4(0.0, 0.0, 0.0, 0.0);
    }

    highp float above = max(viewDir.y, NL_PLANE_CLOUD_HORIZON_MIN);
    float horizon = smoothstep(NL_PLANE_CLOUD_HORIZON_FADE_START, NL_PLANE_CLOUD_HORIZON_FADE_END, above);
    float detail = clamp(above/NL_PLANE_CLOUD_HORIZON_DETAIL, 0.0, 1.0);

    highp vec2 sxz = sd.xz;
    highp vec2 plane = sxz*NL_PLANE_CLOUD_SCALE/above;
    plane.y *= NL_PLANE_CLOUD_STRETCH;
    plane = clamp(plane, vec2(-300.0, -300.0), vec2(300.0, 300.0));

    float fade = clamp(1.0 - length(plane)*NL_PLANE_CLOUD_FAR_FADE, 0.0, 1.0)*horizon;
    if (fade < 0.002) {
      return vec4(0.0, 0.0, 0.0, 0.0);
    }

    highp vec2 uv = plane;
    uv.x += time*NL_PLANE_CLOUD_SPEED;

    float shape = max(planeCloudShape(uv, detail)*NL_PLANE_CLOUD_OPACITY, 0.0);
    float a = clamp(shape*fade, 0.0, 1.0);
    float lit = clamp(shape, 0.0, 1.0);

    float dawn = 1.0 - env.dayFactor*env.dayFactor;
    dawn *= dawn*dawn;
    vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
    vec3 moonCol = NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
    float dayK = smoothstep(-0.1, 0.15, env.dayFactor);
    vec3 lightCol = mix(moonCol*NL_PLANE_CLOUD_NIGHT, sunCol, dayK)*(1.0 - 0.6*env.rainFactor);
    vec3 shadowCol = mix(skycol.zenith, skycol.horizon, 0.5)*vec3(0.78, 0.84, 1.0)*(1.0 - 0.4*env.rainFactor);
    vec3 topAmb = mix(shadowCol, vec3_splat(1.0), 0.45)*mix(NL_PLANE_CLOUD_NIGHT*2.0, 1.0, dayK);

    vec3 c = mix(shadowCol, topAmb, lit) + lightCol*lit*0.9;
    return vec4(c*a, a);
  }
#endif

  #ifdef NL_CUSTOM_STARS
    #ifndef NL_STAR_GRID
      #define NL_STAR_GRID 60.0
    #endif
    #ifndef NL_STAR_DENSITY
      #define NL_STAR_DENSITY 0.22
    #endif
    #ifndef NL_STAR_SIZE
      #define NL_STAR_SIZE 1.1
    #endif
    #ifndef NL_STAR_GLOW
      #define NL_STAR_GLOW 1.0
    #endif
    #ifndef NL_STAR_BRIGHTNESS
      #define NL_STAR_BRIGHTNESS 1.0
    #endif

    float starHash(highp vec3 p) {
      return fract(sin(dot(p, vec3(127.1, 311.7, 74.7)))*43758.5453);
    }

    vec3 CustomStars(vec3 viewDir, nl_environment env, highp float tod, highp float time) {
      float pxAng = max(length(dFdx(viewDir)), length(dFdy(viewDir)));
      float vis = (1.0 - smoothstep(-0.2, 0.05, env.dayFactor))*(1.0 - env.rainFactor)*smoothstep(0.0, 0.12, viewDir.y);
      if (vis < 0.01) {
        return vec3(0.0, 0.0, 0.0);
      }
      highp vec3 q = viewDir;
      q.xz = mul(rmat2(-degToRad(NL_SUN_PATH_YAW)), q.xz);
      q.yz = mul(rmat2(degToRad(NL_SUN_PATH_TILT)), q.yz);
      float ca = v_starRot.x;
      float sa = v_starRot.y;
      q = vec3(ca*q.x - sa*q.y, sa*q.x + ca*q.y, q.z);

      vec3 aq = abs(q);
      float m = max(aq.x, max(aq.y, aq.z));
      vec3 s = q/m;
      vec2 uv;
      float fid;
      if (aq.x >= m) {
        uv = s.yz;
        fid = step(q.x, 0.0);
      } else if (aq.y >= m) {
        uv = s.xz;
        fid = 2.0 + step(q.y, 0.0);
      } else {
        uv = s.xy;
        fid = 4.0 + step(q.z, 0.0);
      }

      float r2 = dot(uv, uv);
      float pxUV = pxAng*(1.0 + r2)/sqrt(1.0 + max(uv.x*uv.x, uv.y*uv.y));
      vec2 g = (uv*0.5 + 0.5)*NL_STAR_GRID;
      vec2 cell = floor(g);
      vec3 id = vec3(cell, fid);
      float dens = NL_STAR_DENSITY*pow(1.0 + r2, -1.5);
      if (starHash(id) > dens) {
        return vec3(0.0, 0.0, 0.0);
      }

      vec2 sp = 0.3 + 0.4*vec2(starHash(id + 1.7), starHash(id + 5.3));
      float d = length(g - cell - sp)/(pxUV*0.5*NL_STAR_GRID);
      float b = starHash(id + 13.1);
      float inten = 0.16 + 4.0*pow(b, 7.0);
      float core = 1.0 - smoothstep(0.45*NL_STAR_SIZE, NL_STAR_SIZE, d);
      float halo = smoothstep(0.93, 1.0, b)*NL_STAR_GLOW*0.6*exp(-0.5*d*d);
      float tw = 1.0 - 0.15*smoothstep(0.5, 1.0, b)*sin(time*(2.0 + 4.0*starHash(id + 3.1)) + 40.0*b);
      vec3 tint = mix(vec3(0.75, 0.86, 1.0), vec3(1.0, 0.92, 0.78), starHash(id + 7.7));
      return tint*inten*(core + halo)*tw*vis*NL_STAR_BRIGHTNESS;
      }
     #endif
  #ifdef NL_SKY_AURORA
    float pow2(float x){return x*x;}
    float clamp01(float x){return clamp(x,0.0,1.0);}
    float sqrt1(float x){return sqrt(max(x,0.0));}

    #ifdef NL_SKY_AURORA_HEAVY
      #ifdef NL_SKY_AURORA_SMOOTH
        #ifndef NL_AURORA_SMOOTH_CENTER
          #define NL_AURORA_SMOOTH_CENTER 0.4
        #endif
        #ifndef NL_AURORA_SMOOTH_SHARP
          #define NL_AURORA_SMOOTH_SHARP 8.0
        #endif

        SAMPLER2D_AUTOREG(s_noisesmooth);

        float smoothAuroraNoise(highp vec2 p){
          highp vec2 quantized = floor(p * 1000.0) / 1000.0 + 0.000003;
          return texture2D(s_noisesmooth, quantized).r;
        }

        vec3 GetAuroraSmooth(vec3 viewDir, highp float t, float dither) {
            float VdotU = clamp(viewDir.y, 0.0, 1.0);
            float visibility = sqrt1(clamp01(VdotU * 4.5 - 0.25));
            visibility *= 8.0 - VdotU * 0.9;
            if (visibility <= 1.0) return vec3_splat(0.0);

            vec3 aurora = vec3(0.0,0.0,0.0);
            highp vec2 wpos = viewDir.xz / max(viewDir.y, 0.1);
            highp vec2 cameraPosM = vec2(t * NL_SKY_AURORA_SPEED, 0.0);

            const int sampleCount = 20;
            const int sampleCountP = sampleCount + 10;

            float ditherM = dither + 10.0;
            float hz = 1.0 - smoothstep(0.1, 0.55, VdotU);

            for (int i = 0; i < sampleCount; i++) {
                float current = pow2((float(i) + ditherM) / float(sampleCountP));
                float currentM = 1.0 - current;

                highp vec2 planePos = wpos * (0.9 + current) * 10.0 * 0.0075 + cameraPosM * 0.003;

                float noise = smoothAuroraNoise(planePos);
                noise = pow(clamp(1.0 - abs(noise - NL_AURORA_SMOOTH_CENTER), 0.0, 1.0), NL_AURORA_SMOOTH_SHARP);

                float anim1 = smoothAuroraNoise(planePos * 0.5);
                float anim2 = smoothAuroraNoise(planePos * 0.8);
                noise *= mix(anim1, anim2, 0.5);

                float tealW = clamp(currentM*currentM + 0.6*hz, 0.0, 1.0);
                vec3 purpleCol = vec3(0.62, 0.55, 1.35);
                vec3 tealCol = vec3(1.2, 3.0, 3.3);
                aurora += noise * mix(purpleCol * pow(max(currentM, 0.001), 0.4), tealCol * currentM, tealW);
            }

            aurora *= 0.8;
            return aurora * visibility / float(sampleCount);
        }
      #else
        SAMPLER2D_AUTOREG(s_noisevoxels);

        float cubicFollowNoise(vec2 p){
          vec2 quantized = floor(p * 128.0) / 128.0 + 2.0 / 128.0;
          return texture2D(s_noisevoxels, quantized).r;
        }

        vec3 GetAuroraHeavy(vec3 viewDir, vec4 ViewPosTime, float dither) {
            float VdotU = clamp(viewDir.y, 0.0, 1.0);
            float visibility = sqrt1(clamp01(VdotU * 4.5 - 0.25));
            visibility *= 8.0 - VdotU * 0.9;
            if (visibility <= 1.0) return vec3_splat(0.0);

            vec3 aurora = vec3(0.0,0.0,0.0);
            vec3 wpos = viewDir;
            wpos.xz /= max(wpos.y, 0.1);

            vec2 cameraPosM = vec2(ViewPosTime.w * NL_SKY_AURORA_SPEED, 0.0);

            const int sampleCount = 20;
            const int sampleCountP = sampleCount + 10;

            float ditherM = dither + 10.0;
            float hz = 1.0 - smoothstep(0.1, 0.55, VdotU);

            for (int i = 0; i < sampleCount; i++) {
                float current = pow2((float(i) + ditherM) / float(sampleCountP));
                float currentM = 1.0 - current;

                vec2 planePos = wpos.xz * (0.9 + current) * 10.0 * 0.0075 + cameraPosM * 0.003;

                float noise = cubicFollowNoise(planePos);
                noise = pow2(pow2(pow2(1.0 - 1.0 * abs(noise - 0.4))));

                float anim1 = cubicFollowNoise(planePos * 0.5 + ViewPosTime.w * 0.00);
                float anim2 = cubicFollowNoise(planePos * 0.8 - ViewPosTime.w * 0.00);
                noise *= mix(anim1, anim2, 0.5);

                float tealW = clamp(currentM*currentM + 0.6*hz, 0.0, 1.0);
                vec3 purpleCol = vec3(0.62, 0.55, 1.35);
                vec3 tealCol = vec3(1.2, 3.0, 3.3);
                aurora += noise * mix(purpleCol * pow(max(currentM, 0.001), 0.4), tealCol * currentM, tealW);
            }

            aurora *= 0.8;
            return aurora * visibility / float(sampleCount);
        }
      #endif
    #endif
  #endif
#endif

#ifdef NL_SKY_AURORA_COMP
  #ifndef NL_AURORA_COMP_STYLE
    #define NL_AURORA_COMP_STYLE 1
  #endif
  #ifndef NL_AURORA_COMP_LAYERS
    #define NL_AURORA_COMP_LAYERS 12
  #endif
  #ifndef NL_AURORA_COMP_SPEED
    #define NL_AURORA_COMP_SPEED 0.04
  #endif
  #ifndef NL_AURORA_COMP_BRIGHTNESS
    #define NL_AURORA_COMP_BRIGHTNESS 0.3
  #endif
  #ifndef NL_AURORA_COMP_COL1
    #define NL_AURORA_COMP_COL1 vec3(7.0, 2.2, 12.0)
  #endif
  #ifndef NL_AURORA_COMP_COL2
    #define NL_AURORA_COMP_COL2 vec3(6.0, 16.0, 18.0)
  #endif

  SAMPLER2D_AUTOREG(s_AuroraNoise);

  vec3 GetAuroraComp(vec3 viewDir, highp float time) {
    float VdotU = clamp(viewDir.y, 0.0, 1.0);
    float visibility = sqrt(max(clamp(VdotU*1.5 - 0.225, 0.0, 1.0), 0.0));
    visibility *= 1.0 - VdotU*0.9;
    if (visibility <= 0.0) {
      return vec3(0.0, 0.0, 0.0);
    }

    float dither = texture2DLod(s_AuroraNoise, gl_FragCoord.xy*0.0078125, 0.0).r;

    highp vec2 wp = viewDir.xz/max(viewDir.y, 0.0001);
    highp vec2 camM = vec2(time*NL_AURORA_COMP_SPEED, 0.0);
    highp float anim = time*0.001;

    float ditherM = dither + 5.0;
    float sampleP = float(NL_AURORA_COMP_LAYERS + 10);
    vec3 aurora = vec3(0.0, 0.0, 0.0);

    for (int i = 0; i < NL_AURORA_COMP_LAYERS; i++) {
      float current = (float(i) + ditherM)/sampleP;
      current *= current;

      highp vec2 pp = wp*(0.8 + current)*11.0 + camM;

      #if NL_AURORA_COMP_STYLE == 1
        pp = floor(pp)*0.0007;
        float n = texture2DLod(s_AuroraNoise, fract(pp), 0.0).b;
        n = 1.0 - 2.0*abs(n - 0.5);
        n *= n; n *= n; n *= n; n *= n;
        float d = texture2DLod(s_AuroraNoise, fract(pp*100.0 + anim), 0.0).b;
        n *= d*sqrt(d);
      #else
        pp *= 0.0007;
        float n = texture2DLod(s_AuroraNoise, fract(pp), 0.0).r;
        n = 1.0 - 2.0*abs(n - 0.5);
        n *= n; n *= n; n *= n; n *= n;
        n *= texture2DLod(s_AuroraNoise, fract(pp*3.0 + anim), 0.0).b;
        n *= texture2DLod(s_AuroraNoise, fract(pp*5.0 - anim), 0.0).b;
      #endif

      float currentM = 1.0 - current;
      float cm2 = currentM*currentM;
      aurora += n*currentM*mix(NL_AURORA_COMP_COL1, NL_AURORA_COMP_COL2, cm2*cm2);
    }

    #if NL_AURORA_COMP_STYLE == 1
      aurora *= 1.3;
    #else
      aurora *= 1.8;
    #endif

    return aurora*visibility*NL_AURORA_COMP_BRIGHTNESS/float(NL_AURORA_COMP_LAYERS);
  }
#endif

void main() {
  #ifndef INSTANCING
    vec3 viewDir = normalize(v_worldPos);

    nl_environment env;
    env.end = false;
    env.nether = false;
    env.underwater = v_underwaterRainTimeDay.x > 0.5;
    env.rainFactor = v_underwaterRainTimeDay.y;
    env.dayFactor = v_underwaterRainTimeDay.w;
    env.fogCol = FogColor.rgb;
    env = calculateSunParams(env, TimeOfDay.x);

    nl_skycolor skycol = nlOverworldSkyColors(env);

    vec3 skyColor = nlRenderSky(skycol, env, -viewDir, v_underwaterRainTimeDay.z, true);

    #ifdef NL_SHOOTING_STAR
      skyColor += NL_SHOOTING_STAR*nlRenderShootingStar(viewDir, env.fogCol, v_underwaterRainTimeDay.z);
    #endif

    #ifdef NL_GALAXY_STARS
      skyColor += NL_GALAXY_STARS*nlRenderGalaxy(viewDir, env.fogCol, env, v_underwaterRainTimeDay.z);
    #endif

    #ifdef NL_CUSTOM_STARS
    if (!env.underwater) {
      skyColor += CustomStars(viewDir, env, TimeOfDay.x, v_underwaterRainTimeDay.z);
    }
    #endif

    #ifdef NL_SUN_FOG_GLOW
      skyColor += nlSunLightColor(env)*nlSunFogGlow(-viewDir, env)*NL_SUN_FOG_GLOW*NL_SUN_FOG_SKY;
    #endif

    #ifdef NL_SUN_SHAFTS
    if (!env.underwater && !env.end && !env.nether) {
      float shm = nlSunShaftMask(env);
      if (shm > 0.01) {
        vec3 sds = vec3(-viewDir.x, viewDir.y, -viewDir.z);
        skyColor += NL_SUN_SHAFTS_COL*nlSunShafts(sds, env.sunDir, v_underwaterRainTimeDay.z)*shm*NL_SUN_SHAFTS;
      }
    }
    #endif

    #ifdef NL_SKY_AURORA
    if (!env.underwater && !env.end && !env.nether) {
      #ifdef NL_SKY_AURORA_HEAVY
        float dither2 = fract(sin(dot(viewDir.xy, vec2(12.9898,78.233))) * 43758.5453);
        float mask = (1.0-1.0*env.rainFactor)*max(1.0 - 3.0*max(v_fogColor.b, v_fogColor.g), 0.0);
        #ifdef NL_SKY_AURORA_SMOOTH
          skyColor += GetAuroraSmooth(viewDir, ViewPositionAndTime.w, dither2) * mask;
        #else
          skyColor += GetAuroraHeavy(viewDir, ViewPositionAndTime, dither2) * mask;
        #endif
      #endif
    }
    #endif

    #ifdef NL_SKY_AURORA_COMP
    if (!env.underwater && !env.end && !env.nether) {
      float cmask = (1.0 - env.rainFactor)*max(1.0 - 3.0*max(v_fogColor.b, v_fogColor.g), 0.0);
      skyColor += GetAuroraComp(viewDir, ViewPositionAndTime.w)*cmask;
    }
    #endif

    #ifdef NL_SKY_BLOCK_CLOUDS
    if (!env.underwater) {
      float cj = fract(52.9829189*fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
      vec3 sdb = vec3(-viewDir.x, viewDir.y, -viewDir.z);
      vec4 cb = BlockClouds(viewDir, sdb, skycol, env, v_underwaterRainTimeDay.z, cj);
      skyColor = skyColor*(1.0 - cb.a) + cb.rgb;
    }
    #endif

    #ifdef NL_SKY_VOLUME_CLOUDS
    if (!env.underwater) {
      float cj = fract(52.9829189*fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
      vec3 sdv = vec3(-viewDir.x, viewDir.y, -viewDir.z);
      vec4 cv = VolumetricClouds(viewDir, sdv, skycol, env, v_underwaterRainTimeDay.z, cj);
      skyColor = skyColor*(1.0 - cv.a) + cv.rgb;
    }
    #endif

    #ifdef NL_SKY_EQUI_CLOUDS
    if (!env.underwater) {
      vec3 sde = vec3(-viewDir.x, viewDir.y, -viewDir.z);
      vec4 ce = EquiClouds(viewDir, sde, skycol, env, v_underwaterRainTimeDay.z);
      skyColor = skyColor*(1.0 - ce.a) + ce.rgb;
    }
    #endif

    #ifdef NL_SKY_PLANE_CLOUDS
    if (!env.underwater) {
      vec3 sdp = vec3(-viewDir.x, viewDir.y, -viewDir.z);
      vec4 cp = PlaneClouds(viewDir, sdp, skycol, env, v_underwaterRainTimeDay.z);
      skyColor = skyColor*(1.0 - cp.a) + cp.rgb;
    }
    #endif

    #ifdef NL_UNDERWATER_STREAKS
    if (env.underwater) {
      vec3 sdu = vec3(-viewDir.x, viewDir.y, -viewDir.z);
      skyColor += NL_UNDERWATER_RAY_COL*nlUnderwaterRays(sdu, nlUnderwaterLightDir(env), v_underwaterRainTimeDay.z)*nlUnderwaterRayMask(env)*NL_UNDERWATER_STREAKS;
    }
    #endif

    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
