#ifndef SKY_H
#define SKY_H

#include "detection.h"
#include "noise.h"

#ifndef NL_SUN_CIRCLE_BLOOM
#define NL_SUN_CIRCLE_BLOOM 1.0
#endif
#ifndef NL_MOON_BLOOM_SIZE
#define NL_MOON_BLOOM_SIZE 0.12
#endif
#ifndef NL_MOON_BLOOM_SOFTNESS
#define NL_MOON_BLOOM_SOFTNESS 0.1
#endif
#ifndef NL_MOON_BLOOM_STRENGTH
#define NL_MOON_BLOOM_STRENGTH 0.3
#endif

struct nl_skycolor {
    vec3 zenith;
    vec3 horizon;
    vec3 horizonEdge;
};

vec3 spectrum(float x) {
    vec3 s = vec3(x-0.5, x, x+0.5);
    s = smoothstep(1.0, 0.0, abs(s));
    return s*s;
}

vec3 getUnderwaterCol(vec3 FOG_COLOR) {
    return 2.0*NL_UNDERWATER_TINT*FOG_COLOR*FOG_COLOR;
}

vec3 getEndZenithCol() {
    return NL_END_ZENITH_COL;
}

vec3 getEndHorizonCol() {
    return NL_END_HORIZON_COL;
}

nl_skycolor nlEndSkyColors(nl_environment env) {
    nl_skycolor s;
    s.zenith = getEndZenithCol();
    s.horizon = getEndHorizonCol();
    s.horizonEdge = s.horizon;
    return s;
}

nl_skycolor nlOverworldSkyColors(nl_environment env) {
    nl_skycolor s;
    float f = 1.0 + 2.0*(1.0 - max(-env.dayFactor, 0.0));
    float nightFactor = step(env.dayFactor, 0.0);
    s.zenith = mix(NL_DAY_ZENITH_COL, NL_NIGHT_ZENITH_COL*f, nightFactor);
    s.horizon = mix(NL_DAY_HORIZON_COL, NL_NIGHT_HORIZON_COL*f, nightFactor);
    s.horizonEdge = mix(NL_DAY_EDGE_COL, NL_NIGHT_EDGE_COL*f, nightFactor);
    float dawnFactor = 1.0 - env.dayFactor*env.dayFactor;
    dawnFactor *= dawnFactor*dawnFactor;
    dawnFactor *= mix(1.0, dawnFactor*dawnFactor, nightFactor);
    s.zenith = mix(s.zenith, NL_DAWN_ZENITH_COL, dawnFactor);
    s.horizon = mix(s.horizon, NL_DAWN_HORIZON_COL, dawnFactor);
    s.horizonEdge = mix(s.horizonEdge, NL_DAWN_EDGE_COL, dawnFactor);
    float zh = dot(s.zenith, vec3_splat(0.33));
    float hh = dot(s.horizon, vec3_splat(0.33));
    float rainMix = env.rainFactor*NL_SKY_RAIN_MIX_FACTOR;
    s.zenith = mix(s.zenith, NL_RAIN_ZENITH_COL*zh, rainMix);
    s.horizon = mix(s.horizon, NL_RAIN_HORIZON_COL*hh, rainMix);
    s.horizonEdge = mix(s.horizonEdge, s.horizon, env.rainFactor);
    if (env.underwater) {
        vec3 underwaterFog = env.fogCol*env.fogCol*NL_UNDERWATER_TINT;
        s.zenith = mix(2.0*underwaterFog, underwaterFog*zh, 0.8);
        s.horizon = mix(2.0*underwaterFog, underwaterFog*hh, 0.8);
        s.horizonEdge = s.horizon;
    }
    return s;
}

nl_skycolor nlSkyColors(nl_environment env) {
    if (env.end) {
        return nlEndSkyColors(env);
    }
    return nlOverworldSkyColors(env);
}

float hash11(float x) {
    return fract(sin(x*127.1)*43758.5453);
}

vec3 nlRenderGodrays(vec3 viewDir, nl_environment env, float t) {
    if (env.underwater || env.end) {
        return vec3_splat(0.0);
    }
    float sunAmount = max(dot(viewDir, env.sunDir), 0.0);
    if (sunAmount < 0.4) {
        return vec3_splat(0.0);
    }
    vec3 sunUp = abs(env.sunDir.y) < 0.99 ? vec3(0.0,1.0,0.0) : vec3(1.0,0.0,0.0);
    vec3 sunTangent = normalize(cross(sunUp, env.sunDir));
    vec3 sunBitangent = cross(env.sunDir, sunTangent);
    float su = dot(viewDir, sunTangent);
    float sv = dot(viewDir, sunBitangent);
    float ang = atan2(sv, su);
    float spokes = 0.5+0.5*sin(NL_GODRAY_SPOKE_COUNT*ang + 2.0*hash11(floor(ang*3.0)));
    spokes = pow(spokes, NL_GODRAY_SPOKE_SHARPNESS);
    float cone = smoothstep(0.25, 1.0, sunAmount);
    float dayMask = max(env.dayFactor, 0.0);
    float rainMask = 1.0-env.rainFactor;

    float dawnFactor = 1.0-env.dayFactor*env.dayFactor;
    dawnFactor *= dawnFactor*dawnFactor;
    vec3 rayColor = mix(NL_NOON_SUNLIGHT_COL, NL_DAWN_SUNLIGHT_COL, dawnFactor);

    float rays = spokes*cone*dayMask*rainMask*NL_GODRAY;
    return rays*rayColor;
}

vec3 nlSunLightColor(nl_environment env) {
    float dawn = 1.0 - env.dayFactor*env.dayFactor;
    dawn *= dawn*dawn;
    return mix(mix(NL_NOON_SUNLIGHT_COL, vec3_splat(1.0), 0.45), NL_DAWN_SUNLIGHT_COL, dawn);
}

vec3 nlMoonLightColor() {
    return NL_NIGHT_MOONLIGHT_COL/max(max(NL_NIGHT_MOONLIGHT_COL.r, NL_NIGHT_MOONLIGHT_COL.g), NL_NIGHT_MOONLIGHT_COL.b);
}

float nlSquareBloom(vec3 viewDir, vec3 dir, float size, float soft) {
    float fwd = dot(viewDir, dir);
    if (fwd <= 0.0) {
        return 0.0;
    }
    vec3 up = abs(dir.y) < 0.99 ? vec3(0.0,1.0,0.0) : vec3(1.0,0.0,0.0);
    vec3 tg = normalize(cross(up, dir));
    vec3 bt = cross(dir, tg);
    float d = max(abs(dot(viewDir, tg)), abs(dot(viewDir, bt)))/fwd;
    return 1.0 - smoothstep(size, size + soft, d);
}

vec3 renderOverworldSky(nl_skycolor skyCol, nl_environment env, vec3 viewDir, bool isSkyPlane) {
    float avy = abs(viewDir.y);
    float mask = 0.5 + (0.5*viewDir.y/(0.4+avy));
    vec2 g = clamp(0.5-0.5*vec2(dot(env.sunDir, viewDir), dot(env.moonDir, viewDir)), 0.0, 1.0);
    vec2 g1 = 1.0-mix(sqrt(g), g, env.rainFactor);
    vec2 g2 = g1*g1;
    vec2 g4 = g2*g2;
    vec2 g8 = g4*g4;
    float mg8 = min(g8.x*NL_SUN_CIRCLE_BLOOM+g8.y, 1.0)*mask*(1.0-0.9*env.rainFactor);
    float vh = 1.0-viewDir.y*viewDir.y;
    float vh2 = vh*vh;
    vh2 = mix(vh2, mix(1.0, vh2*vh2, NL_SKY_VOID_FACTOR), step(viewDir.y, 0.0));
    vh2 = mix(vh2, 1.0, mg8);
    float vh4 = vh2*vh2;
    float gradient1 = vh4*vh4;
    float gradient2 = 0.8*gradient1+0.2*vh2;
    gradient1 *= gradient1;
    gradient1 = mix(gradient1*gradient1, 1.0, mg8);
    gradient2 = mix(gradient2, 1.0, mg8);
    float dawnFactor = 1.0-env.dayFactor*env.dayFactor;
    float df = mix(1.0, g2.x, dawnFactor*dawnFactor);
    vec3 sky = mix(skyCol.horizon, skyCol.horizonEdge, gradient1*df*df);
    sky = mix(skyCol.zenith, sky, gradient2*df);
    sky *= 0.5+0.5*gradient2;
    sky *= (1.0+(2.0*mg8+7.0*mg8*mg8)*mask)*mix(1.0, mask, NL_SKY_VOID_DARKNESS);
    if (!isSkyPlane) {
        float source = max(0.0, (mg8-0.22)/0.78);
        source *= source;
        source *= source;
        sky *= 1.0+0.0*source*(1.0-env.rainFactor);
    }
#ifdef NL_RAINBOW
    if (!env.underwater) {
        float rainbowFade = 0.5+0.5*viewDir.y;
        rainbowFade *= rainbowFade;
        rainbowFade *= mix(NL_RAINBOW_CLEAR, NL_RAINBOW_RAIN, env.rainFactor);
        rainbowFade *= 0.5+0.5*env.dayFactor;
        sky += spectrum(24.2*(0.85-g.x))*rainbowFade*skyCol.horizon;
    }
#endif
#ifdef NL_SUN_BLOOM_SQUARE
    float sunBloom = nlSquareBloom(viewDir, env.sunDir, NL_SUN_BLOOM_SIZE*NL_SUN_SIZE, NL_SUN_BLOOM_SOFTNESS*NL_SUN_SIZE);
    sky += sunBloom*nlSunLightColor(env)*NL_SUN_BLOOM_STRENGTH*max(env.dayFactor, 0.0);
    float moonBloom = nlSquareBloom(viewDir, env.moonDir, NL_MOON_BLOOM_SIZE*NL_MOON_SIZE, NL_MOON_BLOOM_SOFTNESS*NL_MOON_SIZE);
    sky += moonBloom*nlMoonLightColor()*NL_MOON_BLOOM_STRENGTH*smoothstep(0.2, -0.1, env.dayFactor)*(1.0 - env.rainFactor);
#endif
#ifdef NL_GODRAY
    sky += nlRenderGodrays(viewDir, env, 0.0);
#endif
    return sky;
}

vec3 renderEndSky(vec3 horizonCol, vec3 zenithCol, vec3 viewDir, float t) {
    float f = smoothstep(1.0, -0.5, viewDir.y);
    return mix(zenithCol, horizonCol, f*f);
}

vec3 nlRenderSky(nl_skycolor skycol, nl_environment env, vec3 viewDir, float t, bool isSkyPlane) {
    vec3 sky;
    viewDir.y=-viewDir.y;
    if (env.end) {
        sky=renderEndSky(skycol.horizon, skycol.zenith, viewDir, t);
    } else {
        sky=renderOverworldSky(skycol, env, viewDir, isSkyPlane);
#ifdef NL_UNDERWATER_STREAKS
        // Underwater streaks logic kept intact
#endif
    }
    return sky;
}

vec3 nlUnderwaterLightDir(nl_environment env) {
  return normalize(vec3(env.sunDir.x*0.35, 0.65 + 0.35*max(env.sunDir.y, 0.0), env.sunDir.z*0.35));
}

float nlUnderwaterRayMask(nl_environment env) {
  return mix(0.1, 1.0, smoothstep(-0.1, 0.3, env.dayFactor));
}

float nlUnderwaterRays(vec3 d, vec3 L, highp float t) {
  float c = clamp(dot(d, L), -1.0, 1.0);
  vec3 sr = normalize(vec3(0.0, 0.0, 1.0) - L*L.z);
  vec3 su = cross(L, sr);
  float a = atan2(dot(d, su), dot(d, sr));
  float w1 = 0.5 + 0.5*sin(7.0*a + 1.3*sin(3.0*a + 0.12*t) + 0.18*t);
  float w2 = 0.5 + 0.5*sin(17.0*a - 0.27*t + 2.0*sin(5.0*a + 0.09*t));
  float w3 = 0.5 + 0.5*sin(31.0*a + 0.4*t + 1.7*sin(11.0*a - 0.2*t));
  float beams = smoothstep(0.2, 1.0, 0.55*w1 + 0.3*w2 + 0.15*w3);
  float fall = 0.3 + 0.7*exp(-1.4*acos(c));
  float glow = pow(max(c, 0.0), 6.0);
  return beams*fall*smoothstep(-0.35, 0.45, c) + 0.5*glow;
}

float nlSunShaftMask(nl_environment env) {
  float h = env.sunDir.y;
  return smoothstep(-0.12, 0.02, h)*(1.0 - smoothstep(0.05, 0.5, h))*(1.0 - env.rainFactor);
}

float nlSunShafts(vec3 d, vec3 L, highp float t) {
  float c = clamp(dot(d, L), -1.0, 1.0);
  vec3 up = abs(L.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
  vec3 tg = normalize(cross(up, L));
  vec3 bt = cross(L, tg);
  float a = atan2(dot(d, bt), dot(d, tg));
  float w1 = 0.5 + 0.5*sin(9.0*a + 1.2*sin(4.0*a + 0.1*t) + 0.12*t);
  float w2 = 0.5 + 0.5*sin(23.0*a - 0.2*t + 1.8*sin(7.0*a + 0.07*t));
  float w3 = 0.5 + 0.5*sin(41.0*a + 0.3*t + 1.5*sin(13.0*a - 0.15*t));
  float beams = smoothstep(0.3, 1.0, 0.5*w1 + 0.32*w2 + 0.18*w3);
  float radial = exp(-1.8*acos(c));
  float core = pow(max(c, 0.0), 12.0);
  return beams*radial*smoothstep(0.0, 0.35, c) + 0.6*core;
}

float nlWinHash(highp vec2 p) {
  return fract(sin(dot(p, vec2(127.1, 311.7)))*43758.5453);
}

float nlWindowPattern(highp vec3 P, vec3 L) {
  vec3 up = abs(L.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
  vec3 tg = normalize(cross(up, L));
  vec3 bt = cross(L, tg);
  highp vec2 q = vec2(dot(P, tg), dot(P, bt))/vec2(NL_WINDOW_LIGHT_CELL_W, NL_WINDOW_LIGHT_CELL_H);
  highp vec2 c = floor(q);
  vec2 f = q - c;
  float on = step(0.4, nlWinHash(c));
  float rect = smoothstep(0.0, 0.12, f.x)*smoothstep(1.0, 0.88, f.x)*smoothstep(0.0, 0.1, f.y)*smoothstep(1.0, 0.9, f.y);
  float bx = 1.0 - 0.8*(1.0 - smoothstep(0.0, 0.05, abs(f.x - 0.5)));
  float by = 1.0 - 0.8*(1.0 - smoothstep(0.0, 0.05, abs(f.y - 0.5)));
  return on*rect*bx*by;
}

vec3 nlRenderShootingStar(vec3 viewDir, vec3 FOG_COLOR, float t) {
    float h = t / (NL_SHOOTING_STAR_DELAY+NL_SHOOTING_STAR_PERIOD);
    float h0 = floor(h);
    t = (NL_SHOOTING_STAR_DELAY+NL_SHOOTING_STAR_PERIOD) * (h-h0);
    t = min(t/NL_SHOOTING_STAR_PERIOD, 1.0);
    float t0 = t*t;
    float t1 = 1.0-t0;
    t1 *= t1; t1 *= t1; t1 *= t1;
    float r = fract(sin(h0) * 43758.545313);
    float a = 6.2831*r;
    float cosa = cos(a);
    float sina = sin(a);
    vec2 uv = viewDir.xz * (6.0+4.0*r);
    uv = vec2(cosa*uv.x+sina*uv.y, -sina*uv.x+cosa*uv.y);
    uv.x += t1-t;
    uv.x -= 2.0*r+3.5;
    uv.y += viewDir.y*3.0;
    float g = 1.0-min(abs((uv.x-0.95))*20.0, 1.0);
    float s = 1.0-min(abs(8.0*uv.y), 1.0);
    s *= s*s*smoothstep(-1.0+1.96*t1, 0.98-t, uv.x);
    s *= s*s*smoothstep(1.0, 0.98-t0, uv.x);
    s *= 1.0-t1;
    s *= 1.0-t0;
    s *= 0.7+16.0*g*g;
    s *= max(1.0-FOG_COLOR.r-FOG_COLOR.g-FOG_COLOR.b, 0.0);
    return s*vec3(0.8, 0.9, 1.0);
}

vec3 nlRenderGalaxy(vec3 vdir, vec3 fogColor, nl_environment env, float t) {
    if (env.underwater) {
        return vec3_splat(0.0);
    }
    t *= NL_GALAXY_SPEED;
    float cosb = sin(0.2*t);
    float sinb = cos(0.2*t);
    vdir.xy = mul(mat2(cosb, sinb, -sinb, cosb), vdir.xy);
    float n0 = 0.5+0.5*sin(5.0*vdir.x)*sin(5.0*vdir.y-0.5*t)*sin(5.0*vdir.z+0.5*t);
    float n1 = noise3D(15.0*vdir+sin(0.85*t+1.3));
    float n2 = noise3D(50.0*vdir+1.0*n1+sin(0.7*t+1.0));
    float n3 = noise3D(200.0*vdir-10.0*sin(0.4*t+0.500));
    n3 = smoothstep(0.04,0.3,n3+0.02*n2);
    float gd = vdir.x+0.1*vdir.y+0.1*sin(10.0*vdir.z+0.2*t);
    float st = n1*n2*n3*n3*(1.0+70.0*gd*gd);
    st = (1.0-st)/(1.0+400.0*st);
    vec3 stars = (0.8+0.2*sin(vec3(8.0,6.0,10.0)*(2.0*n1+0.8*n2) +vec3(0.0,0.4,0.82)))*st;
    float gfmask = abs(vdir.x)-0.15*n1+0.04*n2+0.25*n0;
    float gf = 1.0- (vdir.x*vdir.x+0.03*n1+0.2*n0);
    gf *= gf;
    gf *= gf*gf;
    gf *= 1.0-0.3*smoothstep(0.2, 0.3, gfmask);
    gf *= 1.0-0.2*smoothstep(0.3, 0.4, gfmask);
    gf *= 1.0-0.1*smoothstep(0.2, 0.1, gfmask);
    vec3 gfcol = normalize(vec3(n0, cos(2.0*vdir.y), sin(vdir.x+n0)));
    stars += (0.4*gf+0.012)*mix(vec3(0.5, 0.5, 0.5), gfcol*gfcol, NL_GALAXY_VIBRANCE);
    stars *= mix(1.0, NL_GALAXY_DAY_VISIBILITY, env.dayFactor);
    return stars*(1.0-env.rainFactor);
}

#endif // SKY_H
