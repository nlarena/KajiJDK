public class Outer {
    protected class Inner {
        final Object a;
        Inner(Object a) { this.a = a; }
    }
    protected Inner make(Object a) { return new Inner(a); }
}
