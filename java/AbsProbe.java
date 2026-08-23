/* Repro de COMPILER_FINDINGS #230: invokevirtual a un metodo resuelto `abstract`.
   El A/B del hallazgo: mismo cuerpo, misma clase, mismo tipo estatico; lo unico que
   cambia es si el metodo sobreescribe un `abstract` declarado en la superclase. */
public class AbsProbe {
    static abstract class Base { abstract int n(); }
    static class Impl extends Base { int n() { return 7; } }

    /* Control: misma forma, pero la superclase declara el metodo CON cuerpo. */
    static class Concrete { int n() { return 3; } }
    static class Sub extends Concrete { int n() { return 7; } }

    /* Por el tipo abstracto: el caso del hallazgo. */
    public static int run() {
        Base b = new Impl();
        return b.n();
    }

    /* Control: por un supertipo concreto. */
    public static int viaConcrete() {
        Concrete c = new Sub();
        return c.n();
    }

    /* Control: por el tipo exacto. */
    public static int viaExact() {
        Impl i = new Impl();
        return i.n();
    }
}
