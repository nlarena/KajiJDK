package java.net;

// Quien fabrica la implementacion de bajo nivel de un `DatagramSocket`.
//
// Existe por la misma razon que `SocketImplFactory`: para que una aplicacion pueda meter su propia
// pila de UDP --un tunel, una simulacion, una capa de prueba-- debajo de la API estandar, sin que
// nadie que use `DatagramSocket` se entere.
//
// Una sola por VM, y se instala con `DatagramSocket.setDatagramSocketImplFactory`.
//
// @deprecated El JDK la deprecio junto con el mecanismo de `DatagramSocketImpl`.
@Deprecated
public interface DatagramSocketImplFactory {

    /** Una implementacion nueva, sin crear todavia el socket del sistema. */
    DatagramSocketImpl createDatagramSocketImpl();
}
