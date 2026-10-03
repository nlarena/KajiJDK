public class A12 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    static String dos(F a, F b) { return a.f() + "," + b.f(); }
    public static void main(String[] a) {
        String s = "s";
        String u = "u";
        System.out.println(dos(
            new F() { public String f() { String t = "t"; return correr(new F() { public String f() { return s + t; } }); } },
            new F() { public String f() { return correr(new F() { public String f() { return u; } }); } }));
    }
}
