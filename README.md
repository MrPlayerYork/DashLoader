# DashLoader for Minecraft 26.3 (unofficial port)

DashLoader makes Minecraft load its resources faster. The first launch builds a cache, later launches read it back instead of
redoing the work, and a few slow steps of the resource reload are made cheaper even without a cache.

This is an **unofficial port** of [alphaqu/DashLoader](https://github.com/alphaqu/DashLoader) to Minecraft 26.3. The original
authors (!alpha, bendy1234, leocth) are not involved: please report problems here, not to them.
**The port was written with Claude (Claude Code), as a fun experiment to see if the mod could be made to work on 26.3.**
It is a beta, and it has not been tested with many modpacks yet (see [Compatibility](#compatibility)).

## Install

You need Minecraft **26.3** with [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.5 or newer** and **Java 25**.
DashLoader only runs on the client (singleplayer and multiplayer), it does nothing on a server.

1. Download `dashloader-<version>.jar` from the [releases page](https://github.com/MrPlayerYork/DashLoader/releases). Don't download the
   `-sources` jar.
2. Put the jar in the `mods` folder of your game:

| Launcher | How |
|---|---|
| **Prism Launcher** | Right-click the instance, *Edit*, *Mods*, *Add file*, pick the jar. Or *Folder*, *Instance Folder*, open `minecraft/mods` and drop it in. |
| **Modrinth App** | Open the instance, *Content* (or *Mods*), *Add content*, *Add from file*, pick the jar. |
| **CurseForge app** | Open the profile, the *...* menu, *Open Folder*, then `mods`, and drop the jar in. |
| **Official launcher or a manual Fabric install** | Drop the jar in `.minecraft/mods`. |

3. Start the game. **The first launch can be slower than normal**, because the cache is built. Later launches with the same mods and
   resource packs use it.

Install [Fabric API](https://modrinth.com/mod/fabric-api) as well, like nearly every Fabric modpack does. DashLoader does not call it itself, but it has
only been tested with it installed. Optionally add [Mod Menu](https://modrinth.com/mod/modmenu) to get a settings screen for DashLoader.

### Putting it in a modpack

Add the jar to the `mods` folder of the pack (for a `.mrpack` that is `overrides/mods/`). Players get their own cache the first time
they launch the pack, no cache is shipped. Platforms have their own rules about which files a pack may contain, check them before
you upload anywhere. DashLoader is licensed under the LGPL-3.0, see `LICENCE`.

## How much faster?

Measured in the development client on a Ryzen 7 3700X (16 threads), cold launches, median of 3 to 4 launches, resource reload only:

| Setup | Without DashLoader | With DashLoader | Change |
|---|---|---|---|
| Fabric API + Mod Menu (vanilla assets) | about 1540 ms reload | about 1157 ms | about **-25%** |
| 77 mods (Sodium, Iris, Continuity, Entity Model Features, C2ME, Lithium, ...) | 4265 ms reload, 1700 ms main thread blocked | 3331 ms reload, 1122 ms main thread blocked | **-22%** reload, **-34%** main thread |

These are one machine and the resource reload, not the whole startup. Machines with fewer threads may gain more, because the
reload is limited by the processor there, but I have not measured that. A launch which has to build the cache costs extra.

## What it does

| Option (in `dashloader.json`) | What it caches or speeds up |
|---|---|
| `CACHE_MODEL_LOADER` | The baked geometry of block and item models, and which block states share a model. |
| `CACHE_FONT` | Font providers and glyph groups, and font images. |
| `CACHE_SPRITE_CONTENT` | Sprite pixels and their mip levels. |
| `CACHE_SPLASH_TEXT` | The splash texts on the main menu. |
| `FAST_RESOURCE_FILTER` | Not a cache: resource packs which block many paths with regular expressions (some packs hide hundreds of vanilla files that way) make the game test every file against every expression. This gives the same answers much faster. |

Everything which is cached is checked when the cache is made. Whatever cannot be cached (or is not in the cache) is loaded the normal
way, so unknown mods and resource packs only lose the speedup, and nothing is replaced with something different.

## Configuration

The settings are in `config/dashloader.json`, or in game through Mod Menu. To turn a feature off:

```json
{
  "options": { "FAST_RESOURCE_FILTER": false },
  "compression": 3,
  "maxCaches": 5,
  "showCachingToast": true
}
```

- The cache lives in `dashloader-cache/` inside the game folder. A cache was about 6 MB in my tests, and the newest `maxCaches` (5) are kept,
  one for each combination of mods and resource packs.
- **To clear the cache**, delete that folder, or press **F3 + T** in game (reload resources) to rebuild it. **Shift + F3 + T**
  reloads resources without touching the cache.
- Options from older versions which no longer exist (`CACHE_ATLASES`, `CACHE_SHADER`, `CACHE_SPRITE_STITCHING`,
  `FAST_MODEL_IDENTIFIER_EQUALS`, `FAST_WALL_BLOCK`, `UNSAFE_MIPMAP_GENERATION`) are ignored.

## Compatibility

DashLoader 5 does not need other mods to know about it: a model, sprite or font it cannot cache is loaded normally.

- Tested in the development client with a 77 mod pack, including Sodium, Iris, Continuity, Entity Model Features and Entity Texture
  Features, C2ME and Lithium. The game loads, and the cached results were compared with freshly computed ones (fonts, geometry,
  model groups, mips: no differences).
- **Not tested yet:** larger or different modpacks, other platforms and graphics cards, VulkanMod, other rendering mods, and
  multiplayer servers with unusual resource packs. If something looks wrong, delete `dashloader-cache` first, and then please open an
  issue with the full log.
- With some packs a cache stays empty for a step which does not apply to them. For instance, if a mod adds a texture whose size forces
  the block atlas to have no mipmaps, there are no mip levels to cache.

### Removed from older versions

The atlas cache, sprite stitching cache and shader cache of the older versions are gone. On 26.3 sprite stitching takes 3 to 40 ms per
atlas on worker threads, atlas upload happens on the GPU and would have to be read back (which also conflicts with Sodium and Iris),
and shader time is spent in the graphics driver. They would have cost more in risk than they gain. `WallBlockMixin`, the unsafe mipmap
mixin and the fast `ModelIdentifier` equals were removed because their targets changed or are covered by the new caches.

## Building from source

You need JDK 25. DashLoader depends on two libraries from the original author, [Hyphen](https://github.com/alphaqu/Hyphen) and
[Taski](https://github.com/alphaqu/Taski). Their maven (`notalpha.dev`) has been unreachable, so build them into your local maven first.
Hyphen's own Gradle wrapper (7.4.2) needs an older JDK; I built it with a newer Gradle on JDK 25 instead, which needs one small change
in Hyphen's `build.gradle` (`archivesBaseName` was removed in Gradle 9). I have not tried building it on an older JDK.

```sh
git clone https://github.com/alphaqu/Hyphen && git clone https://github.com/alphaqu/Taski
# Only when building Hyphen with Gradle 9: in its build.gradle replace
#   rename { "${it}_${project.archivesBaseName}" }   with   rename { "${it}_${base.archivesName.get()}" }
(cd Hyphen && ./gradlew publishToMavenLocal) && (cd Taski && ./gradlew publishToMavenLocal)
./gradlew build          # the jar is in build/libs
./gradlew runClient      # a development client, also loads the tools in src/dev
```

`runClient` also loads the development-only tools in `src/dev` (not part of the jar): set `DASHLOADER_DUMP=1` to write hashes of everything
that is cached, to compare a launch that makes the cache with one that loads it, or `DASHLOADER_PROFILE=1` for the timeline of the
reload. `./gradlew test` runs the unit tests. More notes on the port are in [PORTING-26.3.md](PORTING-26.3.md).

## Third-party software and licenses

The release jar contains three libraries, unchanged, as nested jars in `META-INF/jars/`. Their license texts are in `META-INF/licenses/`
inside the jar and in [`licenses/`](licenses/) in this repository (overview: [`licenses/THIRD-PARTY.md`](licenses/THIRD-PARTY.md)):

| Library | Version | License |
|---|---|---|
| [Hyphen](https://github.com/alphaqu/Hyphen) (serialization) | 0.4.0-rc.5 | MIT, (c) 2021 QuantumFusion |
| [Taski](https://github.com/alphaqu/Taski) (task progress) | 2.1.0 | MIT, (c) 2021 QuantumFusion |
| [zstd-jni](https://github.com/luben/zstd-jni) (compression) | 1.5.7-1 | BSD-2-Clause, (c) 2015-present Luben Karavelov |

DashLoader itself is LGPL-3.0 (`LICENCE`).

## Credits

DashLoader was made by !alpha, bendy1234 and leocth, with thanks to YourKit (profiler) and JetBrains (IDEs) for the licences they
gave the original project. Hyphen and Taski are by !alpha and contributors (QuantumFusion), zstd-jni by Luben Karavelov. The 26.3 port is by MrPlayerYork, with Claude (Anthropic) writing most of the code.
