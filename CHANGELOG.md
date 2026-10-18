# Changelog for Gottsch Books for Forge 1.20.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2026-10-17

### Added
- Project skeleton.
- `gottschbooks:book` datapack registry (`data/<ns>/gottschbooks/book/<book>.json`, `{}` or `{"disabled": true}`),
  and book tags (`data/<ns>/tags/gottschbooks/book/<tag>.json`).
- `gottschbooks:book` stack NBT (a string): which book a stack is.
- `gottschbooks:read_books` in the player's persisted data: the books a player has read, kept through death.
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
  with no code: `/give @p gottschbooks:book{"gottschbooks:book":"mymod:guide"}`, or `set_nbt` in a loot table.
- Book entries (`data/<ns>/gottschbooks/book/<book>.json`) take `"give_on_first_join": true` (each player gets the
  book once), `"item"` (which `BookItem` to give) and `"track_reading": false` (for manuals, so they don't count
  toward "read every book" advancements).
- Any `BookItem` is named by the lang key `book.<namespace>.<path>` when a language file has it.
- `WRITING.md`: the guide to writing books.
- List items have a little space between them.
- `#` and `##` headings are drawn larger (about 1.5x and 1.25x, kept to whole screen pixels) with a faint rule under them, so they stand out from bold text.
