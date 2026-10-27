# Gottsch Books (NeoForge 1.21.1)

An in-game book library: lore books and mod manuals written in a Markdown subset, read in a
vanilla-styled book screen. A replacement for Patchouli in gottsch's mods, usable by any mod.

- **Mod id:** `gottschbooks`
- **Maven:** `gottsch:gottschbooks-neoforge-1.21.1:<version>`
- **No dependencies** beyond NeoForge (not even GottschCore).

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

**Books that are part of the game** (nest it, require it):

```groovy
jarJar(implementation("gottsch:gottschbooks-neoforge-1.21.1")) {
    version { strictly "[1.0,2.0)"; prefer "1.0.0" }
}
```

```toml
[[dependencies.<modid>]]
modId="gottschbooks"
type="required"
versionRange="[1.0,2.0)"
ordering="NONE"
side="BOTH"
```

**A manual only** (optional): `compileOnly` against the library plus a `localRuntime` copy for dev
runs, `type="optional"` with the same range, and touch library classes only after
`ModList.get().isLoaded("gottschbooks")`.

## Versioning

Semver. Every 1.x release stays backwards compatible with books and API calls written for 1.0;
anything breaking is 2.0.

## Building

```
./gradlew build
./gradlew publishAll   # to ../maven and mavenLocal
```

## Example books

`src/example/resources` holds three example books, loaded in dev runs (`./gradlew runClient`) and never put in the
jar:

- `example:journal`: a lore book, *The Lighthouse Keeper's Journal*. It shows illustrations, ornaments, a spotlight
  and an image.
- `example:field_guide`: a manual, *A Field Guide to the Overworld*. It shows headings, lists, colours, notes, links,
  spotlights, recipes of every vanilla kind (*Furnaces and Workstations*), mobs, an image, items with components and
  a picture font (*Potions and Curios*), and an entry locked behind the *Diamonds!* advancement.
- `example:mistakes`: a manual that is wrong on purpose, for `/gottschbooks check example:mistakes` to find.

The first two are given on first join. They are also a working reference for [WRITING.md](WRITING.md), and the source of the
mod page's screenshots.
