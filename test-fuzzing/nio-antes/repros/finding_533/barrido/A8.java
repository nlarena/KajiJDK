public class A8 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String s = "s";
        System.out.println(correr(new F() {
            public String f() {
                String t = "t";
                class In { String g() { return s + t; } }
                return new In().g();
            }
        }));
        class Loc {
            String g() {
                String u = "u";
                return correr(new F() { public String f() { return s + u; } });
            }
        }
        System.out.println(new Loc().g());
    }
}
