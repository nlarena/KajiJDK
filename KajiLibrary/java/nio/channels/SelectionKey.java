package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.SelectionKey — la anotacion de un canal en un selector.
 *
 * <p>Una llave junta tres cosas: **que canal**, **que le interesa** ({@link #interestOps()}) y **que
 * tiene listo** ({@link #readyOps()}). La distincion entre las dos ultimas es todo el mecanismo: uno
 * declara lo que quiere escuchar, el selector contesta lo que ocurrio, y el resto es leer bits.
 *
 * <p>Los cuatro bits no son consecutivos --1, 4, 8, 16 y no 1, 2, 4, 8-- y no es un error de
 * transcripcion: es asi en el JDK desde el principio y hay codigo que tiene los numeros escritos a
 * mano, asi que cambiarlos ahora seria romperlo. Se copian tal cual.
 *
 * <p>El {@link #attach adjunto} existe para no tener que mantener un mapa de canal a estado en
 * paralelo: la llave viaja con el evento, asi que colgarle ahi el contexto de la conexion es lo que
 * evita el `HashMap` global y su candado.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La clase esta entera. Lo abstracto queda abstracto --lo pone quien implemente un selector-- y
 * lo calculable esta implementado: los cuatro `is*` son una mascara sobre {@link #readyOps()}, el
 * adjunto es un campo, y {@link #interestOpsOr}/{@link #interestOpsAnd} son lo mismo que el JDK,
 * leer y volver a fijar.
 *
 * <p>No se puede obtener una instancia porque no hay selectores que fabricar, y eso no la vuelve
 * inutil: {@link java.nio.channels.spi.AbstractSelectionKey} hereda de aca con la parte de
 * validez ya resuelta, que es lo que necesita quien traiga su propio transporte.
 */
public abstract class SelectionKey {

    /** Listo para leer. */
    public static final int OP_READ = 1 << 0;

    /** Listo para escribir. */
    public static final int OP_WRITE = 1 << 2;

    /** Listo para terminar de conectar. */
    public static final int OP_CONNECT = 1 << 3;

    /** Listo para aceptar una conexion. */
    public static final int OP_ACCEPT = 1 << 4;

    private Object adjunto;

    protected SelectionKey() {
    }

    /** El canal de esta llave. Lo devuelve aunque la llave este cancelada. */
    public abstract SelectableChannel channel();

    /** El selector de esta llave. Lo devuelve aunque la llave este cancelada. */
    public abstract Selector selector();

    /** Si la llave sigue valida. Deja de serlo al cancelarla, al cerrar el canal o el selector. */
    public abstract boolean isValid();

    /**
     * Cancela el registro.
     *
     * <p>La llave queda invalida en el acto, pero el canal se saca del selector recien en la
     * seleccion siguiente: sacarlo ahora seria modificar el juego de llaves por debajo de un
     * `select` que podria estar corriendo en otro hilo.
     */
    public abstract void cancel();

    /** Las operaciones que se estan esperando. */
    public abstract int interestOps();

    /** Cambia las operaciones que se esperan. */
    public abstract SelectionKey interestOps(int ops);

    /**
     * Agrega `ops` a lo que se espera y devuelve lo que habia antes.
     *
     * <p>Devolver el valor viejo es lo que la hace util frente a leer y fijar por separado: dos
     * hilos que agreguen bits a la vez no se pisan.
     */
    public int interestOpsOr(int ops) {
        synchronized (this) {
            int antes = this.interestOps();
            this.interestOps(antes | ops);
            return antes;
        }
    }

    /** Deja solo los bits que tambien esten en `ops`, y devuelve lo que habia antes. */
    public int interestOpsAnd(int ops) {
        synchronized (this) {
            int antes = this.interestOps();
            this.interestOps(antes & ops);
            return antes;
        }
    }

    /** Las operaciones que el selector encontro listas. */
    public abstract int readyOps();

    public final boolean isReadable() {
        return (this.readyOps() & OP_READ) != 0;
    }

    public final boolean isWritable() {
        return (this.readyOps() & OP_WRITE) != 0;
    }

    public final boolean isConnectable() {
        return (this.readyOps() & OP_CONNECT) != 0;
    }

    public final boolean isAcceptable() {
        return (this.readyOps() & OP_ACCEPT) != 0;
    }

    /** Cuelga `ob` de la llave y devuelve lo que colgaba antes. `null` descuelga. */
    public final Object attach(Object ob) {
        Object antes = this.adjunto;
        this.adjunto = ob;
        return antes;
    }

    /** Lo que cuelga de la llave, o `null`. */
    public final Object attachment() {
        return this.adjunto;
    }
}
