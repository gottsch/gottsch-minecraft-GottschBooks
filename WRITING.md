# Writing books for Gottsch Books

Gottsch Books reads books written in a small subset of Markdown, one file per entry per language, from your mod's
(or a resource pack's) assets. This page covers everything a book can contain and how to wire it into a mod. The
format and every feature are the same on NeoForge 1.21.1 and Forge 1.20.1. Where the two differ (item data in
commands and loot tables, and the build setup), both forms are shown.

- [Two kinds of book](#two-kinds-of-book)
- [No code needed](#no-code-needed)
- [Quick start: a lore book](#quick-start-a-lore-book)
- [Quick start: a manual](#quick-start-a-manual)
- [Files and languages](#files-and-languages)
- [Front matter](#front-matter)
- [Text](#text)
- [Pictures and blocks](#pictures-and-blocks)
- [Links](#links)
- [The book on the server](#the-book-on-the-server)
- [Opening a book](#opening-a-book)
- [Depending on Gottsch Books](#depending-on-gottsch-books)
- [Tips and gotchas](#tips-and-gotchas)
- [Checking a book](#checking-a-book)

Writing a book with an AI? Give it [AI-INSTRUCTIONS.md](AI-INSTRUCTIONS.md) along with your docs: a version of this
page written for an AI, with the rules that keep it from inventing ids or using Markdown the reader doesn't know.

## Two kinds of book

| | Lore book | Manual |
|---|---|---|
| Text | one file, read front to back | many entries, in categories |
| Opens on | a title page: icon, title, author | the contents: landing text, then each category's entries |
| Getting around | turning pages | clicking entries, links, Back, Contents, Search |
| Extras | illustrations, spotlights, recipes, images, mobs | the same, plus entries locked behind advancements |

Both flow their text onto as many pages as the window needs, one or two pages at a time, so no text runs off a page
in any language or window size.

## No code needed

A book is entirely data: a resource pack holds its text and art, and a datapack holds its server entry. To put it in
players' hands, use the library's own item, `gottschbooks:book`, which opens whatever book its stack names:

```
/give @p gottschbooks:book[gottschbooks:book="mymod:guide"]       (NeoForge 1.21.1: a data component)
/give @p gottschbooks:book{"gottschbooks:book":"mymod:guide"}     (Forge 1.20.1: stack NBT)
```

- **Give it on first join:** `"give_on_first_join": true` in the book's [server entry](#the-book-on-the-server).
- **Announce it on first join:** `"announce_on_first_join": true` tells each player once, in chat, with a
  [Get it](#announcing-a-book-instead-of-giving-it) link, instead of giving it.
- **Put it in loot:** a loot table entry for `gottschbooks:book` with a `set_components` function setting
  `"gottschbooks:book": "mymod:guide"` (on 1.20.1, `set_nbt` with `{"gottschbooks:book":"mymod:guide"}`).
- **Name it:** a lang line `"book.mymod.guide": "My Guide"` in your resource pack, or on the stack (`item_name` on
  1.21.1, a `display` name on 1.20.1).
- **Open it from a command block or function:** `/gottschbooks open @p mymod:guide`.

So a modpack can ship its own guide with no mod at all. A mod only needs code for an item of its own (its own
model, name and creative tab), which the quick starts below show.

## Quick start: a lore book

Three things: the text, a server entry, and an item to read it with. (Without code, use `gottschbooks:book` instead of
your own item: see [No code needed](#no-code-needed).)

**1. The text:** `assets/mymod/books/old_diary/en_us/text.md`

```markdown
---
title: An Old Diary
author: A Lost Traveller
---

The first day. The road was longer than the map said.

---

The second day. I found a ring in the river.

![](item:minecraft:gold_ingot)
```

**2. The server entry:** `data/mymod/gottschbooks/book/old_diary.json` containing just `{}`. It lets the server
count the book toward advancements (see [The book on the server](#the-book-on-the-server)).

**3. The item:** a `BookItem`, with the book on the stack:

```java
public static final DeferredItem<Item> DIARY = ITEMS.register("old_diary",
        () -> new BookItem(new Item.Properties(), ResourceLocation.fromNamespaceAndPath("mymod", "old_diary")));
```

Right-click it and the book opens. One item can also stand for many books: register a plain
`new BookItem(properties)` and give each stack its book with `Books.of(item, bookId)`.

## Quick start: a manual

**1. Turn manual mode on:** `assets/mymod/books/guide/book.md`

```markdown
---
mode: manual
icon: mymod:guide
---
```

**2. The title and landing text:** `assets/mymod/books/guide/en_us/_index.md`

```markdown
---
title: My Mod Guide
---

Everything you need to know about My Mod.
```

**3. A category:** `assets/mymod/books/guide/en_us/categories/basics.md`

```markdown
---
title: Basics
icon: minecraft:crafting_table
sort: 1
---

Where to start.
```

**4. An entry:** `assets/mymod/books/guide/en_us/getting_started.md`

````markdown
---
title: Getting Started
category: basics
icon: mymod:widget
sort: 1
---

Craft a widget first. See [Widgets](entry:widgets#crafting).

## The Widget {#widget}

```spotlight mymod:widget
Your first widget. It does **everything**.
```
````

**5. An item:** `new BookItem(new Item.Properties(), ResourceLocation.fromNamespaceAndPath("mymod", "guide"))`, or
no code: `gottschbooks:book` with `"give_on_first_join": true` (see [No code needed](#no-code-needed)).

A manual needs a server entry only to be given on first join, and then it should say `"track_reading": false` (see
[Tips and gotchas](#tips-and-gotchas)).

## Files and languages

Everything lives under `assets/<namespace>/books/<book>/`. The book's id is `<namespace>:<book>`.

| File | Used by | Holds |
|---|---|---|
| `book.md` | both (optional for lore books) | `mode`, `icon`, `skin`: things that don't change with the language |
| `<locale>/text.md` | lore book | the whole text |
| `<locale>/_index.md` | manual | the manual's title and landing text |
| `<locale>/categories/<id>.md` | manual | one category |
| `<locale>/<entry>.md` | manual | one entry; `<entry>` may include folders (`items/widget.md` is entry `items/widget`) |

`<locale>` is the game's language code: `en_us`, `es_es`, `ko_kr`, and so on. **Each file falls back to `en_us`
on its own**, so a translation can cover only some files and the rest show in English.

Resource packs can replace or add any of these files, as with any other asset.

## Front matter

Every file can start with a block of `key: value` lines between `---` lines. It isn't full YAML: one value per
line, no lists. A value can be quoted (`title: "Ruby #1"`). Without quotes, ` # comment` at the end of a line is
ignored.

| Key | Where | Meaning |
|---|---|---|
| `mode` | book.md | `manual`; anything else (or no book.md) is a lore book |
| `icon` | book.md | the manual's icon on its contents page, when it isn't opened from an item |
| `lookup` | book.md | when the manual answers [item lookups](#item-lookup): `carried` (the default) while the player carries it, `always`, or `never` |
| `lookup_from` | book.md, entries | what an entry answers [item lookups](#item-lookup) for: `all` it shows (the default), only its `items`, or `none`. An entry's setting wins over book.md's |
| `skin` | book.md | a texture folder (`mymod:textures/gui/my_book`) holding `cover.png`, `page.png` and `ornament.png`; see [Tips](#tips-and-gotchas) |
| `pages` | book.md, entries | the [page style](#page-styles) the text starts in: `single` for one wide page, `spread` (the default) for two side by side. An entry's setting wins over book.md's |
| `title` | text.md, _index.md, categories, entries | the title. For a lore book, a leading `# Heading` works too |
| `author` | text.md | shown under the title as "by ..." |
| `icon` | categories, entries | the item shown next to it on the contents and on the entry's title |
| `sort` | categories, entries | a number; lower comes first. Ties, and anything without one, sort by title |
| `category` | entries | a category id (the file name under `categories/`). Entries without a known category are listed last |
| `advancement` | entries | `ns:id`: the entry stays hidden until the player has this advancement |
| `items` | entries | items the entry explains, for [item lookups](#item-lookup): item ids (with NBT to pick out one kind) and `#ns:tag` item tags, separated by commas |

An `icon` (and a spotlight's item) is either an item id, `minecraft:diamond`, or a picture from an illustration
scheme, `scheme:namespace:id` (see [Pictures and blocks](#pictures-and-blocks)). An item may carry components, as
in a `/give` command (see [Items with components](#items-with-components)).

## Text

| Write | Get |
|---|---|
| a blank line | a new paragraph |
| a line ending in `\` or two spaces | a line break inside the paragraph |
| `*italic*`, `**bold**`, `***both***` | emphasis |
| `{gold}text{}` | colour, by any of vanilla's 16 names: `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white` |
| `{#d4a017}text{}` | colour, by hex |
| `{font:mypack:icons}text{}` | a font from a resource pack (see [Fonts](#fonts)) |
| `# Title`, `## Section`, `### Smaller` | headings. `#` and `##` are drawn larger with a rule under them; `###` and below are bold. Add `{#anchor}` at the end to make a link target |
| `---` | an ornament between sections |
| `> text` | an editor's note, in grey italics |
| `- item` or `1. item` | a list (one level). Indented lines continue the item |
| `{page}` | start a new page here (a line of its own). `{page:single}` and `{page:spread}` also switch the page style; see [Page styles](#page-styles) |
| `\| a \| b \|` | a table (see [Tables](#tables)) |
| `\*`, `\{`, `\[`, `\-`, `\|` ... | the character itself, not formatting |

A colour stays on until `{}`. Colours don't nest: a second colour replaces the first. A marker that is never
closed (a lone `*`, a `{gold}` with no `{}`) is shown as plain text, so a stray character never breaks a page.
Anything not listed here (HTML, nested lists) is shown as plain text too.

Prefer the darker colours. Light ones (`yellow`, `white`, `aqua`) are hard to read on the page, and a hex colour
under 3:1 contrast with the paper logs a warning when the book loads.

### Tables

Tables are written as on GitHub: a header row, a row of dashes, then the rows. Colons in the dash row align a
column: `:---` (or `---`) left, `:---:` centred, `---:` right. The pipes at either end of a row are optional.

```markdown
| Ore | Found | Mine with |
|---|:---:|---|
| ![](item:minecraft:coal_ore) Coal | above y 0 | ![](item:minecraft:wooden_pickaxe) any pickaxe |
| ![](item:minecraft:diamond_ore) **Diamond** | y -64 to 16 | ![](item:minecraft:iron_pickaxe) iron or better |
```

- **Cells** hold book text: bold, italics, colours, fonts and links all work. Write `\|` for a pipe inside a cell.
- **Icons:** a cell may *start* with one or more items, written like illustrations (`![](item:ns:id)`, components
  allowed). They are drawn at 16 px with their tooltips, beside the cell's text, or above it when the column is too
  narrow. An icon after the cell's text isn't drawn; it shows as text.
- **No header:** leave every header cell empty (`| | |`) and no header is drawn, which suits a grid of pictures.
- **Fitting:** a table is only as wide as it needs to be. When it is too wide for the page, the columns with the most
  text give way first and their cells wrap; a short column keeps its width. A row is never split across pages, and
  a table that carries on to the next page repeats its header there.
- **Width:** two pages side by side leave a table about 200 px. Keep tables there to two or three short columns, or
  put the table on a single wide page with `{page:single}` (see [Page styles](#page-styles)).
- A row with too few cells is padded with empty ones, and extra cells are left out. `/gottschbooks check` reports
  both.
- Not supported: cells spanning rows or columns, several paragraphs in a cell, and blocks (lists, spotlights) inside
  a cell. Tables work in lore books and manuals, and inside fenced blocks.

### Page styles

A book shows two pages side by side (a *spread*), or one *single* page as wide as the book, which gives tables and
long lines room. A page break can switch between them, and the style lasts until the next break that names one:

```markdown
Two pages of prose...

{page:single}

| a wide | table | with | room |
|---|---|---|---|

{page:spread}

Back to two pages.
```

- `{page}` only starts a new page; `{page:single}` and `{page:spread}` start one and switch the style. Each goes on a
  line of its own; anywhere else it is shown as text.
- The text starts in the style from `pages:` in the entry's front matter, else book.md's, else `spread`.
- After a switch, pages pair up afresh, so a spread page never faces a single one. A spread with one page left
  shows a blank page beside it, as at the end of a book.
- In a window too narrow for two pages, every page is shown one at a time, and a single page is no wider.
- The 1.0 page break, `<!-- page -->`, still works the same as `{page}`.

### Fonts

`{font:<namespace>:<name>}` draws text in a font from `assets/<namespace>/font/<name>.json`, from your mod or any
resource pack. A font only changes how the text looks; the book still lays it out and wraps it as text. A font stays
on until `{}`, which ends a colour too, so a colour and a font can be set together.

A font of pictures, where each character is a small image, puts icons in a line of text. Text is drawn in the
page's ink, which darkens a picture the way it would a letter, so set the colour to `white` to keep the picture's
own colours:

```markdown
You'll need {white}{font:mypack:icons}abc{} to craft it.
```

- A picture taller than a line (`"height"` over 8 in a `bitmap` provider, or `"ascent"` over 7) is fine in a
  paragraph or a list: its line grows to fit it, so it doesn't overlap the lines around it. In a heading, keep
  pictures to `"height": 8`; heading lines keep their height.
- A character the font doesn't have is drawn as a box. Give the font a `space` provider if the run has spaces.
- An id that isn't a font draws every character as a box.
- Vanilla's fonts work too: `{font:minecraft:alt}` is the enchanting table's script, `{font:minecraft:illageralt}`
  the illagers'.

## Pictures and blocks

### Illustrations

```markdown
![caption](item:minecraft:diamond)
```

A picture on a line of its own: an item drawn at about 1.5x, with the caption under it (with no caption, the
item's own name, so it translates by itself). Hover over it for the item's tooltip. Works in lore books and manuals.

The part before the first `:` is the **illustration scheme**. `item` is built in. A mod can add its own for stacks
that aren't just an item id, such as Magic Treasures' named jewelry (`preset:magictreasures:ring_of_life_death`).
See [Depending on Gottsch Books](#depending-on-gottsch-books).

If the item doesn't exist, the page shows `[item ns:id?]` in its place and the log says why.

### Items with components

An item reference (an illustration, a spotlight, an `icon`) may end with the item's data in `/give` syntax, for items
that differ by more than their id: potions, enchanted books, dyed leather, player heads, a mod's item variants. On
NeoForge 1.21.1 that's components in `[...]`; on Forge 1.20.1 it's NBT in `{...}`.

```markdown
![Healing](item:minecraft:potion[potion_contents={potion:"minecraft:healing"}])     (NeoForge 1.21.1)
![Healing](item:minecraft:potion{Potion:"minecraft:healing"})                       (Forge 1.20.1)
```

````markdown
```spotlight minecraft:enchanted_book[stored_enchantments={levels:{"minecraft:mending":1}}]
```
````

````markdown
```spotlight minecraft:enchanted_book{StoredEnchantments:[{id:"minecraft:mending",lvl:1s}]}
```
````

```markdown
icon: minecraft:leather_chestplate[dyed_color={rgb:11546150}]                       (NeoForge 1.21.1)
icon: minecraft:leather_chestplate{display:{color:11546150}}                        (Forge 1.20.1)
```

Anything `/give` accepts works, so the easiest way to write one is to get a `/give` command working in game and copy
the part after the player name. Hover over the picture to check the result: the tooltip is the item's own. A book
that uses them needs its own text for each version, since the two syntaxes differ.

They work on plain item ids and `item:`. A mod's own scheme, such as `preset:`, ignores them and logs a warning. If
they don't parse, the page shows `[item ...?]` and the log gives the game's own error message.

### Fenced blocks

A fenced block names the kind and its argument on the first line. Its body is ordinary book text, drawn under it.
Fenced blocks work in lore books and manuals.

````markdown
```spotlight magictreasures:topaz
The most common gem.

**Tier:** {dark_red}1{}
```
````

| Block | Argument | Shows |
|---|---|---|
| `spotlight` | an item (components allowed), or `scheme:ns:id` | the item at 2x in a frame, with a tooltip |
| `recipe` | a recipe id | the recipe laid out for its block (below), and the result. Ingredients that accept several items cycle through them |
| `image` | `ns:textures/....png`, then optionally `width height` | the image, scaled to fit the page (256x256 assumed without a size) |
| `entity` | an entity id | the mob, slowly turning |

Put a `## Heading` right before a block to give it a title: the heading and the block are kept on the same page. If
the item, recipe, image or mob doesn't exist, the page shows `[recipe ns:id?]` in its place and the log says why.
`/gottschbooks check` finds all of them at once (see [Checking a book](#checking-a-book)).

A `recipe` block draws each kind of recipe the way its block does:

| Recipe | Shows |
|---|---|
| crafting (shaped or shapeless) | a 3x3 grid |
| smelting, blasting, smoking, campfire | the input with the furnace, blast furnace, smoker or campfire under it, and the cooking time under the arrow |
| stonecutting | the input with the stonecutter under it |
| smithing | template, base and addition, with the smithing table under them |
| anything else, a mod's machine included | its ingredients, three to a row, with the block the mod names as its station under them |

A recipe that works out its ingredients in code, such as dyeing armour or cloning a book, has nothing to draw and
shows as missing.

## Links

```markdown
See [the gems](entry:gems) or [rubies](entry:gems#ruby).
Read more [on the wiki](https://example.com/wiki).
```

- `entry:<entry>` opens another entry of the same manual. `#anchor` jumps to the heading with `{#anchor}`.
- `https://` links ask the player before opening the browser (and follow the game's Chat Links setting).
- Links are underlined and blue. **Back** (or Backspace) returns to where you were, and **Contents** goes to the
  contents page.
- A `[text]` that isn't followed by a link target is just text, so `[brackets]` in prose are safe.

## Search

Every manual has **Search** above the book (or Ctrl+F). The player types, and the search page lists the entries
that mention every word typed, ignoring case, each with a line of its text around the word. Entries whose title has
the words come first, then those that mention them most. A click (or Enter, for the first) opens the entry on the
page where the word is, and **Back** returns to the results.

Search reads titles, paragraphs, headings, list items, table cells, captions, and the names of spotlighted and
illustrated items, in the player's language. Locked entries are never found. There is nothing to write for it, but
the words players will search for (an item's name, a mob's) are worth having in the text.

## New entries

An entry the player hasn't opened yet has a small red dot after its name on the contents, and so does its
category; search results show it too. Under the manual's title, *New entries: 5* counts them, with a **Mark all
read** link for a player who already knows the mod. An entry added in a mod's update shows as new, so players find
what changed. What a player has opened is kept on their computer, in `gottschbooks/reading.json` in the game
folder, for every world alike; deleting the file makes every entry new again. There is nothing to write for it.

## Item lookup

While a player carries a manual, hovering over an item in any inventory adds a line to its tooltip naming the key
and the manual, *[B] A Field Guide to the Overworld*, and pressing the key opens the manual at that entry, on the page
that shows the item. Closing the book goes back to the inventory. The key is **Look Up Item in a Manual** under
Controls; the line shows whatever it is bound to, and unbinding it removes the line. The line is one line at most (a
long title is cut short), and is left out while SHIFT is held, which many mods use for a long detail view; the key
works either way.
Once a player has opened the entry that explains an item, the line goes away for that item, since they know where
to look; the key still works (`lookupHintUntilRead = false` in `config/gottschbooks-client.toml` keeps the line).
Players who no longer need the line can hide it with `/gottschbooks lookup hint off` (or `lookupHint = false` in
`config/gottschbooks-client.toml`) and still press the key; `/gottschbooks lookup hint on` brings it back.

An entry explains an item when it has:

1. a `spotlight` of the item, or
2. a `recipe` that makes it, or
3. the item as its `icon`, or
4. the item in its `items:` front matter.

When several entries could answer, that is also the order of preference; after that, the first in the contents
wins. `items:` is for things explained in plain text, and takes tags too:

```markdown
---
title: Tools
icon: minecraft:iron_pickaxe
items: #minecraft:pickaxes, #minecraft:axes, minecraft:shears
---
```

**An icon only for show.** An entry's icon and spotlights answer lookups too, so a torch on a "Bring Light" entry
sends every torch in the game to it. `lookup_from: items` makes the entry answer only for what its `items:` lists;
`lookup_from: none`, for nothing. Put either in book.md to make it every entry's default, and let an entry say
`lookup_from: all` to answer for everything it shows again:

```markdown
---
title: Bring Light
icon: minecraft:torch
lookup_from: items
items: dungeons2:wall_lantern, dungeons2:brazier
---
```

`/gottschbooks check` points out a vanilla icon in a manual about a mod's items, when the entry shows that item
nowhere else.

**One kind of an item.** An id written with NBT answers only for stacks that have them, and comes before any
entry for the plain item, whatever its rank. So a general *Maps* entry can spotlight `minecraft:filled_map` while a
*Dungeon Maps* entry answers for the maps a mod marks:

```markdown
items: minecraft:filled_map{D2DungeonMap:{}}
```

To match, the hovered stack's NBT must *contain* what is written: the tags listed, with the same values, any others
ignored (nested compounds the same way; a list matches when the stack's list holds each listed element). A plain id answers for every stack of the item. The same goes for icons and spotlights written with
NBT, and the key opens the entry at the spotlight of that very kind when it has one.

Locked entries never answer, so a lookup can't give away an entry before its advancement. `lookup: always` in
book.md answers without the manual in the inventory, which suits a manual opened by a plain item or a command;
`lookup: never` turns lookups off for a book. Only manuals answer, never lore books.

## The book on the server

The text is a client asset, so the server only knows a book exists if it has a **server entry**, a datapack file at
`data/<ns>/gottschbooks/book/<book>.json`. `{}` is a whole entry; every field is optional:

```json
{
  "disabled": false,
  "track_reading": true,
  "give_on_first_join": false,
  "announce_on_first_join": false,
  "item": "gottschbooks:book"
}
```

| Field | Default | Meaning |
|---|---|---|
| `disabled` | `false` | switch the book off: it's no longer tracked, counted or given (a pack can do this to your book) |
| `track_reading` | `true` | reading it is recorded and counts for the advancement trigger below. Set `false` for a manual |
| `give_on_first_join` | `false` | each player gets the book once, the first time they join after the entry exists (so a book added to a running world reaches existing players too) |
| `announce_on_first_join` | `false` | instead of giving the book, tell each player about it once, in chat, with a **[Get it]** link (see below). Ignored when `give_on_first_join` is on |
| `item` | `gottschbooks:book` | the item given: any `BookItem`, such as your mod's own manual item |

### Announcing a book instead of giving it

Many players don't want a stack of manuals pushed into their inventory, but they can't use a manual they don't know
about. `"announce_on_first_join": true` tells each player once, on their first join after the entry exists:

> **A Field Guide to the Overworld** is available. **[Get it]**

Clicking **[Get it]** runs `/gottschbooks get <book>`, which any player may use, operator or not, for a book whose
entry gives or announces it on first join (so a player who lost a given book can get another). A player who
already carries the book isn't told. To word the line yourself, say where else the book comes from, add a lang line
`book.<namespace>.<path>.announce`, with `%s` for the book's name; the link is added after it:

```json
"book.mymod.manual.announce": "%s is in the Tools tab, or craft one from a book and a bone."
```

A server owner can turn every announcement off with `announceBooks = false` in the world's
`serverconfig/gottschbooks-server.toml`. That only silences announcements; books given on first join are still
given.

With an entry, reading the book from a `BookItem` records it for the player and fires the
`gottschbooks:book_read` advancement trigger:

```json
{
  "criteria": {
    "read": {
      "trigger": "gottschbooks:book_read",
      "conditions": { "books": "#mymod:diaries" }
    }
  }
}
```

| Condition | Matches when the player has read |
|---|---|
| (none) | any book |
| `"book": "mymod:old_diary"` | that book |
| `"books": "#mymod:diaries"` | every enabled book in that tag |
| `"all_in": "mymod"` | every enabled book in that namespace |

Book tags live at `data/<ns>/tags/gottschbooks/book/<tag>.json` (`{"values": ["mymod:old_diary", ...]}`).

## Opening a book

- **An item:** `gottschbooks:book` (no code), or your own `BookItem` (see the quick starts). Its client side opens
  the reader; its server side records the read. Any `BookItem` takes its name from the lang key
  `book.<namespace>.<path>` (a `/` in the path becomes `.`) when a language file has it.
- **Client code:** `GottschBooksClient.open(book)`, `open(book, iconStack)`, `open(book, "entry#anchor")` for a
  manual, or `open(book, "entry", stack)` to open an entry on the page that shows the stack (closing it returns to
  the screen that was open). Client only: call it from a screen, a keybind or the client branch of `Item#use`.
- **A command:** `/gottschbooks open <players> <book> [entry]` (operators). Handy for tutorials and command
  blocks.

## Depending on Gottsch Books

**Your books are part of the game** (the item must exist): nest the library in your jar and require it.

NeoForge 1.21.1 (ModDevGradle):

```groovy
jarJar(implementation("gottsch:gottschbooks-neoforge-1.21.1")) {
    version { strictly "[1.0,2.0)"; prefer "1.0.0" }
}
```

Forge 1.20.1 (ForgeGradle 6): `jarJar.enable()`, then

```groovy
implementation fg.deobf("gottsch:gottschbooks-forge-1.20.1:1.0.0")
jarJar(group: 'gottsch', name: 'gottschbooks-forge-1.20.1', version: '[1.0,2.0)') { jarJar.pin(it, '1.0.0') }
```

and ship the `jarJar` task's jar. Then declare the dependency in your mods.toml with the same range (`type="required"`
on NeoForge, `mandatory=true` on Forge).

**Only a manual** (the mod works without it): don't nest it. Compile against it (`compileOnly`), add a dev-only
runtime copy, and declare it optional with the same range. Register your manual item as a plain `Item`, and call
into Gottsch Books only after `ModList.get().isLoaded("gottschbooks")`, from a class that is loaded only then.

**Your own illustration scheme:** register it from your mod constructor:

```java
Illustrations.register("preset", id -> MyApi.createNamedStack(id));   // Optional<ItemStack>
```

Then `![](preset:mymod:some_id)`, ` ```spotlight preset:mymod:some_id ` and `icon: preset:mymod:some_id` all work.
An empty result leaves the picture out; an exception is logged and never breaks the book.

## Tips and gotchas

- **A manual's server entry should say `"track_reading": false`.** Otherwise reading the manual counts toward
  `all_in` and "read any book" advancements meant for your lore books. Without `give_on_first_join` or
  `announce_on_first_join` a manual needs no entry at all.
- **Item names are lang keys, not book text.** The server can't read assets, so a book's item name comes from
  `book.<namespace>.<path>` in a language file (or the stack's own name), not from the book's `title:`.
- **Skins:** `cover.png` and `page.png` are 32x32 nine-slices (8 px border on the cover, 5 px on the page; edges
  and centre are tiled, never stretched). `ornament.png` is 21x5. The default skin is in
  `assets/gottschbooks/textures/gui/book/`.
- **Escaping:** text that starts a line with `#`, `>`, `-`, `1.` or `![`, or uses `*` or `{` literally, needs a `\`
  in front.
- **Lists carry on:** a plain line straight after a list item joins that item. Leave a blank line to start a new
  paragraph.
- **Reloading:** books load with the other assets, so F3+T reloads them in game while you write. Then run
  [`/gottschbooks check`](#checking-a-book).

## Checking a book

`/gottschbooks check` reads every book the game has loaded, your resource packs' included, and lists what is wrong
in chat, file by file. `/gottschbooks check mymod:guide` checks one book. Anyone can run it: books are client
assets, so it checks what your game has loaded, and changes nothing. Press F3+T after fixing a file, then check
again.

It finds:

- items, pictures, recipes, images and mobs that don't exist, and components that don't parse
- recipes with nothing to draw (their ingredients are worked out in code)
- `entry:` links to entries that aren't there, or to a `{#anchor}` the entry doesn't have, and entry links in a lore
  book
- fonts that aren't there
- entries whose `category` has no `categories/<id>.md`
- `items:` values that aren't items or item tags, a `lookup` other than `carried`, `always` or `never`, a
  `lookup_from` other than `all`, `items` or `none`, and `lookup_from: items` with no `items:`
- a vanilla `icon` in a manual about a mod's items, when the entry shows that item nowhere else, since it answers
  lookups for every one of that item
- fenced blocks the reader doesn't know (` ```chart `), which show as plain lines
- table rows with too few or too many cells, icons after a cell's text, and empty tables
- a `pages` value, or a `{page:...}` style, other than `single` or `spread`
- a manual with no `_index.md`, a lore book with no `text.md`, and a `skin` missing any of its three textures

It checks every language a book has. The first 10 problems are shown in chat; click a file to copy its path. Every
problem goes to the log and to a report, `logs/gottschbooks-check.txt` in the game folder, grouped by file.
**[Open report]** after the summary opens it in your text editor, and **[Copy]** copies the whole report, ready
to paste into an AI chat or an issue. Each check replaces the report.
