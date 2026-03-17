# IC2 Heavy Machinery — Design Document

Addon mod para IC2 (IndustrialCraft 2) no Minecraft Beta 1.7.3.
Multiblock machines de dois tiers: Heavy (mid-game) e Industrial (endgame).

---

## Sistema de Energia IC2 (dados do código-fonte)

| Tier | Nome | Packet (EU/t) | Cabo | Transformer | Storage |
|------|------|---------------|------|-------------|---------|
| LV | Low Voltage | 32 | Copper (insulated) | LV (32↔128) | BatBox (40K EU) |
| MV | Medium Voltage | 128 | Gold | MV (128↔512) | MFE (600K EU) |
| HV | High Voltage | 512 | Glass Fibre | HV (512↔2048) | MFSU (10M EU) |
| EV | Extreme Voltage | 2048 | HV Cable (3x ins.) | — | — |

### Máquinas IC2 vanilla (referência)
- Macerator: 3 slots, energyConsume=2 EU/t, operationLength=300 ticks, maxInput=32 (LV)
- Todas as máquinas IC2 padrão: LV (32 EU/t max input)
- Se injectEnergy > maxInput → `explodeMachineAt()` (overvoltage)
- EnergyNet distribui pacotes, transformers convertem entre tiers

---

## Tier Novo: Ultra Voltage (UV)

| Tier | Nome | Packet | Cabo | Storage |
|------|------|--------|------|---------|
| UV | Ultra Voltage | 8192 EU/t | Cabo Vulcanizado (novo) | Ultra MFSU (novo) |

### Regras de Voltagem
- **Heavy Machines (3x3x3)** → aceitam HV (512 EU/t max input) — Glass Fibre cable
- **Industrial Machines (5x3x5)** → requerem UV (8192 EU/t) — SÓ cabo vulcanizado
- Conectar cabo normal em Industrial → explode (overvoltage, igual IC2 vanilla)

---

## Cadeia de Circuitos

### IC2 Vanilla (existente)

**Tier 1: Electronic Circuit**
```
[Copper Cable] [Copper Cable] [Copper Cable]
[Redstone]     [Refined Iron]  [Redstone]
[Copper Cable] [Copper Cable] [Copper Cable]
```
Usado em: Macerator, Extractor, Compressor, Electric Furnace, BatBox, Drill, etc.

**Tier 2: Advanced Circuit**
```
[Redstone]     [Glowstone]   [Redstone]
[Lapis Lazuli] [Elec Circuit] [Lapis Lazuli]
[Redstone]     [Glowstone]   [Redstone]
```
Usado em: MFSU, Nuclear Reactor, Mining Laser, Quantum Armor, Advanced Scanner

### IC2 Heavy Machinery (NOVO)

**Tier 3: Heavy Circuit**
```
[Purified Gold Ore]   [Gold Cable]         [Purified Gold Ore]
[Purified Silver Ore] [Adv Circuit]        [Purified Silver Ore]
[Gold Cable]          [Gold Cable]         [Gold Cable]
```
- Requer Ore Washer (Chemical Solvent + Gold/Silver Ore) — força cadeia MV completa
- Sem Glowstone (remove dependência de Nether rush para Heavy tier)
- Usado em: TODAS as Heavy Machines, Heavy chassis, Vulcanized Cable

**Tier 4: Industrial Circuit**
```
[Industrial Diamond]    [Solid Mercury]         [Industrial Diamond]
[Purified Platinum Ore] [Heavy Circuit]         [Purified Platinum Ore]
[Iridium Plate]         [Solid Mercury]         [Iridium Plate]
```
- Requer Industrial Diamond (64 coal → coal chain → Compressor endgame)
- Requer Ore Washer (Purified Platinum), Heavy Retort (Solid Mercury), IC2 endgame (Iridium)
- Cinco cadeias convergem: Industrial Diamond (coal chain) + Ore Washing (Platinum) + Retort (Mercury) + Heavy Circuit (tier anterior) + IC2 endgame (Iridium)
- Usado em: TODAS as Industrial Machines, Ultra MFSU, Industrial Transformer

---

## Materiais Novos

### Minérios (world gen)

| Minério | Y range | Frequência | Bioma |
|---------|---------|-----------|-------|
| Nickel Ore | 5-40 | Raro | Qualquer |
| Silver Ore | 5-30 | Raro | Qualquer |
| Platinum Ore | 5-15 | Muito raro | Qualquer |
| Cinnabar Ore | 10-35 | Raro | Overworld + Nether |
| Lithium Ore | 5-20 | Muito raro | Qualquer |

### Dusts

| Dust | Fonte primária | Subproduto de (crusher) |
|------|---------------|------------------------|
| Nickel Dust | Nickel Ore → Crusher | Iron Ore (10% Heavy, 25% Industrial) |
| Silver Dust | Silver Ore → Crusher | Gold Ore (10% Heavy, 25% Industrial) |
| Platinum Dust | Platinum Ore → Crusher | Nickel Ore (5% Industrial only) |
| Cinnabar Dust | Cinnabar Ore → Crusher | Redstone Ore (15% Industrial) |
| Sulfur Dust | Retort (sub. cinnabar) | Lava Cell (Centrifuge) |
| Invar Blend | 2x Iron Dust + 1x Nickel Dust (Alloy Blender) | — |
| Electrum Blend | 1x Gold Dust + 1x Silver Dust (Alloy Blender) | — |

### Ingots

| Ingot | Fonte | Uso principal |
|-------|-------|--------------|
| Nickel Ingot | Smelt Nickel Dust | Invar blend |
| Silver Ingot | Smelt Silver Dust | Electrum blend |
| Platinum Ingot | Smelt Platinum Dust | Industrial Circuit |
| Invar Ingot | Smelt Invar Blend | Heavy chassis (Invar Plate) |
| Electrum Ingot | Smelt Electrum Blend | Heavy Circuit |

### Plates (via Compressor)

| Plate | Fonte | Uso |
|-------|-------|-----|
| Invar Plate | Invar Ingot → Compressor | Heavy Machine Chassis |
| Platinum Plate | Platinum Ingot → Compressor | Industrial Machine Chassis |

### Componentes

| Item | Como obter | Uso |
|------|-----------|-----|
| Vulcanized Rubber | IC2 Rubber + Sulfur → Heavy Retort | Vulcanized Cable, armor |
| Solid Mercury | Cinnabar Dust → Heavy Retort | Industrial Circuit |
| Coke | Coal → Heavy Retort | Combustível (2x burn time), Steel (futuro) |
| Tar | Subproduto de Coke na Retort | Vulcanized Rubber (alt.), asfalto (futuro) |
| Sulfuric Acid Cell | Sulfur + Water Cell → Chemical Reactor | Processamento Platinum |

### Fluidos (novos)

| Fluido | Fonte | Uso |
|--------|-------|-----|
| Sulfuric Acid | Sulfur Dust + Water → Chemical Mixer | Ingrediente do Chemical Solvent |
| Chemical Solvent | Cinnabar Dust + Sulfuric Acid → Chemical Mixer | Ore Washer (purificação de crushed ores) |

---

## Blocos de Infraestrutura

### Vulcanized Cable
- Suporta 8192 EU/t (UV) sem queimar
- Perda: 0.1 EU/block
- Visual: cabo grosso dourado com listras pretas
```
[Vulcanized Rubber]
[Gold Cable]
[Vulcanized Rubber]
```

### Industrial Transformer (Multiblock 3x3x3)
- Converte HV (512) ↔ UV (8192)
- Necessário para alimentar máquinas Industrial
```
Core: [MFSU] + [4x Iridium Plate] + [2x Industrial Circuit] + [2x HV Cable]
Shell: 26x Heavy Machine Chassis
```

### Ultra MFSU (Multiblock 3x3x3)
- Armazena 100.000.000 EU
- Output: 8192 EU/t (UV)
```
Core: [MFSU] + [6x Lapotron Crystal] + [2x Industrial Circuit] + [4x Iridium Plate]
Shell: 26x Heavy Machine Chassis
```

### Fusion Reactor (Multiblock 5x3x5, UV)
- Input: 2048 EU/t (HV, via Glass Fibre Cable)
- Output: 8192 EU/t (UV, via Vulcanized Cable) — 4x energy gain
- Combustível: Deuterium Cell + Tritium Cell (consumo a cada 10.000 ticks = ~8 min)
- Ratio de consumo: 4x Deuterium Cell + 1x Tritium Cell por ciclo
- A fonte de energia definitiva do endgame — alimenta toda a cadeia Industrial com folga
```
Core: [Ultra MFSU] + [4x Industrial Circuit] + [4x Iridium Plate] + [2x Platinum Plate]
Shell: 74x Industrial Machine Chassis
```

**Cadeia de combustível:**
```
Deuterium (infinito, fonte = água):
  Water → Industrial Electrolyzer → Heavy Water
  Heavy Water → Industrial Electrolyzer → Deuterium Cell + Oxygen (subproduto)

Neutron Fluid (requer Nuclear Reactor ativo):
  IC2 Nuclear Reactor consome Uranium Cell → Depleted Uranium Cell (IC2 vanilla)
  Depleted Uranium Cell → Industrial Chemical Reactor → Neutron Fluid + Uranium Waste
  SEM REATOR NUCLEAR = SEM NEUTRON FLUID = SEM TRITIUM = SEM FUSION

Tritium (finito, fonte = Lithium + Neutron Fluid):
  Lithium Ore → Heavy Crusher → 4x Lithium Dust
  Lithium Dust + Empty Cell → crafting → Lithium Cell
  Lithium Cell + Neutron Fluid → Industrial Chemical Reactor → Tritium Cell + Lithium Waste

Lithium reciclável:
  Lithium Waste → Industrial Centrifuge → Tiny Lithium Dust (50% chance)
  9x Tiny Lithium Dust → 1x Lithium Dust

Lithium renovável (Clay = terras raras):
  4x Clay Ball → IC2 Compressor → Clay Block (nova receita QoL)
  Clay Block → Industrial Centrifuge → Tiny Lithium Dust (subproduto)
  Custo: 512 EU/t × 400 ticks = 204.800 EU por clay block = 1 tiny dust
  9 clay blocks = 1 Lithium Dust = 1.843.200 EU (caro mas infinito)
```

**Dependência Nuclear obrigatória:**
- O Fusion Reactor NÃO substitui o Nuclear Reactor — depende dele
- Nuclear Reactor produz Depleted Uranium Cells → fonte de Neutron Fluid
- Jogador precisa dominar nuclear (gestão de calor, coolant, layout) antes de ter Fusion
- Depleted Uranium Cells ganham propósito real (no IC2 vanilla são quase lixo)

**Sustentabilidade:**
- 1 Lithium Ore = 4 dust → 4 Tritium Cells → ~32 min de Fusion
- Com reciclagem (50%): ~48 min efetivo por ore
- 1 vein (5-10 ores) = 4-8 horas de Fusion contínuo
- Clay farming = infinito mas energeticamente caro (justifica o Fusion como investimento)
- Neutron Fluid depende de Depleted Uranium Cells = Nuclear Reactor precisa estar rodando

---

## Materiais Novos — Fusão Nuclear

### Minério

| Minério | Y range | Frequência | Uso |
|---------|---------|-----------|-----|
| Lithium Ore | 5-20 | Muito raro | Tritium (Fusion Reactor) |

### Itens

| Item | Fonte | Uso |
|------|-------|-----|
| Lithium Dust | Lithium Ore → Heavy Crusher (4x) | Lithium Cell |
| Tiny Lithium Dust | Clay → Industrial Centrifuge (subproduto), Lithium Waste → Centrifuge (50%) | 9x → 1x Lithium Dust |
| Lithium Cell | Lithium Dust + Empty Cell (crafting) | Tritium production (+ Neutron Fluid) |
| Lithium Waste | Subproduto do Chemical Reactor (Tritium production) | Reciclagem → Tiny Lithium Dust |
| Neutron Fluid | Depleted Uranium Cell → Industrial Chemical Reactor | Tritium production (+ Lithium Cell) |
| Uranium Waste | Subproduto do Neutron Fluid extraction | Recycler → 3x Scrap (triplo do normal, alimenta UU-Matter → Iridium loop) |
| Heavy Water Cell | Water → Industrial Electrolyzer | Deuterium Cell production |
| Deuterium Cell | Heavy Water → Industrial Electrolyzer | Fusion Reactor fuel |
| Tritium Cell | Lithium Cell + Neutron Fluid → Industrial Chemical Reactor | Fusion Reactor fuel |

### Receita QoL adicionada ao IC2

| Machine | Input | Output |
|---------|-------|--------|
| IC2 Compressor | 4x Clay Ball | 1x Clay Block |

---

## Máquinas — Lineup Completo

### Alloy Blender (Single Block, MV)

Máquina single-block entry-point para ligas metálicas. Mesmo tier do Rotary Macerator (Advanced Circuit).
2 inputs → 1 output, aceita até MV (128 EU/t).

| Aspecto | Valor |
|---------|-------|
| EU/tick | 8 |
| Ticks | 200 |
| Max Input | 128 EU/t (MV) |
| Slots | 2 input + 1 output |
| Circuito | Advanced Circuit |

**Crafting:**
```
[Refined Iron]   [Piston]          [Refined Iron]
[Adv Circuit]    [Machine Block]   [Adv Circuit]
[Refined Iron]   [MFE]             [Refined Iron]
```

**Receitas do Alloy Blender:**

| Input 1 | Input 2 | Output |
|---------|---------|--------|
| 2x Iron Dust | 1x Nickel Dust | 3x Invar Blend |
| 1x Gold Dust | 1x Silver Dust | 2x Electrum Blend |
| 3x Copper Dust | 1x Tin Dust | 4x Bronze Dust |

### Chemical Mixer (Single Block, MV)

Máquina single-block que produz solventes químicos a partir de reagentes + fluido.
Necessário antes do Ore Washer — cria o Chemical Solvent que purifica minérios.

| Aspecto | Valor |
|---------|-------|
| EU/tick | 8 |
| Ticks | 200 |
| Max Input | 128 EU/t (MV) |
| Slots | 1 input item + 1 input fluid + 1 output fluid |
| Circuito | Advanced Circuit |

**Crafting:**
```
[Glass]          [Iron Bars]      [Glass]
[Adv Circuit]    [Machine Block]  [Adv Circuit]
[Iron]           [Pump]           [Iron]
```

**Receitas do Chemical Mixer:**

| Input Item | Input Fluid | Output Fluid |
|------------|-------------|--------------|
| 1x Sulfur Dust | 1000 mB Water | 1000 mB Sulfuric Acid |
| 1x Cinnabar Dust | 1000 mB Sulfuric Acid | 1000 mB Chemical Solvent |

O Chemical Solvent é o reagente usado no Ore Washer para purificar crushed ores.
Cadeia: Sulfur → Sulfuric Acid → Chemical Solvent → Purified Ores (3x yield).

### Ore Washer (Single Block, MV)

Máquina single-block que refina crushed ores em purified ores usando Chemical Solvent.
Aumenta o yield de processamento (crushed → purified → 3x dust via IC2 centrifuge ou smelt direto).

| Aspecto | Valor |
|---------|-------|
| EU/tick | 8 |
| Ticks | 300 |
| Max Input | 128 EU/t (MV) |
| Slots | 1 input item + 1 input fluid (Chemical Solvent) + 1 output + 1 output secondary |
| Circuito | Advanced Circuit |
| Consumo fluido | 200 mB Chemical Solvent por operação |

**Crafting:**
```
[Iron]           [Cauldron]       [Iron]
[Adv Circuit]    [Machine Block]  [Adv Circuit]
[Iron]           [Pump]           [Iron]
```

**Receitas do Ore Washer:**

| Input | Fluid | Output | Subproduto |
|-------|-------|--------|------------|
| 1x Iron Ore | 200 mB Chemical Solvent | 3x Purified Iron Ore | Tiny Nickel Dust (10%) |
| 1x Gold Ore | 200 mB Chemical Solvent | 3x Purified Gold Ore | Tiny Silver Dust (10%) |
| 1x Copper Ore | 200 mB Chemical Solvent | 3x Purified Copper Ore | Tiny Tin Dust (5%) |
| 1x Tin Ore | 200 mB Chemical Solvent | 3x Purified Tin Ore | — |
| 1x Nickel Ore | 200 mB Chemical Solvent | 3x Purified Nickel Ore | Tiny Platinum Dust (5%) |
| 1x Silver Ore | 200 mB Chemical Solvent | 3x Purified Silver Ore | Tiny Gold Dust (10%) |
| 1x Platinum Ore | 200 mB Chemical Solvent | 3x Purified Platinum Ore | Tiny Nickel Dust (10%) |
| 1x Uranium Ore | 200 mB Chemical Solvent | 3x Purified Uranium Ore | — |

**Cadeia de processamento completa (ore tripling):**
```
Ore + Chemical Solvent → Ore Washer → 3x Purified Ore + tiny dust (10%)
Purified Ore → IC2 Macerator → 1x Dust
Dust → Smelt → 1x Ingot
Total: 1 ore → 3 ingots
```

Sem Ore Washer: Ore → Macerator → 2x Dust → Smelt → 2 ingots (2x yield).
Com Ore Washer: Ore → Ore Washer → 3x Purified → Macerator → 3x Dust → Smelt → 3 ingots (3x yield).
O custo é Chemical Solvent (requer Chemical Mixer + Cinnabar + Sulfur) — cadeia de 2 máquinas + materiais raros.

### HEAVY Tier (3x3x3 multiblock, 4 processing lanes, HV)

| Máquina | EU/t | Ticks | Lanes | Diferencial vs IC2 |
|---------|------|-------|-------|-------------------|
| Heavy Crusher | 64 | 200 | 4 | 2x yield + 10% subproduto |
| Heavy Compressor | 64 | 200 | 4 | 4 lanes parallel |
| Heavy Extractor | 64 | 200 | 4 | 4 lanes parallel |
| Heavy Furnace | 64 | 150 | 4 | 4 lanes, 25% mais rápido |
| Heavy Electrolyzer | 128 | 300 | 4 | 4 lanes, melhores yields |
| Heavy Retort | 128 | 400 | 4 | EXCLUSIVE — não existe no IC2 |
| Heavy Blender | 64 | 200 | 4 | 4 lanes, receitas de liga exclusivas |
| Heavy Ore Washer | 64 | 200 | 4 | 4 lanes, 15% subproduto (vs 10% single) |
| Heavy Chemical Mixer | 64 | 150 | 4 | 4 lanes, 25% mais rápido |

### INDUSTRIAL Tier (5x3x5 multiblock, 8 processing lanes, UV)

| Máquina | EU/t | Ticks | Lanes | Diferencial |
|---------|------|-------|-------|-------------|
| Industrial Crusher | 256 | 150 | 8 | 3x yield + 25% subproduto |
| Industrial Compressor | 256 | 150 | 8 | 8 lanes, mais rápido |
| Industrial Extractor | 256 | 150 | 8 | 8 lanes, mais rápido |
| Industrial Furnace | 256 | 100 | 8 | 8 lanes, 50% mais rápido |
| Industrial Electrolyzer | 512 | 200 | 8 | 8 lanes, receitas exclusivas |
| Industrial Retort | 512 | 300 | 8 | Dual-input, receitas exclusivas |
| Industrial Centrifuge | 512 | 400 | 4-out | EXCLUSIVE endgame |
| Industrial Blender | 256 | 150 | 8 | 8 lanes, receitas de liga exclusivas |
| Industrial Chemical Reactor | 512 | 300 | 2in→2out | EXCLUSIVE endgame |
| Industrial Ore Washer | 256 | 120 | 8 | 8 lanes, 25% subproduto, menos solvent (150 mB) |
| Industrial Chemical Mixer | 256 | 100 | 8 | 8 lanes, 50% mais rápido, yields +50% |

---

## Receitas de Processamento

### Heavy Crusher (4 lanes, 200 ticks, 64 EU/t)

| Input | Output | Subproduto (10% chance) |
|-------|--------|------------------------|
| Iron Ore | 2x Iron Dust | Nickel Dust |
| Gold Ore | 2x Gold Dust | Silver Dust |
| Copper Ore | 2x Copper Dust | — |
| Tin Ore | 2x Tin Dust | — |
| Nickel Ore | 2x Nickel Dust | Platinum Dust (5%) |
| Silver Ore | 2x Silver Dust | — |
| Platinum Ore | 2x Platinum Dust | Nickel Dust |
| Cinnabar Ore | 3x Cinnabar Dust | Sulfur Dust |
| Uranium Ore | 2x Uranium | — |

### Industrial Crusher (8 lanes, 150 ticks, 256 EU/t)

| Input | Output | Subproduto (25% chance) |
|-------|--------|------------------------|
| Iron Ore | **3x** Iron Dust | Nickel Dust |
| Gold Ore | **3x** Gold Dust | Silver Dust |
| Copper Ore | **3x** Copper Dust | Tin Dust |
| Tin Ore | **3x** Tin Dust | — |
| Nickel Ore | **3x** Nickel Dust | Platinum Dust (15%) |
| Silver Ore | **3x** Silver Dust | Gold Dust |
| Platinum Ore | **3x** Platinum Dust | Nickel Dust |
| Cinnabar Ore | **4x** Cinnabar Dust | Sulfur Dust |
| Redstone Ore | 8x Redstone | Cinnabar Dust (15%) |

### Heavy Retort (4 lanes, 400 ticks, 128 EU/t) — EXCLUSIVE

| Input | Output 1 | Output 2 (subproduto) |
|-------|----------|----------------------|
| Cinnabar Dust | Solid Mercury | Sulfur Dust |
| Coal | Coke | Tar |
| Wood Log | 2x Charcoal | Tar |
| Soul Sand | Sulfur Dust | Niter (futuro) |
| IC2 Rubber + Sulfur Dust | Vulcanized Rubber | — |

### Industrial Retort (8 lanes, dual-input, 300 ticks, 512 EU/t) — EXCLUSIVE

| Input 1 | Input 2 | Output 1 | Output 2 |
|---------|---------|----------|----------|
| Cinnabar Dust | — | Solid Mercury | Sulfur Dust |
| IC2 Rubber | Sulfur Dust | 2x Vulcanized Rubber | — |
| Gold Ingot | Solid Mercury | 2x Purified Gold | — |
| Coal | — | Coke | Tar |
| Impure Platinum Dust | — | Pure Platinum Dust | Slag |

### Heavy Blender (4 lanes, 200 ticks, 64 EU/t)

| Input 1 | Input 2 | Output |
|---------|---------|--------|
| 2x Iron Dust | 1x Nickel Dust | 3x Invar Blend |
| 1x Gold Dust | 1x Silver Dust | 2x Electrum Blend |
| 3x Copper Dust | 1x Tin Dust | 4x Bronze Dust |
| 1x Iron Dust | 1x Coal Dust | 2x Steel Blend (futuro) |

4 lanes paralelas = 4x o throughput do Alloy Blender single-block.

### Industrial Blender (8 lanes, 150 ticks, 256 EU/t)

| Input 1 | Input 2 | Output |
|---------|---------|--------|
| 2x Iron Dust | 1x Nickel Dust | **4x** Invar Blend |
| 1x Gold Dust | 1x Silver Dust | **3x** Electrum Blend |
| 3x Copper Dust | 1x Tin Dust | **6x** Bronze Dust |
| 1x Iron Dust | 1x Coal Dust | **3x** Steel Blend (futuro) |
| 1x Platinum Dust | 1x Nickel Dust | 2x Inconel Blend (futuro) |

8 lanes + yields melhorados. Receitas exclusivas para ligas endgame.

### Industrial Centrifuge (4 outputs, 400 ticks, 512 EU/t) — EXCLUSIVE

| Input | Out 1 | Out 2 | Out 3 | Out 4 |
|-------|-------|-------|-------|-------|
| Lava Cell | Copper Dust | Tin Dust | Sulfur Dust | Empty Cell |
| Impure Platinum | Pure Platinum | Nickel Dust | — | — |
| Uranium Ore | Enriched Uranium | Uranium Dust | — | — |

### Chemical Mixer (single-block, 200 ticks, 8 EU/t)

| Input Item | Input Fluid | Output Fluid |
|------------|-------------|--------------|
| 1x Sulfur Dust | 1000 mB Water | 1000 mB Sulfuric Acid |
| 1x Cinnabar Dust | 1000 mB Sulfuric Acid | 1000 mB Chemical Solvent |

### Ore Washer (single-block, 300 ticks, 8 EU/t)

| Input | Fluid (200 mB Chemical Solvent) | Output | Subproduto (10%) |
|-------|---------------------------------|--------|-----------------|
| 1x Iron Ore | Chemical Solvent | 3x Purified Iron Ore | Tiny Nickel Dust |
| 1x Gold Ore | Chemical Solvent | 3x Purified Gold Ore | Tiny Silver Dust |
| 1x Copper Ore | Chemical Solvent | 3x Purified Copper Ore | Tiny Tin Dust (5%) |
| 1x Tin Ore | Chemical Solvent | 3x Purified Tin Ore | — |
| 1x Nickel Ore | Chemical Solvent | 3x Purified Nickel Ore | Tiny Platinum Dust (5%) |
| 1x Silver Ore | Chemical Solvent | 3x Purified Silver Ore | Tiny Gold Dust |
| 1x Platinum Ore | Chemical Solvent | 3x Purified Platinum Ore | Tiny Nickel Dust |
| 1x Uranium Ore | Chemical Solvent | 3x Purified Uranium Ore | — |

### Heavy Ore Washer (4 lanes, 200 ticks, 64 EU/t)

Mesmas receitas do Ore Washer single-block, mas com 4 lanes paralelas e 15% chance de subproduto.

### Heavy Chemical Mixer (4 lanes, 150 ticks, 64 EU/t)

Mesmas receitas do Chemical Mixer single-block, com 4 lanes e 25% mais rápido.

### Industrial Ore Washer (8 lanes, 120 ticks, 256 EU/t)

Mesmas receitas, 8 lanes, 25% chance de subproduto, consome apenas 150 mB de Chemical Solvent por operação.

### Industrial Chemical Mixer (8 lanes, 100 ticks, 256 EU/t)

Mesmas receitas, 8 lanes, 50% mais rápido, yields de fluido +50% (1500 mB output por operação).

### Industrial Chemical Reactor (2in→2out, 300 ticks, 512 EU/t) — EXCLUSIVE

| Input 1 | Input 2 | Output 1 | Output 2 |
|---------|---------|----------|----------|
| Sulfur Dust | Water Cell | Sulfuric Acid Cell | Empty Cell |
| Platinum Dust | Sulfuric Acid Cell | Pure Platinum Dust | Sludge |
| Invar Blend | — | 2x Invar Ingot | — |

---

## Comparativo: IC2 Macerator vs Heavy vs Industrial

| Aspecto | IC2 Macerator | Heavy Crusher | Industrial Crusher |
|---------|--------------|---------------|-------------------|
| Lanes | 1 | 4 | 8 |
| Yield/ore | 2 dust | 2 dust + subproduto | **3 dust** + subproduto |
| Subproduto | Não | 10% chance | 25% chance |
| Speed | 300 ticks | 200 ticks | 150 ticks |
| EU/tick | 2 | 64 | 256 |
| EU total/ore | 600 | 12.800 | 38.400 |
| Throughput | 1 item/15s | 4 items/10s | 8 items/7.5s |
| Receitas exclusivas | Não | Subprodutos | Sim (ores extras) |

Trade-off: custo por item muito maior, mas throughput massivo + bonus yields + receitas exclusivas. Uma parede de macerators = 2 dust/ore. Industrial Crusher = 3 dust + subprodutos raros.

---

## Progressão Completa

### Fase 0: IC2 Vanilla (pré-requisito)
```
Mine → Smelt → Machine Block → Electronic Circuit → Macerator/Extractor/Compressor
                                                   → BatBox (LV)
                                Advanced Circuit   → MFE (MV)
                                                   → MFSU (HV)
```

### Fase 1: Alloy Blender + Chemical Mixer + Ore Washer + Materiais Novos
```
Advanced Circuit → Alloy Blender (single-block, MV)
Advanced Circuit → Chemical Mixer (single-block, MV)
Advanced Circuit → Ore Washer (single-block, MV)

Mine Nickel Ore → IC2 Macerator → 2x Nickel Dust → Smelt → Nickel Ingot
Mine Silver Ore → IC2 Macerator → 2x Silver Dust → Smelt → Silver Ingot

Alloy Blender:
  2x Iron Dust + 1x Nickel Dust → 3x Invar Blend → Smelt → Invar Ingot
  1x Gold Dust + 1x Silver Dust → 2x Electrum Blend → Smelt → Electrum Ingot

Chemical Mixer + Ore Washer (ore tripling chain):
  Sulfur Dust + Water → Chemical Mixer → Sulfuric Acid
  Cinnabar Dust + Sulfuric Acid → Chemical Mixer → Chemical Solvent
  Ore + Chemical Solvent → Ore Washer → 3x Purified Ore + tiny dust (10%)
  Purified Ore → IC2 Macerator → Dust → Smelt → Ingot
  Net: 3x ore yield (vs 2x com Macerator only)
```
Alloy Blender é o entry point do addon — usa Advanced Circuit (mesmo tier do Rotary Macerator).
Chemical Mixer + Ore Washer adicionam o ore tripling (requer cadeia de 2 máquinas + Cinnabar).
Primeiro contato com as ligas metálicas e processamento avançado de minérios.

### Fase 2: Heavy Circuit + Chassis
```
Purified Gold Ore + Purified Silver Ore + Advanced Circuit + Gold Cable → Heavy Circuit
  (requer Ore Washer → requer Chemical Mixer → requer Cinnabar + Sulfur)

Invar Ingot → IC2 Compressor → Invar Plate
8x Invar Plate + Heavy Circuit → Heavy Machine Chassis
```
Gate: requer Ore Washer (Chemical Solvent) + Gold/Silver Ore + Nickel (Invar).

### Fase 3: Heavy Machines (3x3x3, HV)
```
Cada core consome a máquina IC2 original como ingrediente:

Heavy Crusher Core:
  [Diamond]       [Piston]         [Diamond]
  [Heavy Circuit] [IC2 Macerator]  [Heavy Circuit]
  [Diamond]       [MFE]            [Diamond]

Heavy Compressor Core:
  [Diamond]       [Piston]           [Diamond]
  [Heavy Circuit] [IC2 Compressor]   [Heavy Circuit]
  [Diamond]       [MFE]              [Diamond]

Heavy Extractor Core:
  [Diamond]       [Piston]          [Diamond]
  [Heavy Circuit] [IC2 Extractor]   [Heavy Circuit]
  [Diamond]       [MFE]             [Diamond]

Heavy Furnace Core:
  [Diamond]       [Piston]              [Diamond]
  [Heavy Circuit] [IC2 Elec Furnace]    [Heavy Circuit]
  [Diamond]       [MFE]                 [Diamond]

Heavy Electrolyzer Core:
  [Diamond]       [Piston]             [Diamond]
  [Heavy Circuit] [IC2 Electrolyzer]   [Heavy Circuit]
  [Diamond]       [MFSU]               [Diamond]

Heavy Retort Core (EXCLUSIVE — sem máquina IC2 base):
  [Invar Plate]   [Piston]         [Invar Plate]
  [Heavy Circuit] [Iron Furnace]   [Heavy Circuit]
  [Invar Plate]   [MFE]            [Invar Plate]

Heavy Blender Core (upgrade do Alloy Blender single-block):
  [Diamond]       [Piston]          [Diamond]
  [Heavy Circuit] [Alloy Blender]   [Heavy Circuit]
  [Diamond]       [MFE]             [Diamond]

Heavy Ore Washer Core (upgrade do Ore Washer single-block):
  [Diamond]       [Piston]          [Diamond]
  [Heavy Circuit] [Ore Washer]      [Heavy Circuit]
  [Diamond]       [MFE]             [Diamond]

Heavy Chemical Mixer Core (upgrade do Chemical Mixer single-block):
  [Diamond]       [Piston]            [Diamond]
  [Heavy Circuit] [Chemical Mixer]    [Heavy Circuit]
  [Diamond]       [MFE]               [Diamond]

Shell: 26x Heavy Machine Chassis + Core + Energy/Item/Fluid Ports
Alimentação: HV (512 EU/t) via Glass Fibre Cable
```

### Fase 4: Materiais via Heavy Retort (bridge para Industrial)
```
Cinnabar Ore → Heavy Crusher → 3x Cinnabar Dust + Sulfur
Cinnabar Dust → Heavy Retort → Solid Mercury + Sulfur Dust
Coal → Heavy Retort → Coke + Tar
IC2 Rubber + Sulfur → Heavy Retort → Vulcanized Rubber

Mine Platinum Ore → Heavy Crusher → 2x Platinum Dust → Smelt → Platinum Ingot
```
Heavy Retort = gateway para Industrial. Sem ela: sem Mercury, sem Vulcanized Rubber.

### Fase 5: Industrial Circuit (endgame gate)
```
Industrial Diamond + Solid Mercury + Purified Platinum Ore + Heavy Circuit + Iridium Plate → Industrial Circuit

Industrial Diamond: 64x Coal → Macerator → Coal Dust → Compressor chain → Industrial Diamond
Purified Platinum: Platinum Ore + Chemical Solvent → Ore Washer → 3x Purified Platinum Ore
Solid Mercury: Cinnabar Dust → Heavy Retort → Solid Mercury + Sulfur Dust
Iridium: UU-Matter (IC2 Mass Fabricator) → Iridium Ore → Compressor → Iridium Plate
```
Cinco cadeias convergem: Industrial Diamond (coal endgame) + Ore Washing (Purified Platinum) + Retort (Mercury) + Heavy Circuit (tier anterior completo) + IC2 endgame (Iridium).

### Fase 6: Infraestrutura UV
```
Vulcanized Rubber + Gold Cable + Electrum Ingot → Vulcanized Cable (8192 EU/t)

Industrial Transformer (3x3x3 multiblock):
  Core = MFSU + 4x Iridium Plate + 2x Industrial Circuit + 2x HV Cable
  Shell = 26x Heavy Machine Chassis

Ultra MFSU (3x3x3 multiblock):
  Core = MFSU + 6x Lapotron Crystal + 2x Industrial Circuit + 4x Iridium Plate
  Shell = 26x Heavy Machine Chassis
```

### Fase 7: Industrial Machines (5x3x5, UV)
```
Industrial Machine Chassis:
  8x Platinum Plate + 1x Industrial Circuit

Cada Industrial core = 2x Heavy cores + Industrial Circuits + Ultra MFSU + Iridium:

Industrial Crusher Core:
  [Iridium Plate]       [Heavy Crusher Core]  [Iridium Plate]
  [Industrial Circuit]  [Ultra MFSU]          [Industrial Circuit]
  [Iridium Plate]       [Heavy Crusher Core]  [Iridium Plate]

(mesmo padrão para Compressor, Extractor, Furnace, Electrolyzer, Retort)

Shell: 74x Industrial Machine Chassis + Core + Energy/Item Ports
Alimentação: UV (8192 EU/t) via Vulcanized Cable ONLY
```

### Fase 8: Máquinas Exclusivas Industrial
```
Industrial Centrifuge Core (EXCLUSIVE — sem versão Heavy):
  [Iridium Plate]       [IC2 Compressor]     [Iridium Plate]
  [Industrial Circuit]  [Ultra MFSU]         [Industrial Circuit]
  [Iridium Plate]       [IC2 Compressor]     [Iridium Plate]

Industrial Chemical Reactor Core (EXCLUSIVE — sem versão Heavy):
  [Platinum Plate]      [IC2 Electrolyzer]    [Platinum Plate]
  [Industrial Circuit]  [Ultra MFSU]          [Industrial Circuit]
  [Platinum Plate]      [IC2 Electrolyzer]    [Platinum Plate]
```

### Fase 9: Fusion Reactor (endgame energy)
```
Pré-requisito: IC2 Nuclear Reactor rodando (produz Depleted Uranium Cells)

Cadeia de Neutron Fluid (requer Nuclear Reactor):
  IC2 Nuclear Reactor → Depleted Uranium Cell
  Depleted Uranium Cell → Industrial Chemical Reactor → Neutron Fluid + Uranium Waste

Cadeia de Deuterium (infinito):
  Water → Industrial Electrolyzer → Heavy Water Cell
  Heavy Water Cell → Industrial Electrolyzer → Deuterium Cell

Cadeia de Tritium (Lithium + Neutron Fluid):
  Lithium Dust + Empty Cell → crafting → Lithium Cell
  Lithium Cell + Neutron Fluid → Industrial Chemical Reactor → Tritium Cell + Lithium Waste

Reciclagem:
  Lithium Waste → Industrial Centrifuge → Tiny Lithium Dust (50%)
  Clay Block → Industrial Centrifuge → Tiny Lithium Dust (renovável)
  4x Clay Ball → IC2 Compressor → Clay Block (receita QoL nova)

Fusion Reactor (5x3x5 multiblock):
  Core = Ultra MFSU + 4x Industrial Circuit + 4x Iridium Plate + 2x Platinum Plate
  Shell = 74x Industrial Machine Chassis
  Input: 2048 EU/t (HV) + Deuterium Cell + Tritium Cell
  Output: 8192 EU/t (UV) — alimenta toda a cadeia Industrial com folga
  Consumo: 4x Deuterium + 1x Tritium a cada 10.000 ticks (~8 min)
```
O Fusion Reactor é o último milestone — NÃO substitui o Nuclear Reactor, DEPENDE dele.
O jogador precisa: (1) Nuclear Reactor rodando → Depleted Cells → Neutron Fluid,
(2) Industrial machines rodando → Deuterium + Tritium, (3) Lithium mining/clay farming.
Uma vez ativo, fornece 8192 EU/t — energia UV suficiente pra toda a base industrial.

---

## Cadeia de Dependência (visual)

```
                    Adv Circuit ──→ Alloy Blender (single-block, MV)
                         │              │
                         ├──→ Chemical Mixer ──→ Chemical Solvent
                         │                           │
                         ├──→ Ore Washer ←───────────┘ (ore tripling)
                         │
Silver Ore ──→ Alloy Blender ──→ Electrum ───→ Heavy Circuit ───→ Heavy Machines
Nickel Ore ──→ Alloy Blender ──→ Invar ─────→ Heavy Chassis  ───→      │
                                                                  Heavy Retort
Cinnabar ──→ Chemical Mixer ──→ Chemical Solvent                       │
Sulfur ────→ Chemical Mixer ──→ Sulfuric Acid ──┘              Vulcanized Rubber
                                                                       │
Cinnabar ──→ Retort ──→ Solid Mercury ──┐                   Vulcanized Cable
Platinum ──→ Smelt ──→ Platinum Ingot ──┤                          │
IC2 Iridium ────────→ Iridium Plate ────┤                   UV Infrastructure
                                         │                         │
                                  Industrial Circuit                │
                                         │                         │
                                         └──→ Industrial Machines ←─┘
                                                      │
                                         Industrial Electrolyzer ──→ Heavy Water ──→ Deuterium Cell
                                                      │
                                         IC2 Nuclear Reactor ──→ Depleted Uranium Cell
                                                      │
                                         Ind. Chemical Reactor ──→ Neutron Fluid + Uranium Waste
                                                      │
                                         Lithium Ore ──→ Lithium Cell + Neutron Fluid ──→ Tritium Cell
                                         Clay Block ──→ Ind. Centrifuge ──→ Tiny Lithium (renovável)
                                                      │
                                              Fusion Reactor (2048 EU/t in → 8192 EU/t out)
                                              = endgame energy (DEPENDE do Nuclear Reactor)
```

---

## Arquitetura Multiblock

### Conceito: Advanced Machine Block como Shell
- Player constrói a estrutura usando **Advanced Machine Block** do IC2 (blockMachine meta 0)
- NÃO existe bloco de chassis ou port separado — simplifica o addon
- O **controller** é o único bloco novo (1 por máquina)
- Quando o multiblock **forma**, o controller marca posições específicas como ports
- Advanced Machine Blocks nessas posições assumem **textura de port** com ring colorida:
  - **Vermelho** = Energy Port (EU input)
  - **Azul** = Fluid Port
  - **Laranja** = Item Port (input/output)
- Quando o multiblock **quebra**, os blocos voltam ao visual normal de Advanced Machine Block

### Formação do Multiblock
1. Player coloca Advanced Machine Blocks no formato correto (3x3x3 ou 5x3x5)
2. Player coloca o controller na posição central da face frontal
3. Controller detecta a estrutura e marca posições dos ports
4. Render muda: certos Advanced Machine Blocks ganham overlay de port
5. Ports aceitam pipes/cabos do tipo correto na face externa

### Blocos Necessários
- **Controller por máquina** (1 bloco cada): Heavy Crusher, Heavy Compressor, etc.
- **Advanced Machine Block** do IC2 (reusado, sem modificar)
- Nenhum chassis ou port custom necessário

---

## Block IDs (implementados)

| ID | Bloco |
|----|-------|
| 180 | Heavy Crusher (controller) |
| 181 | Heavy Compressor (controller) |
| 182 | Heavy Extractor (controller) |
| 183 | Nickel Ore |
| 184 | Silver Ore |
| 185 | Platinum Ore |
| 186 | Cinnabar Ore |
| 187 | Alloy Blender (single-block) |
| 188 | Heavy Furnace (controller) |
| 189 | Heavy Electrolyzer (controller) |
| 190 | Heavy Retort (controller) |
| 191 | Heavy Blender (controller) |
| 192 | Vulcanized Cable |
| 193 | Chemical Mixer (single-block) |
| 194 | Ore Washer (single-block) |
| 195 | Heavy Ore Washer (controller) |
| 196 | Heavy Chemical Mixer (controller) |
| 197 | Lithium Ore |
| 198 | Fusion Reactor (controller) |
| 199 | Reservado (Industrial controllers) |

## Item IDs (implementados, range 600+)

| ID | Item |
|----|------|
| 600-606 | Dusts: Nickel, Silver, Platinum, Cinnabar, Sulfur, Invar Blend, Electrum Blend |
| 607-611 | Ingots: Nickel, Silver, Platinum, Invar, Electrum |
| 612-613 | Plates: Invar, Platinum |
| 614-615 | Circuits: Heavy, Industrial |
| 616-619 | Components: Vulcanized Rubber, Solid Mercury, Coke, Tar |
| 620 | Lithium Dust |
| 621 | Tiny Lithium Dust |
| 622 | Lithium Cell |
| 623 | Lithium Waste |
| 624 | Neutron Fluid (cell) |
| 625 | Uranium Waste |
| 626 | Heavy Water Cell |
| 627 | Deuterium Cell |
| 628 | Tritium Cell |

---

## Receitas de Crafting

### Heavy Circuit (Tier 3)
```
[Purified Gold Ore]   [Gold Cable]         [Purified Gold Ore]
[Purified Silver Ore] [Adv Circuit]        [Purified Silver Ore]
[Gold Cable]          [Gold Cable]         [Gold Cable]
```

### Alloy Blender (Single-Block, MV)
```
[Refined Iron]   [Piston]        [Refined Iron]
[Adv Circuit]    [Machine Block] [Adv Circuit]
[Refined Iron]   [MFE]           [Refined Iron]
```

### Vulcanized Cable
```
[Vulcanized Rubber]
[Gold Cable]
[Vulcanized Rubber]
```
→ Yields 4 cables
