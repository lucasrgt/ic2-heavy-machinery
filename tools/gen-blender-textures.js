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
    const raw=zlib.inflateSync(Buffer.concat(ids));const bpp=ct===6?4:ct===2?3:1;const stride=w*bpp;
    const rgba=Buffer.alloc(w*h*4);const uf=Buffer.alloc(h*stride);
    for(let y=0;y<h;y++){const ft=raw[y*(1+stride)];const rs=y*(1+stride)+1;const os=y*stride;
    for(let i=0;i<stride;i++){const x=raw[rs+i];const a=i>=bpp?uf[os+i-bpp]:0;const b=y>0?uf[os-stride+i]:0;const c=(y>0&&i>=bpp)?uf[os-stride+i-bpp]:0;
    let v;switch(ft){case 0:v=x;break;case 1:v=(x+a)&0xff;break;case 2:v=(x+b)&0xff;break;case 3:v=(x+Math.floor((a+b)/2))&0xff;break;
    case 4:{const p=a+b-c;const pa=Math.abs(p-a),pb=Math.abs(p-b),pc=Math.abs(p-c);v=(x+(pa<=pb&&pa<=pc?a:pb<=pc?b:c))&0xff;break;}default:v=x;}uf[os+i]=v;}}
    for(let y=0;y<h;y++)for(let x=0;x<w;x++){const si=y*stride+x*bpp;const di=(y*w+x)*4;
    if(ct===6){rgba[di]=uf[si];rgba[di+1]=uf[si+1];rgba[di+2]=uf[si+2];rgba[di+3]=uf[si+3];}
    else if(ct===2){rgba[di]=uf[si];rgba[di+1]=uf[si+1];rgba[di+2]=uf[si+2];rgba[di+3]=255;}}
    return{width:w,height:h,rgba};
}

const assetsDir = path.join(__dirname, '..', 'src', 'ic2heavymachinery', 'assets', 'blocks');
const atlasBuf = decodePNG(fs.readFileSync(path.join(assetsDir, 'ic2_heavy_machinery.png')));

function extractTileFromAtlas(index) {
    const buf = Buffer.alloc(16*16*4);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
        const si = (y * atlasBuf.width + (index * 16 + x)) * 4;
        const di = (y * 16 + x) * 4;
        buf[di] = atlasBuf.rgba[si]; buf[di+1] = atlasBuf.rgba[si+1];
        buf[di+2] = atlasBuf.rgba[si+2]; buf[di+3] = atlasBuf.rgba[si+3];
    }
    return buf;
}

const baseSide = extractTileFromAtlas(0);  // heavy_chassis
const baseTop  = extractTileFromAtlas(1);  // base_top

function px(buf, x, y, r, g, b, a) {
    if (x < 0 || x > 15 || y < 0 || y > 15) return;
    const i = (y * 16 + x) * 4;
    buf[i] = r; buf[i+1] = g; buf[i+2] = b; buf[i+3] = a !== undefined ? a : 255;
}

function getPx(buf, x, y) {
    if (x < 0 || x > 15 || y < 0 || y > 15) return [0,0,0,0];
    const i = (y * 16 + x) * 4;
    return [buf[i], buf[i+1], buf[i+2], buf[i+3]];
}

function blend(buf, x, y, r2, g2, b2, alpha) {
    const [r1, g1, b1, a1] = getPx(buf, x, y);
    const a = alpha / 255;
    px(buf, x, y,
        Math.round(r1*(1-a) + r2*a),
        Math.round(g1*(1-a) + g2*a),
        Math.round(b1*(1-a) + b2*a), a1);
}

// ============================================================
// ALLOY BLENDER FRONT
//
// Concept: Heavy chassis base with a large mixing chamber window.
// The chamber takes up most of the face (cols 2-13, rows 3-12).
// Inside: a crucible with molten material when active.
// Clean IC2 style — beveled frame, functional look.
// No chutes on the front — those are on top only.
// ============================================================

function makeFront(frame) {
    const buf = Buffer.from(baseSide);

    // -- Mixing chamber frame (cols 2-13, rows 3-12) --
    // Top edge (highlight)
    for (let x = 2; x <= 13; x++) px(buf, x, 3, 0xB0, 0xB0, 0xB8);
    // Bottom edge (shadow)
    for (let x = 2; x <= 13; x++) px(buf, x, 12, 0x50, 0x50, 0x58);
    // Left edge (highlight)
    for (let y = 3; y <= 12; y++) px(buf, 2, y, 0xA8, 0xA8, 0xB0);
    // Right edge (shadow)
    for (let y = 3; y <= 12; y++) px(buf, 13, y, 0x58, 0x58, 0x60);
    // Corners
    px(buf, 2, 3, 0xB8, 0xB8, 0xC0);
    px(buf, 13, 3, 0x80, 0x80, 0x88);
    px(buf, 2, 12, 0x78, 0x78, 0x80);
    px(buf, 13, 12, 0x48, 0x48, 0x50);

    // -- Inner bevel (second ring) --
    for (let x = 3; x <= 12; x++) px(buf, x, 4, 0x50, 0x50, 0x58); // shadow top
    for (let y = 5; y <= 11; y++) px(buf, 3, y, 0x48, 0x48, 0x50); // shadow left
    for (let x = 3; x <= 12; x++) px(buf, x, 11, 0x78, 0x78, 0x80); // highlight bottom
    for (let y = 4; y <= 10; y++) px(buf, 12, y, 0x80, 0x80, 0x88); // highlight right
    px(buf, 3, 4, 0x44, 0x44, 0x4C);
    px(buf, 12, 11, 0x88, 0x88, 0x90);

    // -- Dark cavity interior (cols 4-11, rows 5-10) --
    for (let y = 5; y <= 10; y++) for (let x = 4; x <= 11; x++) {
        px(buf, x, y, 0x30, 0x30, 0x38);
    }
    // Depth gradient — darker top-left
    for (let x = 4; x <= 11; x++) px(buf, x, 5, 0x28, 0x28, 0x30);
    for (let y = 5; y <= 10; y++) px(buf, 4, y, 0x2C, 0x2C, 0x34);

    // -- Content based on state --
    if (frame === -1) {
        // OFF: empty chamber. Horizontal mixing bar resting in center.
        for (let x = 5; x <= 10; x++) px(buf, x, 7, 0x60, 0x60, 0x68);
        for (let x = 5; x <= 10; x++) px(buf, x, 8, 0x50, 0x50, 0x58);
        // Axle ends
        px(buf, 4, 7, 0x70, 0x70, 0x78); px(buf, 4, 8, 0x60, 0x60, 0x68);
        px(buf, 11, 7, 0x70, 0x70, 0x78); px(buf, 11, 8, 0x60, 0x60, 0x68);
    } else {
        // ACTIVE: molten material in bottom half, mixing bar, heat glow

        // Molten pool (rows 8-10)
        const moltenSurface = [ // row 8 — surface, brightest
            [0xE8,0x90,0x18],[0xF0,0xA0,0x20],[0xE0,0x88,0x18],[0xE8,0x98,0x1C],
            [0xF0,0xA8,0x20],[0xE8,0x90,0x18],[0xF0,0xA0,0x1C],[0xE0,0x88,0x18]
        ];
        const moltenMid = [ // row 9 — mid
            [0xC8,0x70,0x10],[0xD0,0x78,0x14],[0xC0,0x68,0x10],[0xC8,0x74,0x12],
            [0xD0,0x7C,0x14],[0xC8,0x70,0x10],[0xD0,0x78,0x12],[0xC0,0x6C,0x10]
        ];
        const moltenDeep = [ // row 10 — bottom, darkest
            [0xA0,0x50,0x08],[0xA8,0x58,0x0C],[0x98,0x48,0x08],[0xA0,0x54,0x0A],
            [0xA8,0x5C,0x0C],[0xA0,0x50,0x08],[0xA8,0x58,0x0A],[0x98,0x4C,0x08]
        ];

        const shift = frame;
        for (let i = 0; i < 8; i++) {
            const ci = (i + shift) % 8;
            px(buf, 4+i, 8, ...moltenSurface[ci]);
            px(buf, 4+i, 9, ...moltenMid[(ci+1)%8]);
            px(buf, 4+i, 10, ...moltenDeep[(ci+2)%8]);
        }

        // Heat shimmer above surface (row 7)
        const shimmerAlpha = [45, 65, 55, 70, 40, 60, 50, 55];
        for (let i = 0; i < 8; i++) {
            blend(buf, 4+i, 7, 0xE0, 0x90, 0x18, shimmerAlpha[(i+shift)%8]);
        }
        // Fainter shimmer row 6
        for (let i = 0; i < 8; i++) {
            blend(buf, 4+i, 6, 0xD0, 0x80, 0x10, Math.floor(shimmerAlpha[(i+shift+2)%8] * 0.35));
        }

        // Mixing bar — alternates position for animation
        if (frame === 0 || frame === 2) {
            for (let x = 5; x <= 10; x++) px(buf, x, 7, 0xD0, 0xC0, 0x90);
            px(buf, 4, 7, 0xB0, 0xA0, 0x78); px(buf, 11, 7, 0xB0, 0xA0, 0x78);
        } else {
            for (let x = 5; x <= 10; x++) px(buf, x, 6, 0xD0, 0xC0, 0x90);
            px(buf, 4, 6, 0xB0, 0xA0, 0x78); px(buf, 11, 6, 0xB0, 0xA0, 0x78);
        }

        // Ambient glow on chamber walls
        for (let y = 5; y <= 7; y++) {
            blend(buf, 4, y, 0xC0, 0x60, 0x10, 25);
            blend(buf, 11, y, 0xC0, 0x60, 0x10, 20);
        }
    }

    return buf;
}

// ============================================================
// ALLOY BLENDER TOP
//
// Concept: base_top with a single wide rectangular opening
// running across the center (the input hopper).
// Simple, clean — just a slot/opening in the machine top
// where you pour materials in.
// ============================================================

function makeTop() {
    const buf = Buffer.from(baseTop);

    // Single wide hopper opening (cols 3-12, rows 5-10)
    // Frame
    for (let x = 3; x <= 12; x++) px(buf, x, 5, 0xA8, 0xA8, 0xB0); // top highlight
    for (let x = 3; x <= 12; x++) px(buf, x, 10, 0x58, 0x58, 0x60); // bottom shadow
    for (let y = 5; y <= 10; y++) px(buf, 3, y, 0xA0, 0xA0, 0xA8);  // left highlight
    for (let y = 5; y <= 10; y++) px(buf, 12, y, 0x60, 0x60, 0x68); // right shadow
    // Corners
    px(buf, 3, 5, 0xB0, 0xB0, 0xB8); px(buf, 12, 5, 0x80, 0x80, 0x88);
    px(buf, 3, 10, 0x78, 0x78, 0x80); px(buf, 12, 10, 0x50, 0x50, 0x58);

    // Dark interior
    for (let y = 6; y <= 9; y++) for (let x = 4; x <= 11; x++) {
        px(buf, x, y, 0x24, 0x24, 0x2C);
    }
    // Depth shading — darker top-left
    for (let x = 4; x <= 11; x++) px(buf, x, 6, 0x1C, 0x1C, 0x24);
    for (let y = 6; y <= 9; y++) px(buf, 4, y, 0x20, 0x20, 0x28);
    // Light reflection bottom-right
    for (let x = 5; x <= 11; x++) px(buf, x, 9, 0x30, 0x30, 0x38);
    for (let y = 7; y <= 8; y++) px(buf, 11, y, 0x2C, 0x2C, 0x34);

    // Center divider (thin bar splitting the opening into left/right input)
    for (let y = 6; y <= 9; y++) {
        px(buf, 7, y, 0x70, 0x70, 0x78);
        px(buf, 8, y, 0x60, 0x60, 0x68);
    }
    // Divider connects to frame
    px(buf, 7, 5, 0x90, 0x90, 0x98); px(buf, 8, 5, 0x80, 0x80, 0x88);
    px(buf, 7, 10, 0x60, 0x60, 0x68); px(buf, 8, 10, 0x50, 0x50, 0x58);

    return buf;
}

// ============================================================
// SAVE ALL
// ============================================================

fs.writeFileSync(path.join(assetsDir, 'alloy_blender_front.png'), encodePNG(16, 16, makeFront(-1)));
console.log('alloy_blender_front.png (off)');

for (let i = 0; i < 4; i++) {
    fs.writeFileSync(path.join(assetsDir, `alloy_blender_front_active_${i}.png`), encodePNG(16, 16, makeFront(i)));
    console.log(`alloy_blender_front_active_${i}.png`);
}

fs.writeFileSync(path.join(assetsDir, 'alloy_blender_top.png'), encodePNG(16, 16, makeTop()));
console.log('alloy_blender_top.png');

console.log('\nDone! 6 textures generated.');
