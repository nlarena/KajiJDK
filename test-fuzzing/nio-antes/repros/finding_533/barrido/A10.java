public class A10 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String t = "afuera";
        System.out.println(correr(new F() {
            String usa() { return t; }
            public String f() { String t = "adentro"; return usa() + "/" + t; }
        }));
    }
}
