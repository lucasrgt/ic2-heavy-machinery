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

function decodePNG(b) {
    let p=8,w,h,bd,ct;const ids=[];
    while(p<b.length){const l=b.readUInt32BE(p);const t=b.slice(p+4,p+8).toString('ascii');const d=b.slice(p+8,p+8+l);
    if(t==='IHDR'){w=d.readUInt32BE(0);h=d.readUInt32BE(4);bd=d[8];ct=d[9];}else if(t==='IDAT')ids.push(d);else if(t==='IEND')break;p+=12+l;}
    const raw=zlib.inflateSync(Buffer.concat(ids));const bpp=ct===6?4:ct===2?3:ct===0?1:ct===4?2:4;const stride=w*bpp;
    const rgba=Buffer.alloc(w*h*4);const uf=Buffer.alloc(h*stride);
    for(let y=0;y<h;y++){const ft=raw[y*(1+stride)];const rs=y*(1+stride)+1;const os=y*stride;
    for(let i=0;i<stride;i++){const x=raw[rs+i];const a=i>=bpp?uf[os+i-bpp]:0;const b2=y>0?uf[os-stride+i]:0;const c=(y>0&&i>=bpp)?uf[os-stride+i-bpp]:0;
    let v;switch(ft){case 0:v=x;break;case 1:v=(x+a)&0xff;break;case 2:v=(x+b2)&0xff;break;case 3:v=(x+Math.floor((a+b2)/2))&0xff;break;
    case 4:{const pp=a+b2-c;const pa=Math.abs(pp-a),pb=Math.abs(pp-b2),pc=Math.abs(pp-c);v=(x+(pa<=pb&&pa<=pc?a:pb<=pc?b2:c))&0xff;break;}default:v=x;}uf[os+i]=v;}}
    for(let y=0;y<h;y++)for(let x=0;x<w;x++){const si=y*stride+x*bpp;const di=(y*w+x)*4;
    if(ct===6){rgba[di]=uf[si];rgba[di+1]=uf[si+1];rgba[di+2]=uf[si+2];rgba[di+3]=uf[si+3];}
    else if(ct===2){rgba[di]=uf[si];rgba[di+1]=uf[si+1];rgba[di+2]=uf[si+2];rgba[di+3]=255;}
    else if(ct===0){rgba[di]=rgba[di+1]=rgba[di+2]=uf[si];rgba[di+3]=255;}
    else if(ct===4){rgba[di]=rgba[di+1]=rgba[di+2]=uf[si];rgba[di+3]=uf[si+1];}}
    return {width:w,height:h,rgba};
}

const terrain = decodePNG(fs.readFileSync(path.join(__dirname, '..', '..', 'temp', 'merged', 'terrain.png')));

function extractTile(src, tileX, tileY) {
    const buf = Buffer.alloc(16*16*4);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
        const si = ((tileY*16+y)*src.width + tileX*16+x)*4;
        const di = (y*16+x)*4;
        buf[di]=src.rgba[si]; buf[di+1]=src.rgba[si+1]; buf[di+2]=src.rgba[si+2]; buf[di+3]=src.rgba[si+3];
    }
    return buf;
}

const stone = extractTile(terrain, 1, 0);

function px(buf, x, y, r, g, b) {
    if (x<0||x>15||y<0||y>15) return;
    const i = (y*16+x)*4;
    buf[i]=r; buf[i+1]=g; buf[i+2]=b; buf[i+3]=255;
}

function makeOre(pattern) {
    const buf = Buffer.from(stone);
    for (const [x, y, r, g, b] of pattern) {
        px(buf, x, y, r, g, b);
    }
    return buf;
}

// Extract ore pattern from terrain.png tile and recolor it
// Compares each pixel to stone — if different, it's an ore pixel
// Maps ore pixel brightness to a palette gradient (darkest→brightest)
function recolorOre(tileX, tileY, palette) {
    const oreTile = extractTile(terrain, tileX, tileY);
    const buf = Buffer.from(stone);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
        const i = (y*16+x)*4;
        const sr = stone[i], sg = stone[i+1], sb = stone[i+2];
        const or2 = oreTile[i], og = oreTile[i+1], ob = oreTile[i+2];
        // If pixel differs from stone, it's an ore pixel
        if (or2 !== sr || og !== sg || ob !== sb) {
            // Compute brightness of original ore pixel (0-255)
            const bright = (or2 * 0.299 + og * 0.587 + ob * 0.114);
            // Map to palette index using min/max normalization for better spread
            if (!recolorOre._ranges) recolorOre._ranges = {};
            const key = `${tileX},${tileY}`;
            if (!recolorOre._ranges[key]) {
                let mn=255,mx=0;
                for (let yy=0;yy<16;yy++) for (let xx=0;xx<16;xx++) {
                    const ii=(yy*16+xx)*4;
                    if (oreTile[ii]!==stone[ii]||oreTile[ii+1]!==stone[ii+1]||oreTile[ii+2]!==stone[ii+2]) {
                        const b2=oreTile[ii]*0.299+oreTile[ii+1]*0.587+oreTile[ii+2]*0.114;
                        if(b2<mn)mn=b2;if(b2>mx)mx=b2;
                    }
                }
                recolorOre._ranges[key]={mn,mx};
            }
            const {mn,mx} = recolorOre._ranges[key];
            const norm = mx>mn ? (bright-mn)/(mx-mn) : 0.5;
            const idx = Math.min(palette.length - 1, Math.floor(norm * palette.length));
            buf[i] = palette[idx][0];
            buf[i+1] = palette[idx][1];
            buf[i+2] = palette[idx][2];
            buf[i+3] = 255;
        }
    }
    return buf;
}

// === NICKEL ORE — Gold ore pattern recolored to brassy olive-yellow ===
// Uses vanilla gold ore shape (terrain tile 0,2) — better shading than iron
const nickelOre = recolorOre(0, 2, [
    [0xA0, 0x94, 0x70], // warm tan shadow (stone-like warmth)
    [0xB8, 0xA8, 0x78], // sandy tan
    [0xCC, 0xBC, 0x80], // warm beige
    [0xDC, 0xCC, 0x88], // pale gold
    [0xE8, 0xD8, 0x90], // soft gold
    [0xF0, 0xE4, 0x9C], // warm highlight
]);

// === SILVER ORE — Gold ore pattern recolored to bright silver-white ===
// Uses vanilla gold ore shape (terrain tile 0,2) — same as old platinum
const silverOre = recolorOre(0, 2, [
    [0xA8, 0xB0, 0xB8], // soft grey shadow
    [0xBC, 0xC4, 0xCC], // light silver
    [0xCC, 0xD4, 0xDC], // bright silver
    [0xDC, 0xE4, 0xEC], // pale silver
    [0xE8, 0xEE, 0xF4], // near-white
    [0xF4, 0xF8, 0xFC], // brilliant white highlight
]);

// === PLATINUM ORE — Gold ore pattern recolored to blue-purple tint ===
// Uses vanilla gold ore shape (terrain tile 0,2) — icy blue-violet sparkle
const platinumOre = recolorOre(0, 2, [
    [0x90, 0x98, 0xC0], // muted blue-purple shadow
    [0xA8, 0xB0, 0xD8], // soft lavender
    [0xBC, 0xC4, 0xE8], // light blue-violet
    [0xCC, 0xD4, 0xF0], // pale periwinkle
    [0xDC, 0xE0, 0xF8], // icy lavender
    [0xEC, 0xEE, 0xFF], // white-violet highlight
]);

// === CINNABAR ORE — Lapis ore pattern recolored to vivid red/bordeaux ===
// Uses vanilla lapis ore shape (terrain tile 0,10) with more color variation
const cinnabarOre = recolorOre(0, 10, [
    [0x48, 0x08, 0x04], // near-black bordeaux
    [0x70, 0x10, 0x0C], // deep dark crimson
    [0x98, 0x1C, 0x14], // dark red
    [0xC0, 0x2C, 0x20], // crimson
    [0xE0, 0x40, 0x2C], // bright vermillion
    [0xF0, 0x58, 0x3C], // orange-red highlight
    [0xF8, 0x70, 0x50], // warm highlight
]);

const assetsDir = path.join(__dirname, '..', 'src', 'ic2heavymachinery', 'assets', 'blocks');

fs.writeFileSync(path.join(assetsDir, 'nickel_ore.png'), encodePNG(16, 16, nickelOre));
console.log('nickel_ore.png — angular chunky fragments (pentlandite)');

fs.writeFileSync(path.join(assetsDir, 'silver_ore.png'), encodePNG(16, 16, silverOre));
console.log('silver_ore.png — diagonal wire veins (native silver)');

fs.writeFileSync(path.join(assetsDir, 'platinum_ore.png'), encodePNG(16, 16, platinumOre));
console.log('platinum_ore.png — tiny diamond crystals (sperrylite)');

fs.writeFileSync(path.join(assetsDir, 'cinnabar_ore.png'), encodePNG(16, 16, cinnabarOre));
console.log('cinnabar_ore.png — large rhombus prisms (cinnabar)');

console.log('Done!');
