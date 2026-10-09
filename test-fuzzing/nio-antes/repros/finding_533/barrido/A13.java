public class A13 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    static F e = new F() { public String f() { return correr(new F() { public String f() { return "x"; } }); } };
    public static void main(String[] a) { System.out.println(e.f()); }
}
