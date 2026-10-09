public class A16 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        long L = 1234567890123L; double d = 2.5; int i = 3;
        System.out.println(correr(new F() {
            String r;
            { long m = 7L; r = correr(new F() { public String f() { return "" + L + "/" + d + "/" + i + "/" + m; } }); }
            public String f() { return r; }
        }));
    }
}
