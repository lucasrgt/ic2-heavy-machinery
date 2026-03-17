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

// Extract a 16x16 tile from an atlas
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

// Recolor a tile: map luminance of source to a target palette
// palette = array of [r,g,b] from darkest to brightest (4-5 entries)
function recolorTile(srcBuf, palette) {
    const out = Buffer.alloc(16*16*4);
    for (let i = 0; i < 16*16; i++) {
        const si = i * 4;
        const r = srcBuf[si], g = srcBuf[si+1], b = srcBuf[si+2], a = srcBuf[si+3];
        if (a === 0) {
            // Transparent — keep transparent
            out[si] = 0; out[si+1] = 0; out[si+2] = 0; out[si+3] = 0;
            continue;
        }
        // Compute luminance (0..255)
        const lum = Math.round(0.299 * r + 0.587 * g + 0.114 * b);
        // Map luminance to palette index
        const t = lum / 255; // 0..1
        const idx = t * (palette.length - 1);
        const lo = Math.floor(idx);
        const hi = Math.min(lo + 1, palette.length - 1);
        const frac = idx - lo;
        // Interpolate between palette entries
        out[si]   = Math.round(palette[lo][0] * (1-frac) + palette[hi][0] * frac);
        out[si+1] = Math.round(palette[lo][1] * (1-frac) + palette[hi][1] * frac);
        out[si+2] = Math.round(palette[lo][2] * (1-frac) + palette[hi][2] * frac);
        out[si+3] = a;
    }
    return out;
}

// ---- Load atlases ----
const ic2Atlas = decodePNG(fs.readFileSync(path.join(__dirname, '..', 'temp', 'dep_inject', 'IC2sprites', 'item_0.png')));
const vanillaAtlas = decodePNG(fs.readFileSync(path.join(__dirname, '..', 'temp', 'vanilla', 'gui', 'items.png')));

const assetsDir = path.join(__dirname, '..', 'src', 'ic2heavymachinery', 'assets', 'item');

// ---- Identify source tiles ----
// IC2 item_0.png: iron dust is at (1, 1) — row 1, col 1 (the gray dust pile)
// Vanilla items.png: iron ingot is at (7, 0) — row 0, col 7
const ironDustTile = extractTile(ic2Atlas, 1, 1);
const ironIngotTile = extractTile(vanillaAtlas, 7, 0);

// ---- Invar palette ----
// Invar = iron-nickel alloy (64% Fe, 36% Ni)
// Color: slightly warmer/darker than steel, with a subtle greenish-gray tint
// Distinct from pure iron (blue-gray) and nickel (yellowish-gray)
const invarPalette = [
    [0x30, 0x34, 0x2E],  // darkest — deep greenish gray
    [0x58, 0x5E, 0x54],  // dark
    [0x7C, 0x84, 0x76],  // mid — the characteristic invar green-gray
    [0xA0, 0xA8, 0x9A],  // light
    [0xC4, 0xCC, 0xBE],  // highlight — pale greenish
];

// ---- Generate Invar Blend (from iron dust shape) ----
const invarBlend = recolorTile(ironDustTile, invarPalette);
fs.writeFileSync(path.join(assetsDir, 'ic2hm_invar_blend.png'), encodePNG(16, 16, invarBlend));
console.log('ic2hm_invar_blend.png (invar blend from iron dust shape)');

// ---- Generate Invar Ingot (from iron ingot shape) ----
const invarIngot = recolorTile(ironIngotTile, invarPalette);
fs.writeFileSync(path.join(assetsDir, 'ic2hm_invar_ingot.png'), encodePNG(16, 16, invarIngot));
console.log('ic2hm_invar_ingot.png (invar ingot from iron ingot shape)');

console.log('\nDone!');
