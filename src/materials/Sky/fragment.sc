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

  #ifdef NL_SKY_ROUND_CLOUDS
    #ifndef NL_SKY_ROUND_CLOUDS_NIGHT
      #define NL_SKY_ROUND_CLOUDS_NIGHT 0.15
    #endif
    #ifndef NL_SKY_ROUND_CLOUDS_CELLS
      #define NL_SKY_ROUND_CLOUDS_CELLS 28.0
    #endif
    #ifndef NL_SKY_ROUND_CLOUDS_BOXY
      #define NL_SKY_ROUND_CLOUDS_BOXY 0.4
    #endif

    SAMPLER2D_AUTOREG(s_NoiseTexture);

    vec4 RoundedClouds(vec3 viewDir, float time, float jitter) {
        const int steps = 6;
        const float cloudBase = 1.1;
        const float cloudTop = 1.3;
        float stepSize = (cloudTop - cloudBase) / float(steps);
        vec3 topColor = vec3(1.0, 0.925, 0.875);
        vec3 bottomColor = vec3(0.0, 0.15, 0.25);

        vec3 cloudAccum = vec3(0.35, 0.35, 0.35);
        float alphaAccum = 0.0;

        if (viewDir.y < 0.05) {
            return vec4(cloudAccum, 0.0);
        }
        float invY = 1.0 / viewDir.y;

        vec2 uvMid = (viewDir.xz * min((cloudBase + 0.5*(cloudTop - cloudBase)) * invY, 55.0) * 0.9 + time * 0.05) * 0.025;
        float cellN = texture2D(s_NoiseTexture, (floor(uvMid*NL_SKY_ROUND_CLOUDS_CELLS) + 0.5)/NL_SKY_ROUND_CLOUDS_CELLS + 0.37).r;
        float boxOff = (cellN - 0.5)*NL_SKY_ROUND_CLOUDS_BOXY;

        for (int i = 0; i < steps; i++) {
            float height = cloudBase + stepSize * (float(i) + jitter);
            float t = min(height * invY, 55.0);
            vec2 uv = (viewDir.xz * t * 0.9 + time * 0.05) * 0.025;
            float base = clamp(texture2D(s_NoiseTexture, uv).r + boxOff, 0.0, 1.0);

            float heightNorm = (height - cloudBase) / (cloudTop - cloudBase);
            float heightFactor = smoothstep(0.2, 0.9, heightNorm) * (1.0 - smoothstep(0.6, 1.0, heightNorm));
            heightFactor *= smoothstep(0.2, 0.6, base);

            float density = clamp(2.4 * base, 0.0, 1.0);
            density = density * density * heightFactor;

            float alpha = smoothstep(0.0, 0.01, density) * (1.0 - alphaAccum);
            cloudAccum += mix(bottomColor, topColor, smoothstep(-0.2, 1.0, heightNorm)) * alpha;
            alphaAccum += alpha;

            if (alphaAccum > 0.98) {
                break;
            }
        }
        return vec4(cloudAccum, alphaAccum);
    }
  #endif

  #ifdef NL_SKY_VOLUME_CLOUDS
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

    SAMPLER2D_AUTOREG(s_VolCloudNoise);

    vec4 VolumetricClouds(vec3 viewDir, vec3 sd, nl_skycolor skycol, nl_environment env, float time, float jitter) {
      if (viewDir.y < 0.04) {
        return vec4(0.0, 0.0, 0.0, 0.0);
      }
      const int steps = 6;
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
      vec3 ambient = mix(skycol.horizon, skycol.zenith, 0.6)*(1.0 - 0.4*env.rainFactor);
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
        float prof = smoothstep(0.0, 0.2, h)*(1.0 - h*h);
        float tp = thr + 0.3*(1.0 - prof);
        if (b > tp - 0.12) {
          float dt = texture2D(s_VolCloudNoise, p*3.7 + 0.31).g;
          float n = b + (dt - 0.5)*0.3;
          float d = clamp((n - tp)*3.5, 0.0, 1.0);
          if (d > 0.0) {
            vec2 pl = p + toLight*(1.0 - h)*thick*NL_VOL_CLOUD_SCALE;
            float ld = clamp((texture2D(s_VolCloudNoise, pl).r - tp)*3.5, 0.0, 1.0);
            float lit = exp(-ld*2.2);
            float bs = texture2D(s_VolCloudNoise, p + sdir).r;
            float shade = clamp(1.0 + (b - bs)*NL_VOL_CLOUD_SHADE, 0.35, 1.6);
            float silver = pow(cosv, 8.0)*lit;
            vec3 c = ambient*(0.5 + 0.5*h) + lightCol*(lit + 2.0*silver)*shade;
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
        SAMPLER2D_AUTOREG(s_noisesmooth);

        float cubicFollowNoise(highp vec2 p){
          vec2 quantized = floor(p * 1000.0) / 1000.0 + 0.000003;
          return texture2D(s_noisesmooth, quantized).r;
        }
      #else
        SAMPLER2D_AUTOREG(s_noisevoxels);

        float cubicFollowNoise(highp vec2 p){
          vec2 quantized = floor(p * 128.0) / 128.0 + 2.0 / 128.0;
          return texture2D(s_noisevoxels, quantized).r;
        }
      #endif

      vec3 GetAuroraHeavy(vec3 viewDir, vec4 ViewPosTime, float dither) {
          float VdotU = clamp(viewDir.y, 0.0, 1.0);
          float visibility = sqrt1(clamp01(VdotU * 4.5 - 0.25));
          visibility *= 8.0 - VdotU * 0.9;
          if (visibility <= 1.0) return vec3(0.0);

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

          aurora *= 0.8;
          return aurora * visibility / float(sampleCount);
      }
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

    #ifdef NL_SKY_ROUND_CLOUDS
    if (!env.underwater) {
      float cj = fract(52.9829189*fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
      vec4 cl = RoundedClouds(viewDir, v_underwaterRainTimeDay.z, cj);
      cl.rgb *= mix(NL_SKY_ROUND_CLOUDS_NIGHT, 1.0, smoothstep(-0.2, 0.25, env.dayFactor));
      skyColor = mix(skyColor, cl.rgb, cl.a);
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
        skyColor += GetAuroraHeavy(viewDir, ViewPositionAndTime, dither2) * mask;
      #endif
    }
    #endif

    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
