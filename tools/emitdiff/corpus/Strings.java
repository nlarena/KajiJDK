public class Strings {
  public String cat(String a, int b) { return a + "=" + b; }
  public int sw(String s) { return switch (s) { case "a" -> 1; case "b" -> 2; default -> 0; }; }
}
