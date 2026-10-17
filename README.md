# Gottsch Books (NeoForge 1.21.1)

An in-game book library: lore books and mod manuals written in a Markdown subset, read in a
vanilla-styled book screen. A replacement for Patchouli in gottsch's mods, usable by any mod.

- **Mod id:** `gottschbooks`
- **Maven:** `gottsch:gottschbooks-neoforge-1.21.1:<version>`
- **No dependencies** beyond NeoForge (not even GottschCore).

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

## Registering a book

Text and art are client assets, under `assets/<ns>/books/<book>/`. The server also needs to know the book
exists, so that it counts toward advancements:

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
./gradlew publishAll   # to ../maven and mavenLocal
```
