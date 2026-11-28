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
uniform vec4 P6; // shape, scale, speed, density
uniform vec4 P7; // glow, rotation(rad), fill, -
uniform vec4 ColS1;
uniform vec4 ColS2;
uniform vec4 ColBg;
uniform vec4 ColInk;
uniform vec4 ColInk2;
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


const float TAU = 6.2831853;

vec2 rot2(vec2 v, float a) {
    float c = cos(a), s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

// 4-point sparkle, d in screen units, sz = size
float sparkle(vec2 d, float sz) {
    d /= max(sz, 1e-4);
    float core = exp(-dot(d, d) * 3.0);
    float arms = exp(-abs(d.x) * 1.2 - abs(d.y) * 9.0) + exp(-abs(d.y) * 1.2 - abs(d.x) * 9.0);
    return core + 0.8 * arms;
}

// 1: glittering starburst zoom
vec3 shStarburst(vec2 q, float t, float grow, float sc, float spd, float dens, float seed) {
    float a = atan(q.y, q.x), r = length(q);
    float I = 0.0;
    for (int l = 0; l < 3; l++) {
        float n = (70.0 + 55.0 * float(l)) * dens;
        float k = a / TAU * n;
        float id = floor(k) + float(l) * 91.0;
        float f = fract(k) - 0.5;
        float h1 = hash11(id * 1.37 + seed);
        float h2 = hash11(id * 3.11 + seed + 5.0);
        float ph = fract(h1 + t / 20.0 * (0.5 + h2) * spd * 0.8);
        float head = ph * 1.7 * sc * grow;
        float len = (0.10 + 0.55 * h2) * sc;
        float along = (head - r) / len;
        float rad = step(0.0, along) * step(along, 1.0) * pow(1.0 - along, 1.3);
        float w = 0.12 + 0.35 * h1;
        float prof = exp(-f * f / (w * w * 0.06));
        I += rad * prof * (0.5 + h2) * step(0.25, hash11(id * 5.3 + seed)) * smoothstep(0.02, 0.2, r);
    }
    float S = 0.0;
    for (int i = 0; i < 22; i++) {
        float fi = float(i);
        vec2 pos = (vec2(hash11(fi * 3.7 + seed), hash11(fi * 5.9 + seed + 2.0)) - 0.5) * vec2(1.9, 1.7) * sc;
        float tw = pow(0.5 + 0.5 * sin(t * 0.45 * spd + fi * 2.3), 3.0);
        S += sparkle(q - pos, (0.02 + 0.03 * hash11(fi * 1.3 + seed)) * sc) * tw;
    }
    float glow = exp(-r * r / (0.03 * sc * sc)) * 0.35 * grow;
    return ColS1.rgb * (I + S) + ColS2.rgb * (glow + 0.3 * I);
}

// 2: manga lightning explosion
vec3 shLightning(vec2 q, float t, float grow, float sc, float spd, float dens, float seed) {
    float a = atan(q.y, q.x), r = length(q);
    float tf = floor(t * 0.6 * spd);
    float core = exp(-r * r / (0.012 * sc * sc)) * 1.6 + exp(-r * r / (0.09 * sc * sc)) * 0.6;
    float bolt = 0.0, glow = 0.0;
    for (int i = 0; i < 10; i++) {
        float fi = float(i);
        float a0 = hash11(fi * 7.13 + seed) * TAU;
        float wob = (vnoise(vec2(r * 7.0 / sc + fi * 4.0, tf * 0.7)) - 0.5) * 1.1 * (0.15 + r / sc)
                  + (vnoise(vec2(r * 26.0 / sc + fi * 9.0, tf * 1.3)) - 0.5) * 0.25 * r / sc;
        float da = mod(a - (a0 + wob) + 3.14159265, TAU) - 3.14159265;
        float dist = abs(da) * r;
        float reach = (0.55 + 0.85 * hash11(fi * 3.3 + seed)) * sc * grow;
        float w = (0.010 + 0.014 * hash11(fi * 1.9 + seed)) * sc * (1.0 - 0.75 * clamp(r / max(reach, 1e-3), 0.0, 1.0));
        float on = step(r, reach);
        bolt += on * smoothstep(w, w * 0.25, dist);
        glow += on * exp(-dist / (w * 6.0)) * 0.35;
        for (int b = 0; b < 2; b++) {
            float fb = float(b);
            float r0 = reach * (0.25 + 0.3 * hash11(fi * 5.1 + fb * 2.7 + seed));
            float am = a0 + (vnoise(vec2(r0 * 7.0 / sc + fi * 4.0, tf * 0.7)) - 0.5) * 1.1 * (0.15 + r0 / sc);
            float side = b == 0 ? 1.0 : -1.0;
            float angb = am + side * 0.5 * (r - r0) / max(r0, 0.05)
                       + (vnoise(vec2(r * 30.0 / sc + fi * 3.0 + fb * 7.0, tf * 1.7)) - 0.5) * 0.3 * r / sc;
            float dab = mod(a - angb + 3.14159265, TAU) - 3.14159265;
            float distb = abs(dab) * r;
            float blen = (0.2 + 0.3 * hash11(fi * 8.7 + fb)) * sc * grow;
            float onb = step(r0, r) * step(r, r0 + blen);
            float wb = w * 0.55 * (1.0 - clamp((r - r0) / max(blen, 1e-3), 0.0, 1.0) * 0.7);
            bolt += onb * smoothstep(wb, wb * 0.25, distb);
            glow += onb * exp(-distb / (wb * 6.0)) * 0.2;
        }
    }
    float lines = step(0.78, hash11(floor(a * 130.0 * dens) + seed)) * smoothstep(0.12, 0.9, r / sc) * 0.3 * grow;
    float dust = step(0.985, hash21(floor(q * 180.0) + tf)) * exp(-r / (0.6 * sc)) * 0.8;
    return ColS1.rgb * (clamp(core, 0.0, 1.8) + clamp(bolt, 0.0, 1.0) + lines + dust) + ColS2.rgb * (glow * 0.9 + core * 0.15);
}

// 3: falling light streaks (q already rotated, streaks run along y)
vec3 shRain(vec2 q, float t, float sc, float spd, float dens, float seed) {
    float cols = 34.0 * dens;
    float x = q.x * cols;
    float id = floor(x);
    float f = fract(x) - 0.5;
    float h1 = hash11(id * 1.7 + seed);
    float h2 = hash11(id * 4.3 + seed + 9.0);
    float flow = q.y * 3.0 / sc + t / 20.0 * spd * (1.5 + 3.0 * h2) + h1 * 40.0;
    float n = vnoise(vec2(flow, id * 3.1)) * 0.7 + vnoise(vec2(flow * 2.3 + 7.0, id * 5.7)) * 0.5;
    float band = smoothstep(0.5, 0.85, n);
    float wcol = 5.0 + 60.0 * h1 * h1;
    float core = exp(-f * f * wcol * 4.0) * band;
    float soft = exp(-f * f * wcol * 0.4) * band * 0.35;
    return ColS1.rgb * core * 1.4 + ColS2.rgb * soft * 1.2;
}

// 4: cartoon speed lines + crescent splats
vec3 shSplat(vec2 q, float t, float grow, float sc, float spd, float dens, float seed) {
    float a = atan(q.y, q.x), r = length(q);
    float tf = floor(t * 0.5 * spd);
    float n = 64.0 * dens;
    float k = a / TAU * n;
    float id = floor(k);
    float f = fract(k) - 0.5;
    float h1 = hash11(id * 1.3 + seed + tf * 7.0);
    float h2 = hash11(id * 2.9 + seed + tf * 3.0 + 4.0);
    float r0 = (0.12 + 0.5 * h1) * sc * grow;
    float r1 = r0 + (0.25 + 0.8 * h2) * sc * grow;
    float w = (0.025 + 0.12 * smoothstep(0.0, 1.2, r / sc)) * (0.4 + h2);
    float seg = step(r0, r) * step(r, r1) * step(0.35, hash11(id * 6.1 + seed + tf));
    float lineM = seg * smoothstep(w, w * 0.2, abs(f));
    float lineG = seg * exp(-abs(f) / (w * 3.0)) * 0.25;
    float sM = 0.0, sG = 0.0;
    for (int i = 0; i < 16; i++) {
        float fi = float(i);
        float hs = hash11(fi * 5.3 + seed);
        float tt = hs + t / 20.0 * spd * 0.9;
        float cyc = floor(tt);
        float ph = fract(tt);
        float ang = hash11(fi * 2.1 + seed + cyc * 13.7) * TAU;
        float rad = (0.05 + ph * 0.55) * sc * grow;
        float size = (0.03 + 0.05 * hash11(fi * 7.7 + seed + cyc)) * sc * (1.0 - ph * 0.6);
        vec2 dir = vec2(cos(ang), sin(ang));
        vec2 c = dir * rad;
        float d1 = length(q - c) - size;
        float d2 = length(q - (c - dir * size * 0.55)) - size * 0.85;
        float fade = 1.0 - smoothstep(0.8, 1.0, ph);
        sM += smoothstep(0.006, -0.006, max(d1, -d2)) * fade;
        sG += exp(-max(d1, 0.0) / (0.012 * sc)) * 0.35 * fade;
    }
    return ColS1.rgb * clamp(lineM + sM, 0.0, 1.0) + ColS2.rgb * (lineG + sG);
}

// radial ink wedges (returns ink coverage)
float inkRadial(float a, float r, float n, float seed, float tf, float sc) {
    float rn = r / sc;
    float k = a / TAU * n + (vnoise(vec2(a * 40.0, rn * 18.0)) - 0.5) * 0.9;
    float id = floor(k);
    float f = fract(k);
    float h = hash11(id * 1.7 + seed + tf * 3.0);
    float h2 = hash11(id * 4.1 + seed + tf);
    float w = clamp((0.12 + 0.7 * h) * (0.3 + 1.4 * rn), 0.0, 0.97);
    float ink = smoothstep(w + 0.03, w - 0.03, abs(f - 0.5) * 2.0);
    ink *= step(0.06 + 0.4 * h2, rn) * smoothstep(0.04, 0.3, rn);
    return ink;
}

// 5: manga radial ink lines
float shRadialInk(vec2 q, float t, float sc, float spd, float dens, float seed) {
    float a = atan(q.y, q.x), r = length(q);
    float tf = floor(t * 0.5 * spd);
    float ink = inkRadial(a, r, 70.0 * dens, seed, tf, sc);
    float speck = step(0.96, hash21(floor(q * 160.0) + tf)) * smoothstep(0.1, 0.6, r / sc);
    return max(ink, speck * 0.85);
}

// 6: diagonal ink streaks with splatter (q rotated; stripes run along x)
float shDiagInk(vec2 q, float t, float grow, float sc, float spd, float dens, float seed) {
    float N = 46.0 * dens;
    float u = q.y * N;
    float id = floor(u);
    float f = fract(u);
    float tf = floor(t * 0.2 * spd);
    float h1 = hash11(id * 1.7 + seed + tf);
    float h2 = hash11(id * 3.9 + seed + tf + 7.0);
    float front = (grow * 2.6 - 1.3) * sc;
    float head = front - h1 * 0.7 * sc;
    float len = (0.5 + 1.4 * h2) * sc;
    float tail = head - len;
    float x = q.x;
    float vis = step(tail, x) * step(x, head);
    float tp = clamp((x - tail) / len, 0.0, 1.0);
    float w = (0.15 + 0.75 * h1) * (1.0 - tp * 0.95) * (0.8 + 0.4 * vnoise(vec2(x * 40.0, id)));
    float ink = vis * smoothstep(w + 0.04, w - 0.04, abs(f - 0.5) * 2.0);
    vec2 g = q * 38.0;
    vec2 cell = floor(g);
    float hh = hash21(cell + seed);
    vec2 off = (vec2(hash21(cell + 3.1), hash21(cell + 8.7)) - 0.5) * 0.5;
    float d = length(fract(g) - 0.5 - off);
    float rad = 0.08 + 0.25 * hash21(cell + 5.5);
    float splat = step(0.9, hh) * smoothstep(rad + 0.05, rad - 0.05, d) * step(x, front) * step(front - 1.4 * sc, x);
    return max(ink, splat);
}

// 7: ink eye burst (returns ink coverage; paper is everywhere)
float shEye(vec2 q, float t, float grow, float sc, float spd, float dens, float seed) {
    float a = atan(q.y, q.x), r = length(q);
    float tf = floor(t * 0.5 * spd);
    float R0 = 0.25 * sc * (0.35 + 0.65 * grow);
    float spike = (vnoise(vec2(a * 5.0, tf * 0.3)) - 0.5) * 0.3 + (hash11(floor(a * 70.0) + tf) - 0.5) * 0.12;
    float R = R0 * (1.0 + spike);
    float inside = smoothstep(R + 0.006, R - 0.006, r);
    float outer = inkRadial(a, r, 52.0 * dens, seed, tf, sc) * smoothstep(R * 1.25, R * 1.55, r);
    float rays = inkRadial(a, r, 90.0, seed + 3.0, tf, sc);
    return max(outer, inside * (1.0 - 0.75 * rays));
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

    // overlay shapes (behind the characters)
    int shape = int(P6.x + 0.5);

    if (shape > 0) {
        float sc = max(P6.y, 0.05), spd = P6.z, dens = max(P6.w, 0.05);
        float glowK = P7.x, rotA = P7.y, fillA = P7.z;
        vec2 qs = rot2(p, rotA);
        float grow = 1.0 - pow(1.0 - clamp(prog / 0.2, 0.0, 1.0), 3.0);

        if (shape <= 4) {
            vec3 add = vec3(0.0);
            world = mix(world, ColBg.rgb, fillA);

            if (shape == 1) add = shStarburst(qs, t, grow, sc, spd, dens, seed);
            else if (shape == 2) add = shLightning(qs, t, grow, sc, spd, dens, seed);
            else if (shape == 3) add = shRain(qs, t, sc, spd, dens, seed);
            else add = shSplat(qs, t, grow, sc, spd, dens, seed);

            world = clamp(world + add * glowK, 0.0, 1.0);
        } else {
            float inkA = 0.0;

            if (shape == 5) inkA = shRadialInk(qs, t, sc, spd, dens, seed);
            else if (shape == 6) inkA = shDiagInk(qs, t, grow, sc, spd, dens, seed);
            else inkA = shEye(qs, t, grow, sc, spd, dens, seed);

            world = mix(world, ColS2.rgb, fillA);
            world = mix(world, ColS1.rgb, clamp(inkA, 0.0, 1.0));

            if (shape == 7) {
                world += ColAccent.rgb * exp(-dot(qs, qs) / (0.002 * sc * sc)) * 1.2 * glowK;
            }
        }
    }

    float gr = (vnoise(uv * vec2(asp, 1.0) * 420.0 + floor(t) * 7.3) - 0.5) * grain;
    vec3 charCol = ink;
    int cm = int(P7.w + 0.5);
    float wl = dot(world, vec3(0.299, 0.587, 0.114));
    if (cm == 1) charCol = wl > 0.5 ? ink : ColInk2.rgb;
    else if (cm == 2) charCol = vec3(1.0) - world;
    vec3 actor = clamp(charCol + gr, 0.0, 1.0);
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
