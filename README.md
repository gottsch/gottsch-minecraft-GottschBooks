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

**To have an AI write them,** see [Writing books with an AI](#writing-books-with-an-ai).

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

## Writing books with an AI

[AI-INSTRUCTIONS.md](AI-INSTRUCTIONS.md) teaches an AI the book format, so it can turn your mod's docs or wiki into a
whole manual (or write a lore book). It works in any chat AI, free plans included: claude.ai, ChatGPT, Gemini and
others.

Players and pack makers don't need this repo: each release on CurseForge and Modrinth has an **AI kit**
(`gottschbooks-ai-kit-<version>.zip`) as an additional file, holding the instructions, both scripts and a plain-text
how-to. `./gradlew aiKit` builds it (it is also built with the jar, into `build/libs`).

1. **Start a chat** and attach (or paste) `AI-INSTRUCTIONS.md`, your docs, and a list of your item and recipe ids
   (your `en_us.json` lang file and recipe folder will do). Say what you want: "a manual for my mod, for NeoForge
   1.21.1". It asks for anything it still needs.
2. **Ask for the outline first**, the categories and entries, and fix it before any files are written. Then ask for
   one category per reply ("write the Machines category"), so no reply gets cut off. Free plans limit replies and
   messages, so a big manual may take a couple of sessions.
3. **Save each reply** as a text file (copy the whole reply into `answer1.txt`, `answer2.txt`...), then let a script
   write the files into place:

   ```
   python tools/split-book.py answer1.txt answer2.txt --out path/to/src/main/resources
   ```

   On Windows without Python, the same in PowerShell:

   ```
   powershell -ExecutionPolicy Bypass -File tools\split-book.ps1 answer1.txt answer2.txt -Out path\to\src\main\resources
   ```

   Add `--dry-run` (`-DryRun`) to see the list first. The scripts only write under `assets/` and `data/` in the
   folder you give, and flag any file that looks cut off. For a modpack without a mod, point `--out` at your
   resource pack, then move the `data/` folder into your data pack.
4. **Check it in game:** press F3+T and run `/gottschbooks check`. Paste anything it reports back into the chat
   for the AI to fix.

The AI lists every place it needed an id it didn't have, rather than making one up. In Claude Code the instructions
also work as a skill: save the file as `.claude/skills/gottschbooks-author/SKILL.md`.

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

- `example:journal`: a lore book, *The Lighthouse Keeper's Journal*. It shows illustrations, ornaments, a spotlight,
  an image and a table.
- `example:field_guide`: a manual, *A Field Guide to the Overworld*. It shows headings, lists, colours, notes, links,
  spotlights, recipes of every vanilla kind (*Furnaces and Workstations*), tables with item icons on a single wide
  page (*Ores at a Glance*), mobs, an image, items with components and a picture font (*Potions and Curios*), and an
  entry locked behind the *Diamonds!* advancement.
- `example:mistakes`: a manual that is wrong on purpose, for `/gottschbooks check example:mistakes` to find.

The first two are given on first join. They are also a working reference for [WRITING.md](WRITING.md), and the source of the
mod page's screenshots.
