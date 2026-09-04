package java.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;

/**
 * Una llamada remota en curso, vista como dos corrientes.
 *
 * @deprecated es de la epoca de los stubs generados por {@code rmic}. Los proxies dinamicos hacen lo
 *     mismo sin exponer el protocolo, y nada de esto se usa desde entonces.
 */
@Deprecated(since = "1.2")
public interface RemoteCall {

    /** Donde escribir los argumentos. */
    ObjectOutput getOutputStream() throws IOException;

    /** Termina de escribir los argumentos. */
    void releaseOutputStream() throws IOException;

    /** De donde leer el resultado. */
    ObjectInput getInputStream() throws IOException;

    /** Termina de leer. */
    void releaseInputStream() throws IOException;

    /** Donde escribir el resultado, del lado del servidor. */
    ObjectOutput getResultStream(boolean success) throws IOException, StreamCorruptedException;

    /** Ejecuta la llamada. */
    void executeCall() throws Exception;

    /** Libera lo que quedo tomado. */
    void done() throws IOException;
}
