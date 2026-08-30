public class AbsChk {
    abstract static class Base { public abstract int f(); }
    // NO implementa f(): el JDK lo rechaza con "AbsChk.Sub is not abstract and does not
    // override abstract method f() in AbsChk.Base"
    static class Sub extends Base { }

    interface I { int g(); }
    // Idem por interfaz: este SI lo detecta nuestro javac.
    // static class SubI implements I { }

    public static int run() { return new Sub().f(); }
}
