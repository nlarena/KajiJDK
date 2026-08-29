// Diferencial de emisión de `synchronized` (§14.19): la referencia se evalúa una vez, se `dup`-lica y
// se guarda en un local sintético (`aload e; dup; astore lock; monitorenter`), del que salen todos los
// `monitorexit` —en la salida normal y en el handler catch-all que re-lanza—. Formas de salida normal
// (sin `return`/`break` adentro), que es donde javac y KajiJDK coinciden byte-a-byte; la fidelidad de
// las salidas abruptas se cubre en los tests de `codegen`.
public class Sync {
    static int r;
    Object lock = new Object();

    // synchronized estático sobre un parámetro.
    static void s(Object o) {
        synchronized (o) {
            r = 1;
        }
    }

    // synchronized de instancia sobre un campo (releído a un local para no soltar otro monitor).
    void inc() {
        synchronized (lock) {
            r = r + 1;
        }
    }

    // synchronized anidados: un local de monitor por nivel, handlers de adentro hacia afuera.
    static void two(Object a, Object b) {
        synchronized (a) {
            synchronized (b) {
                r = 2;
            }
        }
    }
}
