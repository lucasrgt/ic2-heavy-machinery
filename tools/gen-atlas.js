const fs = require('fs');
const path = require('path');
const { createCanvas, loadImage } = (() => {
    // Use sharp if available, otherwise fall back to raw PNG generation
    try { return require('canvas'); } catch(e) { return { createCanvas: null, loadImage: null }; }
})();

// ---- Raw PNG encoder (no dependencies) ----
const zlib = require('zlib');

function encodePNG(width, height, rgbaBuffer) {
    // RGBA buffer to PNG
    const raw = Buffer.alloc(height * (1 + width * 4));
    for (let y = 0; y < height; y++) {
        raw[y * (1 + width * 4)] = 0; // filter: none
        rgbaBuffer.copy(raw, y * (1 + width * 4) + 1, y * width * 4, (y + 1) * width * 4);
    }

    const compressed = zlib.deflateSync(raw, { level: 9 });

    function crc32(buf) {
        let crc = 0xffffffff;
        const table = new Int32Array(256);
        for (let i = 0; i < 256; i++) {
            let c = i;
            for (let j = 0; j < 8; j++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
            table[i] = c;
        }
        for (let i = 0; i < buf.length; i++) crc = table[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
        return (crc ^ 0xffffffff) >>> 0;
    }

    function chunk(type, data) {
        const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
        const typeData = Buffer.concat([Buffer.from(type), data]);
        const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(typeData));
        return Buffer.concat([len, typeData, crc]);
    }

    const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(width, 0);
    ihdr.writeUInt32BE(height, 4);
    ihdr[8] = 8; // bit depth
    ihdr[9] = 6; // color type: RGBA

    return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', compressed), chunk('IEND', Buffer.alloc(0))]);
}

function decodePNG(pngBuf) {
    // Minimal PNG decoder for importing tiles
    let pos = 8; // skip signature
    let width, height, bitDepth, colorType;
    const idatChunks = [];

    while (pos < pngBuf.length) {
        const len = pngBuf.readUInt32BE(pos);
        const type = pngBuf.slice(pos + 4, pos + 8).toString('ascii');
        const data = pngBuf.slice(pos + 8, pos + 8 + len);

        if (type === 'IHDR') {
            width = data.readUInt32BE(0);
            height = data.readUInt32BE(4);
            bitDepth = data[8];
            colorType = data[9];
        } else if (type === 'IDAT') {
            idatChunks.push(data);
        } else if (type === 'IEND') {
            break;
        }
        pos += 12 + len;
    }

    const compressed = Buffer.concat(idatChunks);
    const raw = zlib.inflateSync(compressed);

    const bpp = colorType === 6 ? 4 : colorType === 2 ? 3 : colorType === 3 ? 1 : 1;
    const stride = width * bpp;
    const rgba = Buffer.alloc(width * height * 4);

    // Unfilter
    const unfiltered = Buffer.alloc(height * stride);
    for (let y = 0; y < height; y++) {
        const filterType = raw[y * (1 + stride)];
        const rowStart = y * (1 + stride) + 1;
        const outStart = y * stride;

        for (let i = 0; i < stride; i++) {
            const x = raw[rowStart + i];
            const a = i >= bpp ? unfiltered[outStart + i - bpp] : 0;
            const b = y > 0 ? unfiltered[outStart - stride + i] : 0;
            const c = (y > 0 && i >= bpp) ? unfiltered[outStart - stride + i - bpp] : 0;

            let val;
            switch (filterType) {
                case 0: val = x; break;
                case 1: val = (x + a) & 0xff; break;
                case 2: val = (x + b) & 0xff; break;
                case 3: val = (x + Math.floor((a + b) / 2)) & 0xff; break;
                case 4: { // Paeth
                    const p = a + b - c;
                    const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
                    val = (x + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 0xff;
                    break;
                }
                default: val = x;
            }
            unfiltered[outStart + i] = val;
        }
    }

    // Convert to RGBA
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const si = y * stride + x * bpp;
            const di = (y * width + x) * 4;
            if (colorType === 6) { // RGBA
                rgba[di] = unfiltered[si]; rgba[di+1] = unfiltered[si+1];
                rgba[di+2] = unfiltered[si+2]; rgba[di+3] = unfiltered[si+3];
            } else if (colorType === 2) { // RGB
                rgba[di] = unfiltered[si]; rgba[di+1] = unfiltered[si+1];
                rgba[di+2] = unfiltered[si+2]; rgba[di+3] = 255;
            }
        }
    }

    return { width, height, rgba };
}

// ---- Main ----
const W = 256, H = 256;
const atlas = Buffer.alloc(W * H * 4);

// Fill with IC2 missing texture tile (pink #D67FFF with dark purple #6B3F7F border)
const BORDER = [0x6B, 0x3F, 0x7F, 0xFF];
const FILL   = [0xD6, 0x7F, 0xFF, 0xFF];

for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
        const lx = x % 16; // local x within tile
        const ly = y % 16; // local y within tile
        const isBorder = (lx === 0 || lx === 15 || ly === 0 || ly === 15);
        const color = isBorder ? BORDER : FILL;
        const i = (y * W + x) * 4;
        atlas[i] = color[0]; atlas[i+1] = color[1]; atlas[i+2] = color[2]; atlas[i+3] = color[3];
    }
}

console.log('IC2 missing texture base generated (256x256)');

// Import existing textures into row 0
const assetsDir = path.join(__dirname, '..', 'src', 'ic2heavymachinery', 'assets', 'blocks');
const tiles = [
    'heavy_chassis.png',                // 0 - base side
    null,                               // 1 - base top (from ic2.png)
    null,                               // 2 - base bottom (from ic2.png)
    'alloy_blender_front.png',          // 3 - machine front
    'alloy_blender_front_active_0.png', // 4 - machine front active
    'alloy_blender_front_active_1.png', // 5
    'alloy_blender_front_active_2.png', // 6
    'alloy_blender_front_active_3.png', // 7
    'alloy_blender_top.png',            // 8 - machine top
    'nickel_ore.png',                   // 9 - nickel ore
    'silver_ore.png',                   // 10 - silver ore
    'platinum_ore.png',                 // 11 - platinum ore
    'cinnabar_ore.png',                 // 12 - cinnabar ore
    'heavy_port_side.png',             // 13 - port side (hole)
    'heavy_port_top.png',              // 14 - port top (hole)
    'heavy_port_bottom.png',           // 15 - port bottom (hole)
    'alloy_blender_side.png',         // 16 - blender side (row 1, col 0)
    null, null, null, null, null, null, null, null, null, null, null, null, // 17-28 (row 1, cols 1-12: reserved)
    'heavy_port_energy_side.png',     // 29 - energy port side (row 1, col 13)
    'heavy_port_energy_top.png',      // 30 - energy port top (row 1, col 14)
    'heavy_port_energy_bottom.png',   // 31 - energy port bottom (row 1, col 15)
    null, null, null, null, null, null, null, null, null, null, null, null, null, // 32-44 (row 2, cols 0-12: reserved)
    'heavy_port_fluid_side.png',      // 45 - fluid port side (row 2, col 13)
    'heavy_port_fluid_top.png',       // 46 - fluid port top (row 2, col 14)
    'heavy_port_fluid_bottom.png',    // 47 - fluid port bottom (row 2, col 15)
    null, null, null, null, null, null, null, null, null, null, null, null, null, // 48-60 (row 3, cols 0-12: reserved)
    'heavy_port_item_side.png',       // 61 - item port side (row 3, col 13)
    'heavy_port_item_top.png',        // 62 - item port top (row 3, col 14)
    'heavy_port_item_bottom.png',     // 63 - item port bottom (row 3, col 15)
    'item_cable_basic.png',           // 64 - item cable basic (row 4, col 0)
    'item_cable_reinforced.png',      // 65 - item cable reinforced (row 4, col 1)
    'item_cable_advanced.png',        // 66 - item cable advanced (row 4, col 2)
];

let imported = 0;
for (let i = 0; i < tiles.length; i++) {
    if (tiles[i] === null) continue;
    const filePath = path.join(assetsDir, tiles[i]);
    if (fs.existsSync(filePath)) {
        try {
            const png = decodePNG(fs.readFileSync(filePath));
            const tx = (i % 16) * 16;
            const ty = Math.floor(i / 16) * 16;
            for (let py = 0; py < Math.min(16, png.height); py++) {
                for (let px = 0; px < Math.min(16, png.width); px++) {
                    const si = (py * png.width + px) * 4;
                    const di = ((ty + py) * W + (tx + px)) * 4;
                    atlas[di] = png.rgba[si]; atlas[di+1] = png.rgba[si+1];
                    atlas[di+2] = png.rgba[si+2]; atlas[di+3] = png.rgba[si+3];
                }
            }
            console.log(`  [${i}] ${tiles[i]} ✓`);
            imported++;
        } catch(e) {
            console.log(`  [${i}] ${tiles[i]} ERRO: ${e.message}`);
        }
    } else {
        console.log(`  [${i}] ${tiles[i]} (não existe ainda)`);
    }
}

// Import tiles from IC2 reference atlas (ic2.png)
const ic2Path = path.join(assetsDir, 'ic2.png');
if (fs.existsSync(ic2Path)) {
    const ic2 = decodePNG(fs.readFileSync(ic2Path));
    // (x=2, y=2) in ic2.png = chassis top → our atlas index 9
    const ic2Tiles = [
        { sx: 1, sy: 1, idx: 1, name: 'base_top' },
        { sx: 0, sy: 1, idx: 2, name: 'base_bottom' },
    ];
    for (const t of ic2Tiles) {
        const srcX = t.sx * 16, srcY = t.sy * 16;
        const dstX = (t.idx % 16) * 16, dstY = Math.floor(t.idx / 16) * 16;
        for (let py = 0; py < 16; py++) {
            for (let px = 0; px < 16; px++) {
                const si = ((srcY + py) * ic2.width + (srcX + px)) * 4;
                const di = ((dstY + py) * W + (dstX + px)) * 4;
                atlas[di] = ic2.rgba[si]; atlas[di+1] = ic2.rgba[si+1];
                atlas[di+2] = ic2.rgba[si+2]; atlas[di+3] = ic2.rgba[si+3];
            }
        }
        console.log(`  [${t.idx}] ic2.png (${t.sx},${t.sy}) → ${t.name} ✓`);
        imported++;
    }
} else {
    console.log('  ic2.png não encontrado, pulando extração IC2');
}

// Save atlas
const outPath = path.join(assetsDir, 'ic2_heavy_machinery.png');
const pngData = encodePNG(W, H, atlas);
fs.writeFileSync(outPath, pngData);
console.log(`\nAtlas salvo: ${outPath} (${pngData.length} bytes, ${imported} texturas importadas)`);
