#version 150

uniform sampler2D Sampler0;   // scene color copy
uniform sampler2D Sampler1;   // scene depth copy

uniform mat4 InvProj;
uniform mat4 CamRot;
uniform vec4 P0; // time(ticks), progress 0..1, amount, aspect
uniform vec4 P1; // mode, threshold, worldMix, distort
uniform vec4 P2; // actorRadius, actorHeight, aura, auraWidth(cm)
uniform vec4 P3; // starCount, starSize, starSeed, starRate(changes/sec)
uniform vec4 P4; // speedLines, shockwave, inkBurst, flash
uniform vec4 P5; // centerX, centerY, grain, posterize
uniform vec4 ColBg;
uniform vec4 ColInk;
uniform vec4 ColAccent;
uniform vec4 ColStar;
uniform vec4 Actor0;
uniform vec4 Actor1;
uniform vec4 Actor2;
uniform vec4 Actor3;
uniform vec4 Actor4;
uniform vec4 Actor5;

in vec2 uv;
out vec4 fragColor;

float hash11(float x) { return fract(sin(x * 127.1 + 311.7) * 43758.5453); }
float hash21(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1, 0)), f.x),
               mix(hash21(i + vec2(0, 1)), hash21(i + vec2(1, 1)), f.x), f.y);
}
float vnoise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hash21(i.xy + i.z * 17.0), hash21(i.xy + vec2(1, 0) + i.z * 17.0), f.x),
                  mix(hash21(i.xy + vec2(0, 1) + i.z * 17.0), hash21(i.xy + vec2(1, 1) + i.z * 17.0), f.x), f.y);
    float b = mix(mix(hash21(i.xy + (i.z + 1.0) * 17.0), hash21(i.xy + vec2(1, 0) + (i.z + 1.0) * 17.0), f.x),
                  mix(hash21(i.xy + vec2(0, 1) + (i.z + 1.0) * 17.0), hash21(i.xy + vec2(1, 1) + (i.z + 1.0) * 17.0), f.x), f.y);
    return mix(a, b, f.z);
}

// signed distance to a vertical capsule-ish body (feet at a, height H)
float bodyDist(vec3 wp, vec4 act, float R, float H) {
    if (act.w < 0.5) return 100.0;
    vec3 a = act.xyz;
    float h = clamp((wp.y - a.y) / H, 0.0, 1.0);
    vec3 c = vec3(a.x, a.y + h * H, a.z);
    float rad = R * (0.80 + 0.20 * sin(h * 3.14159));
    return length(wp - c) - rad;
}

void main() {
    float t = P0.x, prog = P0.y, amt = P0.z, asp = P0.w;
    int mode = int(P1.x + 0.5);
    float th = P1.y, worldMix = P1.z, distort = P1.w;
    float R = P2.x, H = P2.y, aura = P2.z, auraW = P2.w * 0.01;
    float nStars = P3.x, starSize = P3.y, seed = P3.z, rate = P3.w;
    float lines = P4.x, shock = P4.y, burst = P4.z, flash = P4.w;
    vec2 ctr = P5.xy;
    float grain = P5.z, post = P5.w;

    vec2 p = (uv - ctr) * vec2(asp, 1.0);
    float rr = length(p);
    float tq = floor(t / 2.0);

    // shockwave refraction
    float sr = prog * 1.6;
    float ring = exp(-pow((rr - sr) / 0.06, 2.0)) * shock;
    vec2 uv2 = uv + normalize(p + vec2(1e-5)) * ring * 0.03 * vec2(1.0 / asp, 1.0);

    vec3 scene = texture(Sampler0, uv2).rgb;
    float depth = texture(Sampler1, uv2).r;
    float luma = dot(scene, vec3(0.299, 0.587, 0.114));

    // world position relative to camera
    vec4 ndc = vec4(uv2 * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 vp = InvProj * ndc;
    vp /= vp.w;
    vec3 wp = (CamRot * vec4(vp.xyz, 1.0)).xyz;

    float edge = 100.0;
    if (depth < 0.99999) {
        edge = min(min(bodyDist(wp, Actor0, R, H), bodyDist(wp, Actor1, R, H)),
                   min(min(bodyDist(wp, Actor2, R, H), bodyDist(wp, Actor3, R, H)),
                       min(bodyDist(wp, Actor4, R, H), bodyDist(wp, Actor5, R, H))));
        float n1 = vnoise3(wp * 4.0 + vec3(0.0, 0.0, tq * 1.7));
        float n2 = vnoise3(wp * 16.0 + vec3(tq * 3.1, 0.0, 0.0));
        edge += (n1 - 0.5) * distort * 0.35 + (n2 - 0.5) * distort * 0.10;
    }
    float mask = 1.0 - smoothstep(-0.015, 0.015, edge);

    vec3 bg = ColBg.rgb, ink = ColInk.rgb, acc = ColAccent.rgb;
    vec3 sceneP = post > 0.5 ? floor(scene * post + 0.5) / post : scene;

    vec3 world;
    if (mode == 1) {
        world = luma > th ? bg : ink;
    } else if (mode == 2) {
        world = mix(bg, vec3(1.0) - sceneP, 0.85);
    } else if (mode == 4) {
        vec2 cell = fract(uv * vec2(asp, 1.0) * 70.0) - 0.5;
        float dotR = (1.0 - luma) * 0.75;
        float dots = smoothstep(dotR + 0.06, dotR - 0.06, length(cell));
        world = mix(bg, acc, dots);
    } else {
        world = bg;
    }
    world = mix(world, sceneP, worldMix);

    float gr = (vnoise(uv * vec2(asp, 1.0) * 420.0 + floor(t) * 7.3) - 0.5) * grain;
    vec3 actor = clamp(ink + gr, 0.0, 1.0);
    vec3 col = mix(world, actor, mask);

    // aura glow around the actor
    float halo = aura * exp(-max(edge, 0.0) / max(auraW, 0.001)) * (1.0 - mask);
    col = mix(col, acc, clamp(halo, 0.0, 1.0));

    // speed lines
    if (lines > 0.001) {
        float a = atan(p.y, p.x);
        float k = a * 40.0;
        float cell = floor(k);
        float hh = hash11(cell + tq * 13.1 + seed);
        float w = fract(k);
        float line = step(1.0 - lines * 0.55, hh) * smoothstep(0.0, 0.25, w) * smoothstep(1.0, 0.75, w);
        float inner = smoothstep(0.22, 0.65, rr);
        col = mix(col, acc, line * inner);
    }

    // ink burst
    if (burst > 0.001) {
        float a = atan(p.y, p.x);
        float spike = vnoise(vec2(a * 6.0, 3.0 + tq));
        float br = burst * 0.35 * (0.45 + spike);
        col = mix(col, ink, step(rr, br));
    }

    // shock ring
    col = mix(col, acc, clamp(ring * 0.9, 0.0, 1.0));

    // stars / sparkles
    float sd = seed + floor(t * rate / 20.0) * 17.0;
    for (int i = 0; i < 40; i++) {
        if (float(i) >= nStars) break;
        float fi = float(i);
        vec2 sp = vec2(hash11(fi * 3.17 + sd), hash11(fi * 7.31 + sd + 5.0));
        vec2 q = (uv - sp) * vec2(asp, 1.0);
        float sz = starSize * (0.4 + hash11(fi * 1.7 + sd) * 1.2) * (0.65 + 0.35 * sin(t * 0.8 + fi));
        float st = sqrt(abs(q.x)) + sqrt(abs(q.y));
        float s = sqrt(max(sz, 1e-4));
        float m = 1.0 - smoothstep(s * 0.85, s, st);
        col = mix(col, ColStar.rgb, m);
    }

    // flash
    col = mix(col, bg, clamp(flash * pow(max(1.0 - prog, 0.0), 3.0), 0.0, 1.0));

    fragColor = vec4(mix(scene, col, clamp(amt, 0.0, 1.0)), 1.0);
}
