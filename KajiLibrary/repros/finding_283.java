// Repro de #283 - un local declarado DENTRO de un `synchronized` se llevaba el mismo slot que
// el objeto del monitor. Compilaba sin quejas y reventaba al salir del bloque.
//
//   javac --emit -cp KajiLibrary KajiLibrary\repros\finding_283.java
//   run-headless KajiLibrary\repros\finding_283.class conLocal      -> 7
//
// OJO CON QUE JAVAC: este archivo NO reproduce nada contra ningun binario actual. El defecto
// vivio solo entre el cambio de emision de `synchronized` y su arreglo, y en esa ventana daba:
//
//   panic en bytecode_interpreter.rs:1641
//   "monitorexit: expected an object reference on the stack"
//
// El snapshot congelado ANTERIOR era previo al cambio, asi que tampoco fallaba; el actual ya
// trae el arreglo. Sirve como REGRESION: si el reparto de slots de la atribucion y la reserva
// del codegen se vuelven a desincronizar, este archivo vuelve a explotar.
//
// El bytecode lo decia todo (metodo `conLocal`, antes del arreglo):
//
//     4: dup
//     5: astore_1        <-- la referencia bloqueada, al slot 1
//     6: monitorenter
//    11: istore_1        <-- el local `copia`, AL MISMO SLOT 1: pisa la referencia
//    12: iload_1
//    13: aload_1         <-- carga un int
//    14: monitorexit     <-- y explota
//
// Causa: el codegen reserva dos slots por nivel de `synchronized` (el objeto del monitor y el
// aparcadero de excepcion del handler) justo encima de los parametros, y da por hecho que los
// locales del cuerpo empiezan arriba de esa region. Pero quien reparte los slots es la
// ATRIBUCION, que numeraba desde el primer libre tras los parametros sin saber nada de la
// reserva. Arreglado con `attrib_body`, que hace la misma reserva antes de recorrer el cuerpo.
//
// Por que no lo vio nadie: hace falta un local **declarado adentro** del bloque. Un
// `synchronized` que solo toca campos —que es la forma de casi todos los casos de prueba— no
// asigna ningun slot nuevo y sale correcto. `TimerTask.cancel()` fue el primero en tenerlo.
//
// De paso, el cambio de emision arreglo un defecto viejo que nunca se anoto: la version previa
// NO liberaba el monitor en el retorno normal — emitia `ireturn` desde adentro del bloque y solo
// hacia `monitorexit` por el handler de excepcion, o sea que un `return` dentro de un
// `synchronized` se llevaba el monitor puesto. La emision actual hace
// `aload_1; monitorexit; ireturn`, que es lo correcto (JVMS 3.14).
public class finding_283 {

    private final Object lock = new Object();
    private int n = 7;

    // El caso que fallaba con el javac intermedio: un local declarado dentro del bloque.
    public static int conLocal() {
        finding_283 s = new finding_283();
        synchronized (s.lock) {
            int copia = s.n;
            return copia;
        }
    }

    // Control: sin locales propios el bloque siempre salio bien, porque no se pedia ningun slot.
    public static int sinLocal() {
        finding_283 s = new finding_283();
        synchronized (s.lock) {
            return s.n;
        }
    }

    // Control del anidamiento: dos niveles, dos locales. Cada monitor tiene su slot.
    public static int anidado() {
        finding_283 a = new finding_283();
        finding_283 b = new finding_283();
        synchronized (a.lock) {
            int x = a.n;
            synchronized (b.lock) {
                int y = b.n;
                return x + y;
            }
        }
    }
}
