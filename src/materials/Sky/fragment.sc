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

    float fbmTex(vec3 p) {
        vec2 uv = p.xy * 0.025;
        uv = fract(uv);
        return texture2D(s_NoiseTexture, uv).r;
    }

    vec4 RoundedClouds(vec3 viewDir, float time, float jitter) {
        float cloudBase = 1.1;
        float cloudTop = 1.3;
        const int steps = 32;
        vec3 bottomColor = vec3(0.5,0.55,0.6);
        vec3 sideColor = vec3(0.9,0.95,1.0);
        vec3 colors = mix(sideColor,bottomColor,0.5);
        float stepSize = (cloudTop - cloudBase) / float(steps);

        vec3 rayOrigin = vec3(0.0,0.0,0.0);
        vec3 cloudAccum = vec3(0.35,0.35,0.35);
        float alphaAccum = 0.0;
        float viewLift = smoothstep(0.0, 0.0, viewDir.y);

        float prevDensity = 0.0;

        for (int i = 0; i < steps; i++) {
            float height = cloudBase + stepSize * (float(i) + jitter);
            float t = height / max(viewDir.y, 0.001);
            t = min(t, 55.0);
            vec3 pos = rayOrigin + viewDir * t;

            vec3 noisePos = vec3(pos.xz * 0.9 + time * 0.05, height * 0.8);
            float base = fbmTex(noisePos);

            float heightNorm = (height - cloudBase) / (cloudTop - cloudBase);
            float heightFactor = smoothstep(0.2, 0.9, heightNorm) * (1.0 - smoothstep(0.6, 1.0, heightNorm));
            heightFactor *= smoothstep(0.2, 0.6, base);

            float density = base;
            density = 2.4 * clamp(density - 0.0, 0.0, 1.0);
            density = pow(density, 1.8) * heightFactor;

            float alpha = 1.0 - smoothstep(0.01, 0.0, density);
            alpha *= (1.0 - alphaAccum) * viewLift;

            vec3 cloudColor = colors;

            float edge = abs(density - prevDensity);
            vec3 TopColor = vec3(1.0,0.925,0.875);
            vec3 BottomColor = vec3(0.0,0.15,0.25);
            float heightNormal = smoothstep(-0.2,1.0,heightNorm);
            vec3 coloredScattering = mix(BottomColor,TopColor,heightNormal);
            cloudAccum += vec3(1.0,1.0,1.0) * (coloredScattering)*alpha;
            alphaAccum += alpha;

            if (alphaAccum > 0.98 && viewDir.y < 0.9) break;
        }

        return vec4(cloudAccum, alphaAccum);
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
            if (visibility <= 1.0) return vec3(0.0);

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
      float jitter = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
      vec4 clouds = RoundedClouds(viewDir, v_underwaterRainTimeDay.z, jitter);
      clouds.rgb *= mix(NL_SKY_ROUND_CLOUDS_NIGHT, 1.0, smoothstep(-0.2, 0.25, env.dayFactor));
      float opacity = smoothstep(0.1, 0.3, viewDir.y);
      float cloudMask = clouds.a * 0.5 * opacity;
      skyColor.rgb = mix(skyColor.rgb, clouds.rgb, cloudMask);
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
