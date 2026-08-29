// A6 loose end: a static initializer that throws. First active use of ClinitBoom triggers its <clinit>,
// which throws a RuntimeException. Per JVMS §5.5 the VM must (a) propagate the failure to the
// triggering code, (b) wrap the non-Error in ExceptionInInitializerError, and (c) leave ClinitBoom
// erroneous so a *second* active use throws NoClassDefFoundError. Score: 3 (wrapped) + 5 (NCDFE on
// re-use) = 8. Before the fix the failure was swallowed and the read returned 0. Deterministic →
// green ≡ os-gil ≡ os = 8.
// El nombre del auxiliar lleva el prefijo del probe A PROPOSITO: `java/` es un paquete por
// defecto plano, asi que dos fuentes que declaren un `Boom` escriben el MISMO `Boom.class` y
// gana el ultimo que se compilo. Habia tres (#273).
public class ClinitProbe {
    static int run() {
        int score = 0;
        try {
            int x = ClinitBoom.VALUE; // triggers ClinitBoom.<clinit> → throws
            score += x; // unreached
        } catch (ExceptionInInitializerError e) {
            score += 3; // (a)+(b): failure propagated, wrapped as ExceptionInInitializerError
        }
        try {
            int y = ClinitBoom.VALUE; // second use of the now-erroneous class
            score += y; // unreached
        } catch (NoClassDefFoundError e) {
            score += 5; // (c): erroneous class → NoClassDefFoundError
        }
        return score; // 8
    }
}

class ClinitBoom {
    static final int VALUE = compute();

    static int compute() {
        if (VALUE >= 0) { // VALUE is still 0 (default) while <clinit> runs → always true
            throw new RuntimeException("init boom");
        }
        return 1;
    }
}
