// Repro determinista del hueco de `RunningCtx::pending_exception`.
//
// El `equals` sintetizado de un record pasa por el bootstrap `ObjectMethods`, que compara
// cada componente llamando al `equals` del componente con `call_virtual`. Si ese `equals`
// LANZA, `call_java` vuelve con `None` y la excepción queda estacionada en
// `RunningCtx::pending_exception` — y el brazo `None => false` de `record_equals` la
// interpreta como "el componente no tiene equals" y sigue como si nada.
//
// Consecuencia: en Java nadie vio un throw, pero la VM quedó cargando una excepción que
// el PRÓXIMO opcode con `take_pending_throw()` (un `new`, un `getstatic`, un invoke) va a
// entregar en un lugar que no tiene nada que ver con ella.
//
// `java` real: el equals del componente propaga → 10 + 100 = 110.
// Nuestra VM con el bug: el equals devuelve false → 1, y la excepción reaparece en el
// `new` de más abajo → 1 + 1000 = 1001.
public class PeStale {
    record Box(Bad b) {}

    static class Bad {
        public boolean equals(Object o) {
            throw new RuntimeException("boom");
        }
    }

    public static int run() {
        Box b1 = new Box(new Bad());
        Box b2 = new Box(new Bad());
        int score = 0;
        try {
            b1.equals(b2);
            score += 1; // el `java` real nunca llega acá: el equals del componente propaga
        } catch (RuntimeException e) {
            score += 10; // el `java` real aterriza acá
        }
        // Si la VM estacionó la excepción en vez de lanzarla, sigue pendiente en este punto.
        try {
            new Object(); // `new` → take_pending_throw()
            score += 100; // no había nada pendiente
        } catch (RuntimeException e) {
            score += 1000; // la excepción tragada reapareció ACÁ
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
