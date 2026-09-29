$input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra, v_causticData

#include <bgfx_shader.sh>
#include <newb/main.sh>

uniform vec4 ViewPositionAndTime;
uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 TimeOfDay;
uniform vec4 CameraPosition;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);
SAMPLER2D_AUTOREG(s_CausticTex);

#define DROP_NUMBER 5

float stickyRaindrop(vec2 uv, vec2 center, float baseSize, float stretch) {
    vec2 p = (uv - center);
    p.y /= baseSize * stretch;
    p.x /= baseSize;

    float d = length(p * vec2(1.0, 0.6));
    float y = p.y;

    float shape = smoothstep(0.5, 0.45, d) *
                  smoothstep(0.0, 1.0, y + 0.5) *
                  smoothstep(1.0, -0.3, y);

    return shape;
}

vec3 screenDrops(vec3 baseColor, float time, vec2 uv, bool falling) {
    vec3 kol = baseColor;
    const int drops = DROP_NUMBER;

    for (int i = 0; i < drops; i++) {
        float fi = float(i);

        vec2 dropPos = vec2(
            fract(sin(fi * 12.9898) * 43758.5453),
            fract(sin(fi * 78.233) * 12345.678 + (falling ? time * 0.1 : 0.0))
        );

        float stretch = 1.0;
        if (falling) {
            dropPos.y = mod(dropPos.y - time * 0.18, 1.0);
            stretch = mix(1.0, 2.5, smoothstep(0.1, 0.9, dropPos.y));
        }

        float baseSize = 0.05 + 0.02 * fract(sin(fi * 5.21) * 1000.0);
        float dropMask = stickyRaindrop(uv, dropPos, baseSize, stretch);

        vec3 dropColor = vec3(0.5, 0.7, 0.85);
        kol = mix(kol, dropColor, dropMask * 0.5);
    }
    return kol;
}

 vec3 acesTonemap(vec3 x) {
  return clamp((x*(2.51*x + 0.03))/(x*(2.43*x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
  #if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    return;
  #endif

  nl_environment env = nlDetectEnvironment(TimeOfDay.x, FogColor.rgb, FogAndDistanceControl.xyz);

  vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
  vec4 color = v_color0;

  #ifdef ALPHA_TEST
    if (diffuse.a < 0.6) {
      discard;
    }
  #endif

  #if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
    diffuse.rgb *= mix(vec3(1.0,1.0,1.0), texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0, v_color1.z);
  #endif

  float sideshadow = smoothstep(0.64, 0.62, v_color1.g) * max(v_lightmapUV.x, v_lightmapUV.y);
  diffuse.rgb *= 1.0-0.25*sideshadow;

    vec3 glow = nlGlow(s_MatTexture, v_texcoord0, v_extra.a);

  // === FAKE BLOOM EFFECT FOR TORCHES ===
  #ifdef NL_TORCH_BLOOM
    float glowAmount = max(glow.r, max(glow.g, glow.b));
    float bloom = max(0.0, glowAmount - NL_TORCH_BLOOM_THRESHOLD) * 2.0;
    
    // Add warm bloom glow to the existing glow
    glow += NL_TORCH_BLOOM_COLOR * bloom * NL_TORCH_BLOOM_STRENGTH;
  #endif

  diffuse.rgb *= diffuse.rgb;

  #if defined(TRANSPARENT) && !(defined(SEASONS) || defined(RENDER_AS_BILLBOARDS))
    if (v_extra.b > 0.9) {
      diffuse.rgb = vec3_splat(1.0 - NL_WATER_TEX_OPACITY*(1.0 - diffuse.b*1.8));
      diffuse.a = color.a;
    }
  #else
    diffuse.a = 1.0;
  #endif

  diffuse.rgb *= color.rgb;
  diffuse.rgb += glow;

  if (v_extra.b > 0.9) {
    diffuse.rgb += v_refl.rgb*v_refl.a;

  #ifdef NL_WATER_GLITTER
      if (abs(dFdy(v_extra.g)) < 0.0002 && !env.underwater) {
        highp vec3 wp = vec3(v_causticData.x - CameraPosition.x, v_extra.g, v_causticData.y - CameraPosition.z);
        vec3 toCam = ViewPositionAndTime.xyz - wp;
        float camDist = length(toCam);
        vec3 vdir = toCam/camDist;
        float skyExp = smoothstep(0.7, 0.95, v_lightmapUV.y);
        vec3 glitter = (nlSunGlitter(env, vdir, v_causticData.xy, ViewPositionAndTime.w, camDist) + nlMoonGlitter(env, vdir, v_causticData.xy, ViewPositionAndTime.w, camDist))*skyExp;
        diffuse.rgb += glitter;
        diffuse.a = min(diffuse.a + dot(glitter, vec3_splat(0.33)), 1.0);
      }
    #endif

  } else if (v_refl.a > 0.0) {
    // reflective effect - only on xz plane
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb *= 1.0 - 0.6*mask;
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  #if defined(NL_CAUSTIC_INTENSITY) && !(defined(RENDER_AS_BILLBOARDS) || defined(SEASONS))
    if (v_causticData.z > 0.5 && v_extra.b < 0.9) {
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
        vec2 causticUV = fract(v_causticData.xy);
        float caustic = texture2D(s_CausticTex, causticUV).r;
        diffuse.rgb += caustic * NL_CAUSTIC_INTENSITY * 0.3;
    }
}
  #endif

  float wcPos = v_extra.g + CameraPosition.y;
  float sideFace = step(0.0002, abs(dFdy(v_extra.g)));
  float nearSurface = smoothstep(62.5, 62.85, wcPos) * step(wcPos, 62.89);
  float notWater = step(v_extra.b, 0.9);
  float shadeDown = abs(dFdy(v_lightmapUV.y));
  float shadeSide = abs(dFdx(v_lightmapUV.y));
  float underwaterShade = step(shadeSide + 0.002, shadeDown);
  float foam = nearSurface * sideFace * notWater * underwaterShade; 
  diffuse.rgb = mix(diffuse.rgb, vec3_splat(1.0), foam * 0.5);

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  float rain = mix(smoothstep(0.66, 0.3, FogAndDistanceControl.x), 0.0, step(FogAndDistanceControl.x, 0.0));
  float dropStrength = env.underwater ? 1.0 : rain;

  if (dropStrength > 0.0) {
    vec2 screenUV = gl_FragCoord.xy / vec2(1280.0, 720.0); // match your actual target res
    diffuse.rgb = screenDrops(diffuse.rgb, ViewPositionAndTime.w, screenUV, !env.underwater);
  }

  if (v_extra.b > 0.9) {
  diffuse.rgb = acesTonemap(diffuse.rgb);
} else {
  diffuse.rgb = colorCorrection(diffuse.rgb);
}

gl_FragColor = diffuse;

}
