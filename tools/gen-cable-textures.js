#!/usr/bin/env node
// Generates 3 item cable textures (16x16) inspired by IC2 glass fiber cable aesthetic
// Tier 1 (1/tick): light gray/white translucent
// Tier 2 (8/tick): golden/amber
// Tier 3 (64/tick): emerald green

const { PNG } = require('pngjs');
const fs = require('fs');
const path = require('path');

const SIZE = 16;
const OUT = path.join(__dirname, '..', 'src', 'ic2heavymachinery', 'assets', 'blocks');

// Glass fiber cable style: translucent center tube with diamond grid pattern
// The cable occupies the middle 6 pixels (5-10 inclusive) of the 16px tile
// with a subtle grid/fiber pattern inside

function createCableTexture(filename, baseColor, highlightColor, borderColor, bgAlpha) {
    const png = new PNG({ width: SIZE, height: SIZE });

    // Fill fully transparent
    for (let y = 0; y < SIZE; y++) {
        for (let x = 0; x < SIZE; x++) {
            const idx = (y * SIZE + x) * 4;
            png.data[idx] = 0;
            png.data[idx + 1] = 0;
            png.data[idx + 2] = 0;
            png.data[idx + 3] = 0;
        }
    }

    function setPixel(x, y, r, g, b, a) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        const idx = (y * SIZE + x) * 4;
        png.data[idx] = r;
        png.data[idx + 1] = g;
        png.data[idx + 2] = b;
        png.data[idx + 3] = a;
    }

    // Cable body: columns 5-10 (6px wide tube)
    const cableMin = 5;
    const cableMax = 10;

    for (let y = 0; y < SIZE; y++) {
        for (let x = cableMin; x <= cableMax; x++) {
            // Border pixels (edges of cable)
            if (x === cableMin || x === cableMax) {
                setPixel(x, y, borderColor[0], borderColor[1], borderColor[2], 255);
                continue;
            }

            // Inner cable body
            // Diamond/fiber grid pattern: every 2 pixels offset
            const isGrid = ((x + y) % 3 === 0) || ((x - y + 16) % 4 === 0);

            if (isGrid) {
                // Highlight fiber lines
                setPixel(x, y, highlightColor[0], highlightColor[1], highlightColor[2], 220);
            } else {
                // Base translucent fill
                setPixel(x, y, baseColor[0], baseColor[1], baseColor[2], bgAlpha);
            }
        }
    }

    // Add subtle shading: top and bottom edge of cable slightly darker
    for (let x = cableMin + 1; x < cableMax; x++) {
        // Darken the border-adjacent row slightly
        const idxTop = (0 * SIZE + x) * 4;
        const idxBot = (15 * SIZE + x) * 4;
        // Keep existing color but reduce brightness slightly for the very edges
        for (let edge of [0, 15]) {
            const idx = (edge * SIZE + x) * 4;
            png.data[idx] = Math.max(0, png.data[idx] - 20);
            png.data[idx + 1] = Math.max(0, png.data[idx + 1] - 20);
            png.data[idx + 2] = Math.max(0, png.data[idx + 2] - 20);
        }
    }

    // Write file
    const outPath = path.join(OUT, filename);
    const buffer = PNG.sync.write(png);
    fs.writeFileSync(outPath, buffer);
    console.log(`Generated: ${outPath}`);
}

// Tier 1: Light gray/white (basic item cable) — like glass fiber but slightly tinted
createCableTexture(
    'item_cable_basic.png',
    [200, 200, 210],   // base: light gray-blue
    [240, 240, 255],   // highlight: near-white
    [140, 140, 155],   // border: medium gray
    180                 // semi-translucent
);

// Tier 2: Golden/amber (reinforced item cable)
createCableTexture(
    'item_cable_reinforced.png',
    [210, 175, 80],    // base: golden
    [255, 225, 120],   // highlight: bright gold
    [160, 120, 40],    // border: dark gold
     200                // slightly more opaque
);

// Tier 3: Emerald green (advanced item cable)
createCableTexture(
    'item_cable_advanced.png',
    [70, 200, 120],    // base: emerald green
    [130, 255, 170],   // highlight: bright green
    [30, 140, 70],     // border: dark green
    200                 // slightly more opaque
);

console.log('Done! 3 item cable textures generated.');
