#ifndef CLOUDS_H
#define CLOUDS_H

#include "detection.h"
#include "noise.h"
#include "sky.h"

// simple clouds 2D noise
float cloudNoise2D(vec2 p, highp float t, float rain) {
  t *= NL_CLOUD1_SPEED;
  p += t;
  p.y += 3.0*sin(0.3*p.x + 0.1*t);

  vec2 p0 = floor(p);
  vec2 u = p-p0;
  u *= u*(3.0-2.0*u);
  vec2 v = 1.0-u;

  float n = mix(
    mix(rand(p0),rand(p0+vec2(1.0,0.0)), u.x),
    mix(rand(p0+vec2(0.0,1.0)),rand(p0+vec2(1.0,1.0)), u.x),
    u.y
  );
  n *= 0.5 + 0.5*sin(p.x*0.6 - 0.5*t)*sin(p.y*0.6 + 0.8*t);
  n = min(n*(1.0+rain), 1.0);
  return n*n;
}

// simple clouds
vec4 renderCloudsSimple(nl_skycolor skycol, vec3 pos, highp float t, float rain) {
  pos.xz *= NL_CLOUD1_SCALE;
  float d = cloudNoise2D(pos.xz, t, rain);
  vec4 col = vec4(skycol.horizonEdge + skycol.zenith, smoothstep(0.1,0.6,d));
  col.rgb += 1.5*dot(col.rgb, vec3(0.3,0.4,0.3))*smoothstep(0.6,0.2,d)*col.a;
  col.rgb *= 1.0 - 0.8*rain;
  return col;
}

// rounded clouds
// rounded clouds 3D density map
float cloudDf(vec3 pos, float rain, vec2 boxiness) {
  boxiness *= 0.999;
  vec2 p0 = floor(pos.xz);
  vec2 u = max((pos.xz-p0-boxiness.x)/(1.0-boxiness.x), 0.0);
  u *= u*(3.0 - 2.0*u);

  vec4 r = vec4(rand(p0), rand(p0+vec2(1.0,0.0)), rand(p0+vec2(1.0,1.0)), rand(p0+vec2(0.0,1.0)));
  r = smoothstep(0.1001+0.2*rain, 0.1+0.2*rain*rain, r); // rain transition

  float n = mix(mix(r.x,r.y,u.x), mix(r.w,r.z,u.x), u.y);

  // round y
  n *= 1.0 - 1.5*smoothstep(boxiness.y, 2.0 - boxiness.y, 2.0*abs(pos.y-0.5));


  n = max(1.25*(n-0.2), 0.0); // smoothstep(0.2, 1.0, n)
  n *= n*(3.0 - 2.0*n);
  return n;
}

vec4 renderCloudsRounded(
    vec3 vDir, vec3 vPos, float rain, float time, vec3 horizonCol, vec3 zenithCol,
    const int steps, const float thickness, const float thickness_rain, const float speed,
    const vec2 scale, const float density, const vec2 boxiness
) {
  float height = 7.0*mix(thickness, thickness_rain, rain);
  float stepsf = float(steps);

  // scaled ray offset
  vec3 deltaP;
  deltaP.y = 1.0;
  deltaP.xz = height*scale*vDir.xz/(0.02+0.98*abs(vDir.y));

  // local cloud pos
  vec3 pos;
  pos.y = 0.0;
  pos.xz = scale*(vPos.xz + vec2(1.0,0.5)*(time*speed));
  pos += deltaP;

  deltaP /= -stepsf;

  // alpha, gradient
  vec2 d = vec2(0.0,1.0);
  for (int i=1; i<=steps; i++) {
    float m = cloudDf(pos, rain, boxiness);
    d.x += m;
    d.y = mix(d.y, pos.y, m);
    pos += deltaP;
  }
  d.x *= smoothstep(0.03, 0.1, d.x);
  d.x /= (stepsf/density) + d.x;

  if (vPos.y < 0.0) { // view from top
    d.y = 1.0 - d.y;
  }

  vec4 col = vec4(zenithCol + horizonCol, d.x);
  col.rgb += dot(col.rgb, vec3(0.3,0.4,0.3))*d.y*d.y;
  col.rgb *= 1.0 - 0.8*rain;
  return col;
}

float cloudsNoiseVr(vec2 p, float t) {
  float n = fastVoronoi2(p + t, 1.8);
  n *= fastVoronoi2(3.0*p + t, 1.5);
  n *= fastVoronoi2(9.0*p + t, 0.4);
  n *= fastVoronoi2(27.0*p + t, 0.1);
  //n *= fastVoronoi2(82.0*pos + t, 0.02); // more quality
  return n*n;
}

vec4 renderClouds(vec2 p, float t, float rain, vec3 horizonCol, vec3 zenithCol, const vec2 scale, const float velocity, const float shadow) {
  p *= scale;
  t *= velocity;

  // layer 1
  float a = cloudsNoiseVr(p, t);
  float b = cloudsNoiseVr(p + NL_CLOUD3_SHADOW_OFFSET*scale, t);

  // layer 2
  p = 1.4 * p.yx + vec2(7.8, 9.2);
  t *= 0.5;
  float c = cloudsNoiseVr(p, t);
  float d = cloudsNoiseVr(p + NL_CLOUD3_SHADOW_OFFSET*scale, t);

  // higher = less clouds thickness
  // lower separation betwen x & y = sharper
  vec2 tr = vec2(0.6, 0.7) - 0.12*rain;
  a = smoothstep(tr.x, tr.y, a);
  c = smoothstep(tr.x, tr.y, c);

  // shadow
  b *= smoothstep(0.2, 0.8, b);
  d *= smoothstep(0.2, 0.8, d);

  vec4 col;
  col.a = a + c*(1.0-a);
  col.rgb = horizonCol + horizonCol.ggg;
  col.rgb = mix(col.rgb, 0.5*(zenithCol + zenithCol.ggg), shadow*mix(b, d, c));
  col.rgb *= 1.0-0.7*rain;

  return col;
}

// aurora is rendered on clouds layer
#ifdef NL_AURORA
vec4 renderAurora(vec3 p, float t, float rain, float dayFactor) {
  t *= NL_AURORA_VELOCITY;
  p.xz *= NL_AURORA_SCALE;
  p.xz += 0.05*sin(p.x*4.0 + 20.0*t);

  float d0 = sin(p.x*0.1 + t + sin(p.z*0.2));
  float d1 = sin(p.z*0.1 - t + sin(p.x*0.2));
  float d2 = sin(p.z*0.1 + 1.0*sin(d0 + d1*2.0) + d1*2.0 + d0*1.0);
  d0 *= d0; d1 *= d1; d2 *= d2;
  d2 = d0/(1.0 + d2/NL_AURORA_WIDTH);

  float mask = max(-dayFactor, 0.0);
  mask *= mask*(1.0-0.8*rain);
  return vec4(NL_AURORA*mix(NL_AURORA_COL1,NL_AURORA_COL2,d1),1.0)*d2*mask;
}
#endif

// cloud type 4 shape functions (shared with Clouds/fragment.sc)
highp float hash(highp vec2 p) {
    return fract(cos(p.x + p.y * 332.0) * 335.552);
}

vec2 smoothBlend(vec2 x) {
    vec2 v = x * x * (3.0 - 2.0 * x);
    return smoothstep(0.3, 0.95, v);
}

float gridSDF(vec2 uv) {
    vec2 f = fract(uv);
    vec2 i = floor(uv);

    float bl = step(0.85, hash(i + vec2(0.0, 0.0)));
    float br = step(0.85, hash(i + vec2(1.0, 0.0)));
    float tl = step(0.85, hash(i + vec2(0.0, 1.0)));
    float tr = step(0.85, hash(i + vec2(1.0, 1.0)));

    vec2 fade = smoothBlend(f);

    float mixBottom = mix(bl, br, fade.x);
    float mixTop    = mix(tl, tr, fade.x);
    float totalGrid = mix(mixBottom, mixTop, fade.y);

    return totalGrid;
}

vec3 pixelatedCloud(vec2 uv, float timeVal) {
    float a = 0.0;
    float b = 0.0;
    float isTime = -timeVal * 0.02;

    vec2 shadeDirection = vec2(0.05, 0.03);

    uv *= 6.5;
    uv.y *= 1.5;

    for (int i = 0; i < 4; i++) {
        uv /= 1.015;
        float r = gridSDF(uv + isTime);
        a = max(a, r);
    }

    vec2 shadeSampleUv = uv + isTime - shadeDirection;
    float shadeShape = gridSDF(shadeSampleUv);

    b = smoothstep(0.05, 0.9, shadeShape);
    a = smoothstep(0.1, 0.15, a);

    a -= b * a * 0.15;
    return vec3(clamp(a, 0.0, 1.0));
}

vec4 nlCloudAuroraReflection(nl_skycolor skycol, nl_environment env, vec3 viewDir, vec3 wPos, vec3 CAMERA_POS, highp float t) {
  vec2 cloudPos = wPos.xz;
  float divY = min(viewDir.y, -0.05); // prevent near-zero denominator blowup at grazing angles
  cloudPos += (187.0-(wPos.y+CAMERA_POS.y))*viewDir.xz/divY;
  float fade = clamp(2.0 - 0.0008*length(cloudPos), 0.0, 1.0);
  cloudPos += CAMERA_POS.xz;

  vec4 refl = vec4_splat(0.0);

  #ifdef NL_AURORA
    vec4 aurora = renderAurora(cloudPos.xyy, t, env.rainFactor, smoothstep(0.2, -0.2, env.dayFactor));
    aurora.a *= fade;
    refl = vec4(2.0*aurora.rgb*aurora.a, aurora.a);
  #endif

  #if NL_CLOUD_TYPE == 1
    vec4 clouds = renderCloudsSimple(skycol, cloudPos.xyy, t, env.rainFactor);
    clouds.a *= fade;
    refl = vec4(mix(refl.rgb, clouds.rgb, clouds.a), min(refl.a + clouds.a, 1.0));
  #elif NL_CLOUD_TYPE == 2
    vec3 reflViewDir = viewDir;
    reflViewDir.y = -reflViewDir.y;
    vec3 cloudWorldPos = vec3(cloudPos.x, 0.0, cloudPos.y);
    #define NL_CLOUD2_REFL_PARAMS(x) NL_CLOUD2##x##STEPS, NL_CLOUD2##x##THICKNESS, NL_CLOUD2##x##RAIN_THICKNESS, NL_CLOUD2##x##VELOCITY, NL_CLOUD2##x##SCALE, NL_CLOUD2##x##DENSITY, NL_CLOUD2##x##SHAPE
    vec4 clouds = renderCloudsRounded(reflViewDir, cloudWorldPos, env.rainFactor, t, skycol.horizon, skycol.zenith, NL_CLOUD2_REFL_PARAMS(_));
    clouds.a *= fade;
    refl = vec4(mix(refl.rgb, clouds.rgb, clouds.a), min(refl.a + clouds.a, 1.0));
  #elif NL_CLOUD_TYPE == 4
    vec3 cloudNoise = pixelatedCloud(cloudPos*0.005, t);
    float cloudAlpha = clamp(length(cloudNoise.r), 0.0, 1.0) * fade;
    vec3 cloudcol = skycol.horizonEdge / max(env.dayFactor, 0.01);
    cloudcol = clamp(cloudcol*1.5, 0.0, 1.5);
    refl = vec4(mix(refl.rgb, cloudcol, cloudAlpha), min(refl.a + cloudAlpha, 1.0));
  #endif

  return refl;
}

#endif
