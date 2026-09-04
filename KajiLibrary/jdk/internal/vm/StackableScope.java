package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.StackableScope — un ámbito que se apila por hilo.
 *
 * <p>Es la base de la concurrencia estructurada: un ámbito se abre, se anidan otros adentro, y se
 * cierran **en orden inverso**. La palabra "stackable" es la promesa: si al cerrar uno descubrimos que
 * no era el de arriba, alguien se salteó el orden, y eso se detecta en vez de dejarlo pasar.
 *
 * <p>La pila es **por hilo** y vive en un {@link ThreadLocal}. En el JDK vive en un campo de
 * `Thread`, que es más rápido y llega antes durante el arranque; acá no se puede tocar `Thread` desde
 * este paquete, y un `ThreadLocal` da exactamente la misma semántica. Es un interno, y los internos
 * son libres.
 *
 * <p>La distinción entre {@link #tryPop()} y {@link #popForcefully()} es la que gobierna la clase:
 * el primero saca este ámbito **sólo si es el de arriba** y devuelve `false` si no lo es --el camino
 * normal, donde el desorden se reporta--; el segundo saca todo lo que quedó por encima cerrándolo, y
 * es el camino de la excepción, donde ya sabemos que algo salió mal y hay que dejar la pila limpia.
 */
public class StackableScope {

    // La cabeza de la pila del hilo actual. Un `ThreadLocal` por clase, no por instancia: la pila es
    // una sola por hilo y todos los ambitos comparten esa vista.
    private static final ThreadLocal<StackableScope> CABEZA = new ThreadLocal<StackableScope>();

    private final Thread duenio;
    private StackableScope anterior;
    private boolean apilado;

    /**
     * @param shared si el ámbito **no** pertenece a un hilo en particular
     */
    StackableScope(boolean shared) {
        this.duenio = shared ? null : Thread.currentThread();
    }

    /** Un ámbito del hilo que lo construye. */
    protected StackableScope() {
        this(false);
    }

    /** El hilo dueño, o `null` si es compartido. */
    public Thread owner() {
        return this.duenio;
    }

    /** Apila este ámbito en el hilo actual y lo devuelve, para encadenar. */
    public StackableScope push() {
        this.anterior = StackableScope.CABEZA.get();
        StackableScope.CABEZA.set(this);
        this.apilado = true;
        return this;
    }

    /**
     * Saca este ámbito **si es el de arriba**.
     *
     * @return `false` si no lo era, y entonces la pila queda intacta
     */
    public boolean tryPop() {
        if (StackableScope.CABEZA.get() != this) {
            return false;
        }
        StackableScope.CABEZA.set(this.anterior);
        this.anterior = null;
        this.apilado = false;
        return true;
    }

    /**
     * Saca este ámbito **y todo lo que haya quedado encima**, cerrando cada uno.
     *
     * <p>Se usa cuando ya hubo un error: lo que importa es que el hilo quede con una pila coherente,
     * no respetar un orden que alguien ya rompió. Cada ámbito de arriba recibe {@link #tryClose()},
     * así que una subclase con algo que liberar se entera.
     *
     * @return si este ámbito estaba en la pila
     */
    public boolean popForcefully() {
        if (!this.apilado) {
            return false;
        }
        StackableScope cur = StackableScope.CABEZA.get();
        while (cur != null && cur != this) {
            StackableScope sig = cur.anterior;
            cur.tryClose();
            cur.anterior = null;
            cur.apilado = false;
            cur = sig;
        }
        StackableScope.CABEZA.set(this.anterior);
        this.anterior = null;
        this.apilado = false;
        return true;
    }

    /** Vacía la pila del hilo actual, cerrando todo lo que haya. */
    public static void popAll() {
        StackableScope cur = StackableScope.CABEZA.get();
        while (cur != null) {
            StackableScope sig = cur.anterior;
            cur.tryClose();
            cur.anterior = null;
            cur.apilado = false;
            cur = sig;
        }
        StackableScope.CABEZA.set(null);
    }

    /** El ámbito inmediatamente debajo de éste, o `null`. */
    public StackableScope enclosingScope() {
        return this.anterior;
    }

    /**
     * El ámbito más cercano hacia abajo que sea de ese tipo, o `null`.
     *
     * <p>Se busca por {@link Class#isInstance}, o sea que una **subclase** también cuenta: quien pide
     * un `ThreadContainer` quiere el contenedor más cercano, sea de la clase que sea.
     */
    public <T extends StackableScope> T enclosingScope(Class<T> tipo) {
        StackableScope cur = this.anterior;
        while (cur != null) {
            if (tipo.isInstance(cur)) {
                return (T) cur;
            }
            cur = cur.anterior;
        }
        return null;
    }

    /** El anterior, para el paquete. */
    StackableScope previous() {
        return this.anterior;
    }

    /**
     * Lo que hay que hacer al cerrar este ámbito.
     *
     * <p>Un `StackableScope` pelado no tiene nada que liberar, así que dice que sí. Las subclases que
     * sostienen algo --hilos, ligaduras-- lo redefinen; es el gancho por el que
     * {@link #popForcefully()} y {@link #popAll()} las avisan.
     *
     * @return si se pudo cerrar
     */
    protected boolean tryClose() {
        return true;
    }

    /** La cabeza de la pila del hilo actual, para el paquete. */
    static StackableScope head() {
        return StackableScope.CABEZA.get();
    }
}
