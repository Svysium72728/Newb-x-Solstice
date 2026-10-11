#ifndef INSTANCING
$input v_worldPos, v_underwaterRainTimeDay, v_fogColor
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>
  uniform vec4 TimeOfDay;
  uniform vec4 FogColor;
  uniform vec4 FogAndDistanceControl;
  uniform vec4 ViewPositionAndTime;

  SAMPLER2D_AUTOREG(s_BlockCloudNoise);

  #ifdef NL_SKY_BLOCK_CLOUDS
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

    vec4 BlockClouds(vec3 viewDir, vec3 sd, nl_skycolor skycol, nl_environment env, highp float time, float jitter) {
      if (viewDir.y < 0.05) {
        return vec4(0.0, 0.0, 0.0, 0.0);
      }
      const int steps = NL_BLOCK_CLOUD_STEPS;
      float invY = 1.0/viewDir.y;
      float occThr = clamp(1.0 - NL_BLOCK_CLOUD_COVER - 0.25*env.rainFactor, 0.05, 0.95);
      float w = mix(0.5, 0.12, NL_BLOCK_CLOUD_SQUARE);
      float lo = 0.5 - w;
      float hi = 0.5 + w;
      highp vec2 wind = vec2(time*NL_BLOCK_CLOUD_SPEED, time*NL_BLOCK_CLOUD_SPEED*0.4);

      float daylight = smoothstep(-0.2, 0.25, env.dayFactor);
      vec3 L = mix(env.moonDir, env.sunDir, step(0.0, env.dayFactor));
      float dawn = 1.0 - env.dayFactor*env.dayFactor;
      dawn *= dawn*dawn;
      vec3 sunCol = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
      float bright = mix(NL_BLOCK_CLOUD_NIGHT, 1.0, daylight)*(1.0 - 0.35*env.rainFactor);
      vec3 topCol = NL_BLOCK_CLOUD_LIGHT_COL*mix(vec3_splat(1.0), sunCol, 0.5)*bright;
      vec3 shadeCol = NL_BLOCK_CLOUD_SHADE_COL*mix(vec3_splat(1.0), skycol.horizon*1.4, 0.35)*bright;

      float T = 1.0;
      vec3 col = vec3(0.0, 0.0, 0.0);
      for (int i = 0; i < steps; i++) {
        float h = (float(i) + jitter)/float(steps);
        float t = (NL_BLOCK_CLOUD_BASE + NL_BLOCK_CLOUD_THICK*h)*invY;
        highp vec2 p = sd.xz*t*NL_BLOCK_CLOUD_SCALE + wind;
        vec4 cell = texture2D(s_BlockCloudNoise, (floor(p) + 0.5)*0.00390625);
vec3    fg = blockField(cell, p, occThr, lo, hi);

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
          float diff = clamp(dot(nrm, L)*0.5 + 0.5, 0.0, 1.0);
          diff = diff*diff*(3.0 - 2.0*diff);
          vec3 c = mix(shadeCol, topCol, diff);
          float a = d*NL_BLOCK_CLOUD_OPACITY;
          col += T*a*c;
          T *= 1.0 - a;
          if (T < 0.03) {
            break;
          }
        }
      }
      float f = smoothstep(0.05, 0.25, viewDir.y);
      return vec4(col*f, (1.0 - T)*f);
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

    vec4 VolumetricClouds(vec3 viewDir, vec3 sd, nl_skycolor skycol, nl_environment env, float time, float jitter) {
      if (viewDir.y < 0.04) {
        return vec4(0.0, 0.0, 0.0, 0.0);
      }
      const int steps = NL_VOL_CLOUD_STEPS;
      const float cloudBase = 1.0;
      const float thick = 0.6;
      float invY = 1.0/viewDir.y;
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
      vec2 wind = vec2(time*NL_VOL_CLOUD_SPEED, time*NL_VOL_CLOUD_SPEED*0.4);
      vec2 toLight = L.xz/max(L.y, 0.25);
      vec2 sdir = normalize(L.xz + vec2(0.0001, 0.0001))*NL_VOL_CLOUD_SHADE_DIST;

      float T = 1.0;
      vec3 col = vec3(0.0, 0.0, 0.0);
      for (int i = 0; i < steps; i++) {
        float h = (float(i) + jitter)/float(steps);
        float t = (cloudBase + thick*h)*invY;
        vec2 p = sd.xz*t*NL_VOL_CLOUD_SCALE + wind;
        float b = texture2D(s_VolCloudNoise, p).r;
        float prof = smoothstep(0.0, 0.15, h)*(1.0 - smoothstep(0.35, 1.0, h - (b - 0.5)*0.9));
        float tp = thr + 0.25*(1.0 - prof);
        if (b > tp - 0.12) {
          float dt = texture2D(s_VolCloudNoise, p*3.7 + 0.31).g;
          float n = b + (dt - 0.5)*(0.18 + 0.25*(1.0 - prof));
          float d = smoothstep(0.0, 1.0, clamp((n - tp)*3.0, 0.0, 1.0));
          if (d > 0.0) {
            vec2 pl = p + toLight*(1.0 - h)*thick*NL_VOL_CLOUD_SCALE;
            float ld = clamp((texture2D(s_VolCloudNoise, pl).r - thr)*3.0, 0.0, 1.0);
            float lit = exp(-ld*1.6);
            float bs = texture2D(s_VolCloudNoise, p + sdir).r;
            float shade = clamp(1.0 + (b - bs)*NL_VOL_CLOUD_SHADE, 0.4, 1.5);
            float silver = pow(cosv, 8.0)*lit;
            vec3 c = mix(shadowCol, topAmb, h) + lightCol*(lit*0.9 + 2.0*silver)*shade;
            float a = 1.0 - exp(-d*NL_VOL_CLOUD_DENSITY/float(steps));
            col += T*a*c;
            T *= 1.0 - a;
            if (T < 0.03) {
              break;
            }
          }
        }
      }
      float f = smoothstep(0.04, 0.22, viewDir.y);
      return vec4(col*f, (1.0 - T)*f);
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

            const int sampleCount = 15;
            const int sampleCountP = sampleCount + 10;

            float ditherM = dither + 10.0;

            for (int i = 0; i < sampleCount; i++) {
                float current = pow2((float(i) + ditherM) / float(sampleCountP));
                float currentM = 1.0 - current;

                highp vec2 planePos = (wpos * (0.9 + current) * 10.0 + cameraPosM) * 0.003;

                float noise = smoothAuroraNoise(planePos);
                noise = pow(clamp(1.0 - abs(noise - NL_AURORA_SMOOTH_CENTER), 0.0, 1.0), NL_AURORA_SMOOTH_SHARP);

                float anim1 = smoothAuroraNoise(planePos * 0.5);
                float anim2 = smoothAuroraNoise(planePos * 0.8);
                noise *= mix(anim1, anim2, 0.5);

                aurora += noise * currentM *
                    mix(vec3(0.65, 0.48, 1.35),
                        vec3(0.0, 5.55, 1.85),
                        pow2(pow2(currentM)));
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

            const int sampleCount = 15;
            const int sampleCountP = sampleCount + 10;

            float ditherM = dither + 10.0;

            for (int i = 0; i < sampleCount; i++) {
                float current = pow2((float(i) + ditherM) / float(sampleCountP));
                float currentM = 1.0 - current;

                vec2 planePos = wpos.xz * (0.9 + current) * 10.0 + cameraPosM;
                planePos *= 0.003;

                float noise = cubicFollowNoise(planePos);
                noise = pow2(pow2(pow2(1.0 - 1.0 * abs(noise - 0.4))));

                float anim1 = cubicFollowNoise(planePos * 0.5 + ViewPosTime.w * 0.00);
                float anim2 = cubicFollowNoise(planePos * 0.8 - ViewPosTime.w * 0.00);
                noise *= mix(anim1, anim2, 0.5);

                aurora += noise * currentM *
                    mix(vec3(0.65, 0.48, 1.35),
                        vec3(0.0, 5.55, 1.85),
                        pow2(pow2(currentM)));
            }

            aurora *= 0.5;
            return aurora * visibility / float(sampleCount);
        }
      #endif
    #endif
  #endif
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

    #ifdef NL_SUN_FOG_GLOW
      skyColor += nlSunLightColor(env)*nlSunFogGlow(-viewDir, env)*NL_SUN_FOG_GLOW*NL_SUN_FOG_SKY;
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

    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
