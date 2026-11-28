#ifndef NL_CONFIG_H
#define NL_CONFIG_H

#define NL_SKY_AURORA_COMP
#define NL_BLOCK_CLOUD_PARALLAX 1.0

#define NL_AURORA_COMP_STYLE 1
#define NL_AURORA_COMP_LAYERS 20
#define NL_AURORA_COMP_SPEED 1.5
#define NL_AURORA_COMP_BRIGHTNESS 0.6

#define NL_AURORA_COMP_COL1 vec3(7.0, 2.2, 12.0)
#define NL_AURORA_COMP_COL2 vec3(6.0, 16.0, 18.0)

#define NL_PUDDLES
#define NL_PUDDLE_SCALE 0.0117
#define NL_PUDDLE_THR_LIGHT 0.71
#define NL_PUDDLE_THR_HEAVY 0.42
#define NL_PUDDLE_EDGE 0.05
#define NL_PUDDLE_DAMP 0.08
#define NL_PUDDLE_DARKEN 0.25

#define NL_PLANE_CLOUD_SCALE 0.67
#define NL_PLANE_CLOUD_STRETCH 0.5
#define NL_PLANE_CLOUD_SPEED 0.07
#define NL_PLANE_CLOUD_OPACITY 1.2
#define NL_PLANE_CLOUD_LAYERS 10
#define NL_PLANE_CLOUD_NIGHT 0.30
#define NL_PLANE_CLOUD_REFL_LAYERS 5
#define NL_PLANE_CLOUD_HORIZON_MIN 0.0003
#define NL_PLANE_CLOUD_HORIZON_FADE_START 0.005
#define NL_PLANE_CLOUD_HORIZON_FADE_END 0.115
#define NL_PLANE_CLOUD_HORIZON_DETAIL 0.33
#define NL_PLANE_CLOUD_FAR_FADE 0.008

#define NL_CUSTOM_STARS
#define NL_STAR_DENSITY 0.22
#define NL_STAR_SIZE 1.1
#define NL_STAR_GLOW 1.0
#define NL_STAR_BRIGHTNESS 1.0
#define NL_WINDOW_LIGHT 0.35
#define NL_WINDOW_LIGHT_PATCH 0.8
#define NL_WINDOW_LIGHT_DENSITY 0.05
#define NL_WINDOW_LIGHT_STEPS 4
#define NL_WINDOW_LIGHT_CELL_W 3.0
#define NL_WINDOW_LIGHT_CELL_H 3.0
#define NL_CAVE_DARKEN 0.6
//#define NL_TERRAIN_SUN_GLOW 0.8
#define NL_TERRAIN_SUN_GLOW_INDOOR 0.0
//#define NL_DAY_WHITE 0.22
#define NL_DAY_WHITE_START 0.3
#define NL_DAY_WHITE_FULL 0.85
#define NL_DAY_WHITE_THR 0.05
#define NL_DAY_WHITE_PURE 0.4
#define NL_DAY_WHITE_GAIN 1.6
#define NL_TERRAIN_SUN_GLOW_COL vec3(1.0, 0.82, 0.5)

//#define NL_METAL_REFLECTION 2.1
//#define NL_LAVA_BLUE
#define NL_TAG_WAVE
//#define NL_GLINT_OUTLINE
#define NL_GLINT_OUTLINE_STRENGTH 1.2
#define NL_GLINT_OUTLINE_COL vec3(0.55, 0.25, 1.0)
#define NL_GLINT_OUTLINE_MINTEX 256.0
#define NL_WATER_FOAM_NIGHT 0.15
#define NL_WATER_TEX_BRIGHT 0.5

#define NL_BLACKHOLE_REFL
#define NL_BH_REFL_TERRAIN 1.0
#define NL_BH_REFL_MIRROR 0.0

#define NL_SUN_GLINT 0.0
#define NL_SUN_GLINT_WIDTH 1.0
#define NL_SUN_GLINT_COL vec3(1.0, 0.62, 0.2)
#define NL_SUN_GLINT_BOOST 1.6
#define NL_SUN_GLINT_BASE 0.08
#define NL_SUN_GLINT_TEX 1.5
#define NL_WATER_TEX_CONTRAST 4.0
#define NL_WATER_TEX_CENTER 0.80
#define NL_WATER_TEX_ALPHA 0.4

#define NL_SUN_SHAFTS 0.8
#define NL_SUN_SHAFTS_COL mix(NL_DAWN_SUNLIGHT_COL, vec3(1.0, 1.0, 1.0), 0.15)
#define NL_SUN_SHAFTS_DENSITY 0.012

 //#define NL_SKY_EQUI_CLOUDS
  #define NL_EQUI_CLOUD_COVER 0.50
  #define NL_EQUI_CLOUD_SOFT 0.14
  #define NL_EQUI_CLOUD_OPACITY 1.0
  #define NL_EQUI_CLOUD_SHADE 1.8
  #define NL_EQUI_CLOUD_BUMP 1.0
  #define NL_EQUI_CLOUD_RIM 0.5
  #define NL_EQUI_CLOUD_EDGE 0.25
  #define NL_EQUI_CLOUD_SPEED 0.0004
  #define NL_EQUI_CLOUD_NIGHT 0.30


#define NL_SUN_FOG_GLOW 3.0
#define NL_SUN_FOG_POWER 10.0
#define NL_SUN_FOG_NOON 0.15
#define NL_SUN_FOG_START 0.15
#define NL_SUN_FOG_DENSITY 0.8
#define NL_SUN_FOG_SKY 0.6

#define NL_SKY_BLOCK_CLOUDS_REFL
#define NL_SKY_VOLUME_CLOUDS_REFL
#define NL_VOL_CLOUD_REFL_STEPS 4
#define NL_BLOCK_CLOUD_REFL_STEPS 2
#define NL_CLOUD_REFL_UNIT 600.0
#define NL_CLOUD_REFL_STRENGTH 0.85

#define NL_TORCH_BLOOM_TINT 0.8

#define NL_END_CLOUDS
#define NL_END_CLOUD_SPEED 0.0
#define NL_END_CLOUD_LO 0.15
#define NL_END_CLOUD_HI 0.55
#define NL_END_CLOUD_OPACITY 1.0
#define NL_END_CLOUD_HAZE 0.15
#define NL_END_CLOUD_COL_LIGHT vec3(0.70,0.55,0.94)
#define NL_END_CLOUD_COL_DARK  vec3(0.22,0.13,0.40)
#define NL_END_CLOUD_FOG_COL   vec3(0.30,0.18,0.48)
#define NL_END_NEBULA
#define NL_END_NEBULA_SCALE 2.2
#define NL_END_NEBULA_SPEED 0.02
#define NL_END_NEBULA_PATCH 1.8
#define NL_END_NEBULA_AMOUNT 0.5
#define NL_END_NEBULA_COL_A vec3(0.12, 0.075, 0.16)
#define NL_END_NEBULA_COL_B vec3(0.17, 0.105, 0.22)
#define NL_END_NEBULA_COL_C vec3(0.38, 0.26, 0.56)
#define NL_END_NEBULA_STRENGTH 1.0

#define NL_OCEAN_DEPTH
#define NL_OCEAN_SEA_LEVEL 62.89
#define NL_OCEAN_DEPTH_MAX 40.0
#define NL_OCEAN_DEEP_COL vec3(0.06,0.12,0.22)
#define NL_OCEAN_TORCH_KEEP 0.8
#define NL_OCEAN_VIG_START 0.25
#define NL_OCEAN_VIG_END 0.8
#define NL_OCEAN_VIG_POWER 1.8
#define NL_OCEAN_VIG_STRENGTH 1.0

#define NL_SKY_BLOCK_CLOUDS
#define NL_BLOCK_CLOUD_STEPS 10
#define NL_BLOCK_CLOUD_SCALE 5.5
#define NL_BLOCK_CLOUD_COVER 0.45
#define NL_BLOCK_CLOUD_BASE 1.0
#define NL_BLOCK_CLOUD_THICK 0.07
#define NL_BLOCK_CLOUD_BEVEL 0.65
#define NL_BLOCK_CLOUD_SQUARE 0.75
#define NL_BLOCK_CLOUD_SOFT 0.01
#define NL_BLOCK_CLOUD_SHARP 5.0
#define NL_BLOCK_CLOUD_OPACITY 0.92
#define NL_BLOCK_CLOUD_SPEED 0.14
#define NL_BLOCK_CLOUD_NIGHT 0.22
#define NL_BLOCK_CLOUD_SUN_TINT 0.12
#define NL_BLOCK_CLOUD_LIGHT_COL vec3(1.0, 1.1, 1.25)
#define NL_BLOCK_CLOUD_SHADE_COL vec3(0.32, 0.40, 0.54)

#define NL_WATER_RESOLUTE_WAVES
#define NL_WAVE_COUNT 8
#define NL_RESOLUTE_WAVE_SPEED 0.8
#define NL_WAVE_STEEP 2.0
#define NL_WAVE_FREQ 0.4
#define NL_WAVE_FREQ_STEP 0.2
#define NL_WAVE_AMP 0.4
#define NL_WAVE_AMP_STEP 0.2
#define NL_WAVE_STRENGTH 0.55
#define NL_WAVE_NORMAL 4.0
#define NL_WAVE_DISP 0.12

#define NL_WATER_FOAM
#define NL_WATER_FOAM_STRENGTH 1.0
#define NL_WATER_FOAM_COLOR vec3(3.0, 3.0, 3.0)
#define NL_LAVA_FOAM
#define NL_LAVA_FOAM_STRENGTH 0.85
#define NL_LAVA_FOAM_SPEED 0.25
#define NL_LAVA_FOAM_SCALE 8.0
#define NL_LAVA_FOAM_COLOR_LOW vec3(1.5, 0.22, 0.02)
#define NL_LAVA_FOAM_COLOR_HIGH vec3(2.4, 0.7, 0.1)
#define NL_SUN_CIRCLE_BLOOM 1.0
#define NL_CLOUD4_REFL_HEIGHT 187.0
#define NL_CLOUD4_REFL_SCALE 0.02
#define NL_SUN_REFL_SCALE  0.799
#define NL_SUN_REFL_STRENGTH 0.0
#define NL_SUN_REFL_MIRROR 0.0
#define NL_SHADOW_SATURATION 0.3
#define NL_MOON_BLOOM_SIZE 0.12
#define NL_MOON_BLOOM_SOFTNESS 0.8
#define NL_MOON_BLOOM_STRENGTH 2.0
#define NL_VOL_CLOUD_SHADE 14.0
#define NL_VOL_CLOUD_SHADE_DIST 0.012
#define NL_VOL_CLOUD_SCALE 0.15
#define NL_VOL_CLOUD_COVER 0.70
#define NL_VOL_CLOUD_DENSITY 6.0
#define NL_BLACKHOLE

/*
  NEWB SHADER MAIN CONFIG
  This part contains base configuration options for the shader.

  TOGGLES
  - If [toggle] is mentioned, then
    options can be commented to disable (by adding '//')
  - eg: #define PLANTS_WAVE    -> this is ON
        //#define PLANTS_WAVE  -> this is OFF

  COLOR VALUES
  - Color format: vec3(red,green,blue)
  - 1.0 means 100%, 0.0 means 0%
  - eg: vec3(1.0,1.0,0.0) = yellow

  VALUES
  - Values must be decimal
  - eg. 32 is wrong, 32.0 is correct

  TYPES
  - Should be integer. options to choose will be mentioned there
  - eg: #define NL_CLOUD_TYPE 2

  Remember to rebuild the shader after making changes.
*/

/* Color correction */
#define NL_TONEMAP_TYPE 4              // 1:Exponential, 2:Reinhard, 3:Extended Reinhard, 4:ACES, 5:Extensed Reinhard but extra curve ig
#define NL_GAMMA 1.50                  // 0.3 low ~ 2.0 high
#define NL_EXPOSURE 1.359              // [toggle] 0.5 dark ~ 3.0 bright
#define NL_SATURATION 1.4            // [toggle] 0.0 grayscale ~ 4.0 super saturated
//#define NL_TINT                      // [toggle] enable light/dark tone tinting
#define NL_TINT_LOW  vec3(0.3,0.5,1.4) // color tint for dark tone
#define NL_TINT_HIGH vec3(1.4,0.7,0.3) // color tint for light tone

/* Lighting */
#define NL_SUNLIGHT_INTENSITY   3.2  // 1.0 weak ~ 5.0 bright
#define NL_TORCHLIGHT_INTENSITY 2.0  // 0.5 weak ~ 3.0 bright
#define NL_SHADOW_INTENSITY     0.8  // 0.0 no shadow ~ 1.0 strong shadow
#define NL_MIN_LIGHTING_BOOST   0.7  // 1.0 minimal lighting boost for dark areas ~ 3.0 brighter dark areas
//#define NL_BLINKING_TORCH  // [toggle] flickering light
//#define NL_CLOUD_SHADOW      // [toggle] cloud shadow (simple clouds only)

/* End glowing beam ig */
#define NL_ENDGLOW_COLOR   vec3(0.50, 0.25, 0.72)
#define NL_ENDGLOW_COLOR2  vec3(0.22, 0.12, 0.42)
#define NL_ENDGLOW_STRENGTH 1.2
#define NL_ENDGLOW_HEIGHT   2.4
#define NL_ENDGLOW_GAP      0.02
#define NL_ENDGLOW_GAP_TOP  0.55
#define NL_ENDGLOW_SPEED    0.01
#define NL_ENDGLOW_STREAKS  5.0

/* Ambient light for nether/end */
#define NL_NETHER_AMBIENT vec3(0.102,0.0,0.0)
#define NL_END_AMBIENT    vec3(0.09,0.06,0.14)

/* Sun/moon light color */
#define NL_DAWN_SUNLIGHT_COL   vec3(1.208,0.749,0.255)
#define NL_NOON_SUNLIGHT_COL   vec3(1.0,0.75,0.57)
#define NL_NIGHT_MOONLIGHT_COL vec3(0.01,0.03,0.2)

/* Torch colors */
#define NL_OVERWORLD_TORCH_COL  vec3(1.0,0.52,0.18)
#define NL_UNDERWATER_TORCH_COL vec3(1.0,0.52,0.18)
#define NL_NETHER_TORCH_COL     vec3(1.0,0.52,0.18)
#define NL_END_TORCH_COL        vec3(1.0,0.52,0.18)

/* Fog */
#define NL_FOG 1.0                // [toggle] 0.1 subtle ~ 1.0 blend with sky completely
#define NL_MIST_DENSITY 0.28      // 0.0 no mist ~ 1.0 misty
#define NL_RAIN_MIST_OPACITY 0.12 // [toggle] 0.04 very subtle ~ 0.5 thick rain mist blow
#define NL_CLOUDY_FOG 0.1         // [toggle] 0.0 subtle - 0.8 dense fog clouds

/* Sky */
#define NL_SKY_VOID_FACTOR     0.5
#define NL_SKY_VOID_DARKNESS   0.3
#define NL_SKY_RAIN_MIX_FACTOR 0.9

/* Sky colors - zenith=top, horizon=bottom */
#define NL_DAWN_ZENITH_COL   vec3(0.40, 0.32, 0.43)
#define NL_DAWN_HORIZON_COL  vec3(2.20, 1.05, 0.78)
#define NL_DAWN_EDGE_COL     vec3(3.20, 1.85, 0.85)
#define NL_DAY_ZENITH_COL    vec3(0.37, 0.45, 0.69)
#define NL_DAY_HORIZON_COL   vec3(0.29, 0.36, 0.56)
#define NL_DAY_EDGE_COL      vec3(0.68, 0.72, 0.94)
#define NL_NIGHT_ZENITH_COL  vec3(0.055,0.077,0.112)
#define NL_NIGHT_HORIZON_COL vec3(0.144,0.198,0.301)
#define NL_NIGHT_EDGE_COL    vec3(0.069,0.094,0.139)
#define NL_RAIN_ZENITH_COL   vec3(0.47,0.51,0.56)
#define NL_RAIN_HORIZON_COL  vec3(0.6,0.6,0.6)

#define NL_END_ZENITH_COL vec3(0.053, 0.033, 0.073)
#define NL_END_HORIZON_COL vec3(0.36, 0.26, 0.46)

/* Rainbow */
#define NL_RAINBOW           // [toggle] enable rainbow in sky
#define NL_RAINBOW_CLEAR 0.0 // 0.3 subtle ~ 1.0 bright during clear weather
#define NL_RAINBOW_RAIN  0.4 // 0.3 subtle ~ 1.0 bright during rain weather

/* Ore glow intensity */
#define NL_GLOW_TEX 6.0           // 0.4 weak ~ 8.0 bright
#define NL_GLOW_SHIMMER 0.9       // [toggle] 0.1 subtle ~ 1.0 100% shimmer
#define NL_GLOW_SHIMMER_SPEED 0.9 // 0.5 slow - 2.0 fast
//#define NL_GLOW_LEAK 1.0        // [toggle] 0.08 subtle ~ 1.0 100% brightness of NL_GLOW_TEX

/* Torch bloom */
//#define NL_TORCH_BLOOM // [toggle]
#define NL_TORCH_BLOOM_STRENGTH 0.5  // 0.0 no bloom ~ 1.0 strong bloom
#define NL_TORCH_BLOOM_THRESHOLD 0.25 // 0.0 all blocks ~ 1.0 only brightest
#define NL_TORCH_BLOOM_COLOR vec3(1.0, 0.6, 0.25) // Warm yellow-orange glow

/* Waving */
#define NL_PLANTS_WAVE 0.05    // [toggle] 0.02 gentle ~ 0.4 violent
#define NL_LANTERN_WAVE 0.16   // [toggle] 0.05 subtle ~ 0.4 large swing
#define NL_WAVE_SPEED 2.8      // 0.5 slow wave ~ 5.0 very fast wave
//#define NL_EXTRA_PLANTS_WAVE // [toggle] !dont use! wave using texture coords (1.21.0 vanilla 2048x1024)
#define NL_WAVE_RANGE 16.0     // 6.0 upto 6 blocks ~ 32.0 upto 32 blocks away (slower)

/* Water */
#define NL_WATER_TRANSPARENCY 0.7 // 0.0 transparent ~ 1.0 normal
#define NL_WATER_BUMP 0.0        // 0.001 plain ~ 0.2 bumpy water
#define NL_WATER_WAVE_SPEED  1.2  // 0.2 calm ~ 2.0 turbulent
#define NL_WATER_TEX_OPACITY 1.0 // 0.0 plain water ~ 1.0 vanilla water texture
//#define NL_WATER_WAVE             // [toggle] wave effect
#define NL_WATER_REFL_MASK        // [toggle] fake water reflection mask
#define NL_WATER_TINT vec3(0.37, 0.45, 0.69)

/* Underwater */
#define NL_UNDERWATER_BRIGHTNESS 0.6         // 0.0 dark ~ 3.0 bright
#define NL_CAUSTIC_INTENSITY 0.8             // 0.5 weak ~ 5.0 bright
#define NL_UNDERWATER_WAVE 0.1               // [toggle] 0.02 subtle ~ 0.6 trippy
#define NL_UNDERWATER_STREAKS 0.6
#define NL_UNDERWATER_RAY_COL vec3(0.45, 0.78, 1.0)
#define NL_UNDERWATER_RAY_DENSITY 0.06
#define NL_UNDERWATER_TINT vec3(0.718,0.741,0.969) // fog tint color when underwater

/* Cloud type */
#define NL_CLOUD_TYPE 5 // 0:vanilla, 1:soft, 2:rounded, 3:realistic, 4:minmin's lightweight cloud, 5:Nothing :p

/* Vanilla cloud settings - make sure to remove clouds.png when using this */
#define NL_CLOUD0_THICKNESS 2.1      // 0.5 slim ~ 8.0 fat
#define NL_CLOUD0_RAIN_THICKNESS 4.0 // 0.5 slim ~ 8.0 fat
#define NL_CLOUD0_OPACITY 0.9        // 0.0 invisible ~ 1.0 opaque
#define NL_CLOUD0_MULTILAYER         // [toggle] extra cloud layer

/* Soft cloud settings */
#define NL_CLOUD1_SCALE vec2(0.016, 0.022) // 0.003 large ~ 0.2 tiny
#define NL_CLOUD1_DEPTH 1.3                // 0.0 no bump ~ 10.0 large bumps
#define NL_CLOUD1_SPEED 0.04               // 0.0 static ~ 0.4 fast moving
#define NL_CLOUD1_DENSITY 0.54             // 0.1 less clouds ~ 0.8 more clouds
#define NL_CLOUD1_OPACITY 0.9              // 0.0 invisible ~ 1.0 opaque

/* Rounded cloud settings */
#define NL_CLOUD2_THICKNESS 2.1            // 0.5 slim ~ 5.0 fat
#define NL_CLOUD2_RAIN_THICKNESS 2.5       // 0.5 slim ~ 5.0 fat
#define NL_CLOUD2_STEPS 5                  // 3 low quality ~ 16 high quality
#define NL_CLOUD2_SCALE vec2(0.033, 0.033) // 0.003 large ~ 0.3 tiny
#define NL_CLOUD2_SHAPE vec2(0.5, 0.4)     // 0.0 round ~ 1.0 box vec2(horizontal shape, vertical shape)
#define NL_CLOUD2_DENSITY 25.0             // 1.0 blurry ~ 100.0 sharp
#define NL_CLOUD2_VELOCITY 0.8             // 0.0 static ~ 4.0 very fast
//#define NL_CLOUD2_LAYER2                      // [toggle] extra cloud layer
#define NL_CLOUD2_LAYER2_OFFSET 143.0           // 30.0 near ~ 300.0 very high
#define NL_CLOUD2_LAYER2_THICKNESS 2.5          // 0.7 slim ~ 5.0 fat
#define NL_CLOUD2_LAYER2_RAIN_THICKNESS 3.0     // 0.7 slim ~ 5.0 fat
#define NL_CLOUD2_LAYER2_STEPS 3                // 3 low quality ~ 16 high quality
#define NL_CLOUD2_LAYER2_SCALE vec2(0.03, 0.03) // 0.003 large ~ 0.3 tiny
#define NL_CLOUD2_LAYER2_SHAPE vec2(0.5, 0.4)   // 0.0 round ~ 1.0 box vec2(horizontal shape, vertical shape)
#define NL_CLOUD2_LAYER2_DENSITY 25.0           // 1.0 blurry ~ 100.0 sharp
#define NL_CLOUD2_LAYER2_VELOCITY 0.8           // 0.0 static ~ 4.0 very fast

/* Realistic cloud settings */
#define NL_CLOUD3_SCALE vec2(0.03, 0.03) // 0.003 large ~ 1.0 tiny
#define NL_CLOUD3_SPEED 0.102            // 0.0 static ~ 4.0 fast moving
#define NL_CLOUD3_SHADOW 0.9             // 0.1 subtle ~ 1.0 dark
#define NL_CLOUD3_SHADOW_OFFSET 0.3      // 0.05 minimal ~ 1.0 large

/* Lightweight Clouds */
#define NL_CLOUD4_SCALE vec2(0.020, 0.020) // 0.005 large ~ 0.1 tiny
#define NL_CLOUD4_SHAPE vec2(0.2, 0.3)     // 0.0 round ~ 1.0 box 
#define NL_CLOUD4_SPEED 0.8               // 0.0 static ~ 0.1 fast moving
#define NL_CLOUD4_SHADOW 1.0               // 0.1 subtle ~ 1.0 dark shadow
#define NL_CLOUD4_SHADOW_OFFSET 0.2        // 0.05 minimal ~ 1.0 large
#define NL_CLOUD4_NIGHT_DARKEN 0.14       // 0.1 very dark ~ 1.0 same as day

/* Aurora settings (old sine-based, unused, kept for reference) */
//#define NL_AURORA 1.2           // [toggle] 0.4 dim ~ 4.0 very bright
#define NL_AURORA_VELOCITY 0.03 // 0.0 static ~ 0.3 very fast
#define NL_AURORA_SCALE 0.04    // 0.002 large ~ 0.4 tiny
#define NL_AURORA_WIDTH 0.18    // 0.04 thin line ~ 0.4 thick lines
#define NL_AURORA_COL1 vec3(0.1,1.0,0.0)
#define NL_AURORA_COL2 vec3(0.1,0.0,1.0)

//#define NL_CLOUD_AURORA_REFLECTION // [toggle] (reflects type 4 clouds on water)

/* Shooting star */
#define NL_SHOOTING_STAR 1.0        // [toggle] 0.2 dim ~ 1.0 bright
#define NL_SHOOTING_STAR_PERIOD 2.0 // 0.4 fast ~ 12.0 slow (12 secs)
#define NL_SHOOTING_STAR_DELAY 6.0 // 0.0 no delay ~ 100.0 long time (100 secs)

/* Galaxy */
//#define NL_GALAXY_STARS 2.0           // [toggle] 0.1 dim - 4.0 bright
#define NL_GALAXY_VIBRANCE 0.7          // 0.0 white - 1.0 colorful
#define NL_GALAXY_SPEED 0.03            // 0.01 slow motion - 0.2 fast motion
#define NL_GALAXY_DAY_VISIBILITY 0.0    // 0.0 invisible - 1.0 visible

/* Chunk loading slide in animation */
//#define NL_CHUNK_LOAD_ANIM 100.0 // [toggle] -600.0 fall from top ~ 600.0 rise from bottom

/* Sun/Moon */
#define NL_SUN_SIZE  2.7           // 0.3 tiny ~ 4.0 massive
#define NL_MOON_SIZE 2.0           // 0.3 tiny ~ 4.0 massive
#define NL_SUN_PATH_YAW    15.0 //
#define NL_MOON_PATH_YAW   17.0 //
#define NL_SUN_PATH_TILT   32.0 //
#define NL_MOON_PATH_TILT -28.0 //
#define NL_SUN_TILT        0.0 // 0.0 no tilt ~ 90.0 tilt of 90 degrees
#define NL_MOON_TILT       45.0 // 0.0 no tilt ~ 90.0 tilt of 90 degrees
//#define NL_SUN_BLOOM_SQUARE            // [toggle] square-shaped sun bloom
#define NL_SUN_BLOOM_SIZE 0.03         // 0.05 tight ~ 0.5 large
#define NL_SUN_BLOOM_SOFTNESS 1.0     // 0.0 hard edge ~ 0.3 very soft
#define NL_SUN_BLOOM_STRENGTH 2.1      // 0.5 subtle ~ 3.0 strong

/* Fake godrays during sunrise/sunset */
#define NL_GODRAY 0.0 // [toggle] 0.1 subtle ~ 0.8 strong
#define NL_GODRAY_SPOKE_COUNT 1.0
#define NL_GODRAY_SPOKE_SHARPNESS 0.5

/* Black hole value tuning (feature itself toggled per subpack tier below) */
#define NL_BH_SPEED 0.3
#define NL_BH_DIST 1.5
#define NL_BH_AZIMUTH 2.4
#define NL_BH_HEIGHT 0.5
#define NL_BH_COL_LOW  vec3(0.30, 0.21, 0.47)
#define NL_BH_COL_HIGH vec3(0.49, 0.33, 0.57)
#define NL_BH_REFLECT_STRENGTH 6.0

/* Sky reflection */
//#define NL_GROUND_REFL 0.4       // [toggle] 0.2 slightly reflective ~ 1.0 fully reflect sky
#define NL_GROUND_RAIN_WETNESS 0.8 // 0.0 no wetness ~ 1.0 fully wet blocks when raining
#define NL_GROUND_RAIN_PUDDLES 0.7 // 0.0 no puddles ~ 1.0 puddles
#define NL_END_GROUND_REFL
#define NL_END_GROUND_REFL_STRENGTH 0.6 // 0.1 subtle ~ 1.0 strongly reflective

/* Entity (Actor, ItemInHand) */
#define NL_ENTITY_BRIGHTNESS     0.42 // 0.1 dark ~ 1.6 bright
#define NL_ENTITY_EDGE_HIGHLIGHT 0.38 // [toggle] 0.0 no highlight ~ 1.6 bright highlight

/* Weather particles */
#define NL_WEATHER_SPECK 0.8         // [toggle] 0.0 vanilla texture ~ 1.0 soft speck
#define NL_WEATHER_RAIN_SLANT 2.0    // 1.0 minimal ~ 8.0 violent
#define NL_WEATHER_PARTICLE_SIZE 0.5 // 0.5 tiny ~ 4.0 large

/* Lava effects */
//#define NL_LAVA_NOISE            // [toggle] darken lava in certain regions
//#define NL_LAVA_NOISE_BUMP 0.2 // [toggle] 0.1 subtle ~ 0.8 massive waves
#define NL_LAVA_NOISE_SPEED 0.2  // 0.0 still ~ 0.8 fast

/*
  NEWB SHADER SUBPACK CONFIG
  This part contains custom configuration options for each subpack.

  If a value is already defined,
  then you must undefine it before modifying:
  eg: #undef OPTION_NAME

  Subpack names and flags are inside `pack_config.toml`.
  Build tool will enable corresponding flags when compiling.
*/

#ifdef NO_FOG
  #undef NL_FOG
#endif

#ifdef NO_WAVE
  #undef NL_PLANTS_WAVE
  #undef NL_LANTERN_WAVE
  #undef NL_UNDERWATER_WAVE
  #undef NL_WATER_WAVE
  #undef NL_RAIN_MIST_OPACITY
#endif

/* Effects tiers - aurora & black hole */
#ifdef NL_UNBOUND
  #undef NL_SUN_CIRCLE_BLOOM
  #define NL_SUN_CIRCLE_BLOOM 1.6
  #undef NL_WATER_TEX_OPACITY
  #define NL_WATER_TEX_OPACITY 0.0
  #define NL_SKY_PLANE_CLOUDS
  #undef NL_SKY_VOLUME_CLOUDS_REFL
  #define NL_SKY_PLANE_CLOUDS_REFL
  #define NL_SKY_AURORA_SPEED 4.0
  #define NL_BLINKING_TORCH
  #undef NL_WATER_FOAM
  #undef NL_SKY_BLOCK_CLOUDS
  #undef NL_VOL_CLOUD_STEPS
  #define NL_VOL_CLOUD_STEPS 10
  #define NL_END_CLOUDS
  #define NL_END_CLOUD_SPEED 0.0
  #define NL_END_CLOUD_LO 0.15
  #define NL_END_CLOUD_HI 0.55
  #define NL_END_CLOUD_OPACITY 1.0
  #define NL_END_CLOUD_HAZE 0.15
  #define NL_END_CLOUD_COL_LIGHT vec3(0.70,0.55,0.94)
  #define NL_END_CLOUD_COL_DARK  vec3(0.22,0.13,0.40)
  #define NL_END_CLOUD_FOG_COL   vec3(0.30,0.18,0.48)
  #undef NL_SKY_BLOCK_CLOUDS_REFL
  #undef NL_CAUSTIC_INTENSITY
  #define NL_CAUSTIC_INTENSITY 0.3
  #define NL_CAUSTIC_ATLAS
  #define NL_CAUSTIC_FPS 10.0
  #define NL_CAUSTIC_BLEND 1
  #define NL_CAUSTIC_LO 0.68
  #define NL_CAUSTIC_HI 1.0
  #define NL_CAUSTIC_GAIN 0.6
  #define NL_LAVA_NOISE
  #undef NL_AURORA_COMP_STYLE
  #define NL_AURORA_COMP_STYLE 2
#endif

#ifdef NL_REIMAGINED
  #define NL_SKY_AURORA_SPEED 4.0
  #define NL_SKY_BLOCK_CLOUDS
  #undef NL_WATER_RESOLUTE_WAVES
  #undef NL_WATER_REFL_MASK
  #undef NL_SKY_VOLUME_CLOUDS_REFL
  #undef NL_CAUSTIC_INTENSITY
  #define NL_CAUSTIC_INTENSITY 0.5
  #define NL_CAUSTIC_FPS 10.0
  #define NL_CAUSTIC_BLEND 1
  #undef NL_SKY_EQUI_CLOUDS
  #define NL_WATER_TEX_MEAN 0.30
  #define NL_WATER_TEX_CONTRAST_R 6.0
  #define NL_WATER_HIGHLIGHT 8.0
  #undef NL_SKY_PLANE_CLOUDS
  #undef NL_SKY_PLANE_CLOUDS_REFL
#endif

#endif
