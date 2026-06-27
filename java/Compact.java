// Mark-compact: ver un objeto vivo DESLIZARSE al lugar de la basura.
//
// Se asignan 3 Animals (A, B, C). Se descarta el del MEDIO (B). Al compactar,
// C se desliza hacia abajo a tapar el hueco de B → el heap queda contiguo y la
// referencia de `c` se reescribe al nuevo offset.
//
// Guion en el visor (jvm-step):
//   1. Enter hasta pasar `b = c` (el 2º Animal queda sin dueño → basura del medio).
//   2. Apretá `c` (compactar): C se mueve al hueco de B; mirá
//        · el header: `compactado: 1 movidos · 12 B recuperados`
//        · `[2] = ref@…` (c) cambia a un offset menor
//        · el heap se achica (used baja)
class Compact {
    static int run() {
        Animal a = new Animal();   // A (1º) → vive
        Animal b = new Animal();   // B (2º, el del MEDIO) → quedará basura
        Animal c = new Animal();   // C (3º) → vive
        b = c;                     // B sin referencias → BASURA en el medio
        return c.legs;             // 4 — C se lee en su nuevo lugar tras compactar
    }
}
