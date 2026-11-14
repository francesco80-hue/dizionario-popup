#!/usr/bin/env python3
"""Build a compact Italian offline dictionary from Kaikki/Wiktextract JSONL."""
import gzip
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "build-data" / "itwiktionary.jsonl.gz"
CURATED = ROOT / "app" / "src" / "main" / "assets" / "dictionary-curated.json"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "dictionary.json"

def clean(value):
    return re.sub(r"\\s+", " ", str(value or "")).strip()

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
    curated = json.loads(CURATED.read_text(encoding="utf-8"))
    curated_keys = {clean(x["word"]).casefold() for x in curated}
    imported = {}
    with gzip.open(SOURCE, "rt", encoding="utf-8") as stream:
        for line in stream:
            try:
                item = json.loads(line)
            except json.JSONDecodeError:
                continue
            if item.get("lang_code") != "it":
                continue
            word = clean(item.get("word"))
            key = word.casefold()
            if not word or key in curated_keys or key in imported:
                continue
            senses = item.get("senses") or []
            glosses, examples, synonyms = [], [], []
            for sense in senses:
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
            imported[key] = {
                "word": word,
                "pos": clean(item.get("pos")),
                "definition": " ".join(glosses[:5]),
                "synonyms": list(dict.fromkeys(synonyms))[:12],
                "examples": examples[:3],
                "encyclopedia": "",
                "aliases": list(dict.fromkeys(forms))[:40]
            }
    ordered = curated + list(imported.values())
    OUTPUT.write_text(json.dumps(ordered, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print("Creato dizionario offline: {} voci totali ({} importate).".format(len(ordered), len(imported)))
    print("Dimensione JSON: {:.1f} MB".format(OUTPUT.stat().st_size / 1024 / 1024))

if __name__ == "__main__":
    main()
