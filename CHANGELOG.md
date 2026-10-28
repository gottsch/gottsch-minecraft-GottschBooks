# Changelog for Gottsch Books for NeoForge 1.21.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - Unreleased

### Added
- Item lookup: hovering over an item in any inventory adds a tooltip line naming the manual that explains it, and
  the **Look Up Item in a Manual** key (B by default) opens the manual at that entry, on the page that shows the item.
  Closing the book returns to the inventory. Works while the player carries the manual; locked entries never answer.
  The line shows the key as bound, never wraps, and is left out while SHIFT is held, to keep long tooltips short.
- An entry explains an item when it spotlights it, has a recipe that makes it, has it as its `icon`, or lists it in
  the new `items:` front matter (item ids and `#tags`), in that order of preference.
- `lookup:` in book.md: `always` answers lookups without the manual in the inventory, `never` turns them off.
- "New" marks: entries the player hasn't opened have a red dot on the contents (and their categories, and search
  results), and the contents counts them with a **Mark all read** link. Kept per player in `gottschbooks/reading.json`.
- The item lookup's tooltip line goes away for an item once its entry has been opened (`lookupHintUntilRead`, on by
  default); the key still works.
- WRITING.md has a Skins section on giving a book its own cover, page and ornament; the example journal has a
  sea-blue skin.
- A client config, `config/gottschbooks-client.toml`, whose `lookupHint = false` hides the tooltip line for players
  who know their mods; the key keeps working. `/gottschbooks lookup hint on|off` sets it from chat.
- `"announce_on_first_join": true` in a book's server entry: instead of giving the book, each player is told about it
  once, in chat, with a **[Get it]** link. A `book.<ns>.<path>.announce` lang line words it. `announceBooks = false` in
  `serverconfig/gottschbooks-server.toml` turns announcements off for a world; giving is never affected.
- `/gottschbooks get <book>`: any player takes a book whose entry gives or announces it on first join, unless they
  already carry it. `/gottschbooks open` is still for operators only.
- `BookDef.announceOnFirstJoin()` and `BookDef.free()`; the four-argument constructor still works.
- Manual search: **Search** above the book (or Ctrl+F) lists the entries that mention every word typed, with a
  line of each around the word. A result opens on the page with the word; Back returns to the results. Titles,
  text, tables, captions and spotlighted items' names are searched; locked entries never show.
- `lookup_from:` (entries, or book.md for every entry): `items` answers lookups only for the entry's `items:`, so an
  icon or spotlight can be just for show; `none` answers for nothing; `all` (the default) for everything shown.
- Item lookups match on components: an item written with them (`items: minecraft:filled_map[custom_data={D2DungeonMap:{}}]`) answers only for stacks that
  have them, components (`custom_data` as a subset of the stack's, the rest exactly), and comes before any entry for the plain item.
- `GottschBooksClient.open(book, entry, stack)`: opens an entry on the page that shows a stack (a spotlight of that
  very kind first).
- `/gottschbooks check` writes every problem to `logs/gottschbooks-check.txt`, grouped by file, and chat links to it:
  **[Open report]** opens it, **[Copy]** copies it whole. Chat now shows the first 10 problems.
- `/gottschbooks check` reports `items:` values that aren't items or tags, unknown `lookup:` and `lookup_from:`
  values, `lookup_from: items` without `items:`, and a vanilla icon that answers lookups in a mod's manual when the
  entry shows that item nowhere else.

## [1.2.0] - Unreleased

### Added
- Recipe blocks draw each kind of recipe the way its block does: smelting, blasting, smoking and campfire cooking
  show the input over its furnace with the cooking time; stonecutting shows the stonecutter; smithing shows
  template, base and addition. Any other recipe, a mod's machine included, shows its ingredients in a row with its
  station. Before, every recipe was drawn as a crafting grid.
- `/gottschbooks check [book]`: lists what is wrong in every loaded book (or one) in chat and the log: missing
  items, recipes, images, mobs and fonts, components that don't parse, broken `entry:` links and anchors, unknown
  categories and fenced blocks, and missing `_index.md`, `text.md` or skin textures. Runs on the client, for anyone.
- Tables: GitHub-style pipe tables, with per-column alignment. A cell may start with item icons (`![](item:ns:id)`,
  components allowed), drawn at 16 px with tooltips. Columns share the page by how much text they hold, rows never
  split across pages, and a table that continues on the next page repeats its header. A blank header row draws no
  header. `\|` is a literal pipe.
- Page styles: one *single* page as wide as the book, for wide tables, or the usual two side by side (*spread*).
  `{page:single}` and `{page:spread}` on a line of their own break the page and switch the style until the next one;
  `pages: single` in book.md or an entry's front matter sets the style the text starts in. Pages pair up afresh after
  each switch.
- `{page}` breaks the page, in the same braces as colours and fonts. `<!-- page -->` still works.
- `/gottschbooks check` also reports table rows with the wrong number of cells, icons after a cell's text, empty
  tables, and `pages` values and `{page:...}` styles other than `single` and `spread`.
- Example books: *Furnaces and Workstations* and *Ores at a Glance* (two pages of prose, then a single wide page of
  tables with icons, then two pages again) in the field guide, a tide table in the journal, and `example:mistakes`, a manual that is wrong on purpose for the check to
  find.
- AI-INSTRUCTIONS.md: instructions for an AI to write a whole book from a mod's or pack's own docs, in any chat
  AI (free plans included) or as a Claude Code skill: the files, the exact syntax, rules against invented ids,
  working in reply-sized parts, and a report of what to check. `tools/split-book.py` (and `split-book.ps1` for
  Windows without Python) write the files from the saved replies. README: *Writing books with an AI*.
- The AI kit, `gottschbooks-ai-kit-<version>.zip`: the instructions, both scripts and a plain-text how-to, built by
  `./gradlew aiKit` (and with the jar) to upload next to each release.

### Changed
- A line of text with glyphs taller than a line, such as a font of large pictures, grows to fit them instead of
  overlapping the lines around it. Text with no such glyphs lays out exactly as before. Headings keep their height.
  The field guide's *Potions and Curios* shows two double-size pictures.
- Bold text (`**bold**`, `#` to `######` headings, category names on the contents) is thickened by half a glyph pixel
  instead of vanilla's whole one, which filled in the gaps of letters such as *m* and *w*. Letters sit where they
  did, so pages wrap as before.
- A recipe with nothing to draw (its ingredients worked out in code, such as dyeing armour) shows as missing instead
  of an empty grid.

## [1.1.0] - 2026-10-26

### Added
- Items with components: an illustration, spotlight or `icon` may end with `/give`-style components, e.g.
  `![Healing](item:minecraft:potion[potion_contents={potion:"minecraft:healing"}])`. Parsed with the game's own item
  parser against the world's registries, so potions, enchanted books, dyed items, player heads and mod item variants
  draw as themselves (#1).
- Fonts: `{font:mypack:icons}text{}` draws a run of text in a resource pack font, such as a font of pictures for
  icons in a line of text (#1). `{}` ends a font as it ends a colour.
- Example books: a *Potions and Curios* entry in the field guide (with an `example:icons` picture font), and a
  fifth night in the journal with a spotlight and an image.

### Changed
- An illustration whose item doesn't exist now shows `[item ...?]` on the page, as fenced blocks do, instead of
  being left out.
- WRITING.md: fenced blocks (spotlights, recipes, images, mobs) are documented for lore books too. They always
  worked there.

## 2026-10-17

### Added
- Project skeleton.
- `gottschbooks:book` datapack registry (`data/<ns>/gottschbooks/book/<book>.json`, `{}` or `{"disabled": true}`),
  and book tags (`data/<ns>/tags/gottschbooks/book/<tag>.json`).
- `gottschbooks:book` data component: which book a stack is.
- `gottschbooks:read_books` player attachment: the books a player has read, kept through death.
- `gottschbooks:book_read` advancement trigger, with optional `book`, `books` (`#tag`, every book in it) and
  `all_in` (every book in a namespace) conditions.
- Illustration providers: `Illustrations.register(scheme, provider)` for `![caption](scheme:ns:id)`; `item` built in.
- `Books`: the common API (stack id, read tracking).
- Lore-book reader (moved from Magic Treasures): the book flows onto as many pages as the window needs, one or two
  pages at a time, with a title page, ornaments between sections, illustrations and editor's notes. Opened with
  `GottschBooksClient.open(book, icon)` or by right-clicking a `BookItem`.
- Book files: `assets/<ns>/books/<book>/<locale>/text.md`, falling back to `en_us`; optional
  `assets/<ns>/books/<book>/book.md` with `skin: <ns>:<folder>` for the book's own `cover.png`, `page.png` and
  `ornament.png`. Resource packs can replace any of them.
- Markdown subset: front matter (`title`, `author`), paragraphs, hard breaks, `*italic*`, `**bold**`,
  `{gold}colour{}` / `{#d4a017}colour{}`, `#` headings, `---` ornaments, `>` notes, `![caption](scheme:ns:id)`
  illustrations and `<!-- page -->` page breaks. Anything else is shown as plain text.
- Manuals (`mode: manual` in `book.md`): a contents page (landing text, then each category's entries with their
  icons), entries opened with a click, `[text](entry:id#anchor)` links between entries, Back and Contents above the
  book (Backspace goes back too), and web links `[text](https://...)` that ask before opening. Files:
  `<locale>/_index.md` (title, landing text), `<locale>/categories/<id>.md` (title, icon, sort, description) and
  `<locale>/<entry>.md` (title, category, icon, sort, and `advancement:` to hide an entry until the player has it).
- Entry blocks: ` ```spotlight <item> ` (a large item with its text), ` ```recipe <ns:id> ` (a crafting grid, cycling
  through ingredients that accept several items), ` ```image <texture> [width height] `, ` ```entity <ns:id> ` (a
  turning mob); `- item` / `1. item` lists; `## Heading {#anchor}` for link targets.
- `/gottschbooks open <players> <book> [entry]` (operators) and `GottschBooksClient.open(book, entry)`.
- `BookItem(properties, book)`: an item that is always the same book, such as a mod's manual.
- A warning in the log for a `{#rrggbb}` colour that is hard to read on the page.
- `gottschbooks:book`, a generic book item that opens whatever book its stack names, so packs can hand out books
  with no code: `/give @p gottschbooks:book[gottschbooks:book="mymod:guide"]`, or `set_components` in a loot table.
- Book entries (`data/<ns>/gottschbooks/book/<book>.json`) take `"give_on_first_join": true` (each player gets the
  book once), `"item"` (which `BookItem` to give) and `"track_reading": false` (for manuals, so they don't count
  toward "read every book" advancements).
- Any `BookItem` is named by the lang key `book.<namespace>.<path>` when a language file has it.
- `WRITING.md`: the guide to writing books.
- List items have a little space between them.
- `#` and `##` headings are drawn larger (about 1.5x and 1.25x, kept to whole screen pixels) with a faint rule under them, so they stand out from bold text.
