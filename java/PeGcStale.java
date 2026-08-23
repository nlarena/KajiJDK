// Segunda mitad del repro de `RunningCtx::pending_exception`: además de entregarse en el
// lugar equivocado, la excepción estacionada es **invisible para el colector**.
//
// `gc::roots` recorre `threads[*].frames` (+ mirrors, thread objects y la caché de condy) y
// `Exec::parked` sincroniza sólo `frames`. Una excepción parkeada en `pending_exception` no
// está en ninguno de esos lugares, así que una colección que mueva la deja apuntando a
// basura — o directamente la recolecta, porque para el colector está muerta.
//
// El truco para meter un GC en la ventana: `newarray` y `arraylength` NO llaman a
// `take_pending_throw()` (sólo lo hacen `new`, `getstatic`, `putstatic`, `invokestatic` e
// `invokevirtual`), así que se puede correr una tormenta de alocación con la excepción
// colgada. Recién después un `new` la reclama, ya podrida.
//
// `java` real: 110 — el equals del componente propaga y nunca hay nada estacionado.
// Nuestra VM sin corrupción entregaría RuntimeException → 100000 + 16*100 + 'R'(82) = 101682.
// Cualquier otro número es la clase equivocada; un panic es el offset ya inservible.
public class PeGcStale {
    record Box(Bad b) {}

    static class Bad {
        public boolean equals(Object o) {
            throw new RuntimeException("boom");
        }
    }

    public static int run() {
        Box b1 = new Box(new Bad());
        Box b2 = new Box(new Bad());
        try {
            b1.equals(b2);
        } catch (RuntimeException e) {
            return 110; // el `java` real sale por acá y no ejecuta nada de lo de abajo
        }

        // Tormenta de alocación con la excepción estacionada: el minor GC la mueve o la
        // recolecta sin que nadie remapee `pending_exception`.
        int churn = 0;
        for (int i = 0; i < 60000; i++) {
            int[] junk = new int[16];
            churn += junk.length;
        }

        try {
            new Object(); // take_pending_throw() entrega el offset ya stale
            return 1 + (churn - churn); // no había nada pendiente
        } catch (Throwable ex) {
            // Fingerprint de la clase realmente entregada: largo*100 + primer carácter.
            String n = ex.getClass().getSimpleName();
            return 100000 + n.length() * 100 + n.charAt(0);
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
