// Repro del finding #566 -- ABIERTO. Paso 1 de 2.
//
// El `throws X` de este metodo es una VARIABLE DE TIPO. En el `.class` eso se parte en dos:
//
//   Exceptions: throws java.lang.Throwable     <- el BORRADO, que es lo que manda JVMS 4.7.5
//   Signature:  ()TV;^TX;                      <- la variable, en la firma generica
//
// Nuestro javac emite las dos cosas bien, identico al javac del JDK 25 (verificado con `javap -v`).
// El problema esta al LEERLO: ver `Usa.java`.
public interface Op<V, X extends Throwable> {
    V call() throws X;
}
