public class KB { public static int run(Object o) {
    if (!(o instanceof String)) { return 1; }
    String s = (String) o; return s.length(); }
  public static int run() { return run((Object) "a") == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
