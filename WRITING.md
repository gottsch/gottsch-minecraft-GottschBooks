# Writing books for Gottsch Books

Gottsch Books reads books written in a small subset of Markdown, one file per entry per language, from your mod's
(or a resource pack's) assets. This page covers everything a book can contain and how to wire it into a mod. The
format is the same on NeoForge 1.21.1 and Forge 1.20.1. (Forge 1.20.1 has lore books so far; manuals, links and
the fenced blocks come to it in a later release.)

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

## Two kinds of book

| | Lore book | Manual |
|---|---|---|
| Text | one file, read front to back | many entries, in categories |
| Opens on | a title page: icon, title, author | the contents: landing text, then each category's entries |
| Getting around | turning pages | clicking entries, links, Back, Contents |
| Extras | illustrations | spotlights, recipes, images, mobs, entries locked behind advancements |

Both flow their text onto as many pages as the window needs, one or two pages at a time, so no text runs off a page
in any language or window size.

## No code needed

A book is entirely data: a resource pack holds its text and art, and a datapack holds its server entry. To put it in
players' hands, use the library's own item, `gottschbooks:book`, which opens whatever book its stack names:

```
/give @p gottschbooks:book[gottschbooks:book="mymod:guide"]
```

- **Give it on first join:** `"give_on_first_join": true` in the book's [server entry](#the-book-on-the-server).
- **Put it in loot:** a loot table entry for `gottschbooks:book` with a `set_components` function setting
  `"gottschbooks:book": "mymod:guide"`.
- **Name it:** a lang line `"book.mymod.guide": "My Guide"` in your resource pack, or `item_name` on the stack.
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
| `skin` | book.md | a texture folder (`mymod:textures/gui/my_book`) holding `cover.png`, `page.png` and `ornament.png`; see [Tips](#tips-and-gotchas) |
| `title` | text.md, _index.md, categories, entries | the title. For a lore book, a leading `# Heading` works too |
| `author` | text.md | shown under the title as "by ..." |
| `icon` | categories, entries | the item shown next to it on the contents and on the entry's title |
| `sort` | categories, entries | a number; lower comes first. Ties, and anything without one, sort by title |
| `category` | entries | a category id (the file name under `categories/`). Entries without a known category are listed last |
| `advancement` | entries | `ns:id`: the entry stays hidden until the player has this advancement |

An `icon` (and a spotlight's item) is either an item id, `minecraft:diamond`, or a picture from an illustration
scheme, `scheme:namespace:id` (see [Pictures and blocks](#pictures-and-blocks)).

## Text

| Write | Get |
|---|---|
| a blank line | a new paragraph |
| a line ending in `\` or two spaces | a line break inside the paragraph |
| `*italic*`, `**bold**`, `***both***` | emphasis |
| `{gold}text{}` | colour, by any of vanilla's 16 names: `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white` |
| `{#d4a017}text{}` | colour, by hex |
| `# Title`, `## Section`, `### Smaller` | headings. `#` and `##` are drawn larger with a rule under them; `###` and below are bold. Add `{#anchor}` at the end to make a link target |
| `---` | an ornament between sections |
| `> text` | an editor's note, in grey italics |
| `- item` or `1. item` | a list (one level). Indented lines continue the item |
| `<!-- page -->` | start a new page here |
| `\*`, `\{`, `\[`, `\-` ... | the character itself, not formatting |

A colour stays on until `{}`. Colours don't nest: a second colour replaces the first. A marker that is never
closed (a lone `*`, a `{gold}` with no `{}`) is shown as plain text, so a stray character never breaks a page.
Anything not listed here (tables, HTML, nested lists) is shown as plain text too.

Prefer the darker colours. Light ones (`yellow`, `white`, `aqua`) are hard to read on the page, and a hex colour
under 3:1 contrast with the paper logs a warning when the book loads.

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

### Fenced blocks (manuals)

A fenced block names the kind and its argument on the first line. Its body is ordinary book text, drawn under it.

````markdown
```spotlight magictreasures:topaz
The most common gem.

**Tier:** {dark_red}1{}
```
````

| Block | Argument | Shows |
|---|---|---|
| `spotlight` | an item, or `scheme:ns:id` | the item at 2x in a frame, with a tooltip |
| `recipe` | a recipe id | a 3x3 crafting grid and the result. Ingredients that accept several items cycle through them |
| `image` | `ns:textures/....png`, then optionally `width height` | the image, scaled to fit the page (256x256 assumed without a size) |
| `entity` | an entity id | the mob, slowly turning |

Put a `## Heading` right before a block to give it a title: the heading and the block are kept on the same page. If
the item, recipe, image or mob doesn't exist, the page shows `[recipe ns:id?]` in its place and the log says why.

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

## The book on the server

The text is a client asset, so the server only knows a book exists if it has a **server entry**, a datapack file at
`data/<ns>/gottschbooks/book/<book>.json`. `{}` is a whole entry; every field is optional:

```json
{
  "disabled": false,
  "track_reading": true,
  "give_on_first_join": false,
  "item": "gottschbooks:book"
}
```

| Field | Default | Meaning |
|---|---|---|
| `disabled` | `false` | switch the book off: it's no longer tracked, counted or given (a pack can do this to your book) |
| `track_reading` | `true` | reading it is recorded and counts for the advancement trigger below. Set `false` for a manual |
| `give_on_first_join` | `false` | each player gets the book once, the first time they join after the entry exists (so a book added to a running world reaches existing players too) |
| `item` | `gottschbooks:book` | the item given: any `BookItem`, such as your mod's own manual item |

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
- **Client code:** `GottschBooksClient.open(book)`, `open(book, iconStack)`, or `open(book, "entry#anchor")` for a
  manual. Client only: call it from a screen, a keybind or the client branch of `Item#use`.
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
  `all_in` and "read any book" advancements meant for your lore books. Without `give_on_first_join` a manual needs
  no entry at all.
- **Item names are lang keys, not book text.** The server can't read assets, so a book's item name comes from
  `book.<namespace>.<path>` in a language file (or the stack's `item_name`), not from the book's `title:`.
- **Skins:** `cover.png` and `page.png` are 32x32 nine-slices (8 px border on the cover, 5 px on the page; edges
  and centre are tiled, never stretched). `ornament.png` is 21x5. The default skin is in
  `assets/gottschbooks/textures/gui/book/`.
- **Escaping:** text that starts a line with `#`, `>`, `-`, `1.` or `![`, or uses `*` or `{` literally, needs a `\`
  in front.
- **Lists carry on:** a plain line straight after a list item joins that item. Leave a blank line to start a new
  paragraph.
- **Reloading:** books load with the other assets, so F3+T reloads them in game while you write.
