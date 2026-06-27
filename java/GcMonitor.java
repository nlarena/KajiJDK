// GC-safe monitors: un monitor tomado debe sobrevivir a que el GC mueva su objeto-lock.
// Dentro de un synchronized(lock) forzamos System.gc(): el minor evacúa `lock` (vivo,
// referenciado por el local) a una dirección nueva. Si el mapa de monitores no siguiera
// el movimiento, el monitorexit del cierre del bloque tiraría IllegalMonitorStateException.
// Devuelve 5 sólo si el monitor siguió a su objeto reubicado.
public class GcMonitor {
    static int run() {
        Object lock = new Object();
        synchronized (lock) {
            System.gc(); // colecta: el minor mueve `lock` a otro offset
        }
        return 5; // alcanzado sólo si monitorexit encontró el monitor (re-keyed)
    }
}
