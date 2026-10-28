# Porting notes: Minecraft 26.3 (Fabric)

Branch `fabric-26.3`, based on `fabric-1.21.4`. Builds with `JAVA_HOME=<JDK 25 with javac> ./gradlew build`.

## Build setup
- Loom 1.19.3 (`net.fabricmc.fabric-loom`), Gradle 9.8.2, Java 25. 26.x is unobfuscated: no `mappings` line,
  no `mod*` configurations, no `remapJar`. Source uses Mojang names (`Identifier`, `Minecraft`, ...), converted from Yarn.
- `Hyphen` 0.4.0-rc.5 and `Taski` 2.1.0 were only published on `notalpha.dev/maven`, which was unreachable. They were built
  from the `alphaqu/Hyphen` / `alphaqu/Taski` repos (same versions) with `publishToMavenLocal` and are resolved from `mavenLocal()`.
  The `notalpha.dev` repo is restricted to group `dev.notalpha` so a dead server cannot stall other lookups.

## Working (compiles, mixin targets checked statically against the 26.3 jar, NOT yet run in game)
Fonts (bitmap, unihex, space, TrueType), sprite contents + images, splash texts, loading hook, caching toast (simplified drawing),
config screen, cache plumbing, and **model geometry** (`client/model`, `GeometryBakeMixin`): the baked quads of cuboid models are cached
per (model name, texture slots, model-state matrices). Entries are verified by rebuilding them on save; keys that bake to different
results are dropped. Anything not in the cache is baked normally, so unknown models cost nothing but the speedup.
Generated item models (`ItemGeneratorBakeMixin`) go through the same caching.
Also cached, each verified against freshly computed results:
- **font sets** (`FontSetModule`): which providers a font really uses and its glyphs-by-width groups, the largest single-threaded step on the main thread;
- **model groups** (`ModelGroupModule`): which block states look the same, checked against a checksum of the block registry;
- **mip levels** (`MipmapModule`): the mip chains of sprites, and sprites coming from the sprite cache skip the redundant transparency pass.
Launched and tested in the dev client (cache build + cache load).

Measured on a Ryzen 7 3700X (16 threads), median of 3 launches, no caches vs all caches: reload 1670 ms -> 1330 ms, main thread blocking 544 -> 458 ms,
CPU used by the reload (JFR samples) -> 28%. The gain is larger on machines with fewer threads, where the reload is CPU bound.

Measured against a real no-DashLoader client (Fabric API + Modmenu only, 11 mods, same machine and world; 4 cold launches without, 3 with the cached path):
| | without DashLoader | with DashLoader | change |
|---|---|---|---|
| resource reload | 1514 / 1535 / 1544 / 1636 ms (median ~1540) | 1157 / 1155 / 1180 ms (median 1157) | about -25% |
| main thread blocking | 596 / 570 / 574 / 675 ms (median 585) | 501 / 538 / 601 ms (median 538) | about -8%, ranges overlap, not proven |

The reload gain is solid; the main-thread gain is small and noisy at this sample size (most of the saving is on worker threads). The "all caches off" figures above
still had the mod's mixins loaded, hence the lower gain there. Only the cached path was compared (a first launch also pays for building the cache); bootstrap and total
startup were not measured. The no-DashLoader run is a temporary `-Pbaseline` Gradle block (source set without the mod) driven by `scratch/tools/{run_baseline,compare}.sh`.

## Disabled (code kept under `src/disabled/`, not compiled)
| Feature | Why |
|---|---|
| Old model cache (`client/model` in `src/disabled`, `client/blockstate`, `DashModelIdentifier`) | `BakedModel` & friends were replaced by the new model system; replaced by the geometry cache above |
| Shader cache (`client/shader`, shader mixins) | shader/GPU pipeline rewritten (`renderpearl`) |
| Atlas cache + sprite stitcher cache (`client/atlas`, `client/sprite/stitch`, `SpriteAtlasTextureMixin`, `StitchSpriteLoaderMixin`) | atlas upload/readback goes through the new GPU texture layer |
| `WallBlockMixin`, `MipmapHelperMixin` | micro-optimisations whose target code changed |
| Still not cached (ideas, by size): block state loading/instantiation, sprite stitching and atlas upload, shader loading | profiled, see notes above |
| Toast glow/vertex effects | extractor GUI has no raw vertex drawing; lines are drawn as stepped rectangles |

Removed access-widener lines are kept in `src/disabled/dashloader.accesswidener.disabled`.
The config screen still lists the options of the disabled features.

## Tools (git-excluded `scratch/tools`)
`check_mixins.py` validates mixin targets/shadows/accessors/registration against the 26.3 jar; `run_client.sh <log>` launches the dev client and stops it once DashLoader is done; `cycle.sh`/`bench.sh` rebuild the cache and measure (JFR + timeline); `analyze_jfr.py` groups profiler samples.
`DASHLOADER_PROFILE=1` enables the reload timeline mixins (`[timeline]` log lines). `DASHLOADER_DUMP=1` writes `run/dashloader-debug/{save,load}.txt` with hashes of everything that is cached; the two files must be identical (`diff`).

## Known risks
- Mixin names/descriptors are validated statically and the client launches; injection points beyond that are only exercised by the launch.
- `SpriteOpenerMixin` hooks `SpriteResourceLoader.lambda$create$0` (a synthetic lambda name).
- `DashTrueTypeFont` now builds through the real `TrueTypeGlyphProvider` constructor and copies its state.
- Old caches are invalid (new Minecraft version; some Dash object layouts changed).
