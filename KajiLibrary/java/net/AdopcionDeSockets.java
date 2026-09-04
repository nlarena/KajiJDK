package java.net;

/**
 * Lo que `java.net` sabe hacer y `java.nio.channels` no puede: envolver un socket ya abierto de la VM
 * en el objeto de este paquete que le corresponde.
 *
 * <p>Existe porque {@code SocketChannel.socket()} y sus dos hermanos tienen que devolver un objeto de
 * `java.net` **sobre el mismo socket del sistema** que el canal ya tiene. Envolver un handle es una
 * operacion de paquete --no esta en el contrato publico de ninguna de las tres clases, y agregarla
 * seria inventar un miembro que el JDK no tiene-- asi que se hace aca y se ofrece por el puente
 * {@link jdk.internal.net.Adopcion}. La nota de esa clase explica por que el puente existe.
 *
 * <p>Los tres objetos que salen de aca **comparten el socket con el canal**: cerrar cualquiera de los
 * dos cierra el mismo descriptor, que es exactamente lo que promete el JDK para el par
 * canal/socket.
 */
final class AdopcionDeSockets implements jdk.internal.net.Adopcion.Fabrica {

    public Object tcp(int handle) {
        Socket s = new Socket();
        s.adoptar(handle);
        return s;
    }

    public Object servidor(int handle) {
        try {
            ServerSocket s = new ServerSocket();
            s.adoptar(handle);
            return s;
        } catch (java.io.IOException e) {
            // `new ServerSocket()` no ata nada y por eso no puede fallar; el `throws` es del
            // contrato. Si alguna vez fallara, un `null` seria peor que decirlo.
            throw new IllegalStateException("no se pudo envolver el socket", e);
        }
    }

    public Object datagrama(int handle) {
        try {
            DatagramSocket s = new DatagramSocket((SocketAddress) null);
            s.adoptar(handle);
            return s;
        } catch (SocketException e) {
            // Igual que arriba: `new DatagramSocket(null)` es el caso que no ata.
            throw new IllegalStateException("no se pudo envolver el socket", e);
        }
    }
}
