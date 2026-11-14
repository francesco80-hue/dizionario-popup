#!/usr/bin/env python3
"""Build a compact indexed SQLite dictionary from Kaikki/Wiktextract JSONL."""
import gzip
import json
import re
import sqlite3
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "build-data" / "itwiktionary.jsonl.gz"
CURATED = ROOT / "app" / "src" / "main" / "assets" / "dictionary-curated.json"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "dictionary.db"

def clean(value):
    return re.sub(r"\s+", " ", str(value or "")).strip()

def fold(value):
    value = unicodedata.normalize("NFD", clean(value).lower())
    value = "".join(ch for ch in value if unicodedata.category(ch) != "Mn")
    return value.replace("’", "'").strip()

def words(items, limit=12):
    out = []
    for item in items or []:
        word = clean(item.get("word") if isinstance(item, dict) else item)
        if word and word not in out:
            out.append(word)
        if len(out) >= limit:
            break
    return out

def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT.exists():
        OUTPUT.unlink()
    db = sqlite3.connect(OUTPUT)
    db.execute("PRAGMA journal_mode=OFF")
    db.execute("PRAGMA synchronous=OFF")
    db.execute("""CREATE TABLE entries (
        id INTEGER PRIMARY KEY,
        word TEXT NOT NULL,
        word_key TEXT NOT NULL UNIQUE,
        pos TEXT NOT NULL,
        definition TEXT NOT NULL,
        synonyms TEXT NOT NULL,
        examples TEXT NOT NULL,
        encyclopedia TEXT NOT NULL
    )""")
    db.execute("CREATE INDEX idx_entries_word_key ON entries(word_key)")
    db.execute("""CREATE TABLE aliases (
        alias_key TEXT NOT NULL,
        entry_id INTEGER NOT NULL,
        UNIQUE(alias_key, entry_id),
        FOREIGN KEY(entry_id) REFERENCES entries(id)
    )""")
    db.execute("CREATE INDEX idx_aliases_key ON aliases(alias_key)")

    def insert_entry(item):
        word = clean(item.get("word"))
        key = fold(word)
        if not word or not key:
            return False
        try:
            cur = db.execute(
                "INSERT INTO entries(word,word_key,pos,definition,synonyms,examples,encyclopedia) VALUES(?,?,?,?,?,?,?)",
                (word, key, clean(item.get("pos")), clean(item.get("definition")),
                 json.dumps(item.get("synonyms") or [], ensure_ascii=False),
                 json.dumps(item.get("examples") or [], ensure_ascii=False),
                 clean(item.get("encyclopedia")))
            )
        except sqlite3.IntegrityError:
            return False
        entry_id = cur.lastrowid
        for alias in item.get("aliases") or []:
            alias_key = fold(alias)
            if alias_key and alias_key != key:
                db.execute("INSERT OR IGNORE INTO aliases(alias_key,entry_id) VALUES(?,?)", (alias_key, entry_id))
        return True

    curated = json.loads(CURATED.read_text(encoding="utf-8"))
    for item in curated:
        insert_entry(item)

    imported = 0
    with gzip.open(SOURCE, "rt", encoding="utf-8") as stream:
        for line in stream:
            try:
                item = json.loads(line)
            except json.JSONDecodeError:
                continue
            if item.get("lang_code") != "it":
                continue
            word = clean(item.get("word"))
            if not word:
                continue
            glosses, examples, synonyms = [], [], []
            for sense in item.get("senses") or []:
                for gloss in sense.get("glosses") or []:
                    value = clean(gloss)
                    if value and value not in glosses:
                        glosses.append(value)
                for example in sense.get("examples") or []:
                    value = clean(example.get("text") if isinstance(example, dict) else example)
                    if value and value not in examples:
                        examples.append(value)
                synonyms.extend(words(sense.get("synonyms"), 12))
            if not glosses:
                continue
            forms = []
            for form in item.get("forms") or []:
                if isinstance(form, dict):
                    forms.extend(words([form.get("form")], 40))
            row = {
                "word": word,
                "pos": item.get("pos", ""),
                "definition": " ".join(glosses[:5]),
                "synonyms": list(dict.fromkeys(synonyms))[:12],
                "examples": examples[:3],
                "encyclopedia": "",
                "aliases": list(dict.fromkeys(forms))[:40]
            }
            if insert_entry(row):
                imported += 1
            if imported and imported % 5000 == 0:
                db.commit()
    db.commit()
    total = db.execute("SELECT COUNT(*) FROM entries").fetchone()[0]
    aliases = db.execute("SELECT COUNT(*) FROM aliases").fetchone()[0]
    db.execute("ANALYZE")
    db.commit()
    db.close()
    print(f"Creato database offline: {total} voci ({imported} importate), {aliases} forme indicizzate.")
    print(f"Dimensione database: {OUTPUT.stat().st_size / 1024 / 1024:.1f} MB")

if __name__ == "__main__":
    main()
