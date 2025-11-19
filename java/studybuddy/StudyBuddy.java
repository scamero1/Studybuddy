package studybuddy;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudyBuddy {
    static String currentUser;
    static class Note implements Serializable {
        String title;
        String content;
        boolean isPublic;
        Note(String t, String c) { this.title = t; this.content = c; this.isPublic = false; }
        Note(String t, String c, boolean p) { this.title = t; this.content = c; this.isPublic = p; }
        public String toString() { return title; }
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
        static String corpus(){
            StringBuilder sb = new StringBuilder();
            List<Pub> pubs = load();
            for (Pub p : pubs) {
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
        static String corpusPrivate(){
            String json = readAll(file());
            List<Note> list = parseNotes(json);
            StringBuilder sb = new StringBuilder();
            for (Note n : list) {
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
        static String corpusCombined(){
            String priv = corpusPrivate();
            String pub = PublicStore.corpus();
            if (priv.isEmpty()) return pub;
            if (pub.isEmpty()) return priv;
            return priv + "\n\n" + pub;
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
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error al guardar notas: " + e.getMessage());
            }
        }
        static String readAll(String path){
            try(FileInputStream in = new FileInputStream(path)){
                byte[] b = in.readAllBytes();
                return new String(b, StandardCharsets.UTF_8);
            }catch(Exception e){ return null; }
        }
        static void writeAll(String path, String s){
            try{
                java.io.File f = new java.io.File(path);
                java.io.File d = f.getParentFile();
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

static List<Note> notes = new ArrayList<>();

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
            }
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            String u = LoginDialog.prompt(null);
            if (u == null || u.isEmpty()) return;
            currentUser = u;
            notes = NoteStore.load();
            notes = NoteStore.migrateFromDatIfPossible(notes);
            new StudyBuddyFrame().setVisible(true);
        });
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
    static String simpleSummary(String text, int maxSentences){
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

    static String combinedPrivateContent(){
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
    static String combinedAllContent(){
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

    static Set<String> stopwords() {
        List<String> list = Arrays.asList(
                "el","la","los","las","un","una","unos","unas","de","del","al","a","y","o","u","que","se","es","en","por","para","con","sin","no","sí","su","sus","lo","como","más","menos","muy","ya","pero","también","si","porque","cuando","donde","desde","hasta","entre","sobre","antes","después","esto","eso","estos","esas","esta","ese","estar","ser","hay"
        );
        return new HashSet<>(list);
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
            } catch (Exception e) {
                return new HashMap<>();
            }
        }
        static void save(Map<String, User> users) {
            try {
                Map<String,String> raw = new HashMap<>();
                for (Map.Entry<String,User> e : users.entrySet()) raw.put(e.getKey(), e.getValue().passHash);
                String json = toJsonMap(raw);
                writeAll(FILE, json);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error al guardar usuarios: " + e.getMessage());
            }
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
                java.io.File f = new java.io.File(path);
                java.io.File d = f.getParentFile();
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
    static class LoginDialog extends JDialog {
        Color accent = new Color(22,163,74);
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton login = new JButton("Entrar");
        JButton goRegister = new JButton("Registrarse");
        String acceptedUser;
        LoginDialog(Frame owner){
            super(owner, "Inicio de sesión", true);
            setLayout(new BorderLayout());
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(accent);
            JLabel title = new JLabel("  StudyBuddy");
            title.setForeground(Color.WHITE);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            header.add(title, BorderLayout.WEST);
            add(header, BorderLayout.NORTH);
            JPanel form = new JPanel(new GridLayout(2,2,8,8));
            form.setBorder(new TitledBorder(new LineBorder(accent,1,true), "Inicio de sesión"));
            form.add(new JLabel("Usuario"));
            form.add(user);
            form.add(new JLabel("Contraseña"));
            form.add(pass);
            JPanel actions = new JPanel(new GridLayout(1,2,8,8));
            stylePrimary(login); styleOutline(goRegister);
            actions.add(login);
            actions.add(goRegister);
            add(form, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);
            setSize(420, 220);
            setLocationRelativeTo(owner);
            login.addActionListener(e->{
                String u = user.getText().trim();
                String p = new String(pass.getPassword());
                if(UserStore.authenticate(u,p)){ acceptedUser=u; dispose(); }
                else JOptionPane.showMessageDialog(this, "Credenciales inválidas");
            });
            goRegister.addActionListener(e->{
                String u = RegisterDialog.prompt(this);
                if(u!=null && !u.isEmpty()){ acceptedUser=u; dispose(); }
            });
        }
        void stylePrimary(JButton b){ b.setBackground(new Color(22,163,74)); b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setBorder(new LineBorder(new Color(16,120,54),1,true)); }
        void styleOutline(JButton b){ b.setBackground(Color.WHITE); b.setForeground(new Color(22,163,74)); b.setFocusPainted(false); b.setBorder(new LineBorder(new Color(22,163,74),1,true)); }
        static String prompt(Frame owner){ LoginDialog d = new LoginDialog(owner); d.setVisible(true); return d.acceptedUser; }
    }
    static class RegisterDialog extends JDialog {
        Color accent = new Color(22,163,74);
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton regBtn = new JButton("Crear cuenta");
        JButton cancelBtn = new JButton("Cancelar");
        String acceptedUser;
        RegisterDialog(Window owner){
            super(owner, "Registro", ModalityType.APPLICATION_MODAL);
            setLayout(new BorderLayout());
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(accent);
            JLabel title = new JLabel("  Crear cuenta");
            title.setForeground(Color.WHITE);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            header.add(title, BorderLayout.WEST);
            add(header, BorderLayout.NORTH);
            JPanel form = new JPanel(new GridLayout(2,2,8,8));
            form.setBorder(new TitledBorder(new LineBorder(accent,1,true), "Registro"));
            form.add(new JLabel("Usuario")); form.add(user);
            form.add(new JLabel("Contraseña")); form.add(pass);
            JPanel actions = new JPanel(new GridLayout(1,2,8,8));
            stylePrimary(regBtn); styleOutline(cancelBtn);
            actions.add(regBtn); actions.add(cancelBtn);
            add(form, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);
            setSize(420, 220);
            setLocationRelativeTo(owner);
            regBtn.addActionListener(e->{
                String u = user.getText().trim();
                String p = new String(pass.getPassword());
                if(u.isEmpty()||p.isEmpty()){ JOptionPane.showMessageDialog(this, "Completa usuario y contraseña"); return; }
                boolean ok = UserStore.register(u,p);
                if(ok){ JOptionPane.showMessageDialog(this, "¡Bienvenido, " + u + "! Registro completado."); acceptedUser=u; dispose(); }
                else JOptionPane.showMessageDialog(this, "El usuario ya existe");
            });
            cancelBtn.addActionListener(e->{ dispose(); });
        }
        void stylePrimary(JButton b){ b.setBackground(new Color(22,163,74)); b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setBorder(new LineBorder(new Color(16,120,54),1,true)); }
        void styleOutline(JButton b){ b.setBackground(Color.WHITE); b.setForeground(new Color(22,163,74)); b.setFocusPainted(false); b.setBorder(new LineBorder(new Color(22,163,74),1,true)); }
        static String prompt(Window owner){ RegisterDialog d = new RegisterDialog(owner); d.setVisible(true); return d.acceptedUser; }
    }
    interface AiService {
        String summarize(String content, int level);
        List<String> keywords(String content, int n);
        List<String> exercises(String content, int n);
        String status();
    }
    static class SimpleAiService implements AiService {
        public String summarize(String content, int level) { return ""; }
        public List<String> keywords(String content, int n) { return new ArrayList<>(); }
        public List<String> exercises(String content, int n) { return new ArrayList<>(); }
        public String status() { return "Disabled"; }
    }
    static class NoAiService implements AiService {
        public String summarize(String content, int level) { return ""; }
        public List<String> keywords(String content, int n) { return new ArrayList<>(); }
        public List<String> exercises(String content, int n) { return new ArrayList<>(); }
        public String status() { return "No key"; }
    }
    static AiService ai = createAi();
    static AiService createAi(){
        String proxy = System.getenv("GROQ_PROXY_URL");
        if (proxy == null || proxy.trim().isEmpty()) proxy = System.getProperty("groq.proxy.url");
        if (proxy != null) proxy = proxy.trim();
        if (proxy != null && !proxy.isEmpty()) return new ProxyAiService(proxy);
        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.trim().isEmpty()) key = System.getProperty("groq.api.key");
        if (key == null || key.trim().isEmpty()) key = NoteStore.readAll("groq.key");
        if (key == null || key.trim().isEmpty()) key = NoteStore.readAll("keys/groq.key");
        if (key == null || key.trim().isEmpty()) key = NoteStore.readAll("../groq.key");
        if (key == null || key.trim().isEmpty()) key = NoteStore.readAll("../keys/groq.key");
        if (key == null || key.trim().isEmpty()) key = NoteStore.readAll("../../groq.key");
        if (key == null || key.trim().isEmpty()) key = NoteStore.readAll("../../keys/groq.key");
        if (key != null) key = key.trim();
        if (key != null && !key.isEmpty()) return new GroqAiService(key);
        return new NoAiService();
    }
    static class ProxyAiService implements AiService {
        final String baseUrl;
        volatile String lastError;
        ProxyAiService(String url){ this.baseUrl = url; }
        public String summarize(String content, int level){
            String range = level<=1?"60–90":"120–160"; if (level>=3) range = "200–240";
            String prompt = "Resume en español el texto.\nLongitud: " + range + " palabras, conciso y completo.\nEvita introducción y cierre; enfócate en ideas clave.\n\n" + content;
            return chat(prompt);
        }
        public java.util.List<String> keywords(String content, int n){
            String prompt = "Escribe exactamente " + n + " palabras clave en español, separadas por comas, sin explicaciones, del texto.\n\n" + content;
            String out = chat(prompt);
            java.util.List<String> res = new java.util.ArrayList<>();
            for (String p : out.split(",")) { String t = p.trim(); if (!t.isEmpty()) res.add(t); }
            return res;
        }
        public java.util.List<String> exercises(String content, int n){
            String prompt = "Genera exactamente " + n + " ejercicios en español basados en el texto.\nFormato: lista simple, una línea por ejercicio, sin introducciones ni títulos, sin soluciones.\n\n" + content;
            String out = chat(prompt);
            java.util.List<String> res = new java.util.ArrayList<>(); if (out == null) out = ""; res.add(out.trim()); return res;
        }
        public String status(){ return lastError==null?"OK":lastError; }
        String chat(String prompt){
            try {
                lastError = null;
                java.net.URL url = new java.net.URL(baseUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String body = "{" +
                        "\"prompt\":\"" + NoteStore.escape(prompt) + "\"," +
                        "\"temperature\":0.2,\"max_tokens\":800}";
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(30000);
                conn.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                int code = conn.getResponseCode();
                java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder(); String line; while ((line = br.readLine()) != null) sb.append(line); br.close();
                String json = sb.toString();
                if (code >= 400) { lastError = "HTTP " + code + ": " + json; return ""; }
                String content = extractContent(json);
                if (content == null || content.trim().isEmpty()) { lastError = "Empty response"; return ""; }
                return content.trim();
            } catch (Exception e) { lastError = e.getMessage(); return ""; }
        }
        String extractContent(String json){
            java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("\"content\"\s*:\s*\"(.*?)\"", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m1 = p1.matcher(json);
            if (m1.find()) return m1.group(1).replace("\\n","\n").replace("\\\"","\"");
            return null;
        }
    }
    static class GroqAiService implements AiService {
        final String apiKey;
        volatile String lastError;
        GroqAiService(String key){ this.apiKey = key; }
        public String summarize(String content, int level){
            String range = level<=1?"60–90":"120–160";
            if (level>=3) range = "200–240";
            String prompt = "Resume en español el texto.\n"+
                           "Longitud: " + range + " palabras, conciso y completo.\n"+
                           "Evita introducción y cierre; enfócate en ideas clave.\n\n" + content;
            return chat(prompt);
        }
        public List<String> keywords(String content, int n){
            String prompt = "Escribe exactamente " + n + " palabras clave en español, separadas por comas, sin explicaciones, del texto.\n\n" + content;
            String out = chat(prompt);
            List<String> res = new ArrayList<>();
            for (String p : out.split(",")) { String t = p.trim(); if (!t.isEmpty()) res.add(t); }
            return res;
        }
        public List<String> exercises(String content, int n){
            String prompt = "Genera exactamente " + n + " ejercicios en español basados en el texto.\n"+
                           "Formato: lista simple, una línea por ejercicio, sin introducciones ni títulos, sin soluciones.\n\n" + content;
            String out = chat(prompt);
            List<String> res = new ArrayList<>();
            if (out == null) out = "";
            res.add(out.trim());
            return res;
        }
        public String status(){ return lastError==null?"OK":lastError; }
        String chat(String prompt){
            try {
                lastError = null;
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String body = "{"+
                    "\"model\":\"llama-3.3-70b-versatile\","+
                    "\"temperature\":0.2,"+
                    "\"max_tokens\":800,"+
                    "\"messages\":[{"+
                        "\"role\":\"system\",\"content\":\"Responde en español.\"},{"+
                        "\"role\":\"user\",\"content\":\"" + NoteStore.escape(prompt) + "\"}]}";
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(30000);
                conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                int code = conn.getResponseCode();
                java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line; while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String json = sb.toString();
                if (code >= 400) { lastError = "HTTP " + code + ": " + json; return ""; }
                String content = extractContent(json);
                if (content == null || content.trim().isEmpty()) { lastError = "Empty response"; return ""; }
                return content.trim();
            } catch (Exception e) {
                lastError = e.getMessage();
                return "";
            }
        }
        String extractContent(String json){
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\\"choices\\\"\\s*:\\s*\\[\\s*\\{[\\s\\S]*?\\\"content\\\"\\s*:\\s*\\\"(.*?)\\\"", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                String val = m.group(1);
                return val.replace("\\n","\n").replace("\\\"","\"");
            }
            return null;
        }
    }
    static class StudyBuddyFrame extends JFrame {
        DefaultListModel<Note> model = new DefaultListModel<>();
        JList<Note> list = new JList<>(model);
        JTextField titleField = new JTextField();
        JTextArea contentField = new JTextArea();
        JTextArea output = new JTextArea();
        JLabel brand = new JLabel();
        JButton logoutBtn = new JButton("Cerrar sesión");
        JButton addBtn = new JButton("Agregar");
        JButton saveBtn = new JButton("Guardar");
        JButton delBtn = new JButton("Eliminar");
        JButton sumBtn = new JButton("Resumen");
        JButton kwBtn = new JButton("Claves");
        JButton exBtn = new JButton("Ejercicios");
        JLabel status = new JLabel();
        Color accent = new Color(22,163,74);
        Color danger = new Color(239,68,68);
        public StudyBuddyFrame() {
            setTitle("StudyBuddy – " + (currentUser==null?"Invitado":currentUser));
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setSize(1100, 720);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());
            Color bg = new Color(245,247,250);
            JPanel left = new JPanel(new BorderLayout(8,8));
            JPanel right = new JPanel(new BorderLayout(8,8));
            left.setBackground(bg);
            right.setBackground(bg);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane listScroll = new JScrollPane(list);
            listScroll.getViewport().setBackground(Color.WHITE);
            JLabel listTitle = new JLabel("Notas");
            listTitle.setFont(listTitle.getFont().deriveFont(Font.BOLD, 16f));
            left.add(listTitle, BorderLayout.NORTH);
            DefaultListModel<PublicStore.Pub> pubModel = new DefaultListModel<>();
            JList<PublicStore.Pub> pubList = new JList<>(pubModel);
            JScrollPane pubScroll = new JScrollPane(pubList);
            pubScroll.getViewport().setBackground(Color.WHITE);
            javax.swing.border.TitledBorder pl = BorderFactory.createTitledBorder(new LineBorder(accent,1,true), "Públicas");
            pl.setTitleColor(accent.darker());
            pubScroll.setBorder(pl);
            JPanel listsPanel = new JPanel(new GridLayout(2,1,8,8));
            listsPanel.add(listScroll);
            listsPanel.add(pubScroll);
            left.add(listsPanel, BorderLayout.CENTER);
            JPanel leftActions = new JPanel(new GridLayout(1,4,8,8));
            leftActions.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            JComboBox<String> sumSize = new JComboBox<>(new String[]{"Corto","Medio","Largo"});
            leftActions.add(sumBtn);
            leftActions.add(sumSize);
            leftActions.add(kwBtn);
            leftActions.add(new JLabel(""));
            JPanel leftActionsWrap = new JPanel(new GridLayout(2,1,8,8));
            JPanel topRow = new JPanel(new GridLayout(1,3,8,8));
            topRow.add(sumBtn);
            topRow.add(sumSize);
            topRow.add(kwBtn);
            leftActionsWrap.add(topRow);
            JPanel bottomRow = new JPanel(new GridLayout(1,3,8,8));
            bottomRow.add(exBtn);
            JSpinner exCount = new JSpinner(new javax.swing.SpinnerNumberModel(5,1,10,1));
            bottomRow.add(exCount);
            bottomRow.add(new JLabel(""));
            leftActionsWrap.add(bottomRow);
            left.add(leftActionsWrap, BorderLayout.SOUTH);
            JPanel form = new JPanel(new BorderLayout(8,8));
            JPanel topForm = new JPanel(new BorderLayout(8,8));
            topForm.add(new JLabel("Título"), BorderLayout.WEST);
            topForm.add(titleField, BorderLayout.CENTER);
            JCheckBox publicCheck = new JCheckBox("Pública");
            publicCheck.setOpaque(false);
            topForm.add(publicCheck, BorderLayout.EAST);
            form.add(topForm, BorderLayout.NORTH);
            contentField.setLineWrap(true);
            contentField.setWrapStyleWord(true);
            JScrollPane contentScroll = new JScrollPane(contentField);
            javax.swing.border.TitledBorder ct = BorderFactory.createTitledBorder(new LineBorder(accent,1,true), "Contenido");
            ct.setTitleColor(accent.darker());
            contentScroll.setBorder(ct);
            form.add(contentScroll, BorderLayout.CENTER);
            JPanel formActions = new JPanel(new GridLayout(1,4,8,8));
            formActions.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            JButton newBtn = new JButton("Nueva");
            formActions.add(newBtn);
            formActions.add(addBtn);
            formActions.add(saveBtn);
            formActions.add(delBtn);
            form.add(formActions, BorderLayout.SOUTH);
            JButton copyBtn = new JButton("Copiar");
            styleOutline(copyBtn);
            JButton clearBtn = new JButton("Limpiar");
            styleOutline(clearBtn);
            output.setEditable(false);
            output.setLineWrap(true);
            output.setWrapStyleWord(true);
            JScrollPane outScroll = new JScrollPane(output);
            outScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            javax.swing.border.TitledBorder ot = BorderFactory.createTitledBorder(new LineBorder(accent,1,true), "Resultados (IA)");
            ot.setTitleColor(accent.darker());
            outScroll.setBorder(ot);
            JPanel outToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8));
            outToolbar.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
            outToolbar.add(copyBtn);
            outToolbar.add(clearBtn);
            JPanel outPanel = new JPanel(new BorderLayout());
            outPanel.add(outToolbar, BorderLayout.NORTH);
            outPanel.add(outScroll, BorderLayout.CENTER);
            JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, form, outPanel);
            rightSplit.setDividerLocation(480);
            rightSplit.setContinuousLayout(true);
            right.add(rightSplit, BorderLayout.CENTER);
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
            split.setDividerLocation(360);
            split.setContinuousLayout(true);
            JPanel center = new JPanel(new BorderLayout());
            center.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            center.add(split, BorderLayout.CENTER);
            JPanel header = new JPanel(new BorderLayout()){
                protected void paintComponent(Graphics g){
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    GradientPaint gp = new GradientPaint(0,0,accent, getWidth(),0, accent.darker());
                    g2.setPaint(gp);
                    g2.fillRect(0,0,getWidth(),getHeight());
                }
            };
            header.setOpaque(true);
            brand.setText("  StudyBuddy – " + (currentUser==null?"Invitado":currentUser));
            brand.setForeground(Color.WHITE);
            brand.setFont(brand.getFont().deriveFont(Font.BOLD, 18f));
            header.add(brand, BorderLayout.WEST);
            JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8));
            headerRight.setOpaque(false);
            styleOutline(logoutBtn);
            JButton testIaBtn = new JButton("Probar IA");
            styleOutline(testIaBtn);
            headerRight.add(testIaBtn);
            headerRight.add(logoutBtn);
            header.add(headerRight, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
            status.setText("Notas: " + notes.size() + " • IA: " + (ai instanceof GroqAiService ? "activo" : "inactivo"));
            status.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
            add(status, BorderLayout.SOUTH);
            applyTheme();
            for (Note n : notes) model.addElement(n);
            for (PublicStore.Pub p : PublicStore.load()) pubModel.addElement(p);
            list.setSelectionBackground(accent);
            list.setSelectionForeground(Color.WHITE);
            list.setCellRenderer(new DefaultListCellRenderer(){
                public Component getListCellRendererComponent(JList<?> l,Object value,int index,boolean isSelected,boolean cellHasFocus){
                    JLabel c=(JLabel)super.getListCellRendererComponent(l,((Note)value).title,index,isSelected,cellHasFocus);
                    Note n=(Note)value;
                    int wc = n.content == null ? 0 : Math.max(0, n.content.trim().split("\\s+").length);
                    c.setText(n.title + " • " + wc + " palabras");
                    c.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));
                    return c;
                }
            });
            list.addListSelectionListener(e -> {
                int i = list.getSelectedIndex();
                if (i >= 0) {
                    Note n = model.get(i);
                    titleField.setText(n.title);
                    contentField.setText(n.content);
                    publicCheck.setSelected(n.isPublic);
                    contentField.setEditable(true);
                    pubList.clearSelection();
                }
            });
            pubList.setSelectionBackground(accent);
            pubList.setSelectionForeground(Color.WHITE);
            pubList.setCellRenderer(new DefaultListCellRenderer(){
                public Component getListCellRendererComponent(JList<?> l,Object value,int index,boolean isSelected,boolean cellHasFocus){
                    PublicStore.Pub p = (PublicStore.Pub) value;
                    JLabel c=(JLabel)super.getListCellRendererComponent(l, p==null?"":(p.user+" • "+p.title), index, isSelected, cellHasFocus);
                    c.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));
                    return c;
                }
            });
            pubList.addListSelectionListener(e -> {
                int i = pubList.getSelectedIndex();
                if (i >= 0) {
                    PublicStore.Pub p = pubModel.get(i);
                    titleField.setText(p.title);
                    contentField.setText(p.content);
                    publicCheck.setSelected(true);
                    contentField.setEditable(false);
                    list.clearSelection();
                }
            });
            newBtn.addActionListener(e -> {
                list.clearSelection();
                titleField.setText("");
                contentField.setText("");
                publicCheck.setSelected(false);
                output.setText("Nueva nota");
            });
            addBtn.addActionListener(e -> {
                String t = titleField.getText().trim();
                String c = contentField.getText().trim();
                if (t.isEmpty() || c.isEmpty()) { output.setText("Completa título y contenido"); return; }
                Note n = new Note(t, c, publicCheck.isSelected());
                notes.add(n);
                model.addElement(n);
                NoteStore.save(notes);
                PublicStore.replaceForUser(currentUser, notes);
                output.setText("Nota guardada");
                updateStatus();
            });
            saveBtn.addActionListener(e -> {
                int i = list.getSelectedIndex();
                if (i < 0) { output.setText("Selecciona una nota"); return; }
                String t = titleField.getText().trim();
                String c = contentField.getText().trim();
                if (t.isEmpty() || c.isEmpty()) { output.setText("Completa título y contenido"); return; }
                Note n = model.get(i);
                n.title = t;
                n.content = c;
                n.isPublic = publicCheck.isSelected();
                list.repaint();
                NoteStore.save(notes);
                PublicStore.replaceForUser(currentUser, notes);
                output.setText("Cambios guardados");
            });
            delBtn.addActionListener(e -> {
                int i = list.getSelectedIndex();
                if (i < 0) { output.setText("Selecciona una nota"); return; }
                Note n = model.get(i);
                notes.remove(n);
                model.remove(i);
                NoteStore.save(notes);
                PublicStore.replaceForUser(currentUser, notes);
                titleField.setText("");
                contentField.setText("");
                output.setText("Nota eliminada");
                updateStatus();
            });
            copyBtn.addActionListener(e -> {
                String t = output.getText();
                if (t != null && !t.trim().isEmpty()) {
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(t), null);
                    status.setText("Copiado al portapapeles • IA: " + (ai instanceof GroqAiService ? "activo" : "inactivo"));
                }
            });
            clearBtn.addActionListener(e -> {
                output.setText("");
            });
            sumBtn.addActionListener(e -> {
                String all = contentField.getText()==null?"":contentField.getText().trim();
                if (all.isEmpty()) all = NoteStore.corpusCombined();
                if (all == null || all.trim().isEmpty()) all = corpusLive();
                status.setText((all==null||all.isEmpty())?"Fuente: vacío":"Fuente: texto ("+all.length()+")");
                int sel = sumSize.getSelectedIndex();
                int level = sel<0?1:(sel==0?1:(sel==1?2:3));
                String s = ai.summarize(all, level);
                if (s == null || s.trim().isEmpty()) {
                    output.setText("IA no disponible. Verifica tu clave y conexión.");
                } else {
                    output.setText("Resumen (IA):\n\n" + s);
                }
            });
            kwBtn.addActionListener(e -> {
                String all = contentField.getText()==null?"":contentField.getText().trim();
                if (all.isEmpty()) all = NoteStore.corpusCombined();
                if (all == null || all.trim().isEmpty()) all = corpusLive();
                status.setText((all==null||all.isEmpty())?"Fuente: vacío":"Fuente: texto ("+all.length()+")");
                java.util.List<String> ks = ai.keywords(all, 8);
                if (ks == null || ks.isEmpty()) {
                    output.setText("IA no disponible. Verifica tu clave y conexión.");
                } else {
                    output.setText("Palabras clave (IA):\n\n" + String.join(", ", ks));
                }
            });
            exBtn.addActionListener(e -> {
                String all = contentField.getText()==null?"":contentField.getText().trim();
                if (all.isEmpty()) all = NoteStore.corpusCombined();
                if (all == null || all.trim().isEmpty()) all = corpusLive();
                status.setText((all==null||all.isEmpty())?"Fuente: vacío":"Fuente: texto ("+all.length()+")");
                int cnt = ((Integer)exCount.getValue()).intValue();
                java.util.List<String> ex = ai.exercises(all, cnt);
                if (ex == null || ex.isEmpty()) {
                    output.setText("IA no disponible. Verifica tu clave y conexión.");
                } else {
                    String text;
                    if (ex.size() == 1) {
                        text = ex.get(0);
                    } else {
                        text = String.join("\n", ex);
                    }
                    output.setText("Ejercicios (IA):\n\n" + text);
                }
            });
            // Eliminado botón de diálogo "Públicas"; ahora están integradas en la barra izquierda
            testIaBtn.addActionListener(e -> {
                java.util.List<String> ks = ai.keywords("prueba", 1);
                String st = ai.status();
                if ("OK".equals(st) && ks != null) {
                    output.setText("IA: OK");
                } else {
                    output.setText("IA error: " + st);
                }
            });
            logoutBtn.addActionListener(e -> {
                dispose();
                String u = LoginDialog.prompt(null);
                if (u != null && !u.isEmpty()) {
                    currentUser = u;
                    notes = NoteStore.load();
                    new StudyBuddyFrame().setVisible(true);
                }
            });
        }
        String corpusLive(){
            StringBuilder sb = new StringBuilder();
            for (Note n : notes) {
                if (n.content != null) {
                    String t = n.content.trim();
                    if (!t.isEmpty()) { if (sb.length()>0) sb.append("\n\n"); sb.append(t); }
                }
            }
            String curr = contentField.getText();
            if (curr != null) {
                String t = curr.trim();
                if (!t.isEmpty()) { if (sb.length()>0) sb.append("\n\n"); sb.append(t); }
            }
            java.util.List<PublicStore.Pub> pubs = PublicStore.load();
            for (PublicStore.Pub p : pubs) {
                if (p.content != null) {
                    String t = p.content.trim();
                    if (!t.isEmpty()) { if (sb.length()>0) sb.append("\n\n"); sb.append(t); }
                }
            }
            return sb.toString();
        }
        void applyTheme(){
            Font base = new Font("Segoe UI", Font.PLAIN, 16);
            setFont(base);
            list.setFont(base);
            titleField.setFont(base);
            contentField.setFont(base);
            output.setFont(new Font("Consolas", Font.PLAIN, 14));
            stylePrimary(addBtn);
            stylePrimary(saveBtn);
            styleDanger(delBtn);
            styleOutline(sumBtn);
            styleOutline(kwBtn);
            styleOutline(exBtn);
        }
        void stylePrimary(JButton b){
            b.setBackground(accent);
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            b.setBorder(new LineBorder(accent.darker(), 2, true));
            b.setMargin(new Insets(10,18,10,18));
        }
        void styleOutline(JButton b){
            b.setBackground(Color.WHITE);
            b.setForeground(accent.darker());
            b.setFocusPainted(false);
            b.setBorder(new LineBorder(accent, 2, true));
            b.setMargin(new Insets(10,18,10,18));
        }
        void styleDanger(JButton b){
            b.setBackground(danger);
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            b.setBorder(new LineBorder(danger.darker(), 2, true));
            b.setMargin(new Insets(10,18,10,18));
        }
        void updateStatus(){
            status.setText("Notas: " + model.getSize() + " • IA: " + (ai instanceof GroqAiService ? "activo" : "inactivo"));
        }
    }
}
