/** Base con una clase interna que usa su instancia envolvente. */
class Base {
    int valor = 7;
    class Inner {
        int leer() { return Base.this.valor; }
    }
}

/** Sub hereda de Base; su interna hereda de la interna de Base. */
class Sub extends Base {
    class SubInner extends Inner {
    }
    Inner armar() { return new SubInner(); }
}

public class Nest {
    public static int run() {
        Sub s = new Sub();
        if (s.armar().leer() != 7) { return 1; }
        return -1;
    }
    public static void main(String[] a) { System.out.println(run()); }
}
