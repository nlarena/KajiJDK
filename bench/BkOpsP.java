// Los receptores **venenosos** de `BkMonoC`/`BkMegaC`: `BkOpA`..`BkOpD` con un `invokedynamic` en
// la rama muerta en vez de una suma.
//
// La secuencia ejecutada es la misma que la de los limpios —`getstatic; ifeq; iload_1; iconst_1;
// iadd; ireturn`— porque la rama no se toma nunca. Lo que cambia es lo que el **escaneo** ve, y por
// eso estos métodos no entran al subconjunto compilable mientras que sus gemelos sí. Ver `BkOps.java`.
final class BkOpPA implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + ("" + x).length();
        }
        return x + 1;
    }
}

final class BkOpPB implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + ("" + x).length();
        }
        return x + 1;
    }
}

final class BkOpPC implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + ("" + x).length();
        }
        return x + 1;
    }
}

final class BkOpPD implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + ("" + x).length();
        }
        return x + 1;
    }
}
