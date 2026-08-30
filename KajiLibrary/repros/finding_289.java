// Repro de #289 - faltaba el autoboxing en un INICIALIZADOR DE ARREGLO de tipo referencia.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_289.java
//   run-headless KajiLibrary\repros\finding_289.class enArreglo         -> 7
//   run-headless KajiLibrary\repros\finding_289.class enAsignacion      -> 7
//   run-headless KajiLibrary\repros\finding_289.class enArgumento       -> 7
//   run-headless KajiLibrary\repros\finding_289.class soloReferencias   -> 2
//
// ANTES, para `Object[] a = { "x", 7 };`, compilaba sin una queja y emitia bytecode INVALIDO: un
// `aastore` con un `int` crudo en la pila. Nuestra VM lo cazaba al ejecutar
// (`array_operations.rs:777: expected a reference, found Int(7)`); un JVM de verdad lo habria
// rechazado en la verificacion.
//
//   javac del JDK 25                       el nuestro, antes
//   ------------------------------------   ------------------------------------
//   11: bipush 7                           11: bipush 7
//   13: invokestatic Integer.valueOf(I)     (nada)
//   16: aastore                            13: aastore     <- un int donde va una referencia
//
// El autoboxing SI funcionaba en los otros contextos, y eso fue lo que acoto el hallazgo:
//
//   Object o = 7;        asignacion         -> boxeaba bien
//   toma(7)              argumento          -> boxeaba bien
//   Object[] a = { 7 };  inicializador      -> NO boxeaba
//
// O sea que la conversion existia y estaba implementada; lo que no la aplicaba era el camino del
// inicializador de arreglo.
//
// AHORA: arreglado en `transtypes.rs`. La pasada ya convertia las DIMENSIONES a `int`
// (`self.coerce(d, Prim(Int))`) pero a los elementos del inicializador solo los recorria. Se le
// agrego el `coerce` al tipo del componente, que sale de `e.ty` — leido antes de prestar
// `e.kind`, como ya hacia el caso de la lambda por el mismo motivo.
//
// El bytecode emitido es ahora identico al del JDK, offset por offset:
//
//   11: bipush 7
//   13: invokestatic Integer.valueOf(I)
//   16: aastore
//
// Los tres controles se conservan: son los que ubicaron el defecto en un solo camino y los que
// detectarian una regresion parcial.
//
// Queda como REGRESION.
public class finding_289 {

    // El caso que fallaba: el `7` iba crudo al `aastore`.
    public static int enArreglo() {
        Object[] a = { "x", 7 };
        return ((Integer) a[1]).intValue();
    }

    // Control: la misma conversion en una asignacion.
    public static int enAsignacion() {
        Object o = 7;
        return ((Integer) o).intValue();
    }

    static int toma(Object o) {
        return ((Integer) o).intValue();
    }

    // Control: la misma conversion en un argumento.
    public static int enArgumento() {
        return toma(7);
    }

    // Control: el arreglo de referencias puro nunca estuvo afectado.
    public static int soloReferencias() {
        Object[] a = { "x", "yy" };
        return ((String) a[1]).length();
    }
}
