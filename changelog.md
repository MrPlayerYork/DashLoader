# DashLoader 5.1.0-beta.10 for Minecraft 26.3

Unofficial port of DashLoader to Minecraft 26.3, written with Claude (Claude Code). It has been tested in the development client, including with a 77 mod pack (Sodium, Iris, Continuity, Entity Model Features, C2ME, Lithium and more), but not yet widely.

## New

- **Model geometry cache**: the baked quads of models (including generated item models) are cached, and checked when the cache is made. Models which can't be cached are baked as usual.
- **Model group cache**: which block states look the same, so the single threaded grouping step is skipped. Block states are identified by block name and state, not by registry id, because the registry order is not the same on every launch with many mods.
- **Font set cache**: which providers a font uses and its glyph groups.
- **Mip level cache**: mip chains of sprites are loaded from the cache.
- **Fast resource pack filters**: resource packs which block hundreds of paths with regular expressions (for example a pack hiding all vanilla recipe advancements) made the game test every resource against every expression, many times over. The same answers are now worked out much faster. This is not a cache and has an option (`FAST_RESOURCE_FILTER`).

## Changed

- Ported to Minecraft 26.3 (Mojang names, Java 25, Fabric Loader 0.19.5 or newer).
- Development tools (state dumps and stage timings) are no longer part of the mod, they only exist in the development environment.

## Removed

- Atlas cache, sprite stitching cache, shader cache, the wall block mixin, the unsafe mipmap mixin and the fast model identifier equals. On 26.3 they either gain close to nothing (stitching takes 3 to 40 ms per atlas on worker threads), need reading back from the GPU, conflict with Sodium and Iris, or target code that no longer exists. Their options in `dashloader.json` are ignored.
- Publishing to Modrinth, CurseForge and the notalpha maven.
