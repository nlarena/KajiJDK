// Reuso de hueco: ver un objeto nuevo GRABARSE en el espacio que el GC liberó.
//
// Guion en el visor (jvm-step):
//   1. Enter hasta pasar los DOS primeros `new Animal()` y la línea `b = a`
//      (ahí el 2º Animal queda sin dueño → basura).
//   2. Apretá `s` (barrer): el 2º Animal se libera → free list: 1 hueco · 12 bytes.
//   3. Seguí con Enter hasta ejecutar el TERCER `new Animal()`.
//      → `c` debería quedar en el MISMO offset que tenía el Animal liberado
//        (malloc reusó el hueco en vez de crecer; el heap no aumenta de tamaño).
class Reuse {
    static int run() {
        Animal a = new Animal();   // obj1 → vive (lo guarda `a`)
        Animal b = new Animal();   // obj2 → vive (lo guarda `b`)
        b = a;                     // obj2 queda SIN referencias → BASURA   (← barré acá con `s`)
        Animal c = new Animal();   // obj3 → REUSA el hueco que dejó obj2
        return c.legs;             // 4
    }
}
