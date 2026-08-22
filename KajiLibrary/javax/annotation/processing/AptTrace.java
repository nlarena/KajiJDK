package javax.annotation.processing;
// Puente de salida mínimo para el round loop de APT (fase 2): un processor imprime llamando a este
// método estático, que la VM despacha a un native (escribe en la consola del intérprete). Existe
// porque `System.out.println` todavía no compila en el javac del proyecto (la resolución de
// `System.out` como campo estático falla); es la versión mínima de un `Messager` que delega en Rust.
public class AptTrace {
    public static native void trace(String msg);
}
