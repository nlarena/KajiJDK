package jdk.nio;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SelectableChannel;

/**
 * La puerta para envolver un descriptor de archivo prestado en un canal seleccionable.
 *
 * <h2>Qué problema resuelve</h2>
 *
 * <p>NIO sabe hacer canales sobre las cosas que él mismo abre: un socket, un archivo, un pipe. Lo
 * que no sabe hacer es tomar un descriptor que consiguió <em>otro</em> —una biblioteca nativa, un
 * proceso padre que lo heredó, un dispositivo abierto por JNI— y meterlo en un {@link
 * java.nio.channels.Selector}. Este método es esa costura, y por eso vive en un módulo aparte y no
 * en {@code java.nio.channels}: es una puerta de servicio, no API general.
 *
 * <h2>Por qué hace falta el {@link SelectableChannelCloser}</h2>
 *
 * <p>Porque el descriptor <strong>no es de quien lo envuelve</strong>. Un canal normal cierra su
 * descriptor cuando lo cierran a él, y eso acá sería un error: el dueño puede seguir usándolo. Como
 * NIO no puede saber cuál es la política correcta, la delega — el cierre y la liberación se los
 * pregunta a un objeto que provee quien llama.
 *
 * <p>Que sean <em>dos</em> métodos y no uno es la parte fina. {@code implCloseChannel} corre cuando
 * cierran el canal, pero el descriptor puede seguir en uso por una operación de E/S que todavía no
 * volvió; {@code implReleaseChannel} corre cuando esa última operación termina y ahí sí no queda
 * nadie. Un solo método obligaría a elegir entre cerrar demasiado pronto o no cerrar nunca.
 *
 * <h2>Lo que esta VM no puede</h2>
 *
 * <p>{@link #readWriteSelectableChannel} <strong>no está implementado acá</strong> y tira
 * {@link UnsupportedOperationException}. No es una omisión que se pueda tapar escribiendo más Java:
 * hace falta un canal seleccionable construido sobre un descriptor crudo, o sea la maquinaria que en
 * el JDK vive en {@code sun.nio.ch} y que en esta VM no existe — el selector propio sólo conoce los
 * canales que abrió él. Queda declarado, con el tipo exacto del JDK, y diciendo que no puede: es
 * preferible a fingir que devuelve un canal que después no seleccionaría nada.
 */
public final class Channels {

    private Channels() {
    }

    /**
     * Envuelve {@code fd} en un canal seleccionable de lectura y escritura.
     *
     * @param fd el descriptor, que sigue siendo de quien lo pasó
     * @param closer quién decide qué hacer al cerrar y al liberar
     * @throws UnsupportedOperationException siempre, en esta VM — ver la nota de la clase
     */
    public static SelectableChannel readWriteSelectableChannel(FileDescriptor fd,
            SelectableChannelCloser closer) {
        throw new UnsupportedOperationException(
                "esta VM no sabe hacer un canal seleccionable sobre un descriptor prestado");
    }

    /**
     * La política de cierre de un canal que envuelve un descriptor ajeno.
     *
     * <p>Ver la descripción de {@link Channels} para por qué son dos métodos y no uno.
     */
    public interface SelectableChannelCloser {

        /**
         * Cierran el canal.
         *
         * <p>Puede haber E/S en vuelo todavía, así que acá va lo que desbloquea a quien esté
         * esperando — no necesariamente cerrar el descriptor.
         */
        void implCloseChannel(SelectableChannel sc) throws IOException;

        /**
         * Terminó la última operación de E/S sobre el canal ya cerrado.
         *
         * <p>Recién acá el descriptor no lo está usando nadie.
         */
        void implReleaseChannel(SelectableChannel sc) throws IOException;
    }
}
