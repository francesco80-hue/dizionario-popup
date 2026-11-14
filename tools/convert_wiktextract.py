import gzip, json, sys, unicodedata
from pathlib import Path

def norm(s):
    s = unicodedata.normalize('NFD', s.casefold())
    return ''.join(c for c in s if unicodedata.category(c) != 'Mn')

def main(src, dst):
    opener = gzip.open if str(src).endswith('.gz') else open
    rows = {}
    with opener(src, 'rt', encoding='utf-8') as f:
        for line in f:
            try: obj = json.loads(line)
            except Exception: continue
            if obj.get('lang_code') != 'it': continue
            word = (obj.get('word') or '').strip()
            if not word or len(word) > 80: continue
            defs = []
            for sense in obj.get('senses') or []:
                if not isinstance(sense, dict): continue
                for key in ('glosses', 'raw_glosses'):
                    vals = sense.get(key, [])
                    if isinstance(vals, list):
                        for val in vals:
                            if isinstance(val, str): defs.append(val)
                            elif isinstance(val, dict) and (val.get('gloss') or val.get('text')): defs.append(str(val.get('gloss') or val.get('text')))
            if not defs: continue
            key = norm(word)
            item = {'word': word, 'pos': obj.get('pos', ''), 'definition': ' • '.join(dict.fromkeys(defs))[:5000], 'synonyms': [], 'examples': [], 'encyclopedia': ''}
            if key in rows: item['definition'] = ' • '.join(dict.fromkeys((rows[key]['definition'] + ' • ' + item['definition']).split(' • ')))[:5000]
            rows[key] = item
    Path(dst).parent.mkdir(parents=True, exist_ok=True)
    with open(dst, 'w', encoding='utf-8') as out: json.dump(list(rows.values()), out, ensure_ascii=False, separators=(',', ':'))
    print(f'Voci create: {len(rows):,}')
    print(f'Dimensione: {Path(dst).stat().st_size / 1024 / 1024:.1f} MB')

if __name__ == '__main__':
    if len(sys.argv) != 3: raise SystemExit('Uso: convert_wiktextract.py INPUT.jsonl[.gz] OUTPUT.json')
    main(sys.argv[1], sys.argv[2])
