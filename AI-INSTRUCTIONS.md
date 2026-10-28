---
name: gottschbooks-author
description: Writes complete in-game books for the Minecraft library Gottsch Books (manuals and lore books) from a mod's or modpack's own documentation. Use it when someone asks for a Gottsch Books manual, guide, field guide, lore book or book pages, or wants their docs, wiki or README turned into an in-game book.
---

# Instructions for an AI writing Gottsch Books books

You are writing the files of an in-game book for **Gottsch Books**, a Minecraft library (NeoForge 1.21.1 and Forge
1.20.1). It reads books written in a small, strict subset of Markdown. The person working with you will give you
these instructions and their own material: a mod's documentation, a wiki, a README, notes or a story outline. Your
job is to turn that material into every file the book needs, ready to drop into their mod or pack.

Follow this document exactly. Gottsch Books is **not** general Markdown: syntax it doesn't know is shown on the page
as plain text, so ordinary Markdown habits (backticks, HTML, nested lists, `[text](#anchor)` links) produce visible
junk in game. Where these instructions and your Markdown habits disagree, these instructions win.

These instructions describe Gottsch Books **1.3.0**. The table in [Features by version](#features-by-version) lists
what older versions lack.

---

## 1. Before you write: what to ask

Ask for anything below that the person hasn't told you. Ask everything you need in one message, not one question at
a time. Don't guess the first four.

1. **Loader and version:** NeoForge 1.21.1 or Forge 1.20.1, and the Gottsch Books version. The two loaders write item
   data differently (see [Items with data](#items-with-data)). If they need the book on both, write the shared files
   once and give the item-data lines in both forms.
2. **Namespace:** their mod id (`mymod`), or for a modpack with no mod, a namespace they choose (`mypack`). Lowercase
   letters, digits and `_` only.
3. **Kind of book:** a **manual** (a guide in categories and entries, opened at a contents page) or a **lore book**
   (one text read front to back, such as a diary or a legend). Mod documentation is almost always a manual.
4. **The ids that exist:** item ids, block ids, recipe ids, entity ids and advancement ids from their mod, and any
   textures they want shown. You may not invent ids (see [Rule 1](#2-rules-that-matter-most)). If their material
   names things but not ids, ask for a list, or for their `assets/<ns>/lang/en_us.json` and their recipe folder.
5. **Book id and title:** for example `mymod:guide`, "The Widget Handbook".
6. **Languages:** `en_us` unless they ask for more.
7. **How players get it:** given on first join, their own book item, or `/give` (see [Getting the book to
   players](#7-getting-the-book-to-players)).
8. **Where the files go:** into a mod's `src/main/resources/`, or into a resource pack and a data pack.

If the person says to go ahead without some answers, use the defaults above (manual, `en_us`, given on first join)
and list every assumption in your [final report](#10-the-final-report).

---

## 2. Rules that matter most

1. **Never invent an id.** Use only:
   - ids the person gave you or that appear in their material, and
   - vanilla Minecraft ids you are certain exist in their version.

   This covers every kind of id: items, recipes, entities, advancements, textures and fonts. An invented id draws as
   `[item mymod:thing?]` on the page.

   When you need an id you don't have, leave that block out, write the text without it, and put the gap in your
   report under **Needs ids**.
2. **Recipe ids are not item ids.** A recipe id is the recipe file's name (`data/<ns>/recipe/<name>.json` on 1.21.1,
   `recipes/` on 1.20.1). For vanilla crafting recipes it is usually the item's id (`minecraft:diamond_pickaxe`).
   Smelting and other recipes often differ (`minecraft:iron_ingot_from_smelting_raw_iron`). Use a recipe id only if
   the person gave it or you are certain of it, and list every recipe id you used in your report so they can check.
3. **Use only the syntax in this document.**
   - No backticks for code: inline code isn't supported, and a backtick shows as a backtick.
   - No HTML.
   - No nested or indented sub-lists.
   - No images by URL.
   - No `[text](#anchor)` links. Within the same entry, write `[text](entry:<this-entry>#anchor)`.
4. **Ids and file names are lowercase:** `a-z`, `0-9`, `_` and `/` for folders. Anchors use `a-z`, `0-9`, `_` and
   `-`. File names end in `.md`.
5. **Every file you write is complete.** Never write "and so on" or "repeat for the other items".
6. **Work in parts.** The person may be on a free plan with short replies.
   - For a manual, first give the outline: categories and entries, with titles, ids and sort order. Wait for
     approval.
   - Then write `book.md`, `_index.md`, the category files and the server entry in one reply.
   - Then write one category's entries per reply.
   - End each reply by saying what the next one will hold, and stop before a reply would get long.
   - Never cut a file in half across two replies.

---

## 3. Files and where they go

Everything for one book lives under `assets/<namespace>/books/<book>/`. The book's id is `<namespace>:<book>`.

### A manual

```
assets/<ns>/books/<book>/
├── book.md                     mode: manual (required), icon, skin, pages, lookup, lookup_from
└── en_us/
    ├── _index.md               the manual's title and its landing text (shown above the contents)
    ├── categories/
    │   ├── basics.md           one file per category; the file name is the category id
    │   └── machines.md
    ├── getting_started.md      one file per entry; the file name (without .md) is the entry id
    ├── widgets.md
    └── machines/smelter.md     an entry may sit in a folder: its id is then machines/smelter
```

### A lore book

```
assets/<ns>/books/<book>/
├── book.md                     optional: skin, pages
└── en_us/
    └── text.md                 the whole text
```

A long lore book may be split into chapters, one file each, read in the order of their numbers:

```
assets/<ns>/books/<book>/
└── en_us/
    ├── text.md                 optional: title:, author: and any foreword
    ├── 01-arrival.md           a chapter: a number, a hyphen, a lowercase name (a-z, 0-9, - and _)
    └── 02-the-storm.md         each starts on a new page; its first heading (# The Storm) is its title
```

- Number every chapter differently. Pad with zeros (`01-`, `02-`) so they also sort in order as files.
- Start every chapter with a `#` heading: it is the chapter's title on the book's chapter list.
- With no `text.md`, put `title:` and `author:` in the first chapter's front matter.
- No other files in a lore book's language folder: they are never shown.

### Also, for both

- **Server entry:** `data/<ns>/gottschbooks/book/<book>.json`. It's needed to give the book on first join, or to
  count reading it toward advancements (see [Section 7](#7-getting-the-book-to-players)).
- **Lang line:** `"book.<ns>.<book>": "Title"` in `assets/<ns>/lang/en_us.json`, so the book's item has a name. A
  `/` in the book's path becomes `.` in the key.

### Paths and translations

- **In a mod**, both trees go under `src/main/resources/`.
- **In a modpack without a mod**, `assets/` goes in a resource pack and `data/` in a data pack. Each pack needs its
  own `pack.mcmeta`.
- **Other languages:** each language is a folder next to `en_us` (`es_es`, `de_de`, `ko_kr` ...). Each file falls
  back to `en_us` on its own, so a translation may cover only some files.
- **When translating:** keep every file name, id, entry id, anchor, category id and front matter key exactly as in
  `en_us`. Translate only the human text: titles, prose, captions and link text. A lore book's chapter files keep
  their `en_us` names too (`ko_kr/02-the-storm.md`); a chapter left untranslated shows in English, in its place.

---

## 4. Front matter

A file may start with `key: value` lines between two `---` lines.

- It isn't YAML: one value per line, with no lists and no nesting.
- Put a value in double quotes if it contains `#` or starts with a quote character: `title: "Ruby #1"`.
- Without quotes, ` # anything` at the end of a line is a comment.

| Key | File | Value |
|---|---|---|
| `mode` | book.md | `manual` for a manual. Leave it out for a lore book |
| `icon` | book.md | the manual's icon on its contents page: an item |
| `lookup` | book.md | leave it out (the manual answers item lookups while the player carries it). `always` only if the person says their manual is opened by a plain item or a command; `never` to turn lookups off |
| `skin` | book.md | only if the person has custom book textures: a folder such as `mymod:textures/gui/my_book` holding `cover.png`, `page.png`, `ornament.png` |
| `pages` | book.md, entries | `single` (one wide page) or `spread` (two pages, the default): the [page style](#page-styles) the text starts in. A lore book takes it from book.md only |
| `title` | _index.md, categories, entries, text.md | the title. A lore book in chapters with no text.md: in the first chapter |
| `author` | text.md | the lore book's author, shown as "by ..." (with no text.md: in the first chapter) |
| `icon` | categories, entries | the item shown beside it on the contents and on the entry's title: give every category and entry one |
| `sort` | categories, entries | a whole number; lower comes first. Use 10, 20, 30 ... so the person can insert things later |
| `category` | entries | the id of a category file (`basics` for `categories/basics.md`). Every entry needs one that exists |
| `advancement` | entries | an advancement id; the entry stays hidden until the player has it, and a toast tells them when it unlocks. Only with a real id of an advancement that has a `display` |
| `items` | entries | the items the entry explains in plain text, beyond its spotlights, recipes and icon: item ids and `#ns:tag` item tags, separated by commas, in double quotes: `items: "mymod:wrench, #mymod:wrenches"`. Only real ids. An id written with NBT answers only for stacks that have them; use that only when the person says how their item is told apart |
| `lookup_from` | entries, book.md | leave it out (the entry answers for everything it shows). `items` when the entry's icon or spotlights are only for show, such as a vanilla torch on a lighting entry; then list what it explains in `items:`. `none` for an entry that explains no item |

An entry's title comes from `title:`. Don't also start the entry with a `# Title` heading.

**Item lookup.** A player hovering over an item in their inventory can press a key to open the entry that explains
it. An entry explains an item when it has a `spotlight` of it, a `recipe` that makes it, the item as its `icon`, or
the item in `items:`. So every important item should have one entry that spotlights it; use `items:` for items an
entry covers only in its text (the tiers of a tool, say). In a manual about a mod, don't give an entry a vanilla
icon it doesn't explain without `lookup_from: items`: the icon would send every one of that vanilla item to it.

---

## 5. Book text

### Blocks (each starts on its own line)

| Write | Result |
|---|---|
| text lines | a paragraph; lines next to each other join into one. A blank line starts a new paragraph |
| a line ending in two spaces or `\` | a line break inside the paragraph |
| `## Section` or `## Section {#anchor}` | a heading. `#` and `##` are large with a rule under them; `###` to `######` are bold. `{#anchor}` makes it a link target |
| `---` | an ornament between sections (also `***` or `___`) |
| `> text` | an editor's note: grey and italic. Good for tips and warnings |
| `- item` or `1. item` | a list item. One level only. A plain line right after an item continues it; leave a blank line after the list |
| `![caption](item:ns:id)` | an illustration: the item drawn large, centred, with the caption under it. An empty caption shows the item's name |
| `{page}` | start a new page |
| `{page:single}` / `{page:spread}` | start a new page and switch the [page style](#page-styles) |
| a table | see [Tables](#tables) |
| a fenced block | see [Fenced blocks](#fenced-blocks) |

### Inside text

| Write | Result |
|---|---|
| `*italic*`, `**bold**`, `***both***` | emphasis |
| `{gold}text{}` | colour by name: `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white` |
| `{#a0522d}text{}` | colour by hex |
| `{font:ns:name}text{}` | the text in a resource pack font (only fonts the person has) |
| `[text](entry:widgets)` | a link to another entry of the same manual |
| `[text](entry:widgets#crafting)` | a link to the heading with `{#crafting}` in that entry |
| `[text](https://example.com)` | a web link; the game asks before opening it |
| `\*` `\{` `\[` `\|` `\#` ... | the character itself, not formatting |

Rules for inline text:

- A colour or font lasts until `{}`. Colours don't nest: a second colour replaces the first.
- Prefer dark colours: `dark_red`, `dark_green`, `dark_aqua`, `dark_purple`, `gold`, `dark_blue`. The page is
  parchment, so `yellow`, `white` and `aqua` are hard to read.
- Use colour sparingly, for key terms and values.
- `entry:` links lead between a manual's entries. In a lore book they lead only to its chapters:
  `[the storm](entry:02-the-storm)`, or `entry:02-the-storm#anchor` for a heading in one.
- Escape a line that would otherwise start a block: `\# not a heading`, `\- not a list`, `\> not a note`.
- Escape `*`, `{` or `|` that should show as themselves.

### Tables

```
| Ore | Found | Mine with |
|---|:---:|---|
| ![](item:minecraft:coal_ore) Coal | above y 0 | ![](item:minecraft:wooden_pickaxe) any pickaxe |
| ![](item:minecraft:diamond_ore) **Diamond** | y -64 to 16 | ![](item:minecraft:iron_pickaxe) iron or better |
```

- **Layout:** a header row, then a row of dashes, then the rows. `:---:` centres a column and `---:` right-aligns it.
- **Cells:** each cell is one line of inline text: emphasis, colours and links.
- **Icons:** a cell may **start** with one or more icons, written `![](item:ns:id)`. Icons after the cell's text are
  shown as text, so don't do that.
- **No header:** to draw no header (a grid of pictures), leave every header cell empty: `| | |`.
- **Cell count:** every row must have the same number of cells as the header.
- **Not supported:** merged cells, and lists or blocks inside cells.
- **Width:** in the normal two-page view a table has only about 200 px. Keep it to two or three short columns, or put
  it on a single wide page (below).

### Page styles

A book normally shows two pages side by side (*spread*). A *single* page spans the whole book, about twice as wide,
which suits wide tables:

```
Prose on two pages...

{page:single}

## Every Ore {#ores}

| Ore | Found | Most common at | Mine with |
|---|---|:---:|---|
| ...

{page:spread}

Back to two pages.
```

- The style lasts until the next `{page:...}`.
- `pages: single` in an entry's front matter starts that entry wide.
- Use `single` for a table of four or more columns, or one with long cells. Leave prose in `spread`.

### Fenced blocks

A fenced block's first line is three backticks, a kind and an argument. Its body (any book text, often one or two
sentences) is drawn under it, and it ends with three backticks.

````
```spotlight mymod:widget
The heart of every machine. It does **everything**.
```
````

| Kind | Argument | Shows |
|---|---|---|
| `spotlight` | an item | the item at 2x in a frame, with its tooltip on hover |
| `recipe` | a recipe id | the recipe as its block shows it (crafting grid, furnace, stonecutter, smithing table, or a mod's machine) and the result |
| `image` | a full texture path with `.png`, then optionally `width height`: `mymod:textures/gui/map.png 128 64` | the picture, scaled to the page |
| `entity` | a mob's entity id | the mob, slowly turning |

- Put `## A Heading` on the line before a block to title it. The two then stay on the same page.
- A recipe block with an empty body is fine. Close every block with three backticks.
- These are the only kinds: any other kind shows its lines as plain text.

### Items with data

Anywhere an item goes (`icon:`, illustrations, spotlights, table icons), the item may carry its `/give` data, for
potions, enchanted books, dyed items, heads and a mod's item variants. The syntax depends on the loader:

| | NeoForge 1.21.1 (components) | Forge 1.20.1 (NBT) |
|---|---|---|
| potion | `minecraft:potion[potion_contents={potion:"minecraft:healing"}]` | `minecraft:potion{Potion:"minecraft:healing"}` |
| enchanted book | `minecraft:enchanted_book[stored_enchantments={levels:{"minecraft:mending":1}}]` | `minecraft:enchanted_book{StoredEnchantments:[{id:"minecraft:mending",lvl:1s}]}` |
| dyed leather | `minecraft:leather_chestplate[dyed_color={rgb:11546150}]` | `minecraft:leather_chestplate{display:{color:11546150}}` |

In an illustration it is `![Healing](item:minecraft:potion[...])`. Use item data only when the item really differs
by it. If you aren't sure of the exact data, leave it out and say so in your report.

---

## 6. Writing a good manual

1. **Plan before writing.** Read all the material. Then list:
   - the categories, three to seven, each one area of the mod: Getting Started, Items, Blocks, Machines, Mobs,
     World Generation, Progression
   - the entries in each category, one topic per entry: an item or a small family of items, a machine, a mechanic,
     a mob

   Show the person this outline before writing every file, if they want to review it.
2. **The landing text** (`_index.md`): two to four sentences on what the mod is, and a link to the first entry a new
   player should read.
3. **Categories:** a short title, an icon, a `sort`, and one sentence of description in the body.
4. **Entries:**
   - Open with one or two sentences saying what the thing is and why a player cares.
   - Then use `##` sections with anchors (`## Crafting {#crafting}`, `## Uses {#uses}`).
   - Give each key item a `spotlight`, and show how to make it with a `recipe` block (only with a real recipe id).
   - Use a table for comparisons (tiers, ores, drops), a list for steps, and `>` notes for tips and warnings.
5. **Length:** a short entry reads better than a long one. If an entry passes about 400 words, split it. Each entry
   should make sense alone.
6. **Links:** link related entries to each other with `entry:` links, especially on a first mention. Every entry should be
   reachable from its category, which is automatic, and from at least one link where it fits.
7. **Voice:** second person ("you"), present tense, short sentences, concrete numbers from the material. Don't
   invent numbers, recipes, drop rates or mechanics the material doesn't state. Leave them out, or ask.
8. **Progression:** sort categories and entries in the order a player meets them.
9. **Hidden entries:** use `advancement:` to hide late-game entries only when the person gives the advancement ids.

## 7. Getting the book to players

**Server entry**, at `data/<ns>/gottschbooks/book/<book>.json`:

```json
{
  "give_on_first_join": true,
  "track_reading": false
}
```

| Field | Use |
|---|---|
| `give_on_first_join` | `true` gives each player the book once, on first join |
| `announce_on_first_join` | `true` tells each player about the book once, in chat, with a [Get it] link, instead of giving it. Ask the person which they want; for a manual, suggest this one |
| `track_reading` | `false` for a manual (so it doesn't count toward "read every book" advancements). Leave it out for a lore book |
| `item` | the item given; leave it out to use the library's own `gottschbooks:book`, or name the mod's own book item |
| `disabled` | `true` switches the book off. Don't write it unless asked |

A manual that isn't given or announced on first join needs no server entry.

**Without code, by command:**

```
/give @p gottschbooks:book[gottschbooks:book="mymod:guide"]       NeoForge 1.21.1
/give @p gottschbooks:book{"gottschbooks:book":"mymod:guide"}     Forge 1.20.1
/gottschbooks open @p mymod:guide                                  opens it on screen (operators)
```

**The item's name:** `"book.mymod.guide": "The Widget Handbook"` in `assets/mymod/lang/en_us.json`.

**A mod's own book item** (Java, if the person wants one):

```java
new BookItem(new Item.Properties(), ResourceLocation.fromNamespaceAndPath("mymod", "guide"))   // NeoForge 1.21.1
new BookItem(new Item.Properties(), new ResourceLocation("mymod", "guide"))                     // Forge 1.20.1
```

## 8. Writing a lore book

- **Files:** one `en_us/text.md` with `title:` and `author:`, plus a server entry with `{}` (so reading it can count
  toward advancements) or `{"give_on_first_join": true}`.
- **Chapters:** for a long book (more than ten or so pages of text), split it into chapter files (`01-arrival.md`,
  see [Section 3](#a-lore-book)), each opening with a `#` heading. Keep `title:` and `author:` in `text.md`.
- **Voice:** write in the voice the person asks for: a diary, a chronicle, a letter.
- **Sections:** separate them with `---` ornaments. A section is a scene or a day.
- **Pictures:** use an illustration (`![caption](item:ns:id)`) at a moment that matters, not on every page.
- **Notes:** use `>` for an editor's note or a later hand writing in the margin.
- **Links:** only to chapters (`entry:02-the-storm`), and only when a reader would want to jump back; most lore
  books need none.

## 9. Check your work before you answer

Go through every file:

- [ ] Every item, recipe, entity, advancement, texture and font id comes from the person's material or is a vanilla id
      you are certain of.
- [ ] Every entry has `title`, `category` (a category that has a file), `icon` and `sort`.
- [ ] Every `entry:` link names an entry you wrote, and every `#anchor` matches a `{#anchor}` heading in that entry.
- [ ] No backticks outside fenced blocks, no HTML, no nested lists, no `#anchor`-only links.
- [ ] Every fenced block is closed, and its kind is `spotlight`, `recipe`, `image` or `entity`.
- [ ] Every table row has as many cells as its header, and icons start their cells.
- [ ] Tables with four or more columns are on a `{page:single}` page or in a `pages: single` entry.
- [ ] Every colour span is closed with `{}`.
- [ ] Item data uses the syntax of the person's loader.
- [ ] Front matter values containing `#` are quoted.
- [ ] Translations keep every id, file name, anchor and key identical to `en_us`.

## 10. The final report

**Write every file in full.** Give each one as its path on a line of its own, followed by its contents in a fenced
block of **four** backticks (````). Entries contain three-backtick blocks of their own, and four keeps them intact.

The person may save your reply and run a script (`tools/split-book.py`) that writes the files from it. So:

- The path line holds only the path, starting at `assets/` or `data/`.
- Nothing goes between the path line and its block.
- Every block is closed.

In your reply, a file looks like this (the path line, then the four-backtick block):

`````
assets/mymod/books/guide/en_us/widgets.md
````markdown
---
title: Widgets
...
````
`````

**Then end with a short report:**

- **Files:** the full list of files, in order.
- **Needs ids:** every place you left out a block because you lacked an id, and what id would go there.
- **Check these ids:** every recipe id, and any id you inferred rather than were given.
- **Assumptions:** the loader, the namespace, the defaults you used.
- **Next steps:** put the files in place, run the game, press F3+T to reload, and run `/gottschbooks check <book>`.
  It lists every missing item, recipe or link, and every malformed table, file by file. Ask them to click **[Copy]**
  after its summary and paste the report back to you.

## Features by version

Use a feature only if the person's Gottsch Books version has it:

| Feature | Since |
|---|---|
| everything else in this document | 1.0.0 |
| item data on items (components or NBT) | 1.1.0 |
| `{font:...}` | 1.1.0 |
| tables, `{page}`, `{page:single}`, `{page:spread}`, `pages:`, recipe layouts for furnaces, stonecutters and smithing, `/gottschbooks check` | 1.2.0 |
| `items:`, `lookup:` (item lookup) | 1.3.0 |
| `lookup_from:`, NBT in `items:` | 1.3.0 |
| lore book chapters (`01-name.md`), links to chapters | 1.3.0 |
| `announce_on_first_join` | 1.3.0 |

Before 1.2.0, a page break is written `<!-- page -->` (1.2.0 and later read it too).
