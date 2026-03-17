const fs = require('fs'), zlib = require('zlib');

function decodePNG(b) {
  let p = 8, w, h, ct;
  const ids = [];
  while (p < b.length) {
    const l = b.readUInt32BE(p);
    const t = b.slice(p + 4, p + 8).toString('ascii');
    const d = b.slice(p + 8, p + 8 + l);
    if (t === 'IHDR') { w = d.readUInt32BE(0); h = d.readUInt32BE(4); ct = d[9]; }
    else if (t === 'IDAT') ids.push(d);
    else if (t === 'IEND') break;
    p += 12 + l;
  }
  const raw = zlib.inflateSync(Buffer.concat(ids));
  const bpp = ct === 6 ? 4 : ct === 2 ? 3 : 1;
  const stride = w * bpp;
  const uf = Buffer.alloc(h * stride);
  for (let y = 0; y < h; y++) {
    const ft = raw[y * (1 + stride)];
    const rs = y * (1 + stride) + 1;
    const os = y * stride;
    for (let i = 0; i < stride; i++) {
      const x = raw[rs + i];
      const a = i >= bpp ? uf[os + i - bpp] : 0;
      const b2 = y > 0 ? uf[os - stride + i] : 0;
      const c = (y > 0 && i >= bpp) ? uf[os - stride + i - bpp] : 0;
      let v;
      switch (ft) {
        case 0: v = x; break;
        case 1: v = (x + a) & 0xff; break;
        case 2: v = (x + b2) & 0xff; break;
        case 3: v = (x + Math.floor((a + b2) / 2)) & 0xff; break;
        case 4: {
          const p2 = a + b2 - c;
          const pa = Math.abs(p2 - a), pb = Math.abs(p2 - b2), pc = Math.abs(p2 - c);
          v = (x + (pa <= pb && pa <= pc ? a : pb <= pc ? b2 : c)) & 0xff;
          break;
        }
        default: v = x;
      }
      uf[os + i] = v;
    }
  }
  const rgba = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const si = y * stride + x * bpp;
    const di = (y * w + x) * 4;
    if (ct === 6) { rgba[di] = uf[si]; rgba[di+1] = uf[si+1]; rgba[di+2] = uf[si+2]; rgba[di+3] = uf[si+3]; }
    else if (ct === 2) { rgba[di] = uf[si]; rgba[di+1] = uf[si+1]; rgba[di+2] = uf[si+2]; rgba[di+3] = 255; }
    else { rgba[di] = uf[si]; rgba[di+1] = uf[si]; rgba[di+2] = uf[si]; rgba[di+3] = 255; }
  }
  return { width: w, height: h, rgba };
}

const atlas = decodePNG(fs.readFileSync('temp/dep_inject/IC2sprites/item_0.png'));

function getTile(tx, ty) {
  const pixels = [];
  for (let y = 0; y < 16; y++) {
    const row = [];
    for (let x = 0; x < 16; x++) {
      const si = ((ty * 16 + y) * atlas.width + (tx * 16 + x)) * 4;
      row.push([atlas.rgba[si], atlas.rgba[si+1], atlas.rgba[si+2], atlas.rgba[si+3]]);
    }
    pixels.push(row);
  }
  return pixels;
}

const t60 = getTile(6, 0);
const t61 = getTile(6, 1);

// Compare pixel by pixel
console.log('Pixels that DIFFER between (6,0) electronic and (6,1) advanced:');
console.log('[y,x] electronic => advanced');
let diffCount = 0;
for (let y = 0; y < 16; y++) {
  for (let x = 0; x < 16; x++) {
    const a = t60[y][x], b = t61[y][x];
    if (a[0] !== b[0] || a[1] !== b[1] || a[2] !== b[2] || a[3] !== b[3]) {
      console.log(`[${y},${x}] ${a.join(',')} => ${b.join(',')}`);
      diffCount++;
    }
  }
}
console.log(`\nTotal different pixels: ${diffCount}`);

// Unique colors
const colors60 = new Map(), colors61 = new Map();
for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
  const k0 = t60[y][x].join(',');
  const k1 = t61[y][x].join(',');
  colors60.set(k0, (colors60.get(k0) || 0) + 1);
  colors61.set(k1, (colors61.get(k1) || 0) + 1);
}
console.log('\nPalette (6,0) electronic circuit:');
for (const [k, v] of colors60) console.log(`  rgba(${k}) x${v}`);
console.log('\nPalette (6,1) advanced circuit:');
for (const [k, v] of colors61) console.log(`  rgba(${k}) x${v}`);
