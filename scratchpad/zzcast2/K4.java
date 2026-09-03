public class K4 { public static int run() { Object o = "a"; String s = (String) o; return s.length() == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
