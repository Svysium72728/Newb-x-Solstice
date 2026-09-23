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

    #if defined(NL_CLOUD_AURORA_REFLECTION)
    if (distortedRefl.y > 0.0) {
        vec4 cloudRefl = nlCloudAuroraReflection(skycol, env, distortedRefl, wPos, CAMERA_POS, t);
        waterRefl = mix(waterRefl, cloudRefl.rgb, cloudRefl.a);
    }
    #endif

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

    if (fractCposY > 0.0 && !env.end) {
        vec3 lightReflDir = reflect(-normalize(light), nrm);
        
        float thickSpec = max(dot(normalize(vec3(lightReflDir.x * 0.12, lightReflDir.y, lightReflDir.z * 0.12)), -normalize(viewDir)), 0.0);
        float sunSpecular = pow(thickSpec, 24.0);
        
        vec3 sunTrailColor = mix(vec3(1.0, 0.35, 0.05), vec3(1.0, 0.95, 0.8), smoothstep(0.05, 0.4, light.y));
        
        float glitter = 0.5 + 0.5 * sin(gPos.x * 25.0 + t * 6.0) * cos(gPos.z * 25.0 - t * 5.0);
        float finalTrail = sunSpecular * mix(0.5, 1.3, glitter) * 6.5;
        
        finalTrail *= smoothstep(-0.1, 0.3, viewDir.y);
        
        color.rgb += sunTrailColor * finalTrail;
    }

    #ifdef NL_WATER_WAVE
    if (camDist < 14.0) {
        wPos.y -= 0.5 * (bump.x + 0.5) * NL_WATER_BUMP;
    }
    #endif

    return vec4(waterRefl, fresnel);
}

#endif
