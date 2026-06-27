public class Inner {
    static class Nested {}
    class Member {}
    interface INested {}
    private static final class Priv {}
    void m() {
        Runnable r = new Runnable() { public void run() {} };
        class Local {}
        Local l = new Local();
    }
}
