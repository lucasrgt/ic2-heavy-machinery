const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function encodePNG(width, height, rgbaBuffer) {
    const raw = Buffer.alloc(height * (1 + width * 4));
    for (let y = 0; y < height; y++) { raw[y*(1+width*4)] = 0; rgbaBuffer.copy(raw, y*(1+width*4)+1, y*width*4, (y+1)*width*4); }
    const compressed = zlib.deflateSync(raw, { level: 9 });
    function crc32(buf) { let crc=0xffffffff; const t=new Int32Array(256); for(let i=0;i<256;i++){let c=i;for(let j=0;j<8;j++)c=(c&1)?(0xedb88320^(c>>>1)):(c>>>1);t[i]=c;} for(let i=0;i<buf.length;i++)crc=t[(crc^buf[i])&0xff]^(crc>>>8); return(crc^0xffffffff)>>>0; }
    function chunk(type, data) { const len=Buffer.alloc(4);len.writeUInt32BE(data.length); const td=Buffer.concat([Buffer.from(type),data]); const c=Buffer.alloc(4);c.writeUInt32BE(crc32(td)); return Buffer.concat([len,td,c]); }
    const sig = Buffer.from([137,80,78,71,13,10,26,10]);
    const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(width,0); ihdr.writeUInt32BE(height,4); ihdr[8]=8; ihdr[9]=6;
    return Buffer.concat([sig, chunk('IHDR',ihdr), chunk('IDAT',compressed), chunk('IEND',Buffer.alloc(0))]);
}

function decodePNG(pngBuf) {
    let pos=8;let w,h,ct;const ids=[];
    while(pos<pngBuf.length){const l=pngBuf.readUInt32BE(pos);const t=pngBuf.slice(pos+4,pos+8).toString('ascii');const d=pngBuf.slice(pos+8,pos+8+l);
    if(t==='IHDR'){w=d.readUInt32BE(0);h=d.readUInt32BE(4);ct=d[9];}else if(t==='IDAT')ids.push(d);else if(t==='IEND')break;pos+=12+l;}
    const raw=zlib.inflateSync(Buffer.concat(ids));const bpp=ct===6?4:ct===2?3:ct===0?1:1;const stride=w*bpp;
    const rgba=Buffer.alloc(w*h*4);const uf=Buffer.alloc(h*stride);
    for(let y=0;y<h;y++){const ft=raw[y*(1+stride)];const rs=y*(1+stride)+1;const os=y*stride;
    for(let i=0;i<stride;i++){const x=raw[rs+i];const a=i>=bpp?uf[os+i-bpp]:0;const b=y>0?uf[os-stride+i]:0;const c=(y>0&&i>=bpp)?uf[os-stride+i-bpp]:0;
    let v;switch(ft){case 0:v=x;break;case 1:v=(x+a)&0xff;break;case 2:v=(x+b)&0xff;break;case 3:v=(x+Math.floor((a+b)/2))&0xff;break;
    case 4:{const p=a+b-c;const pa=Math.abs(p-a),pb=Math.abs(p-b),pc=Math.abs(p-c);v=(x+(pa<=pb&&pa<=pc?a:pb<=pc?b:c))&0xff;break;}default:v=x;}uf[os+i]=v;}}
    for(let y=0;y<h;y++)for(let x=0;x<w;x++){const si=y*stride+x*bpp;const di=(y*w+x)*4;
    if(ct===6){rgba[di]=uf[si];rgba[di+1]=uf[si+1];rgba[di+2]=uf[si+2];rgba[di+3]=uf[si+3];}
    else if(ct===2){rgba[di]=uf[si];rgba[di+1]=uf[si+1];rgba[di+2]=uf[si+2];rgba[di+3]=255;}
    else if(ct===0){rgba[di]=uf[si];rgba[di+1]=uf[si];rgba[di+2]=uf[si];rgba[di+3]=255;}}
    return{width:w,height:h,rgba};
}

function extractTile(atlas, tileX, tileY) {
    const buf = Buffer.alloc(16*16*4);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
        const si = ((tileY*16+y) * atlas.width + (tileX*16+x)) * 4;
        const di = (y*16+x) * 4;
        buf[di] = atlas.rgba[si]; buf[di+1] = atlas.rgba[si+1];
        buf[di+2] = atlas.rgba[si+2]; buf[di+3] = atlas.rgba[si+3];
    }
    return buf;
}

// Recolor: map source luminance → target palette (darkest to brightest)
function recolorTile(srcBuf, palette) {
    const out = Buffer.alloc(16*16*4);
    for (let i = 0; i < 16*16; i++) {
        const si = i * 4;
        const r = srcBuf[si], g = srcBuf[si+1], b = srcBuf[si+2], a = srcBuf[si+3];
        if (a === 0) { out[si]=0; out[si+1]=0; out[si+2]=0; out[si+3]=0; continue; }
        const lum = Math.round(0.299*r + 0.587*g + 0.114*b);
        const t = lum / 255;
        const idx = t * (palette.length - 1);
        const lo = Math.floor(idx);
        const hi = Math.min(lo+1, palette.length-1);
        const frac = idx - lo;
        out[si]   = Math.round(palette[lo][0]*(1-frac) + palette[hi][0]*frac);
        out[si+1] = Math.round(palette[lo][1]*(1-frac) + palette[hi][1]*frac);
        out[si+2] = Math.round(palette[lo][2]*(1-frac) + palette[hi][2]*frac);
        out[si+3] = a;
    }
    return out;
}

// ---- Load atlases ----
const ic2Atlas = decodePNG(fs.readFileSync(path.join(__dirname, '..', 'temp', 'dep_inject', 'IC2sprites', 'item_0.png')));
const vanillaAtlas = decodePNG(fs.readFileSync(path.join(__dirname, '..', 'temp', 'vanilla', 'gui', 'items.png')));

const assetsDir = path.join(__dirname, '..', 'src', 'ic2heavymachinery', 'assets', 'item');

// ---- Source shapes ----
// IC2 item_0.png layout (visually verified by extracting individual tiles):
// Row 0: c0=coal dust, c1=iron dust, c2=gold dust, c3=copper dust, c4=tin dust, c5=bronze dust,
//        c6=small crystal, c7=refined iron ingot, c8=copper ingot, c9=tin ingot, c10=bronze ingot,
//        c11=mixed metal ingot, c12=green plate, c13=rubber ball, c14=iridium, c15=RE-battery
// Row 1: cells and fuel items
// Row 2: batteries and cells
// Row 3: c0-c2=bronze tools, c3-c4=CF items, c5=lapis item, c6=wrench, c7=resin,
//        c8=treetap, c9-c10=rods, c11-c12=circuit boards, c13=screwdriver, c14-c15=cables
// Row 4: c0=clay ball, c1=coal ball, c2=cutter, c3=boots, c4=electronic circuit, c5=advanced circuit,
//        c6=chainsaw, c7=iron plate, c8=circuit board, c9=jetpack, c10=carbon fiber,
//        c11=carbon plate, c12=dark plate, c13-c15=nano saber

// Vanilla items.png: (7,0) = iron ingot (verified), (7,3) = coal

const ironDustIC2 = extractTile(ic2Atlas, 1, 0);      // iron dust shape (gray dust pile) — ROW 0!
const ironIngotVanilla = extractTile(vanillaAtlas, 7, 1); // iron ingot shape — ROW 1 col 7 (verified visually)
const ironPlateIC2 = extractTile(ic2Atlas, 7, 4);      // iron plate shape
const electronicCircuitIC2 = extractTile(ic2Atlas, 0, 6); // electronic circuit (r6,c0 — gray/neutral)
const advancedCircuitIC2 = extractTile(ic2Atlas, 1, 6);   // advanced circuit (r6,c1 — bluish center tint)

// ---- Palettes ----

// Nickel: warm gray, slightly yellowish (like real nickel)
const nickelPalette = [
    [0x34, 0x32, 0x28],
    [0x64, 0x62, 0x54],
    [0x98, 0x96, 0x88],
    [0xC8, 0xC6, 0xB8],
    [0xF0, 0xEE, 0xE0],
];

// Silver: bright white-blue, very cool and shiny
const silverPalette = [
    [0x40, 0x48, 0x58],
    [0x70, 0x7C, 0x94],
    [0xA4, 0xB4, 0xCC],
    [0xCC, 0xDC, 0xEC],
    [0xF0, 0xF8, 0xFF],
];

// Platinum: warm cream-white metallic (distinct from silver — yellowish tint)
const platinumPalette = [
    [0x48, 0x44, 0x38],
    [0x7C, 0x78, 0x64],
    [0xB0, 0xAC, 0x98],
    [0xD8, 0xD4, 0xC0],
    [0xF4, 0xF0, 0xE0],
];

// Cinnabar: deep red (mercury sulfide)
const cinnabarPalette = [
    [0x50, 0x10, 0x10],
    [0x88, 0x20, 0x18],
    [0xB8, 0x30, 0x24],
    [0xD8, 0x48, 0x38],
    [0xF0, 0x68, 0x50],
];

// Sulfur: pale yellow
const sulfurPalette = [
    [0x58, 0x54, 0x18],
    [0x88, 0x84, 0x30],
    [0xB8, 0xB4, 0x50],
    [0xD8, 0xD4, 0x70],
    [0xF0, 0xEC, 0x90],
];

// Invar: greenish-gray (iron-nickel alloy)
const invarPalette = [
    [0x28, 0x2C, 0x24],
    [0x50, 0x58, 0x4C],
    [0x80, 0x8C, 0x78],
    [0xB0, 0xBC, 0xA8],
    [0xE0, 0xEC, 0xD8],
];

// Electrum: pale gold (gold-silver alloy), lighter than gold
const electrumPalette = [
    [0x48, 0x3C, 0x18],
    [0x80, 0x74, 0x38],
    [0xB8, 0xAC, 0x60],
    [0xE0, 0xD4, 0x88],
    [0xF8, 0xF0, 0xB0],
];

// Invar plate: same palette, slightly darker overall
const invarPlatePalette = [
    [0x28, 0x2C, 0x26],
    [0x4C, 0x52, 0x48],
    [0x70, 0x78, 0x6A],
    [0x94, 0x9C, 0x8E],
    [0xB4, 0xBC, 0xAE],
];

// Platinum plate: same as platinum, slightly darker
const platinumPlatePalette = [
    [0x44, 0x48, 0x50],
    [0x70, 0x78, 0x88],
    [0x9C, 0xA8, 0xB8],
    [0xBC, 0xC8, 0xD4],
    [0xD8, 0xE0, 0xE8],
];

// ---- Hue-shift circuit recolor (IC2 style) ----
// The advanced circuit (6,1) is a red PCB. We shift its hue to make variants.
// This preserves the exact same gradient structure, saturation, and luminance.
function rgb2hsl(r, g, b) {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h, s, l = (max + min) / 2;
    if (max === min) { h = s = 0; }
    else {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
        else if (max === g) h = ((b - r) / d + 2) / 6;
        else h = ((r - g) / d + 4) / 6;
    }
    return [h, s, l];
}
function hsl2rgb(h, s, l) {
    if (s === 0) { const v = Math.round(l * 255); return [v, v, v]; }
    function hue2rgb(p, q, t) {
        if (t < 0) t += 1; if (t > 1) t -= 1;
        if (t < 1/6) return p + (q - p) * 6 * t;
        if (t < 1/2) return q;
        if (t < 2/3) return p + (q - p) * (2/3 - t) * 6;
        return p;
    }
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    return [
        Math.round(hue2rgb(p, q, h + 1/3) * 255),
        Math.round(hue2rgb(p, q, h) * 255),
        Math.round(hue2rgb(p, q, h - 1/3) * 255),
    ];
}

// Hue-shift an entire tile: shift all non-transparent pixels by targetHue (0-1)
// and optionally adjust saturation multiplier
function hueShiftTile(srcBuf, targetHue, satMul) {
    satMul = satMul || 1.0;
    const out = Buffer.alloc(16*16*4);
    for (let i = 0; i < 16*16; i++) {
        const si = i * 4;
        const r = srcBuf[si], g = srcBuf[si+1], b = srcBuf[si+2], a = srcBuf[si+3];
        if (a === 0) { out[si]=0; out[si+1]=0; out[si+2]=0; out[si+3]=0; continue; }
        const [h, s, l] = rgb2hsl(r, g, b);
        const newS = Math.min(1, s * satMul);
        const [nr, ng, nb] = hsl2rgb(targetHue, newS, l);
        out[si] = nr; out[si+1] = ng; out[si+2] = nb; out[si+3] = a;
    }
    return out;
}

// Heavy Circuit: green PCB (hue ~0.33) from red advanced circuit (hue ~0.0)
// Industrial Circuit: blue PCB (hue ~0.62) from red advanced circuit

// Solid Mercury: silvery metallic blob
const solidMercuryPalette = [
    [0x38, 0x3C, 0x44],
    [0x68, 0x70, 0x80],
    [0x98, 0xA4, 0xB8],
    [0xC0, 0xCC, 0xDC],
    [0xE0, 0xE8, 0xF0],
];

// Vulcanized Rubber: dark matte black with slight brown
const vulcanizedRubberPalette = [
    [0x18, 0x14, 0x10],
    [0x30, 0x28, 0x20],
    [0x48, 0x3C, 0x30],
    [0x58, 0x4C, 0x40],
    [0x68, 0x5C, 0x50],
];

// Coke: whitish-lilac, lighter than coal with purple tint
const cokePalette = [
    [0x50, 0x44, 0x58],
    [0x78, 0x6C, 0x84],
    [0xA0, 0x94, 0xAC],
    [0xC4, 0xB8, 0xD0],
    [0xE0, 0xD8, 0xEC],
];

// Tar: dark brown-black, glossy
const tarPalette = [
    [0x10, 0x0C, 0x08],
    [0x28, 0x1C, 0x14],
    [0x40, 0x30, 0x20],
    [0x54, 0x40, 0x2C],
    [0x68, 0x54, 0x3C],
];

// ---- Shape sources ----
// For circuits, mercury, rubber, coke, tar — use IC2 shapes
// IC2 electronic circuit shape: (0,0) - the battery? Let me check.
// Row 0: col0=RE battery, col1=charged battery, col2=energy crystal, col3=lapotron crystal,
//         col4=cell, col5=fuel cell, col6=uranium cell, ...
// The circuit shapes are somewhere in row 0 or row 2.
// Looking at IC2 atlas more carefully:
// (6,0) = electronic circuit (green chip), (7,0) = advanced circuit (blue chip)?
// Actually from the image: row 0 seems to have batteries/cells/crystals
// Let me use (5,0) or nearby for circuits

// For items that don't have a clear IC2 shape match, I'll use the vanilla shapes:
// Ingot shape for: all ingots
// Dust shape for: all dusts and blends
// Ingot shape (slightly modified) for: plates, circuits, mercury, rubber, coke, tar

// ---- Vanilla reference shapes ----
// vanilla (7,0) = iron ingot  -- but let me double-check by looking at pixel data
// Actually in Beta 1.7.3 items.png the layout is:
// Row 0: iron_shovel, iron_pick, iron_axe, flint_and_steel, apple, bow, arrow,
//         ...the ingots might be elsewhere
// Let me look at row 1: col 7,8 area

// From the vanilla image I saw:
// Row 0 col 7 seems like a bar shape. Let me trust what we extracted before - it worked for invar.

// For items without a perfect shape match, use these vanilla shapes:
// redstone dust at (8, 3) in vanilla? Or just use the IC2 iron dust for all dusts.
// coal at (7, 0)? No, coal is somewhere else.
// Let me use vanilla (10, 3) = coal for coke shape, (7, 0) for ingots

// Actually let's be practical: for special items (mercury, rubber, coke, tar, circuits),
// I'll use custom pixel art since they're unique items, not recolors.

function px(buf, x, y, r, g, b, a) {
    if (x<0||x>15||y<0||y>15) return;
    const i=(y*16+x)*4;
    buf[i]=r; buf[i+1]=g; buf[i+2]=b; buf[i+3]=a!==undefined?a:255;
}

// ---- Generate all items ----
const items = [];

function gen(name, srcTile, palette) {
    const result = recolorTile(srcTile, palette);
    fs.writeFileSync(path.join(assetsDir, name), encodePNG(16, 16, result));
    items.push(name);
}

// === DUSTS (from IC2 iron dust shape) ===
gen('ic2hm_nickel_dust.png', ironDustIC2, nickelPalette);
gen('ic2hm_silver_dust.png', ironDustIC2, platinumPalette);
gen('ic2hm_platinum_dust.png', ironDustIC2, silverPalette);
gen('ic2hm_cinnabar_dust.png', ironDustIC2, cinnabarPalette);
gen('ic2hm_sulfur_dust.png', ironDustIC2, sulfurPalette);
gen('ic2hm_invar_blend.png', ironDustIC2, invarPalette);
gen('ic2hm_electrum_blend.png', ironDustIC2, electrumPalette);

// === INGOTS (from vanilla iron ingot shape) ===
gen('ic2hm_nickel_ingot.png', ironIngotVanilla, nickelPalette);
gen('ic2hm_silver_ingot.png', ironIngotVanilla, platinumPalette);
gen('ic2hm_platinum_ingot.png', ironIngotVanilla, silverPalette);
gen('ic2hm_invar_ingot.png', ironIngotVanilla, invarPalette);
gen('ic2hm_electrum_ingot.png', ironIngotVanilla, electrumPalette);

// === PLATES (from IC2 iron plate shape) ===
gen('ic2hm_invar_plate.png', ironPlateIC2, invarPlatePalette);
gen('ic2hm_platinum_plate.png', ironPlateIC2, platinumPlatePalette);

// === CIRCUITS (selective recolor: pins vs PCB body) ===
// In the advanced circuit (r6,c1), pixels with R=255,G=0,B=0 are the pin tips.
// All other opaque pixels are the PCB body (bluish tones).
function makeCircuit(srcBuf, bodyHue, bodySatMul, pinColor) {
    const out = Buffer.alloc(16*16*4);
    for (let i = 0; i < 16*16; i++) {
        const si = i * 4;
        const r = srcBuf[si], g = srcBuf[si+1], b = srcBuf[si+2], a = srcBuf[si+3];
        if (a === 0) { out[si]=0; out[si+1]=0; out[si+2]=0; out[si+3]=0; continue; }
        // Pin tips: pure red (255,0,0)
        if (r === 255 && g === 0 && b === 0) {
            out[si] = pinColor[0]; out[si+1] = pinColor[1]; out[si+2] = pinColor[2]; out[si+3] = 255;
        } else {
            // PCB body: hue-shift
            const [h, s, l] = rgb2hsl(r, g, b);
            const newS = Math.min(1, s * bodySatMul);
            const [nr, ng, nb] = hsl2rgb(bodyHue, newS, l);
            out[si] = nr; out[si+1] = ng; out[si+2] = nb; out[si+3] = a;
        }
    }
    return out;
}
// Heavy Circuit: green PCB body + red pin tips (like IC2 style)
saveCustom('ic2hm_heavy_circuit.png', makeCircuit(advancedCircuitIC2, 0.33, 1.0, [255, 0, 0]));
// Industrial Circuit: dark gray PCB body (darkened) + red pin tips — alien tech vibe
function makeIndustrialCircuit(srcBuf) {
    const out = Buffer.alloc(16*16*4);
    for (let i = 0; i < 16*16; i++) {
        const si = i * 4;
        const r = srcBuf[si], g = srcBuf[si+1], b = srcBuf[si+2], a = srcBuf[si+3];
        if (a === 0) { out[si]=0; out[si+1]=0; out[si+2]=0; out[si+3]=0; continue; }
        if (r === 255 && g === 0 && b === 0) {
            // Pin tips: red (like IC2 originals)
            out[si] = 255; out[si+1] = 0; out[si+2] = 0; out[si+3] = 255;
        } else {
            // PCB body: convert to very dark gray with slight purple tint
            const lum = Math.round(0.299*r + 0.587*g + 0.114*b);
            // Darken (0.50 factor) and add subtle purple
            const dark = Math.round(lum * 0.50);
            out[si]   = Math.min(255, dark + 8);  // slight red
            out[si+1] = dark;
            out[si+2] = Math.min(255, dark + 5);  // slight blue
            out[si+3] = a;
        }
    }
    return out;
}
saveCustom('ic2hm_industrial_circuit.png', makeIndustrialCircuit(advancedCircuitIC2));

// === SPECIAL ITEMS (hand-drawn pixel art) ===

// -- Solid Mercury: small metallic droplet/ball --
function makeSolidMercury() {
    const buf = Buffer.alloc(16*16*4);
    // Sphere/droplet shape, very reflective
    const c = [
        [0x58,0x60,0x70],[0x78,0x84,0x98],[0x98,0xA8,0xC0],
        [0xB8,0xC8,0xDC],[0xD8,0xE4,0xF0],[0xE8,0xF0,0xF8]
    ];
    // Main body (7x7 centered at 7.5, 8.5 — rows 5-11, cols 5-10)
    // Row 5-6: top of sphere
    px(buf,7,5,  ...c[2]); px(buf,8,5,  ...c[1]);
    px(buf,6,6,  ...c[2]); px(buf,7,6,  ...c[4]); px(buf,8,6, ...c[3]); px(buf,9,6, ...c[1]);
    // Row 7-8: middle
    px(buf,5,7,  ...c[1]); px(buf,6,7,  ...c[3]); px(buf,7,7,  ...c[5]); px(buf,8,7, ...c[4]); px(buf,9,7, ...c[2]); px(buf,10,7,...c[0]);
    px(buf,5,8,  ...c[1]); px(buf,6,8,  ...c[3]); px(buf,7,8,  ...c[4]); px(buf,8,8, ...c[3]); px(buf,9,8, ...c[2]); px(buf,10,8,...c[0]);
    // Row 9-10: bottom
    px(buf,6,9,  ...c[1]); px(buf,7,9,  ...c[2]); px(buf,8,9, ...c[2]); px(buf,9,9, ...c[1]);
    px(buf,7,10, ...c[0]); px(buf,8,10, ...c[0]);

    return buf;
}

// -- Vulcanized Rubber: dark rubber sheet/piece --
function makeVulcanizedRubber() {
    const buf = Buffer.alloc(16*16*4);
    const dk = [0x20,0x1C,0x18];
    const md = [0x38,0x30,0x28];
    const lt = [0x50,0x44,0x38];
    const hi = [0x60,0x54,0x48];
    // Irregular rubber piece shape (like IC2 rubber but darker)
    // Body: rows 5-11, cols 4-11
    for (let y=6;y<=10;y++) for (let x=5;x<=10;x++) px(buf,x,y,...md);
    // Edges
    for (let x=5;x<=10;x++) px(buf,x,5,...lt);
    for (let x=5;x<=10;x++) px(buf,x,11,...dk);
    for (let y=5;y<=11;y++) px(buf,4,y,...lt);
    for (let y=5;y<=11;y++) px(buf,11,y,...dk);
    // Highlight streak (rubber sheen)
    px(buf,6,6,...hi); px(buf,7,6,...hi); px(buf,8,7,...hi);
    // Rounded corners
    px(buf,4,5, 0,0,0,0); px(buf,11,5, 0,0,0,0);
    px(buf,4,11, 0,0,0,0); px(buf,11,11, 0,0,0,0);
    // Sulfur specks (tiny yellow dots)
    px(buf,7,8, 0x60,0x58,0x20); px(buf,9,9, 0x58,0x50,0x1C);

    return buf;
}

// -- Coke: based on vanilla coal shape (r0,c7) --
function makeCoke() {
    const coalVanilla = extractTile(vanillaAtlas, 7, 0);
    return recolorTile(coalVanilla, cokePalette);
}

// -- Tar: dark droplet/blob --
function makeTar() {
    const buf = Buffer.alloc(16*16*4);
    const c0 = [0x14,0x0E,0x08];
    const c1 = [0x28,0x1C,0x14];
    const c2 = [0x3C,0x2C,0x20];
    const c3 = [0x50,0x3C,0x2C];
    const hi = [0x64,0x50,0x3C];
    // Droplet shape
    px(buf,7,4,...c2); px(buf,8,4,...c1);
    px(buf,6,5,...c2); px(buf,7,5,...c3); px(buf,8,5,...c2); px(buf,9,5,...c1);
    px(buf,5,6,...c2); px(buf,6,6,...c3); px(buf,7,6,...hi); px(buf,8,6,...c3); px(buf,9,6,...c2); px(buf,10,6,...c1);
    for (let x=5;x<=10;x++) px(buf,x,7, ...c2);
    px(buf,6,7,...c3); px(buf,7,7,...c3);
    for (let x=5;x<=10;x++) px(buf,x,8, ...c1);
    px(buf,6,8,...c2); px(buf,7,8,...c2);
    for (let x=6;x<=9;x++) px(buf,x,9, ...c1);
    for (let x=6;x<=9;x++) px(buf,x,10, ...c0);
    px(buf,7,11,...c0); px(buf,8,11,...c0);

    return buf;
}

// Save special items
function saveCustom(name, buf) {
    fs.writeFileSync(path.join(assetsDir, name), encodePNG(16, 16, buf));
    items.push(name);
}

saveCustom('ic2hm_solid_mercury.png', makeSolidMercury());
saveCustom('ic2hm_vulcanized_rubber.png', makeVulcanizedRubber());
saveCustom('ic2hm_coke.png', makeCoke());
saveCustom('ic2hm_tar.png', makeTar());

console.log(`Generated ${items.length} item textures:`);
items.forEach(n => console.log(`  ${n}`));
console.log('\nDone!');
