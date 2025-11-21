package studybuddy;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

class StudyBuddyConsole {
    static String currentUser;
    static List<Note> notes = new ArrayList<>();
    static Scanner in = new Scanner(System.in);

    static class Note implements Serializable {
        String title;
        String content;
        boolean isPublic;
        Note(String t, String c) { this.title = t; this.content = c; this.isPublic = false; }
        Note(String t, String c, boolean p) { this.title = t; this.content = c; this.isPublic = p; }
        public String toString() { return title; }
    }

    public static void main(String[] args) {
        while (true) {
            System.out.println("StudyBuddy (consola)");
            System.out.println("1) Iniciar sesión");
            System.out.println("2) Registrarse");
            System.out.println("0) Salir");
            System.out.print("> ");
            String opt = in.nextLine().trim();
            if ("1".equals(opt)) {
                login();
                if (currentUser != null) session();
            } else if ("2".equals(opt)) {
                register();
            } else if ("0".equals(opt)) {
                System.out.println("Adiós");
                break;
            }
        }
    }

    static void login() {
        System.out.print("Usuario: ");
        String u = in.nextLine().trim();
        System.out.print("Contraseña: ");
        String p = in.nextLine();
        if (UserStore.authenticate(u, p)) {
            currentUser = u;
            notes = NoteStore.load();
            notes = NoteStore.migrateFromDatIfPossible(notes);
            System.out.println("Bienvenido, " + currentUser);
        } else {
            System.out.println("Credenciales inválidas");
        }
    }

    static void register() {
        System.out.print("Nuevo usuario: ");
        String u = in.nextLine().trim();
        System.out.print("Contraseña: ");
        String p = in.nextLine();
        if (u.isEmpty() || p.isEmpty()) { System.out.println("Completa usuario y contraseña"); return; }
        boolean ok = UserStore.register(u, p);
        if (ok) System.out.println("Registro completado. Ya puedes iniciar sesión.");
        else System.out.println("El usuario ya existe");
    }

    static void session() {
        while (true) {
            System.out.println("\nUsuario: " + currentUser + " • Notas: " + notes.size());
            System.out.println("1) Agregar nota");
            System.out.println("2) Listar notas");
            System.out.println("3) Ver nota");
            System.out.println("4) Eliminar nota");
            System.out.println("5) Resumen del texto");
            System.out.println("6) Palabras clave");
            System.out.println("7) Ejercicios");
            System.out.println("8) Marcar/Desmarcar pública");
            System.out.println("9) Ver públicas");
            System.out.println("10) Importar pública");
            System.out.println("11) Eliminar pública propia");
            System.out.println("0) Cerrar sesión");
            System.out.print("> ");
            String opt = in.nextLine().trim();
            if ("1".equals(opt)) addNote();
            else if ("2".equals(opt)) listNotes();
            else if ("3".equals(opt)) viewNote();
            else if ("4".equals(opt)) deleteNote();
            else if ("5".equals(opt)) summarizeAction();
            else if ("6".equals(opt)) keywordsAction();
            else if ("7".equals(opt)) exercisesAction();
            else if ("8".equals(opt)) togglePublic();
            else if ("9".equals(opt)) listPublic();
            else if ("10".equals(opt)) importPublic();
            else if ("11".equals(opt)) deletePublicOwn();
            else if ("0".equals(opt)) { currentUser = null; notes.clear(); System.out.println("Sesión cerrada"); break; }
        }
    }

    static void addNote() {
        System.out.print("Título: ");
        String t = in.nextLine().trim();
        System.out.println("Contenido (línea única, pega texto si quieres):");
        String c = in.nextLine().trim();
        if (t.isEmpty() || c.isEmpty()) { System.out.println("Completa título y contenido"); return; }
        Note n = new Note(t, c, false);
        notes.add(n);
        NoteStore.save(notes);
        PublicStore.replaceForUser(currentUser, notes);
        System.out.println("Nota guardada");
    }

    static void listNotes() {
        if (notes.isEmpty()) { System.out.println("No hay notas"); return; }
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            int wc = n.content == null ? 0 : Math.max(0, n.content.trim().split("\\s+").length);
            System.out.println((i + 1) + ") " + n.title + " • " + wc + " palabras" + (n.isPublic ? " • pública" : ""));
        }
    }

    static int pickIndex() {
        listNotes();
        if (notes.isEmpty()) return -1;
        System.out.print("Número de nota: ");
        try { int idx = Integer.parseInt(in.nextLine().trim()) - 1; if (idx >= 0 && idx < notes.size()) return idx; } catch (Exception ignore) {}
        System.out.println("Selección inválida");
        return -1;
    }

    static void viewNote() {
        int i = pickIndex();
        if (i < 0) return;
        Note n = notes.get(i);
        System.out.println("\n=== " + n.title + (n.isPublic?" (pública)":"") + " ===");
        System.out.println(n.content);
    }

    static void deleteNote() {
        int i = pickIndex();
        if (i < 0) return;
        Note n = notes.remove(i);
        NoteStore.save(notes);
        PublicStore.replaceForUser(currentUser, notes);
        System.out.println("Nota eliminada: " + n.title);
    }

    static void summarizeAction() {
        String all = combinedAllContent();
        if (all == null || all.trim().isEmpty()) { System.out.println("Fuente vacía"); return; }
        String s = simpleSummary(all, 3);
        System.out.println("\nResumen:\n" + s);
    }

    static void keywordsAction() {
        String all = combinedAllContent();
        if (all == null || all.trim().isEmpty()) { System.out.println("Fuente vacía"); return; }
        List<String> ks = keywords(all, 8);
        System.out.println("\nPalabras clave:\n" + String.join(", ", ks));
    }

    static void exercisesAction() {
        String all = combinedAllContent();
        if (all == null || all.trim().isEmpty()) { System.out.println("Fuente vacía"); return; }
        List<String> ks = keywords(all, 5);
        List<String> ex = new ArrayList<>();
        for (String k : ks) ex.add("Define: " + k);
        String s = simpleSummary(all, 2);
        if (!s.isEmpty()) ex.add("Explica: " + s);
        System.out.println("\nEjercicios:");
        for (int i = 0; i < ex.size(); i++) System.out.println((i + 1) + ") " + ex.get(i));
    }

    static void togglePublic() {
        int i = pickIndex();
        if (i < 0) return;
        Note n = notes.get(i);
        n.isPublic = !n.isPublic;
        NoteStore.save(notes);
        PublicStore.replaceForUser(currentUser, notes);
        System.out.println(n.isPublic ? "Marcada como pública" : "Desmarcada pública");
    }

    static void listPublic() {
        List<PublicStore.Pub> pubs = PublicStore.load();
        if (pubs.isEmpty()) { System.out.println("No hay públicas"); return; }
        for (int i = 0; i < pubs.size(); i++) {
            PublicStore.Pub p = pubs.get(i);
            System.out.println((i + 1) + ") " + p.user + " • " + p.title);
        }
    }

    static void importPublic() {
        List<PublicStore.Pub> pubs = PublicStore.load();
        if (pubs.isEmpty()) { System.out.println("No hay públicas"); return; }
        listPublic();
        System.out.print("Número de pública a importar: ");
        try {
            int idx = Integer.parseInt(in.nextLine().trim()) - 1;
            if (idx < 0 || idx >= pubs.size()) { System.out.println("Selección inválida"); return; }
            PublicStore.Pub p = pubs.get(idx);
            notes.add(new Note(p.title, p.content, false));
            NoteStore.save(notes);
            System.out.println("Importada como nota privada");
        } catch (Exception e) { System.out.println("Entrada inválida"); }
    }

    static void deletePublicOwn() {
        List<PublicStore.Pub> pubs = PublicStore.load();
        List<PublicStore.Pub> mine = new ArrayList<>();
        for (PublicStore.Pub p : pubs) if (currentUser != null && currentUser.equals(p.user)) mine.add(p);
        if (mine.isEmpty()) { System.out.println("No tienes públicas"); return; }
        for (int i = 0; i < mine.size(); i++) System.out.println((i + 1) + ") " + mine.get(i).title);
        System.out.print("Número a eliminar: ");
        try {
            int idx = Integer.parseInt(in.nextLine().trim()) - 1;
            if (idx < 0 || idx >= mine.size()) { System.out.println("Selección inválida"); return; }
            PublicStore.Pub p = mine.get(idx);
            PublicStore.delete(p.user, p.title, p.content);
            for (Note n : notes) {
                String tt = n.title == null ? "" : n.title;
                String cc = n.content == null ? "" : n.content;
                if (tt.equals(p.title) && cc.equals(p.content)) n.isPublic = false;
            }
            NoteStore.save(notes);
            System.out.println("Pública eliminada");
        } catch (Exception e) { System.out.println("Entrada inválida"); }
    }

    static String combinedPrivateContent() {
        StringBuilder sb = new StringBuilder();
        for (Note n : notes) {
            if (n.content != null) {
                String t = n.content.trim();
                if (!t.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(t);
                }
            }
        }
        return sb.toString();
    }

    static String combinedAllContent() {
        StringBuilder sb = new StringBuilder();
        String priv = combinedPrivateContent();
        if (!priv.isEmpty()) sb.append(priv);
        List<PublicStore.Pub> pubs = PublicStore.load();
        for (PublicStore.Pub p : pubs) {
            if (p.content != null) {
                String t = p.content.trim();
                if (!t.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(t);
                }
            }
        }
        return sb.toString();
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

    static String simpleSummary(String text, int maxSentences) {
        List<String> sentences = splitSentences(text);
        if (!sentences.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(maxSentences, sentences.size()); i++) {
                if (i > 0) sb.append(" ");
                sb.append(sentences.get(i).trim());
            }
            return sb.toString();
        }
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return "";
        int cut = Math.min(240, t.length());
        return t.substring(0, cut);
    }

    static Set<String> stopwords() {
        List<String> list = Arrays.asList("el","la","los","las","un","una","unos","unas","de","del","al","a","y","o","u","que","se","es","en","por","para","con","sin","no","sí","su","sus","lo","como","más","menos","muy","ya","pero","también","si","porque","cuando","donde","desde","hasta","entre","sobre","antes","después","esto","eso","estos","esas","esta","ese","estar","ser","hay");
        return new HashSet<>(list);
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

    static List<String> keywords(String text, int n) {
        Map<String, Integer> tf = termFreq(text);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(tf.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> res = new ArrayList<>();
        for (int i = 0; i < Math.min(n, entries.size()); i++) res.add(entries.get(i).getKey());
        return res;
    }

    static class PublicStore {
        private static final String FILE = "data/public_notes.json";
        static class Pub implements Serializable { String user; String title; String content; Pub(String u,String t,String c){user=u;title=t;content=c;} }
        static List<Pub> load(){
            List<Pub> res = new ArrayList<>();
            String json = NoteStore.readAll(FILE);
            if (json == null) return res;
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{\\s*\\\"user\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"title\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"content\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*\\}", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);
            while (m.find()) {
                String u = m.group(1).replace("\\n","\n").replace("\\\"","\"");
                String t = m.group(2).replace("\\n","\n").replace("\\\"","\"");
                String c = m.group(3).replace("\\n","\n").replace("\\\"","\"");
                res.add(new Pub(u,t,c));
            }
            return res;
        }
        static void replaceForUser(String user, List<Note> notes){
            List<Pub> all = load();
            List<Pub> filtered = new ArrayList<>();
            for (Pub p : all) if (!p.user.equals(user)) filtered.add(p);
            for (Note n : notes) if (n.isPublic) filtered.add(new Pub(user, n.title, n.content));
            writeAll(filtered);
        }
        static void writeAll(List<Pub> pubs){
            StringBuilder sb = new StringBuilder();
            sb.append('{').append("\"public\":").append('[');
            boolean first = true;
            for (Pub p : pubs) {
                if (!first) sb.append(','); first = false;
                sb.append('{')
                  .append("\"user\":\"").append(NoteStore.escape(p.user)).append("\",")
                  .append("\"title\":\"").append(NoteStore.escape(p.title)).append("\",")
                  .append("\"content\":\"").append(NoteStore.escape(p.content)).append("\"}");
            }
            sb.append(']').append('}');
            NoteStore.writeAll(FILE, sb.toString());
        }
        static void delete(String user, String title, String content){
            List<Pub> all = load();
            List<Pub> filtered = new ArrayList<>();
            for (Pub p : all) {
                boolean sameUser = p.user != null && p.user.equals(user);
                boolean sameTitle = p.title != null && p.title.equals(title);
                boolean sameContent = p.content != null && p.content.equals(content);
                if (!(sameUser && sameTitle && sameContent)) filtered.add(p);
            }
            writeAll(filtered);
        }
    }

    static class NoteStore {
        private static String file() {
            String base = "notes";
            if (currentUser != null && !currentUser.isEmpty()) base = base + "_" + currentUser;
            return "data/" + base + ".json";
        }
        static List<Note> load() {
            try {
                String json = readAll(file());
                return parseNotes(json);
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        static List<Note> migrateFromDatIfPossible(List<Note> current){
            if (current != null && !current.isEmpty()) return current;
            String dat = file().replace(".json", ".dat");
            try {
                if (!new File(dat).exists()) return current;
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(dat))) {
                    Object obj = in.readObject();
                    List<?> list = (List<?>) obj;
                    List<Note> notes = new ArrayList<>();
                    for (Object o : list) {
                        Note n = (Note) o;
                        notes.add(new Note(n.title, n.content, false));
                    }
                    save(notes);
                    return notes;
                }
            } catch (Exception ignore) { }
            return current;
        }
        static void save(List<Note> notes) {
            try {
                String json = toNotesJson(notes);
                writeAll(file(), json);
            } catch (Exception e) { }
        }
        static String readAll(String path){
            try(FileInputStream in = new FileInputStream(path)){
                byte[] b = in.readAllBytes();
                return new String(b, StandardCharsets.UTF_8);
            }catch(Exception e){ return null; }
        }
        static void writeAll(String path, String s){
            try{
                File f = new File(path);
                File d = f.getParentFile();
                if (d != null) d.mkdirs();
                try(FileOutputStream out = new FileOutputStream(f)){
                    out.write(s.getBytes(StandardCharsets.UTF_8));
                }
            }catch(Exception e){ }
        }
        static List<Note> parseNotes(String json){
            List<Note> res = new ArrayList<>();
            if (json == null) return res;
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{\\s*\\\"title\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"content\\\"\\s*:\\s*\\\"(.*?)\\\"(,\\s*\\\"isPublic\\\"\\s*:\\s*(true|false))?\\s*\\}", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);
            while (m.find()) {
                String title = m.group(1).replace("\\n","\n").replace("\\\"","\"");
                String content = m.group(2).replace("\\n","\n").replace("\\\"","\"");
                boolean pub = m.group(4) != null && m.group(4).equals("true");
                res.add(new Note(title, content, pub));
            }
            return res;
        }
        static String toNotesJson(List<Note> notes){
            StringBuilder sb = new StringBuilder();
            sb.append('{').append("\"notes\":").append('[');
            boolean first = true;
            for (Note n : notes) {
                if (!first) sb.append(','); first = false;
                sb.append('{')
                  .append("\"title\":\"").append(escape(n.title)).append("\",")
                  .append("\"content\":\"").append(escape(n.content)).append("\",")
                  .append("\"isPublic\":").append(n.isPublic ? "true" : "false")
                  .append('}');
            }
            sb.append(']').append('}');
            return sb.toString();
        }
        static String escape(String s){ return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n"); }
    }

    static class User implements Serializable {
        String username;
        String passHash;
        User(String u, String h){ username=u; passHash=h; }
    }
    static class UserStore {
        private static final String FILE = "data/users.json";
        static Map<String, User> load() {
            try {
                String json = readAll(FILE);
                Map<String,String> raw = parseJsonMap(json);
                Map<String,User> res = new HashMap<>();
                for (Map.Entry<String,String> e : raw.entrySet()) res.put(e.getKey(), new User(e.getKey(), e.getValue()));
                return res;
            } catch (Exception e) { return new HashMap<>(); }
        }
        static void save(Map<String, User> users) {
            try {
                Map<String,String> raw = new HashMap<>();
                for (Map.Entry<String,User> e : users.entrySet()) raw.put(e.getKey(), e.getValue().passHash);
                String json = toJsonMap(raw);
                writeAll(FILE, json);
            } catch (Exception e) { }
        }
        static boolean register(String username, String password) {
            Map<String,User> users = load();
            if (users.containsKey(username)) return false;
            users.put(username, new User(username, hash(password)));
            save(users);
            return true;
        }
        static boolean authenticate(String username, String password) {
            Map<String,User> users = load();
            User u = users.get(username);
            if (u == null) return false;
            return u.passHash.equals(hash(password));
        }
        static String hash(String s){
            try{
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for(byte x: b) sb.append(String.format("%02x", x));
                return sb.toString();
            }catch(Exception e){ return s; }
        }
        static String readAll(String path){
            try(FileInputStream in = new FileInputStream(path)){
                byte[] b = in.readAllBytes();
                return new String(b, StandardCharsets.UTF_8);
            }catch(Exception e){ return null; }
        }
        static void writeAll(String path, String s){
            try{
                File f = new File(path);
                File d = f.getParentFile();
                if (d != null) d.mkdirs();
                try(FileOutputStream out = new FileOutputStream(f)){
                    out.write(s.getBytes(StandardCharsets.UTF_8));
                }
            }catch(Exception e){ }
        }
        static Map<String,String> parseJsonMap(String json){
            Map<String,String> m = new HashMap<>();
            if (json == null) return m;
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"(.*?)\"", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = p.matcher(json);
            while (matcher.find()) {
                String k = matcher.group(1);
                String v = matcher.group(2);
                m.put(k, v);
            }
            return m;
        }
        static String toJsonMap(Map<String,String> m){
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String,String> e : m.entrySet()) {
                if (!first) sb.append(','); first = false;
                sb.append('"').append(e.getKey()).append('"').append(':').append('"').append(e.getValue()).append('"');
            }
            sb.append('}');
            return sb.toString();
        }
    }
}