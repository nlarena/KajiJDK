public class B2 { public static int run() {
    String s = "abc";
    String t = s.concat("d");        // invokevirtual real sobre String
    Object[] o = new Object[1];
    o[0] = t;
    return t.length() == 4 ? -1 : 0; }
  public static void main(String[] a){System.out.println(run());} }
