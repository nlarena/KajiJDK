// try-with-resources (JLS §14.20.3): baja a try/finally con close() y la excepción del close
// suprimida en la primaria (addSuppressed). Debe emitir bytecode VERIFICABLE.
public class TryRes {
    static class R implements AutoCloseable {
        int v;
        public void close() { v = -1; }
    }

    // Cuerpo que retorna a traves del finally: el recurso se cierra antes de devolver.
    static int use(int x) {
        try (R r = new R()) {
            r.v = x;
            return r.v + 1;
        }
    }

    // Cuerpo que lanza: el recurso igual se cierra y la excepcion se propaga como primaria.
    static int useThrowing(int x) {
        try (R r = new R()) {
            r.v = x;
            if (x < 0) throw new RuntimeException("neg");
            return r.v;
        }
    }
}
