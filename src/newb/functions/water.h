#ifndef WATER_H
#define WATER_H

#include "utils.h"
#include "detection.h"
#include "sky.h"
#include "clouds.h"
#include "noise.h"

// Swapped to physically accurate Schlick's approximation for water
float calculateFresnel(float cosR, float r0) {
    return r0 + (1.0 - r0) * pow(1.0 - cosR, 5.0);
}

vec2 nlGlitterPath(vec3 rd, vec3 lightDir) {
  vec2 sh = normalize(lightDir.xz + vec2(0.0001, 0.0001));
  vec2 vh = normalize(rd.xz + vec2(0.0001, 0.0001));
  float lane = pow(max(dot(vh, sh), 0.0), NL_GLITTER_LANE);
  float along = 1.0 - smoothstep(0.0, NL_GLITTER_LENGTH, rd.y);
  return vec2(lane, along);
}

float nlGlitterCell(highp vec2 gxz, vec3 rd, vec3 lightDir, highp float t) {
  highp vec2 p = gxz*NL_GLITTER_DENSITY;
  highp vec2 cell = floor(p);
  #ifdef NL_GLITTER_USE_TEX
    float shape = texture2D(s_Glitter, fract(p)).r;
  #else
    vec2 f = fract(p) - 0.5;
    float shape = 1.0 - step(0.3, max(abs(f.x), abs(f.y)));
  #endif
  float h = fract(sin(dot(cell, vec2(127.1, 311.7)))*43758.5453);
  highp float ph = t*NL_GLITTER_SPEED + h*17.0;
  highp float id = floor(ph);
  float life = sin(fract(ph)*3.14159);
  highp vec2 seed = cell + id*7.13;
  vec2 r = vec2(fract(sin(dot(seed, vec2(269.5, 183.3)))*43758.5453), fract(sin(dot(seed, vec2(419.2, 371.9)))*43758.5453));
  vec2 pa = nlGlitterPath(rd, lightDir);
  float mask = pa.x*pa.y;
  return step(r.x, mask)*(0.5 + 0.5*r.y)*life*shape;
}

vec3 nlSunGlitter(nl_environment env, vec3 viewDir, highp vec2 gxz, highp float t, float camDist) {
  float vis = smoothstep(0.0, 0.12, env.sunDir.y)*(1.0 - env.rainFactor);
  if (vis <= 0.0) {
    return vec3_splat(0.0);
  }
  float dawn = 1.0 - env.dayFactor*env.dayFactor;
  dawn *= dawn*dawn;
  vec3 col = mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
  float fade = 1.0 - smoothstep(NL_GLITTER_FADE_START, NL_GLITTER_FADE_END, camDist);

  vec3 rd = vec3(-viewDir.x, viewDir.y, -viewDir.z);
  vec2 pa = nlGlitterPath(rd, env.sunDir);

  float sparkle = nlGlitterCell(gxz, rd, env.sunDir, t)*fade*NL_GLITTER_STRENGTH;
  float glow = sqrt(pa.x)*(1.0 - smoothstep(0.0, 0.2, rd.y))*NL_SUN_BOUNCE_STRENGTH;
  return col*((sparkle + glow)*vis);
}

vec3 nlMoonGlitter(nl_environment env, vec3 viewDir, highp vec2 gxz, highp float t, float camDist) {
  float vis = smoothstep(0.0, 0.12, env.moonDir.y)*smoothstep(0.2, -0.1, env.dayFactor)*(1.0 - env.rainFactor);
  if (vis <= 0.0) {
    return vec3_splat(0.0);
  }
  vec3 mc = NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
  vec3 col = mix(mc, vec3_splat(1.0), NL_MOON_GLITTER_WHITE);
  float fade = 1.0 - smoothstep(NL_GLITTER_FADE_START, NL_GLITTER_FADE_END, camDist);
  vec3 rd = vec3(-viewDir.x, viewDir.y, -viewDir.z);
  return col*(nlGlitterCell(gxz, rd, env.moonDir, t)*vis*fade*NL_MOON_GLITTER_STRENGTH);
}

vec4 nlWater(
    inout vec4 color, inout vec3 wPos, nl_skycolor skycol, nl_environment env, vec4 COLOR, vec3 viewDir,
    vec3 cPos, vec3 tiledCpos, vec3 gPos, vec3 CAMERA_POS, vec3 light, vec3 torchColor, vec2 lit,
    float fractCposY, float camDist, highp float t
) {

    float waveTime = NL_WATER_WAVE_SPEED * t;
    
    vec2 bump;
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

    vec3 nrm = vec3(0.0, 1.0, 0.0);
    
    if (fractCposY > 0.0) {
        nrm.xz = bump * NL_WATER_BUMP;
        nrm.y  = 1.0; 
    } else {
        bump *= 0.5 + 0.5 * sin(3.0 * waveTime + cPos.y * PI_HALF);
        nrm.xz = normalize(viewDir.xz) + bump.y * (vec2(1.0) - viewDir.xz * viewDir.xz) * NL_WATER_BUMP;
        nrm.y  = bump.x * NL_WATER_BUMP;
    }
    nrm = normalize(nrm);

    float cosR = dot(nrm, viewDir);
    vec3 reflDir = reflect(viewDir, nrm);

    vec3 distortedRefl = normalize(reflDir + vec3(nrm.x * 0.15, 0.0, nrm.z * 0.15));
    
    float roughness = smoothstep(5.0, 40.0, camDist) * 0.04;
    distortedRefl.x += sin(gPos.x * 50.0) * roughness;
    distortedRefl.z += cos(gPos.z * 50.0) * roughness;
    distortedRefl = normalize(distortedRefl);

    vec3 waterRefl = nlRenderSky(skycol, env, distortedRefl, t, false);

    // Torchlight
    float tc = 0.5 + 0.5 * sin(16.0 * distortedRefl.x) * sin(16.0 * distortedRefl.z);
    waterRefl += torchColor * NL_TORCHLIGHT_INTENSITY * lit.x * (tc * tc);

    if (!env.end) {
        waterRefl *= 0.1 + lit.y * 0.9;
    }

    #ifdef NL_WATER_REFL_MASK
    float mask = 0.05 + 0.05 * sin(distortedRefl.x * 12.0) * sin(distortedRefl.z * 6.0);
    waterRefl *= smoothstep(mask - 0.2, mask + 0.13, distortedRefl.y * distortedRefl.y);
    #endif

    cosR = abs(cosR);
    // 0.02 is the natural baseline reflectivity coefficient of clean water
    float fresnel = calculateFresnel(cosR, 0.02); 
    
    float viewAngleFactor = 1.0 - viewDir.y;
    float opacity = mix(NL_WATER_TRANSPARENCY, 1.0, pow(viewAngleFactor, 4.0));

    color.rgb *= 0.22 * NL_WATER_TINT * (1.0 - 0.75 * fresnel);
    color.a = mix(COLOR.a * opacity, 1.0, fresnel);

    #ifdef NL_WATER_WAVE
    if (camDist < 14.0) {
        wPos.y -= 0.5 * (bump.x + 0.5) * NL_WATER_BUMP;
    }
    #endif

    return vec4(waterRefl, fresnel);
}

#endif
