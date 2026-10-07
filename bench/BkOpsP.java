// Los receptores **venenosos** de `BkMonoC`/`BkMegaC`: `BkOpA`..`BkOpD` con un `multianewarray` de
// dos dimensiones en la rama muerta en vez de una suma.
//
// La secuencia ejecutada es la misma que la de los limpios —`getstatic; ifeq; iload_1; iconst_1;
// iadd; ireturn`— porque la rama no se toma nunca. Lo que cambia es lo que el **escaneo** ve, y por
// eso estos métodos no entran al subconjunto compilable mientras que sus gemelos sí.
//
// El veneno es el mismo que el de `BkArithC`, y ahí está escrito por qué dejó de ser un
// `invokedynamic`: la negativa de un sitio frío ya no refuta el método. Ver `BkOps.java` para los
// gemelos limpios.
final class BkOpPA implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + new int[2][2].length;
        }
        return x + 1;
    }
}

final class BkOpPB implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + new int[2][2].length;
        }
        return x + 1;
    }
}

final class BkOpPC implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + new int[2][2].length;
        }
        return x + 1;
    }
}

final class BkOpPD implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + new int[2][2].length;
        }
        return x + 1;
    }
}
