public class Sub extends Base {
    public class SubInner extends Inner {
    }
    public Inner armar() { return new SubInner(); }
}
