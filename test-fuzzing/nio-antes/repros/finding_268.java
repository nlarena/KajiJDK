// Repro de #268 - faltaba el CAST SINTETICO tras una llamada de retorno borrado.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_268.java
//   javap -p -c KajiLibrary\repros\finding_268.class
//
// `Caja<T>.get()` esta declarado `T`, asi que su DESCRIPTOR dice `Object` (JLS 4.6): lo que
// queda en la pila es un Object. Encadenar ahi emitia
//
//   invokeinterface Caja.get:()Ljava/lang/Object;
//   invokevirtual   finding_268$Hoja.n:()I          <- sobre un Object
//
// y eso NO VERIFICA: JVMS 4.10.1.9 pide que el objectref sea asignable al tipo del metodo. La
// JVM real lo rechaza con VerifyError antes de ejecutar una sola instruccion.
//
// De este lado no se veia porque nuestro interprete despacha por el objeto REAL, no por el
// tipo estatico: es de la familia "compila, corre aca, revienta en la JVM de verdad", que es
// la peor de todas porque el gate propio la deja pasar.
//
// Esperado ahora: un `checkcast finding_268$Hoja` entre las dos llamadas, igual que el javac
// real.
//
// Diferencia deliberada con el javac real: el javac lo inserta solo donde el contexto pide el
// tipo angosto y lo omite cuando el destino es mas ancho (`Object o = c.get();`) o cuando el
// valor se descarta. Aca se inserta siempre que el tipo del sitio sea estrictamente mas
// angosto: es correcto -el cast no puede fallar en un programa bien tipado- y cuesta un
// checkcast de mas en esas dos formas. Saber el contexto pide un pase aparte.
public class finding_268 {

    interface Caja<T> { T get(); }

    static class Hoja { int n() { return 3; } }

    /* Encadenar sobre el retorno borrado: es el que no verificaba. */
    public static int encadenada(Caja<Hoja> c) { return c.get().n(); }

    /* Devolverlo con el tipo angosto: tampoco verificaba (el `areturn` pide `Hoja`). */
    public static Hoja devuelta(Caja<Hoja> c) { return c.get(); }
}
