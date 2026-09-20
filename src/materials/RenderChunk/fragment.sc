#ifdef NL_EFFECTS_HEAVY
  $input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra, v_causticData, v_viewDir
#else
  $input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra, v_causticData
#endif

#include <bgfx_shader.sh>
#include <newb/main.sh>

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);
SAMPLER2D_AUTOREG(s_CausticTex);

#ifdef NL_EFFECTS_HEAVY
vec2 nlHeightmapOffset(vec2 uv, vec3 viewDir, sampler2D tex, float strength) {
  float height = texture2D(tex, uv).r;
  vec2 offset = viewDir.xy * (height - 0.5) * strength;
  return uv + offset;
}
#endif

void main() {
  #if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    return;
  #endif

  #ifdef NL_EFFECTS_HEAVY
    vec2 parallaxUV = nlHeightmapOffset(v_texcoord0, v_viewDir, s_MatTexture, 0.02);
    vec4 diffuse = texture2D(s_MatTexture, parallaxUV);
  #else
    vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
  #endif

  vec4 color = v_color0;

  #ifdef ALPHA_TEST
    if (diffuse.a < 0.6) {
      discard;
    }
  #endif

  #if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
    diffuse.rgb *= mix(vec3(1.0,1.0,1.0), texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0, v_color1.z);
  #endif

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
      vec2 causticUV = fract(v_causticData.xy);
      float caustic = texture2D(s_CausticTex, causticUV).r;
      diffuse.rgb += caustic * NL_CAUSTIC_INTENSITY * 0.3;
    }
  #endif

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}
