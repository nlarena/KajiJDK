public class A14 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String s = "s";
        class Loc { String g() { return correr(new F() { public String f() { return "loc" + s; } }); } }
        System.out.println(new Loc().g());
    }
}
