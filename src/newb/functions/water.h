#ifndef WATER_H
#define WATER_H

#include "utils.h"
#include "detection.h"
#include "sky.h"
#include "clouds.h"
#include "noise.h"

// fresnel - Schlick's approximation
float calculateFresnel(float cosR, float r0) {
  float a = 1.0-cosR;
  float a2 = a*a;
  return r0 + (1.0-r0)*a2*a2*a;
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

  vec2 bump;
  float bumpK;
  float waveH = 0.0;

  #ifdef NL_WATER_RESOLUTE_WAVES
    highp vec3 waterPos = CAMERA_POS + wPos;
    float h0 = nlWaveHeight(waterPos.xz, t);
    float hx = nlWaveHeight(waterPos.xz + vec2(0.05, 0.0), t);
    float hz = nlWaveHeight(waterPos.xz + vec2(0.0, 0.05), t);
    bump = -vec2(hx - h0, hz - h0)/0.05;
    waveH = h0/NL_WAVE_STRENGTH;
    bumpK = NL_WAVE_NORMAL;
  #else
    bump = vec2_splat(movingNoise2D(gPos.xz + gPos.yy, NL_WATER_WAVE_SPEED*t, 0.6));
    bumpK = NL_WATER_BUMP;
  #endif

  vec3 nrm;
  if (fractCposY > 0.0) { // top plane
    nrm.xz = bump*bumpK;
    #ifdef NL_WATER_RESOLUTE_WAVES
      nrm.y = 1.0;
    #else
      nrm.y = -1.0;
    #endif
  } else { // reflection for side plane
    bump *= 0.5 + 0.5*sin(3.0*t*NL_WATER_WAVE_SPEED + cPos.y*PI_HALF);
    nrm.xz = normalize(viewDir.xz) + bump.y*(1.0-viewDir.xz*viewDir.xz)*bumpK;
    nrm.y = bump.x*bumpK;
  }
  nrm = normalize(nrm);

  float cosR = dot(nrm, viewDir);
  viewDir = viewDir - 2.0*cosR*nrm ; // reflect(viewDir, nrm)

  vec3 waterRefl = nlRenderSky(skycol, env, viewDir, t, false);

  #if defined(NL_CLOUD_AURORA_REFLECTION)
    if (viewDir.y < 0.0) {
      vec4 cloudRefl = nlCloudAuroraReflection(skycol, env, viewDir, wPos, CAMERA_POS, t);
      waterRefl = mix(waterRefl, cloudRefl.rgb, cloudRefl.a);
    }
  #endif

  // torch light reflection
  float tc = 0.5+0.5*sin(16.0*viewDir.x)*sin(16.0*viewDir.z);
  waterRefl += torchColor*NL_TORCHLIGHT_INTENSITY*lit.x*tc*tc;

  // mask sky reflection under shade
  if (!env.end) {
    waterRefl *= 0.05 + lit.y*1.14;
  }

  #ifdef NL_WATER_REFL_MASK
    float mask = 0.05+0.05*sin(viewDir.x*12.0)*sin(viewDir.z*6.0);
    waterRefl *= smoothstep(mask-0.2,mask+0.13,viewDir.y*viewDir.y);
  #endif

  cosR = abs(cosR);
  float fresnel = calculateFresnel(cosR, 0.07);
  float opacity = 1.0-cosR;

  color.rgb = vec3_splat(0.22)*NL_WATER_TINT*(1.0-0.8*fresnel);
  color.a = mix(COLOR.a*NL_WATER_TRANSPARENCY, 1.0, opacity*opacity);

  #ifdef NL_WATER_WAVE
    #ifdef NL_WATER_RESOLUTE_WAVES
      wPos.y -= (waveH*0.5 + 0.5)*NL_WAVE_DISP*(1.0 - smoothstep(8.0, 20.0, camDist));
    #else
      if (camDist < 14.0) {
        wPos.y -= 0.5*(bump.x+0.5)*NL_WATER_BUMP;
      }
    #endif
  #endif

  return vec4(waterRefl, fresnel);
}

#endif
