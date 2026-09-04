public class EsProbe2 {
    enum C { A, B }
    static class Box<E extends Enum> { }
    static class Box2<E extends Enum<E>> { }
    static <E extends Enum> Box<E> mk(E e) { return new Box<E>(); }
    static <E extends Enum<E>> Box2<E> mk2(E e) { return new Box2<E>(); }
    public static void main(String[] x) {
        Box<C> b = mk(C.A);
        Box2<C> b2 = mk2(C.A);
        System.out.println("ok");
    }
}
