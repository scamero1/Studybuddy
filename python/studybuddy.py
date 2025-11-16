import json
import os
import re

NOTES_FILE = "notes.json"

def load_notes():
    if not os.path.exists(NOTES_FILE):
        return []
    try:
        with open(NOTES_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return []

def save_notes(notes):
    with open(NOTES_FILE, "w", encoding="utf-8") as f:
        json.dump(notes, f, ensure_ascii=False, indent=2)

def input_note():
    title = input("Título: ").strip()
    if not title:
        return None
    print("Contenido (línea vacía para terminar):")
    lines = []
    while True:
        line = input()
        if not line:
            break
        lines.append(line)
    content = "\n".join(lines).strip()
    if not content:
        return None
    return {"title": title, "content": content}

def list_notes(notes):
    if not notes:
        print("No hay notas")
        return
    for i, n in enumerate(notes, 1):
        print(f"{i}. {n['title']}")

def select_note(notes):
    if not notes:
        print("No hay notas")
        return None
    list_notes(notes)
    try:
        idx = int(input("Selecciona número: "))
    except Exception:
        return None
    if idx < 1 or idx > len(notes):
        return None
    return notes[idx - 1]

def split_sentences(text):
    parts = re.split(r"(?<=[.!?])\s+", text)
    return [p.strip() for p in parts if p.strip()]

def stopwords():
    return set([
        "el","la","los","las","un","una","unos","unas","de","del","al","a","y","o","u","que","se","es","en","por","para","con","sin","no","sí","su","sus","lo","como","más","menos","muy","ya","pero","también","si","porque","cuando","donde","desde","hasta","entre","sobre","antes","después","esto","eso","estos","esas","esta","ese","estar","ser","hay"
    ])

def term_freq(text):
    tokens = re.sub(r"[^a-zA-ZáéíóúñüÁÉÍÓÚÑÜ0-9 ]", " ", text.lower()).split()
    sw = stopwords()
    tf = {}
    for t in tokens:
        if not t or t in sw:
            continue
        tf[t] = tf.get(t, 0) + 1
    return tf

def score_sentence(sentence, tf):
    tokens = re.sub(r"[^a-zA-ZáéíóúñüÁÉÍÓÚÑÜ0-9 ]", " ", sentence.lower()).split()
    sw = stopwords()
    s = 0
    for t in tokens:
        if not t or t in sw:
            continue
        s += tf.get(t, 0)
    return s

def summarize(text, max_sentences=3):
    sentences = split_sentences(text)
    if not sentences:
        return ""
    tf = term_freq(text)
    scored = sorted(sentences, key=lambda s: score_sentence(s, tf), reverse=True)
    return " ".join(scored[:max_sentences]).strip()

def keywords(text, n=8):
    tf = term_freq(text)
    items = sorted(tf.items(), key=lambda x: x[1], reverse=True)
    return [k for k, _ in items[:n]]

def exercises_from_note(note):
    k = keywords(note["content"], 5)
    ex = [f"Define: {kw}" for kw in k]
    s = summarize(note["content"], 2)
    if s:
        ex.append(f"Explica: {s}")
    return ex

def main():
    notes = load_notes()
    while True:
        print("\nStudyBuddy")
        print("1) Agregar nota")
        print("2) Listar notas")
        print("3) Ver nota")
        print("4) Resumen")
        print("5) Palabras clave")
        print("6) Ejercicios")
        print("7) Salir")
        try:
            choice = int(input("Opción: "))
        except Exception:
            continue
        if choice == 7:
            save_notes(notes)
            break
        if choice == 1:
            n = input_note()
            if n:
                notes.append(n)
                save_notes(notes)
                print("Nota guardada")
        if choice == 2:
            list_notes(notes)
        if choice == 3:
            n = select_note(notes)
            if n:
                print(n["content"]) 
        if choice == 4:
            n = select_note(notes)
            if n:
                print(summarize(n["content"]))
        if choice == 5:
            n = select_note(notes)
            if n:
                print(", ".join(keywords(n["content"])))
        if choice == 6:
            n = select_note(notes)
            if n:
                for i, e in enumerate(exercises_from_note(n), 1):
                    print(f"{i}) {e}")

if __name__ == "__main__":
    main()