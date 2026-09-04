package java.net;

// Quien fabrica la implementacion que hay debajo de un socket.
//
// Un solo metodo, y su razon de ser es que `Socket` y `ServerSocket` no traigan su transporte
// cableado adentro: se instala una fabrica y todos los sockets nuevos pasan a usar otra
// implementacion --un tunel, un socket de prueba, un transporte propio-- sin tocar el codigo que
// los usa.
//
// En KajiJDK no hay `Socket` ni `ServerSocket` que la consulten (no hay nativos de red), pero la
// interfaz no promete que los haya: promete que **si** alguien fabrica un `SocketImpl`, se lo pide
// por aca. Eso es cierto tal como esta escrito.
public interface SocketImplFactory {

    /** Una implementacion nueva, sin crear todavia el socket del sistema. */
    SocketImpl createSocketImpl();
}
