GOTTSCH BOOKS - AI KIT
======================

This kit lets an AI write a Gottsch Books book for you: a manual for your mod or modpack, or a lore book. It works in
any chat AI, free plans included: claude.ai, ChatGPT, Gemini and others.

What's inside:

  AI-INSTRUCTIONS.md   the instructions you give the AI: the book format, and the rules that keep it from inventing
                       item ids or using Markdown the book can't show
  split-book.py        turns the AI's replies into the book's files (needs Python 3.8 or later)
  split-book.ps1       the same for Windows, if you don't have Python
  README.txt           this file


1. START A CHAT
---------------
Attach (or paste) AI-INSTRUCTIONS.md, then your material:
  - your mod's docs, wiki pages or README, or your story notes for a lore book
  - a list of your item and recipe ids: your assets/<modid>/lang/en_us.json and your recipe folder will do

Then say what you want, for example:
  "Write a manual for my mod Widgetry, for NeoForge 1.21.1, using Gottsch Books 1.2.0."
  (or Forge 1.20.1: say which, since the two write item data differently)

The AI asks for anything else it needs.


2. OUTLINE FIRST, THEN ONE CATEGORY AT A TIME
---------------------------------------------
Ask for the outline first (the categories and entries), and fix it before any files are written. Then ask for the
files a part at a time: "write the core files", then "write the Machines category", and so on. Short parts don't get
cut off. Free plans limit replies and messages, so a big manual may take a couple of sessions.


3. SAVE THE REPLIES AND WRITE THE FILES
---------------------------------------
Copy each whole reply into a text file next to these scripts: answer1.txt, answer2.txt, and so on.

With Python (Windows, macOS, Linux), in a terminal opened in this folder:

  python split-book.py answer1.txt answer2.txt --out "C:\path\to\mymod\src\main\resources"

On Windows without Python, in a PowerShell window opened in this folder:

  powershell -ExecutionPolicy Bypass -File split-book.ps1 answer1.txt answer2.txt -Out "C:\path\to\mymod\src\main\resources"

  --out (-Out)      where the files go: your mod's src/main/resources, or your resource pack's folder
  --dry-run (-DryRun)   only list the files, without writing anything

The scripts only write under assets/ and data/ in the folder you give. They say which files are new and which they
replaced, and flag any file that looks cut off, so you can ask the AI for it again.

For a modpack without a mod: point --out at your resource pack, then move the data/ folder it creates into your data
pack.


4. CHECK IT IN GAME
-------------------
Start the game (or press F3+T if it's running), open a world, and type:

  /gottschbooks check

It lists every missing item, recipe or link, file by file. Paste anything it reports back into the chat, ask the AI
to fix it, and run the script again with the new reply.

The AI lists every place it needed an id it didn't have, rather than making one up; fill those in, or give it the ids.


MORE
----
The full guide to the book format, and the source:
  https://github.com/gottsch/gottsch-minecraft-GottschBooks
