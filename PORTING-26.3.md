# Porting notes: Minecraft 26.3 (Fabric)

Branch `fabric-26.3`, based on `fabric-1.21.4`. Builds with `JAVA_HOME=<JDK 25 with javac> ./gradlew build`; `./gradlew test` runs the unit tests.
The port was written with Claude (Claude Code).

## Build setup
- Loom 1.19.3 (`net.fabricmc.fabric-loom`), Gradle 9.8.2, Java 25. 26.x is unobfuscated: no `mappings` line,
  no `mod*` configurations, no `remapJar`. Source uses Mojang names (`Identifier`, `Minecraft`, ...), converted from Yarn.
- `Hyphen` 0.4.0-rc.5 and `Taski` 2.1.0 were only published on `notalpha.dev/maven`, which was unreachable. They were built
  from the `alphaqu/Hyphen` / `alphaqu/Taski` repos (same versions) with `publishToMavenLocal` and are resolved from `mavenLocal()`.
  Hyphen's own wrapper is Gradle 7.4.2 (needs an older JDK); built with Gradle 9 on JDK 25 it needs one fix in its build script (`base.archivesName.get()`). A PR for that was closed as not needed (a commenter said it builds fine with its own wrapper; I did not try that). The `notalpha.dev` repo is restricted to group `dev.notalpha`.
- `src/dev` is a second source set (mod id `dashloader_dev`) with the development tools; it is on the `runClient` classpath but not in the jar.
  The only thing the main code knows about it is the no-op `misc/DebugHooks`.

## What works
Fonts (bitmap, unihex, space, TrueType), sprite contents + images, splash texts, loading hook, caching toast (simplified drawing),
config screen, cache plumbing, and the new caches, all verified against freshly computed results when the cache is made:
- **model geometry** (`client/model`, `GeometryBakeMixin`, `ItemGeneratorBakeMixin`): the baked quads of cuboid models, keyed by (model name, texture slots,
  model-state matrices). Keys which bake to different results are dropped. Anything not in the cache is baked normally.
- **font sets** (`FontSetModule`): which providers a font really uses and its glyphs-by-width groups.
- **model groups** (`ModelGroupModule`): which block states look the same. Block states are named by block + index inside the block's own state
  list, **not** by global id: with many mods the registry order differs between launches (found on the 77 mod pack).
- **mip levels** (`MipmapModule`): mip chains of sprites; sprites coming from the sprite cache skip the transparency pass.
- **fast resource pack filters** (`misc/filter`, `ResourceFilterMixin`, not a cache): a pack with a `filter` section of hundreds of regular expressions
  made `ResourceFilterSection.isPathFiltered` the biggest CPU consumer of the reload. `RequiredLiterals` extracts the text a regex needs, `LiteralSearch`
  (Aho-Corasick) finds it in a path in one pass, and a regex only runs when its text is present. Answers are memoised per path. Short lists (< 8) are left
  to the game. Unit-tested against running every regex (`PathFilterIndexTest`, including random patterns).

## Measurements
Ryzen 7 3700X (16 threads), dev client, cold launches, resource reload only.

Fabric API + Mod Menu (11 mods without DashLoader), median of 3-4:

| | without DashLoader | with DashLoader | change |
|---|---|---|---|
| resource reload | 1514 / 1535 / 1544 / 1636 ms (median ~1540) | 1157 / 1155 / 1180 ms (median 1157) | about -25% |
| main thread blocking | 596 / 570 / 574 / 675 ms (median 585) | 501 / 538 / 601 ms (median 538) | about -8%, ranges overlap, not proven |

77 mods ("Better Adventures+", with Sodium, Iris, Continuity, Entity Model Features/Texture Features, C2ME, Lithium), median of 4:

| | without DashLoader | dev run, before filter fix | **release jar** loaded from `mods/` |
|---|---|---|---|
| resource reload | 4265 ms | 3838 ms | **3331 ms (-22%)** |
| main thread blocking | 1700 ms | 1472 ms | **1122 ms (-34%)** |

The first launch with the jar (builds the cache) finished its reload in 3724 ms. Reload CPU samples (JFR) dropped from 2115 to 1476 after the filter fix.
Equivalence (`DASHLOADER_DUMP=1`, save run vs load run): 0 differing lines of 12,466 (vanilla assets: 9,175 geometries, 3,281 mips, groups, fonts) and of
15,074 (pack: 10,527 geometries, 4,530 mips, groups, fonts).
Not measured: bootstrap and total startup, machines with fewer threads, other GPUs/OS.

## Retired (measured, then removed rather than ported)

| Feature | Why |
|---|---|
| Old model cache (`BakedModel` objects) | the model system was replaced; the geometry + group caches take its place. Profile of the 77 mod pack: JSON parsing is only ~8% of the remaining reload CPU, so a parsed-JSON cache would not pay |
| Sprite stitcher cache | stitching takes 3-42 ms per atlas on workers (~190 ms aggregate, almost nothing in wall time) |
| Atlas cache | atlas upload is a GPU copy; caching needs a GPU readback, and conflicts with Sodium/Iris; sprite pixels and mips are already cached |
| Shader cache | the cost is the driver compiling GLSL, which cannot be cached safely; config/source reading is a few ms |
| `WallBlockMixin` | `getShapeMap` no longer exists, shapes are lazy |
| `MipmapHelperMixin` | the target changed (`MipmapGenerator`) and `MipmapModule` covers the work |
| `ModelIdentifierMixin` | `ModelResourceLocation` equality is no longer a hot spot |
| Toast glow/vertex effects | extractor GUI has no raw vertex drawing; lines are drawn as stepped rectangles |

Their options are listed in `ConfigHandler.RETIRED_OPTIONS` and ignored (with an info line) if still present in a config file.
The old code is in git history (`origin/fabric-1.21.4`).

## Findings worth knowing
- Iris/Sodium/others do not stop the caches from working; the mip cache saved nothing on the pack because a mod's 19x6 texture forces the block atlas to
  mip level 0 (nothing to cache), not because of a conflict.
- `MessageDigest`/`ZipFile.getEntryPos` show up in profiles of big packs: jar signature checks and per-jar lookups of the game's resource system, not DashLoader.
- The dev Minecraft jar built by Loom has Fabric API's interface injections in it, so a dev run without Fabric API crashes; "DashLoader without Fabric API" is
  therefore untested.

## Tools (git-excluded `scratch/tools`, not in the repo)
`check_mixins.py` validates mixin targets/shadows/accessors/registration against the 26.3 jar (also covers `src/dev`); `run_client.sh <log>` launches the dev client
and stops it once DashLoader is done; `cycle.sh`/`bench.sh` rebuild the cache and measure; `analyze_jfr.py` groups profiler samples; `run_pack.sh`, `run_jar.sh`,
`compare*.sh` and the `*.init.gradle` files run the copied modpack and the release jar. `DASHLOADER_PROFILE=1` enables the timeline mixins, `DASHLOADER_DUMP=1`
writes `run/dashloader-debug/{save,load}.txt` (the two must be identical after sorting).

## Known risks
- Mixin names/descriptors are validated statically; injection points beyond that are only exercised by launching.
- `SpriteOpenerMixin` hooks `SpriteResourceLoader.lambda$create$0` (a synthetic lambda name).
- `DashTrueTypeFont` builds through the real `TrueTypeGlyphProvider` constructor and copies its state.
- Old caches are invalid (new Minecraft version; Dash object layouts changed).
- Tested only on one machine with one pack, in a dev client with the jar loaded from `mods/` (not through a launcher).
