public class A6 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String s1 = "1";
        System.out.println(correr(new F() {
            public String f() {
                String s2 = "2";
                return correr(new F() {
                    public String f() {
                        String s3 = "3";
                        return correr(new F() {
                            final String k = s1 + s2 + s3;
                            public String f() { return k + s1 + s2 + s3 + getClass().getName(); }
                        });
                    }
                });
            }
        }));
    }
}
