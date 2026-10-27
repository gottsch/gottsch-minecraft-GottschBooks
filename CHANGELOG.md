# Changelog for Gottsch Books for NeoForge 1.21.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
