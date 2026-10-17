# Gottsch Books (Forge 1.20.1)

An in-game book library: lore books and mod manuals written in a Markdown subset, read in a
vanilla-styled book screen. A replacement for Patchouli in gottsch's mods, usable by any mod.

- **Mod id:** `gottschbooks`
- **Maven:** `gottsch:gottschbooks-forge-1.20.1:<version>`
- **No dependencies** beyond Forge (not even GottschCore).

Books are pure data: a resource pack and a datapack can add one, and the library's own `gottschbooks:book` item
(or `"give_on_first_join": true`) puts it in players' hands with no code.

**To write books, see [WRITING.md](WRITING.md):** the file layout, front matter, the Markdown subset, pictures,
spotlights and recipes, links, advancements, and how to depend on the library.

A taste of a lore book (`assets/<ns>/books/<book>/en_us/text.md`):

```markdown
---
title: The Tidewalker's Log
author: Selene Tidewalker
---

![](item:magictreasures:white_pearl)

The first section. *Italic*, **bold**, {dark_aqua}colour{}.

---

The second section, after an ornament.
```

## Using it from another mod

**Books that are part of the game** (nest it, require it). ForgeGradle 6:

```groovy
jarJar.enable()

dependencies {
    implementation fg.deobf("gottsch:gottschbooks-forge-1.20.1:1.0.0")
    jarJar(group: 'gottsch', name: 'gottschbooks-forge-1.20.1', version: '[1.0,2.0)') { jarJar.pin(it, '1.0.0') }
}
```

Ship the `jarJar` task's jar (reobfuscated by `reobfJarJar`), not `jar`'s.

```toml
[[dependencies.<modid>]]
modId="gottschbooks"
mandatory=true
versionRange="[1.0,2.0)"
ordering="NONE"
side="BOTH"
```

**A manual only** (optional): `compileOnly fg.deobf(...)` against the library plus a `runtimeOnly fg.deobf(...)` copy
for dev runs, `mandatory=false` with the same range, and touch library classes only after
`ModList.get().isLoaded("gottschbooks")`.

## Versioning

Semver. Every 1.x release stays backwards compatible with books and API calls written for 1.0;
anything breaking is 2.0.

## Building

```
./gradlew build
./gradlew publishAll   # to ../maven and mavenLocal (the jar is reobfuscated: SRG names)
```
