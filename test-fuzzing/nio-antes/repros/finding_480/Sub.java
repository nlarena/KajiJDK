public class Sub extends Outer {
    protected class SubInner extends Inner {
        SubInner(Object a) { super(a); }
    }
    protected Inner make(Object a) { return new SubInner(a); }
}
