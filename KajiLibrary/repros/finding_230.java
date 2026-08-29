// Finding #230 — VM: `invokevirtual` sobre un metodo resuelto `abstract` no despachaba.
//
// El A/B minimo que lo aisla: mismo objeto, mismo cuerpo, mismo tipo estatico. Lo unico que
// cambia entre `run()` y `porTipoConcreto()` es si el metodo llamado sobreescribe un `abstract`
// declarado en la superclase. Antes del fix, `build_vtable` hacia
//
//     let Some(m) = resolve_method(..) else { continue };
//
// y `resolve_method` devolvia None para un miembro sin `Code`. Una declaracion `abstract` nunca
// recibia slot, y como el call site lee el slot del tipo ESTATICO, llamar por el tipo abstracto
// daba NoSuchMethodError (o el panic de `field_offset`), mientras que por el tipo exacto andaba.
//
// Esperado: run() = 25, porTipoConcreto() = 25, porSupertipoConcreto() = 7, plantilla() = 50.
public class finding_230 {

    static abstract class Forma {
        abstract int area();

        // Metodo plantilla: llama al abstracto desde la propia superclase.
        int doble() {
            return area() * 2;
        }
    }

    static class Cuadrado extends Forma {
        int lado;

        Cuadrado(int l) {
            lado = l;
        }

        int area() {
            return lado * lado;
        }
    }

    // Una superclase CONCRETA con el mismo metodo: el caso que siempre anduvo, para contrastar.
    static class Base {
        int area() {
            return 7;
        }
    }

    static class Hija extends Base {
        int area() {
            return 7;
        }
    }

    // EL CASO: tipo estatico abstracto, metodo resuelto sobre la declaracion `abstract`.
    public static int run() {
        Forma f = new Cuadrado(5);
        return f.area();
    }

    // Mismo objeto por su tipo exacto: andaba antes del fix.
    public static int porTipoConcreto() {
        Cuadrado c = new Cuadrado(5);
        return c.area();
    }

    // Supertipo concreto: tambien andaba.
    public static int porSupertipoConcreto() {
        Base b = new Hija();
        return b.area();
    }

    // El abstracto invocado desde la superclase misma (metodo plantilla).
    public static int plantilla() {
        Forma f = new Cuadrado(5);
        return f.doble();
    }

    // ---- la forma de java.nio: abstracto de DOS niveles ----
    // Buffer (abstract) <- ByteBuffer (abstract, redeclara) <- HeapByteBuffer (concreta).
    // El slot lo tiene que tomar la declaracion mas alta, y el override aterrizar ahi.

    static abstract class Buffer {
        abstract int capacidad();
    }

    static abstract class BufferDeBytes extends Buffer {
        // Re-declara el abstracto: el caso que rompia el calculo de slots.
        abstract int capacidad();
    }

    static class BufferEnMemoria extends BufferDeBytes {
        int cap;

        BufferEnMemoria(int c) {
            cap = c;
        }

        int capacidad() {
            return cap;
        }
    }

    // Por el tipo abstracto de MAS ARRIBA, saltando el intermedio.
    public static int dosNivelesPorRaiz() {
        Buffer b = new BufferEnMemoria(9);
        return b.capacidad();
    }

    // Por el abstracto intermedio, que re-declara.
    public static int dosNivelesPorMedio() {
        BufferDeBytes b = new BufferEnMemoria(9);
        return b.capacidad();
    }

    // ---- un abstracto que implementa una interfaz y NO implementa su metodo ----
    // La concreta de abajo lo provee; la llamada va por el tipo abstracto.

    interface Medible {
        int medida();
    }

    static abstract class Figura implements Medible {
        // No implementa medida(): queda abstracto heredado de la interfaz.
        int porTres() {
            return medida() * 3;
        }
    }

    static class Circulo extends Figura {
        public int medida() {
            return 4;
        }
    }

    public static int abstractoConInterfaz() {
        Figura f = new Circulo();
        return f.porTres();
    }
}
