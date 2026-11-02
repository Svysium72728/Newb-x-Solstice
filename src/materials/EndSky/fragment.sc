#ifndef INSTANCING
$input v_texcoord0, v_posTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
#include <newb/main.sh>

SAMPLER2D_AUTOREG(s_SkyTexture);

#ifdef NL_END_CLOUDS
SAMPLER2D_AUTOREG(s_EndCloudTex);
#endif

#ifdef NL_BLACKHOLE
vec4 renderBlackhole(vec3 vdir, float t) {
    t *= NL_BH_SPEED;

    float cr = cos(NL_BH_AZIMUTH);
    float sr = sin(NL_BH_AZIMUTH);

    vec3 baseDir = normalize(vec3(0.0, NL_BH_HEIGHT, 1.0));
    vec3 center = vec3(
        baseDir.x * cr - baseDir.z * sr,
        baseDir.y,
        baseDir.x * sr + baseDir.z * cr
    );

    vec3 vd = vdir - center;
    float nl = sin(15.0 * vd.x + t)
             * sin(15.0 * vd.y - t)
             * sin(15.0 * vd.z + t);

    float a = atan2(vd.z, vd.x);
    float d = NL_BH_DIST * length(vd + 0.003 * nl);
    float d0 = (0.6 - d) / 0.6;
    float dm0 = 1.0 - max(d0, 0.0);

    float gl = 1.0 - clamp(-0.3 * d0, 0.0, 1.0);
    float gla = pow(1.0 - min(abs(d0), 1.0), 8.0);
    float gl8 = pow(gl, 8.0);

    float hole = 0.9 * pow(dm0, 32.0)
               + 0.1 * pow(dm0, 3.0);

    float bh = (gla + 0.8 * gl8 + 0.2 * gl8 * gl8) * hole;

    float df = sin(
        3.0 * a - 4.0 * d
        + 24.0 * pow(1.4 - d, 4.0) + t
    );

    df *= 0.9 + 0.1 * sin(
        8.0 * a + d + 4.0 * t - 4.0 * df
    );

    bh *= 1.0 + pow(df, 4.0) * hole * max(1.0 - bh, 0.0);

    vec3 col = bh * 4.0
             * mix(NL_BH_COL_LOW, NL_BH_COL_HIGH, min(bh, 1.0));

    return vec4(col, hole);
}
#endif

  vec3 renderEndAmbientGlow(vec3 viewDir, float t) {
      float e = viewDir.y;
      float up = clamp(e, 0.0, 1.0);
      float a = atan2(viewDir.x, viewDir.z)*NL_ENDGLOW_STREAKS;
      float drift = t*NL_ENDGLOW_SPEED;

      float s = 0.5*sin(a*5.0 + drift + 1.3)
              + 0.3*sin(a*11.0 - 1.4*drift + 4.1)
              + 0.2*sin(a*23.0 + 0.7*drift + 0.7 + 1.5*sin(a*3.0));
      float streak = smoothstep(-0.7, 0.8, s);

      float profile = pow(1.0 - up, NL_ENDGLOW_HEIGHT);
      float valley = mix(NL_ENDGLOW_GAP, NL_ENDGLOW_GAP_TOP, smoothstep(0.0, 0.7, up));
      float intensity = profile*mix(valley, 1.0, streak);

      vec3 col = mix(NL_ENDGLOW_COLOR, NL_ENDGLOW_COLOR2, smoothstep(0.0, 0.6, up));

      return col*intensity*NL_ENDGLOW_STRENGTH;
  }

vec3 renderEndStarLayer(
    vec3 viewDir,
    float scale,
    float density,
    float brightness,
    float seed
) {
    vec3 p = viewDir * scale;
    vec3 cell = floor(p);
    vec3 f = fract(p);

    float h = fract(
        sin(dot(cell + seed, vec3(12.9898, 78.233, 45.164)))
        * 43758.5453
    );

    float star = step(1.0 - density, h);
    float d = length(f - 0.5);

    star *= 1.0 - smoothstep(0.1, 0.5, d);

    float twinkle = 0.7 + 0.3 * sin(
        h * 123.4 + dot(cell, vec3_splat(1.0))
    );

    vec3 tint = mix(
        vec3(0.7, 0.8, 1.0),
        vec3(1.0, 0.75, 0.95),
        fract(h * 17.0)
    );

    return tint * star * brightness * twinkle;
}

#ifdef NL_END_CLOUDS
vec4 renderEndClouds(vec3 vdir, float t) {
    float k = -vdir.y;

    if (k <= 0.0) {
        return vec4(0.0, 0.0, 0.0, 0.0);
    }

    float u = atan2(vdir.x, vdir.z) * 0.15915494
            + 0.5 + t * NL_END_CLOUD_SPEED;

    float v = clamp(
        asin(min(k, 1.0)) * 0.63661977,
        0.002,
        0.998
    );

    float L = texture2DLod(s_EndCloudTex, vec2(u, v), 0.0).r;

    float a = smoothstep(
        NL_END_CLOUD_LO,
        NL_END_CLOUD_HI,
        L
    ) * NL_END_CLOUD_OPACITY;

    vec3 col = mix(
        NL_END_CLOUD_COL_DARK,
        NL_END_CLOUD_COL_LIGHT,
        L
    );

    float haze = smoothstep(0.0, NL_END_CLOUD_HAZE, k);
    col = mix(NL_END_CLOUD_FOG_COL, col, haze);

    return vec4(col, a);
}
#endif

#ifdef NL_END_NEBULA
vec3 renderEndNebula(vec3 vdir, float t) {
    float ts = t * NL_END_NEBULA_SPEED;

    float cr = cos(NL_BH_AZIMUTH);
    float sr = sin(NL_BH_AZIMUTH);
    vec3 bb = normalize(vec3(0.0, NL_BH_HEIGHT, 1.0));
    vec3 bhc = vec3(bb.x * cr - bb.z * sr, bb.y, bb.x * sr + bb.z * cr);

    float pn = noise3D(vdir * NL_END_NEBULA_PATCH + vec3(7.3 + 0.03 * ts, 2.1, 4.7));
    float mask = smoothstep(NL_END_NEBULA_AMOUNT, NL_END_NEBULA_AMOUNT + 0.3, pn);
    mask *= smoothstep(0.55, 1.1, length(vdir - bhc));
    if (mask < 0.01) {
        return vec3(0.0, 0.0, 0.0);
    }

    vec3 p = vdir * NL_END_NEBULA_SCALE;
    float w = noise3D(p * 0.7 + vec3(0.0, 0.0, 0.1 * ts));
    float n1 = noise3D(p + 1.8 * w + vec3(0.06 * ts, 0.0, 0.0));
    float n2 = noise3D(p * 2.3 - 1.4 * w + vec3(0.0, 0.08 * ts, 0.0));
    float n3 = noise3D(p * 5.0 + 1.2 * n1);
    float f = 0.5 * n1 + 0.3 * n2 + 0.2 * n3;

    float dens = smoothstep(0.38, 0.85, f) * mask;
    float fil = pow(1.0 - abs(2.0 * n3 - 1.0), 5.0);

    vec3 col = mix(NL_END_NEBULA_COL_A, NL_END_NEBULA_COL_B, smoothstep(0.35, 0.9, n2));
    col = mix(col, NL_END_NEBULA_COL_C, fil * 0.5 * dens);

    vec3 neb = col * (dens * dens * 1.4 + 0.5 * fil * dens) * NL_END_NEBULA_STRENGTH;
    vec3 sp = renderEndStarLayer(vdir, 650.0, 0.03, 2.2, 41.0) * (0.15 + 1.5 * dens);
    return neb + sp * mask;
}
#endif

#endif

void main() {
    #ifndef INSTANCING

    vec4 diffuse = texture2D(s_SkyTexture, v_texcoord0);

    vec3 viewDir = normalize(v_posTime.xyz);
    float t = v_posTime.w;

    vec3 color = renderEndSky(
        getEndHorizonCol(),
        getEndZenithCol(),
        viewDir,
        t
    );

    color += 2.8 * diffuse.rgb;
    color += renderEndAmbientGlow(viewDir, t);

    color += renderEndStarLayer(viewDir, 300.0, 0.002, 1.2, 0.0);
    color += renderEndStarLayer(viewDir, 550.0, 0.004, 0.8, 7.0);
    color += renderEndStarLayer(viewDir, 900.0, 0.006, 0.5, 19.0);

    #ifdef NL_END_NEBULA
    color += renderEndNebula(viewDir, t);
    #endif

    #ifdef NL_SHOOTING_STAR
    color += NL_SHOOTING_STAR * nlRenderShootingStar(viewDir, vec3(0.5, 0.4, 0.8), t);
    #endif

    #ifdef NL_END_CLOUDS
    vec4 cl = renderEndClouds(viewDir, t);
    color = mix(color, cl.rgb, cl.a);
    #endif

    #ifdef NL_BLACKHOLE
    vec4 bh = renderBlackhole(viewDir, t);
    color *= bh.a;
    color += bh.rgb;
    #endif

    color = colorCorrection(color);

    gl_FragColor = vec4(color, 1.0);

    #else

    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);

    #endif
}
