public class A17 {
    public static void run() { new A17().go(5); }
    public static void main(String[] a) { run(); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    String top = "TOP";
    String m() { return "m"; }
    void go(int k) {
        String s = "s";
        System.out.println(correr(new F() {
            String mid = "MID";
            String h() { return "h" + k; }
            public String f() {
                int j = 2;
                return correr(new F() {
                    String low = "LOW";
                    public String f() {
                        return correr(new F() {
                            public String f() { return top + m() + mid + h() + low + s + k + j + A17.this.top.length(); }
                        });
                    }
                });
            }
        }));
    }
}
