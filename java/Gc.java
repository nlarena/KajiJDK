// Disparador EXPLÍCITO: System.gc(). Se descarta un Animal y se pide una colección;
// la VM la atiende en el próximo safepoint (no inline). En el panel de salida del
// visor aparece una línea `[gc] Explicit: …`.
class Gc {
    static int run() {
        Animal a = new Animal();
        a = new Animal();   // el 1er Animal queda sin dueño → basura
        System.gc();        // pedido explícito → se barre en el próximo safepoint
        return 0;
    }
}
