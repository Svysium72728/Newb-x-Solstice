#ifndef WATER_H
#define WATER_H

#include "utils.h"
#include "detection.h"
#include "sky.h"
#include "clouds.h"
#include "noise.h"

float calculateFresnel(float cosR, float r0) {
    return r0 + (1.0 - r0) * pow(1.0 - cosR, 5.0);
}

#ifdef NL_WATER_RESOLUTE_WAVES
float nlWaveHeight(highp vec2 uv, highp float t) {
    float h = 0.0;
    float totalAmp = 0.0;
    for (int i = 0; i < NL_WAVE_COUNT; i++) {
        float fi = float(i);
        float ang = fi*(6.28318/float(NL_WAVE_COUNT)) + sin(fi*1.91)*0.1;
        vec2 dir = vec2(cos(ang), sin(ang));
        float freq = NL_WAVE_FREQ + fi*NL_WAVE_FREQ_STEP;
        float amp = NL_WAVE_AMP/(1.0 + fi*NL_WAVE_AMP_STEP);
        float arg = dot(dir, uv*freq) + sqrt(9.8*freq)*t*NL_RESOLUTE_WAVE_SPEED + sin(fi*3.17)*6.2831;
        h += sin(arg)*amp;
        totalAmp += amp;
        uv += dir*cos(arg)*amp*NL_WAVE_STEEP;
    }
    return h/totalAmp*NL_WAVE_STRENGTH;
}
#endif

vec4 nlWater(
    inout vec4 color, inout vec3 wPos, nl_skycolor skycol, nl_environment env, vec4 COLOR, vec3 viewDir,
    vec3 cPos, vec3 tiledCpos, vec3 gPos, vec3 CAMERA_POS, vec3 light, vec3 torchColor, vec2 lit,
    float fractCposY, float camDist, highp float t
) {

    highp vec3 waterPos = CAMERA_POS + wPos;
    float waveTime = NL_WATER_WAVE_SPEED * t;

    vec2 bump;
    float bumpK;
    float waveH = 0.0;

    #ifdef NL_WATER_RESOLUTE_WAVES
        float h0 = nlWaveHeight(waterPos.xz, t);
        float hx = nlWaveHeight(waterPos.xz + vec2(0.05, 0.0), t);
        float hz = nlWaveHeight(waterPos.xz + vec2(0.0, 0.05), t);
        bump = -vec2(hx - h0, hz - h0)/0.05;
        waveH = h0/NL_WAVE_STRENGTH;
        bumpK = NL_WAVE_NORMAL;
    #else
      #ifdef NL_UNBOUND
        bump.x = movingNoise2D(gPos.xz + gPos.yy, waveTime, 0.6);
        bump.y = movingNoise2D(gPos.zx + vec2(1.5, 3.2), waveTime * 0.9, 0.6);

        vec2 detail;
        detail.x = movingNoise2D(gPos.xz * 2.5 + gPos.yy, waveTime * 1.5, 0.6);
        detail.y = movingNoise2D(gPos.zx * 2.5 + vec2(0.7, 4.1), waveTime * 1.3, 0.6);

        vec2 micro;
        micro.x = movingNoise2D(gPos.xz * 5.0 + gPos.yy, waveTime * 2.0, 0.6);
        micro.y = movingNoise2D(gPos.zx * 5.0 + vec2(2.1, 0.5), waveTime * 1.8, 0.6);

        bump = bump * abs(bump);
        detail = detail * abs(detail);

        bump = bump * 0.6 + detail * 0.3 + micro * 0.1;

        bump.x *= 1.2;
        bump.y *= 0.8;
      #else
        bump = vec2_splat(movingNoise2D(gPos.xz + gPos.yy, waveTime, 0.6));
      #endif
        bumpK = NL_WATER_BUMP;
    #endif

    vec3 nrm = vec3(0.0, 1.0, 0.0);

    if (fractCposY > 0.0) {
        nrm.xz = bump * bumpK;
        nrm.y  = 1.0;
    } else {
        bump *= 0.5 + 0.5 * sin(3.0 * waveTime + cPos.y * PI_HALF);
        nrm.xz = normalize(viewDir.xz) + bump.y * (vec2(1.0) - viewDir.xz * viewDir.xz) * bumpK;
        nrm.y  = bump.x * bumpK;
    }
    nrm = normalize(nrm);

    float cosR = dot(nrm, viewDir);

    #ifdef NL_UNBOUND
      vec3 reflDir = reflect(viewDir, nrm);
      vec3 distortedRefl = normalize(reflDir + vec3(nrm.x * 0.15, 0.0, nrm.z * 0.15));

      float roughness = smoothstep(5.0, 40.0, camDist) * 0.04;
      distortedRefl.x += sin(gPos.x * 50.0) * roughness;
      distortedRefl.z += cos(gPos.z * 50.0) * roughness;
      distortedRefl = normalize(distortedRefl);
    #else
      vec3 distortedRefl = viewDir - 2.0 * cosR * nrm;
    #endif

    vec3 waterRefl = nlRenderSky(skycol, env, distortedRefl, t, false);

    float tc = 0.5 + 0.5 * sin(16.0 * distortedRefl.x) * sin(16.0 * distortedRefl.z);
    waterRefl += torchColor * NL_TORCHLIGHT_INTENSITY * lit.x * (tc * tc);

    if (!env.end) {
        waterRefl *= 0.05 + lit.y * 1.14;
    }

    #ifdef NL_WATER_REFL_MASK
    float mask = 0.05 + 0.05 * sin(distortedRefl.x * 12.0) * sin(distortedRefl.z * 6.0);
    waterRefl *= smoothstep(mask - 0.2, mask + 0.13, distortedRefl.y * distortedRefl.y);
    #endif

    cosR = abs(cosR);
    // 0.02 is the natural baseline reflectivity coefficient of clean water
    float fresnel = calculateFresnel(cosR, 0.07);

    float opacity = 1.0 - cosR;

    color.rgb *= 0.22 * NL_WATER_TINT * (1.0 - 0.8 * fresnel);
    color.a = mix(COLOR.a * NL_WATER_TRANSPARENCY, 1.0, opacity * opacity);

    #ifdef NL_WATER_WAVE
    #ifdef NL_WATER_RESOLUTE_WAVES
        wPos.y -= (waveH*0.5 + 0.5)*NL_WAVE_DISP*(1.0 - smoothstep(8.0, 20.0, camDist));
    #else
        if (camDist < 14.0) {
            wPos.y -= 0.5 * (bump.x + 0.5) * NL_WATER_BUMP;
        }
    #endif
    #endif

    return vec4(waterRefl, fresnel);
}

#endif
