// GC mark-only — ver un objeto convertirse en BASURA.
//
// Cómo probarlo en el visor (jvm-step):
//   · Enter            = ejecutar un opcode
//   · espacio + Enter  = correr el marcado (mirá el contador `N vivos · M basura`
//                        en la cabecera del panel del heap, a la derecha)
//
// Guion sugerido:
//   1. Apretá Enter hasta pasar las DOS líneas `new Animal()` (los dos objetos ya
//      creados y guardados en `live` y `drop`). Apretá espacio+Enter:
//         → ... vivos · 0 basura   (los dos Animals están referenciados)
//   2. Seguí con Enter hasta pasar `drop = live` (un aload + astore). Apretá
//      espacio+Enter otra vez:
//         → ... vivos · 1 basura   (¡el 2º Animal quedó sin dueño → inalcanzable!)
class Garbage {
    static int run() {
        Animal live = new Animal();   // (1) Animal #1, referenciado por `live`  → VIVO
        Animal drop = new Animal();   // (2) Animal #2, referenciado por `drop`  → VIVO
        drop = live;                  // (3) `drop` apunta ahora al #1; el #2 queda SIN referencias → BASURA
        return live.legs;             // 4
    }
}
