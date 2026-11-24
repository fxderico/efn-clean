# EFN Client 1.2.0: cleaned rebuild

A buildable source reconstruction of **EFN Client 1.2.0** (Fabric, Minecraft 1.21.11) with the
**malware removed**. The jar distributed as `efn-client-1.2.0.jar` was trojanized with a session
stealer + Python info-stealer loader. Full analysis, IOCs and detection rules:
[fxderico/efn-scan](https://github.com/fxderico/efn-scan) (`efn-client-decompiled/README.md`).

```sh
./gradlew build          # -> build/libs/efn-client-1.2.0-clean.jar
```
Requires JDK 21. The wrapper fetches Gradle 9.7.1. Loom 1.17.21, Yarn 1.21.11+build.6.

## What was removed

| Removed | Why |
|---|---|
| `net/efn/agent/AgentInit` | Dropper. Decrypted and loaded the stealer at every game start. |
| `assets/efn/data/runtime.dat` | The encrypted stealer (stage 2). |
| `"main": ["net.efn.agent.AgentInit"]` in `fabric.mod.json` | The entrypoint that ran the dropper. |
| `Main-Class: net.efn.agent.AgentInit` | Pointed back at the original `net.efn.launcher.InstallNotice` ("put this in your mods folder" dialog). |

Nothing else in the client loads remote code. The bundled libraries (7 Fabric API modules,
imgui-java 1.90.0, netty-socks/proxy 4.2.7) were verified **byte-identical** to their Maven
artifacts; the only difference is the `fabric.mod.json` that Loom adds. They're now pulled from Maven
instead of shipped as binaries.

**Evidence the jar was tampered with after it was built:**
- Every class is timestamped 17:08.
- `fabric.mod.json` was rewritten at 17:37 and the manifest at 17:47.
- `AgentInit.class` and `runtime.dat` were added at **18:11**.
- `net.efn.bootstrap.EfnNativeBootstrap` (a `preLaunch` entrypoint, a no-op in this build) is left
  unregistered, which suggests the dropper's `main` entry replaced it.

## How the source was rebuilt

1. Remapped intermediary → Yarn with tiny-remapper, including the Mixin extension, with Minecraft,
   Litematica and MaLiLib on the classpath.
2. Decompiled with **Vineflower 1.11.1**. For the 135 files Vineflower couldn't produce compilable
   code for, **CFR 0.152** output was used where it was better. The rest were fixed by hand (raw
   generics, mixin `(Object) this` casts, re-sugared try-with-resources/finally, etc.). 0 compile errors.
3. **Verified against the original bytecode.** `tools/BytecodeDiff.java` compares every method of the
   built jar with the original jar. It normalizes local-slot numbers by first use, so using the wrong
   variable shows up. It ignores checkcasts, labels, branch inversion and synthetic lambda numbering.

   Result: **792/1350 classes and 11,017/13,179 methods are instruction-identical**. Every remaining
   difference was triaged: return/stack layout, `arr[i]++` vs `arr[i] = arr[i] + 1`, field re-reads,
   interface-vs-class call targets, implicit array zeros, and bridge naming.

### Readability pass

Local variable names don't exist in compiled code, so the originals can't be recovered. These names
are made up, but they're derived from what each variable holds (`tools/Renamer.java`, ~38,000
renames):

- **From the initializer or first assignment:** `this.findSourceSlot(..)` gives `sourceSlot`,
  `player.getEyePos()` gives `eyePos`, `this.range.get()` gives `range`, `players.get(i)` gives
  `player`, and `EfnWidgets.s(14)` gives `px14` (14px at UI scale).
- **For-each variables** take the singular of the collection: `this.modules` gives `module`.
- **Parameters:** `this.foo = x` gives `foo`, `setFoo(x)` gives `foo`, and mixin conventions give
  `ci`/`cir`.
- **Fallback:** a type-based name (`client`, `player`, `blockHitResult`, `entityStatusPacket`), and
  Yarn-style letters (`i`, `j`, `f`, `g`, `bl`) for math temporaries and loop counters.

The renamer is scope-aware, so no new name can shadow a field or a captured variable. Then
google-java-format (AOSP: 4-space indent, 100 columns) was applied. Checked with
`tools/BytecodeDiff.java` against the build from just before the rename: **13,194/13,194 methods
identical**.

What's left: math-heavy rendering code (character models, HUD layout) still uses `f`/`g`/`h`
temporaries for intermediate values, the same as vanilla's decompiled code. There are also some
decompiler expression shapes and `(Iterable<T>)` casts. No comments survived compilation.

### Decompiler bugs caught by that verification and fixed

These would have compiled fine and silently changed behavior:

- `TextureAtlas.pow2`: a `byte` doubling loop (`16→32→64→-128`) meant an **infinite loop** for sizes over 64.
- `CharacterBuilder.emitClipped`: a `byte` polygon-fan counter overflows past 128 vertices.
- `SimulationSignals.decay`: a missing `(double)` cast turned `exp(-t/T)` into integer division.
- `CrystalBotModule.worth`: Vineflower printed `+= 1.5` as `++`.
- `WeaponInspectModule.sink`: a shadowed parameter turned `(!bl || otherFree)` into `(!bl || bl)`.
- CFR mistranslations, each checked against Vineflower and the bytecode:
  - `Luna.paintBand` / `Luna.capeColor`: wrong variable used.
  - `BlockLayer.render`: an alpha check read an uninitialized local.
  - `ChunkScanner`: a `blockIndex++` assignment was dropped.
  - `AntiVanishModule`, `ElytraAutopilotModule`, `GoliathRtpModule`, `WaypointsModule`: garbled nested ternaries and squared-distance expressions.
  - `ThemeImages.decode`, `CharacterBake.uvBounds`: CFR "failed to decompile" stubs replaced with
    Vineflower's versions.
- Byte/short loop counters that were `int` in the original were restored.

Build-specific: Yarn names two distinct intermediary methods `getShadowRadius`, so javac emits an
inherited bridge into the two `*AvatarRenderer` mixins. The original has no such bridge, and Mixin
would merge it into `PlayerEntityRenderer`. `build.gradle` strips those bridges after compiling.

## Not verified

- **Not run in-game.** Equivalence is established at the bytecode level, not by playtesting.
- About 100 methods differ in ways I classified as benign by pattern rather than reading each one
  line by line. `tools/BytecodeDiff.java` is included so you can re-check any class:
  `java -cp asm.jar:asm-tree.jar:asm-util.jar tools/BytecodeDiff.java <original.jar> build/libs/<built>.jar net/efn/agent/`

## Notes

- `config/efn/accounts.json` (the alt manager) stores Microsoft **refresh tokens in plaintext**, the
  same as the original client. Treat that file like a password.
- The original is "All-Rights-Reserved" software by its authors; this repo is a decompilation.
  Keep the repo private if you don't want to deal with takedown requests.
