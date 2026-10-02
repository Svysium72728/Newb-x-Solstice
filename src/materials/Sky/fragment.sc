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

        for (int i = 0; i < steps; i++) {
            float height = cloudBase + stepSize * (float(i) + jitter);
            float t = min(height * invY, 55.0);
            vec2 uv = (viewDir.xz * t * 0.9 + time * 0.05) * 0.025;
            float base = texture2D(s_NoiseTexture, uv).r;

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

  #ifdef NL_SKY_AURORA
    float pow2(float x){return x*x;}
    float pow1_5(float x){return pow(x,1.5);}
    float clamp01(float x){return clamp(x,0.0,1.0);}
    float sqrt1(float x){return sqrt(max(x,0.0));}

    #ifdef NL_SKY_AURORA_MEDIUM
      SAMPLER2D_AUTOREG(s_NoiseTex);

      vec3 GetAuroraMedium(vec3 viewDir, float t, float dither, float dayFactor) {
          float nightFactor = smoothstep(0.1, -0.15, dayFactor);
          if (nightFactor < 0.01) return vec3(0.0);

          float VdotU = clamp(viewDir.y, 0.0, 1.0);
          float visibility = sqrt1(clamp01(VdotU * 4.5 - 0.225));
          visibility *= 4.0 - VdotU * 0.9;
          if (visibility <= 1.0) return vec3(0.0);

          vec3 aurora = vec3(0.0);
          vec3 wpos = viewDir;
          wpos.xz /= max(wpos.y, 0.1);
          vec2 cameraPosM = vec2(0.0);
          cameraPosM.x += t * NL_SKY_AURORA_SPEED;

          const int sampleCount = 13;
          const int sampleCountP = sampleCount + int(0.667 * float(sampleCount));

          float ditherM = dither + 10.0;
          float auroraAnimate = t * 0.0;

          for (int i = 0; i < sampleCount; i++) {
              float current = pow2((float(i) + ditherM) / float(sampleCountP));
              vec2 planePos = wpos.xz * (0.8 + current) * 10.0 + cameraPosM;
              planePos *= 0.0007;
              float noise = texture2D(s_NoiseTex, planePos).r;
              noise = clamp((1.0 - 0.8*abs(noise - 0.5)) * NL_SKY_AURORA_SHARPNESS - (NL_SKY_AURORA_SHARPNESS - 1.0), 0.0, 1.0);
              noise = pow2(pow2(noise));
              noise *= texture2D(s_NoiseTex, planePos * 8.0 + auroraAnimate).g;
              noise *= texture2D(s_NoiseTex, planePos * 1.0 - auroraAnimate).r;
              float currentM = 1.0 - current;
              float layerWeight = pow(currentM, NL_SKY_AURORA_FALLOFF);
              aurora += noise * layerWeight * mix(vec3(0.65, 0.48, 1.05), vec3(0.0, 4.5, 3.0), pow2(pow2(currentM)));
          }

          aurora *= 3.8;
          return aurora * visibility * nightFactor / float(sampleCount);
      }
    #endif

    #ifdef NL_SKY_AURORA_HEAVY
      SAMPLER2D_AUTOREG(s_noisevoxels);

      float cubicFollowNoise(vec2 p){
        vec2 quantized = floor(p * 128.0) / 128.0 + 2.0 / 128.0;
        return texture2D(s_noisevoxels, quantized).r;
      }

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

    #ifdef NL_SKY_AURORA
    if (!env.underwater && !env.end && !env.nether) {
      #ifdef NL_SKY_AURORA_MEDIUM
        float dither = texture2D(s_NoiseTex, mod(gl_FragCoord.xy, 256.0)/256.0).r;
        skyColor += GetAuroraMedium(viewDir, v_underwaterRainTimeDay.z, dither, env.dayFactor);
      #endif
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
