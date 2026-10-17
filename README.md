# Gottsch Books (Forge 1.20.1)

An in-game book library: lore books and mod manuals written in a Markdown subset, read in a
vanilla-styled book screen. A replacement for Patchouli in gottsch's mods, usable by any mod.

- **Mod id:** `gottschbooks`
- **Maven:** `gottsch:gottschbooks-forge-1.20.1:<version>`
- **No dependencies** beyond Forge (not even GottschCore).

## Using it from another mod

**Books that are part of the game** (nest it, require it):

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

**A manual only** (optional): `compileOnly fg.deobf(...)` against the library plus a `runtimeOnly fg.deobf(...)` copy for
dev runs, `mandatory=false` with the same range, and touch library classes only after
`ModList.get().isLoaded("gottschbooks")`.

## Writing a book

A lore book is one Markdown file per language, at `assets/<ns>/books/<book>/<locale>/text.md`. A language the book
doesn't have falls back to `en_us`:

```markdown
---
title: The Tidewalker's Log
author: Selene Tidewalker
---

![](item:magictreasures:white_pearl)

The first section. A blank line starts a new paragraph; *italic*, **bold**, {gold}gold{} and {#d4a017}hex{}.

> [An editor's note, in grey italics.]

---

The second section, after an ornament.
```

An illustration is `![caption](scheme:ns:id)` on a line of its own. With no caption, the item's name is shown.
`item` is built in; a mod adds more with `Illustrations.register` (Magic Treasures adds `preset`). An optional
`assets/<ns>/books/<book>/book.md` holding `skin: <ns>:<folder>` points the book at its own `cover.png`, `page.png`
and `ornament.png` (32x32 nine-slices, and a 21x5 ornament).

Open a book by right-clicking a `BookItem` (a stack with the `gottschbooks:book` NBT string), or from client code with
`GottschBooksClient.open(book, icon)`.

## Registering a book

The server also needs to know that the book exists, so that it counts toward advancements:

- `data/<ns>/gottschbooks/book/<book>.json`: `{}` (or `{"disabled": true}` to switch one off)
- optional tags: `data/<ns>/tags/gottschbooks/book/<tag>.json`

Advancements use the `gottschbooks:book_read` trigger:

```json
{ "trigger": "gottschbooks:book_read",
  "conditions": { "book": "ns:id", "books": "#ns:tag", "all_in": "ns" } }
```

All three conditions are optional. `books` and `all_in` need every enabled book in the tag or namespace to have
been read.

## Versioning

Semver. Every 1.x release stays backwards compatible with books and API calls written for 1.0;
anything breaking is 2.0.

## Building

```
./gradlew build
./gradlew publishAll   # to ../maven and mavenLocal (the jar is reobfuscated: SRG names)
```
