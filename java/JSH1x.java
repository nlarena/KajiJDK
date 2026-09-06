/** The test snippet: what JShell would compile out of what the user writes. */
public class JSH1x {
    public static int V = 7;
    public static String T = "hello";
    public static int f() { return 42; }
    public static String g() { return "echo"; }
    public static Object nothing() { return null; }
    public static int blowUp() { throw new IllegalStateException("on purpose"); }
}
