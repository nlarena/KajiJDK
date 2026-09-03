public class K9 { public static int run(Object o) {
    if (!(o instanceof String)) { return 1; }
    String s = (String) o; return s.length() == 1 ? -1 : 1; }
  public static int run() { return run((Object) "a"); }
  public static void main(String[] a){ System.out.println(run()); } }
