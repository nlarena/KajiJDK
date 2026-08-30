package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

// KajiLibrary's java.nio.channels.ReadableByteChannel -- a channel that can be read into a buffer.
//
// Un solo metodo, y esa es toda la interfaz. Lo que la hace util es que la fuente la aporta **quien
// llama**: `new Scanner(canal)` no necesita que la biblioteca sepa abrir archivos ni sockets, porque
// el canal ya viene abierto de afuera. Por eso se puede implementar de verdad en KajiJDK, que no
// tiene acceso al sistema de archivos, mientras que `new Scanner(File)` no.
public interface ReadableByteChannel extends Channel {

    /**
     * Lee una secuencia de bytes en `dst`, y devuelve **cuantos** leyo.
     *
     * <p>Devuelve `-1` en fin de flujo, `0` si `dst` no tenia lugar (o si un canal no bloqueante no
     * tenia nada listo), y entre 1 y `dst.remaining()` en el caso normal. Los tres son resultados
     * legitimos y distintos: un lector que trate el `0` como fin de flujo se cuelga o se corta
     * antes de tiempo, segun el canal.
     *
     * <p>Un canal solo admite **una** lectura a la vez: si otro hilo esta leyendo, esta llamada se
     * bloquea hasta que la primera termine.
     */
    int read(ByteBuffer dst) throws IOException;
}
