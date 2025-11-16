package studybuddy;
public class StudyBuddyTests {
  static int pass=0, fail=0;
  static void check(String name, boolean cond){ if(cond){ pass++; } else { fail++; System.out.println("FAIL: "+name); } }
  public static void main(String[] args){
    testHashAdmin();
    testNotesRoundtrip();
    testSummarize();
    testKeywords();
    System.out.println("Passed: "+pass+", Failed: "+fail);
    if(fail>0) System.exit(1);
  }
  static void testHashAdmin(){
    String h = StudyBuddy.UserStore.hash("admin");
    check("hash-admin", h.equals("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"));
  }
  static void testNotesRoundtrip(){
    java.util.List<StudyBuddy.Note> list = new java.util.ArrayList<>();
    list.add(new StudyBuddy.Note("T1","C1",true));
    list.add(new StudyBuddy.Note("T2","C2",false));
    String json = StudyBuddy.NoteStore.toNotesJson(list);
    java.util.List<StudyBuddy.Note> parsed = StudyBuddy.NoteStore.parseNotes(json);
    boolean ok = parsed.size()==2 && parsed.get(0).isPublic && !parsed.get(1).isPublic;
    check("notes-roundtrip", ok);
  }
  static void testSummarize(){
    String s = StudyBuddy.summarize("Java es un lenguaje. Sirve para múltiples plataformas. Tiene POO.", 2);
    check("summarize-nonempty", s!=null && s.trim().length()>0);
  }
  static void testKeywords(){
    java.util.List<String> ks = StudyBuddy.keywords("Java es un lenguaje de programación. Programación en Java.", 3);
    boolean ok = ks.size()>=1;
    check("keywords-basic", ok);
  }
}