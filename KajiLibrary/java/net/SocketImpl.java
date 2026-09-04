package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import java.util.Objects;
import java.util.Set;

// El transporte que hay debajo de un socket TCP, separado del socket mismo.
//
// ===========================================================================================
// QUE ES ESTA CLASE, Y POR QUE PUEDE ESTAR SIN QUE HAYA RED
// ===========================================================================================
//
// `Socket` es lo que usa el programa; `SocketImpl` es lo que **hace el trabajo**. La separacion es
// vieja y sigue siendo util: cambiando la implementacion se cambia el transporte sin tocar una linea
// del codigo que abre conexiones.
//
// Todos los metodos que tocan la red --`create`, `connect`, `bind`, `listen`, `accept`, `close`,
// `getInputStream`, `available`, `sendUrgentData`-- son **abstractos**. Esta clase no los escribe:
// declara que alguien los va a escribir. Eso es exactamente lo que es en el JDK, y es la unica
// forma honesta de que el tipo exista en una VM sin sockets: **no hay aca un solo metodo que
// prometa conectar**. Hay una lista de lo que haria falta implementar.
//
// Lo poco concreto que trae son accesores de campos (`getInetAddress`, `getPort`, `getLocalPort`,
// `getFileDescriptor`), el `toString`, y las traducciones entre las dos formas de nombrar opciones
// --la vieja de `SocketOptions`, con enteros, y la nueva de `SocketOption<T>`, con tipos--. Nada de
// eso toca la red: son campos y un `switch`.
//
// `shutdownInput`/`shutdownOutput` y `setPerformancePreferences` se declaran con el mismo cuerpo que
// en el JDK: los dos primeros tiran `IOException("Method not implemented!")` --es la implementacion
// base del JDK, no un stub de KajiJDK-- y el tercero no hace nada, porque son sugerencias que una
// implementacion puede ignorar por contrato.
//
// ===========================================================================================
// QUIEN LA IMPLEMENTA
// ===========================================================================================
//
// **Nadie, en este arbol.** No hay `Socket` ni `ServerSocket`, y no los va a haber sin nativos de
// red: un `Socket.connect()` que no conecte pero tampoco falle es lo peor que se puede escribir.
// Esta clase es el lugar por donde entraria un transporte el dia que exista uno.
//
// Los veintiocho miembros estan.
public abstract class SocketImpl implements SocketOptions {

    /** El descriptor del socket del sistema, o null si todavia no se creo. */
    protected FileDescriptor fd;

    /** La direccion del otro extremo. */
    protected InetAddress address;

    /** El puerto del otro extremo. */
    protected int port;

    /** El puerto de este lado. */
    protected int localport;

    public SocketImpl() {
    }

    /**
     * Crea el socket del sistema.
     *
     * @param stream true para TCP, false para UDP
     * @throws IOException si no se pudo crear
     */
    protected abstract void create(boolean stream) throws IOException;

    /**
     * Conecta a {@code host}:{@code port}, resolviendo el nombre.
     *
     * @throws IOException si no se pudo conectar o el nombre no resolvio
     */
    protected abstract void connect(String host, int port) throws IOException;

    /**
     * Conecta a esa direccion y puerto.
     *
     * @throws IOException si no se pudo conectar
     */
    protected abstract void connect(InetAddress address, int port) throws IOException;

    /**
     * Conecta con limite de tiempo.
     *
     * @param timeout milisegundos, o 0 para esperar sin limite
     * @throws IOException si no se pudo conectar o se acabo el tiempo
     */
    protected abstract void connect(SocketAddress address, int timeout) throws IOException;

    /**
     * Ata el socket a una direccion y un puerto locales.
     *
     * @throws IOException si el puerto esta tomado o la direccion no es de esta maquina
     */
    protected abstract void bind(InetAddress host, int port) throws IOException;

    /**
     * Empieza a aceptar conexiones, encolando hasta {@code backlog} sin atender.
     *
     * @throws IOException si no se pudo
     */
    protected abstract void listen(int backlog) throws IOException;

    /**
     * Espera una conexion entrante y la deja en {@code s}.
     *
     * <p>El resultado se escribe **en el parametro** y no se devuelve, que es de las firmas mas
     * confusas del JDK: `s` llega vacio y sale conectado.
     *
     * @throws IOException si fallo la espera
     */
    protected abstract void accept(SocketImpl s) throws IOException;

    /**
     * El flujo para leer de la conexion.
     *
     * @throws IOException si no se puede abrir
     */
    protected abstract InputStream getInputStream() throws IOException;

    /**
     * El flujo para escribir a la conexion.
     *
     * @throws IOException si no se puede abrir
     */
    protected abstract OutputStream getOutputStream() throws IOException;

    /**
     * Cuantos bytes se pueden leer sin bloquear.
     *
     * @throws IOException si fallo la consulta
     */
    protected abstract int available() throws IOException;

    /**
     * Cierra el socket.
     *
     * @throws IOException si fallo el cierre
     */
    protected abstract void close() throws IOException;

    /**
     * Cierra la mitad de lectura dejando abierta la de escritura.
     *
     * <p>El cuerpo base tira, igual que en el JDK: no toda implementacion sabe cerrar media
     * conexion, y las que saben pisan el metodo.
     *
     * @throws IOException siempre, en la implementacion base
     */
    protected void shutdownInput() throws IOException {
        throw new IOException("Method not implemented!");
    }

    /**
     * Cierra la mitad de escritura dejando abierta la de lectura.
     *
     * @throws IOException siempre, en la implementacion base
     */
    protected void shutdownOutput() throws IOException {
        throw new IOException("Method not implemented!");
    }

    /** El descriptor del sistema, o null si el socket no se creo. */
    protected FileDescriptor getFileDescriptor() {
        return this.fd;
    }

    /** La direccion del otro extremo. */
    protected InetAddress getInetAddress() {
        return this.address;
    }

    /** El puerto del otro extremo. */
    protected int getPort() {
        return this.port;
    }

    /**
     * Si esta implementacion sabe mandar datos urgentes.
     *
     * <p>La base dice que no, y es la respuesta correcta para una clase que no implementa nada: la
     * que sepa, pisa el metodo. Decir que si obligaria a `sendUrgentData` a funcionar.
     */
    protected boolean supportsUrgentData() {
        return false;
    }

    /**
     * Manda un byte fuera de banda.
     *
     * @throws IOException si fallo el envio
     */
    protected abstract void sendUrgentData(int data) throws IOException;

    /** El puerto de este lado. */
    protected int getLocalPort() {
        return this.localport;
    }

    @Override
    public String toString() {
        return "Socket[addr=" + getInetAddress() + ",port=" + getPort()
                + ",localport=" + getLocalPort() + "]";
    }

    /**
     * Sugiere que le importa mas a esta conexion, en importancia relativa.
     *
     * <p>No hace nada, aca y en el JDK: son **sugerencias**, y el contrato dice explicitamente que
     * una implementacion puede ignorarlas. Un cuerpo vacio no es un hueco tapado; es la
     * implementacion base.
     */
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * Fija una opcion nombrada con la forma nueva, la de {@link SocketOption}.
     *
     * <p>**La implementacion base tira siempre**, despues de chequear que el nombre no sea null. No
     * es un hueco de KajiJDK: es literalmente lo que hace el JDK, y esta bien que lo haga. Esta
     * clase tiene dos vocabularios de opciones --el viejo de enteros que hereda de
     * {@link SocketOptions}, y este-- y **no los puentea**, porque una implementacion que solo
     * atienda el viejo no tiene por que aceptar los nombres del nuevo. Traducir de uno al otro por
     * su cuenta haria que una opcion pareciera soportada cuando la subclase nunca la considero.
     *
     * <p>La subclase que quiera soportarlos pisa este metodo y {@link #supportedOptions}.
     *
     * @throws UnsupportedOperationException siempre, en la implementacion base
     * @throws NullPointerException          si {@code name} es null
     * @throws IOException                   si el socket la rechaza
     */
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * El valor de una opcion nombrada con la forma nueva.
     *
     * <p>Tira siempre en la base, por la misma razon que {@link #setOption(SocketOption, Object)}.
     *
     * @throws UnsupportedOperationException siempre, en la implementacion base
     * @throws NullPointerException          si {@code name} es null
     * @throws IOException                   si el socket no la puede leer
     */
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * Las opciones que esta implementacion entiende.
     *
     * <p>**El conjunto vacio**, igual que el JDK, y por la misma razon: la clase base no atiende
     * ninguna opcion de la forma nueva. La subclase que atienda alguna la declara aca.
     */
    protected Set<SocketOption<?>> supportedOptions() {
        return Collections.emptySet();
    }
}
