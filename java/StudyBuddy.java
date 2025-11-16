import javax.swing.JOptionPane;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudyBuddy {
    static class Note implements Serializable {
        String title;
        String content;
        Note(String t, String c) { this.title = t; this.content = c; }
        public String toString() { return title; }
    }

    static class NoteStore {
        private static final String FILE = "notes.dat";
        static List<Note> load() {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
                Object obj = in.readObject();
                List<?> list = (List<?>) obj;
                List<Note> notes = new ArrayList<>();
                for (Object o : list) notes.add((Note) o);
                return notes;
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        static void save(List<Note> notes) {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
                out.writeObject(notes);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error al guardar notas: " + e.getMessage());
            }
        }
    }

    static List<Note> notes = NoteStore.load();

    public static void main(String[] args) {
        while (true) {
            String[] options = {"Agregar nota", "Listar notas", "Ver nota", "Resumen", "Palabras clave", "Ejercicios", "Salir"};
            int choice = JOptionPane.showOptionDialog(null, "StudyBuddy", "Menú", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == -1 || choice == options.length - 1) break;
            if (choice == 0) addNote();
            if (choice == 1) listNotes();
            if (choice == 2) viewNote();
            if (choice == 3) summarizeNote();
            if (choice == 4) extractKeywords();
            if (choice == 5) generateExercises();
        }
        NoteStore.save(notes);
    }

    static void addNote() {
        String title = JOptionPane.showInputDialog(null, "Título de la nota:");
        if (title == null || title.trim().isEmpty()) return;
        String content = JOptionPane.showInputDialog(null, "Contenido de la nota:");
        if (content == null || content.trim().isEmpty()) return;
        notes.add(new Note(title.trim(), content.trim()));
        NoteStore.save(notes);
        JOptionPane.showMessageDialog(null, "Nota guardada");
    }

    static void listNotes() {
        if (notes.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No hay notas");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) sb.append((i + 1)).append(". ").append(notes.get(i).title).append("\n");
        JOptionPane.showMessageDialog(null, sb.toString());
    }

    static Note selectNote() {
        if (notes.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No hay notas");
            return null;
        }
        String[] titles = new String[notes.size()];
        for (int i = 0; i < notes.size(); i++) titles[i] = notes.get(i).title;
        String sel = (String) JOptionPane.showInputDialog(null, "Selecciona una nota", "Notas", JOptionPane.PLAIN_MESSAGE, null, titles, titles[0]);
        if (sel == null) return null;
        for (Note n : notes) if (n.title.equals(sel)) return n;
        return null;
    }

    static void viewNote() {
        Note n = selectNote();
        if (n == null) return;
        JOptionPane.showMessageDialog(null, n.content);
    }

    static void summarizeNote() {
        Note n = selectNote();
        if (n == null) return;
        String s = summarize(n.content, 3);
        JOptionPane.showMessageDialog(null, s.isEmpty() ? "Sin contenido" : s);
    }

    static void extractKeywords() {
        Note n = selectNote();
        if (n == null) return;
        List<String> kws = keywords(n.content, 8);
        JOptionPane.showMessageDialog(null, String.join(", ", kws));
    }

    static void generateExercises() {
        Note n = selectNote();
        if (n == null) return;
        List<String> kws = keywords(n.content, 5);
        List<String> ex = new ArrayList<>();
        for (String k : kws) ex.add("Define: " + k);
        String s = summarize(n.content, 2);
        if (!s.isEmpty()) ex.add("Explica: " + s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ex.size(); i++) sb.append((i + 1)).append(") ").append(ex.get(i)).append("\n");
        JOptionPane.showMessageDialog(null, sb.toString());
    }

    static String summarize(String text, int maxSentences) {
        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) return "";
        Map<String, Integer> tf = termFreq(text);
        List<SentenceScore> scored = new ArrayList<>();
        for (String s : sentences) scored.add(new SentenceScore(s, scoreSentence(s, tf)));
        Collections.sort(scored);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(maxSentences, scored.size()); i++) {
            if (i > 0) sb.append(" ");
            sb.append(scored.get(i).sentence.trim());
        }
        return sb.toString();
    }

    static class SentenceScore implements Comparable<SentenceScore> {
        String sentence;
        int score;
        SentenceScore(String s, int c) { sentence = s; score = c; }
        public int compareTo(SentenceScore o) { return Integer.compare(o.score, this.score); }
    }

    static List<String> splitSentences(String text) {
        String[] parts = text.split("(?<=[.!?])\\s+");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    static Map<String, Integer> termFreq(String text) {
        Set<String> stop = stopwords();
        Map<String, Integer> tf = new HashMap<>();
        String[] tokens = text.toLowerCase().replaceAll("[^a-záéíóúñüA-ZÁÉÍÓÚÑÜ0-9 ]", " ").split("\\s+");
        for (String t : tokens) {
            if (t.isEmpty() || stop.contains(t)) continue;
            tf.put(t, tf.getOrDefault(t, 0) + 1);
        }
        return tf;
    }

    static int scoreSentence(String sentence, Map<String, Integer> tf) {
        int s = 0;
        String[] tokens = sentence.toLowerCase().replaceAll("[^a-záéíóúñüA-ZÁÉÍÓÚÑÜ0-9 ]", " ").split("\\s+");
        Set<String> stop = stopwords();
        for (String t : tokens) {
            if (t.isEmpty() || stop.contains(t)) continue;
            s += tf.getOrDefault(t, 0);
        }
        return s;
    }

    static List<String> keywords(String text, int n) {
        Map<String, Integer> tf = termFreq(text);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(tf.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> res = new ArrayList<>();
        for (int i = 0; i < Math.min(n, entries.size()); i++) res.add(entries.get(i).getKey());
        return res;
    }

    static Set<String> stopwords() {
        List<String> list = Arrays.asList(
                "el","la","los","las","un","una","unos","unas","de","del","al","a","y","o","u","que","se","es","en","por","para","con","sin","no","sí","su","sus","lo","como","más","menos","muy","ya","pero","también","si","porque","cuando","donde","desde","hasta","entre","sobre","antes","después","esto","eso","estos","esas","esta","ese","estar","ser","hay"
        );
        return new HashSet<>(list);
    }
}