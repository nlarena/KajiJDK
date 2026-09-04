package jdk.internal.vm;

import java.util.stream.Stream;

/**
 * KajiLibrary's jdk.internal.vm.ThreadContainer — un grupo de hilos con dueño.
 *
 * <p>Es lo que reemplaza a `ThreadGroup` para la concurrencia estructurada, y la diferencia importa:
 * un `ThreadGroup` es una jerarquía suelta que nadie cierra, mientras que un contenedor **es un
 * ámbito** --extiende {@link StackableScope}-- y por lo tanto tiene principio y fin. Ese es todo el
 * punto: cuando el ámbito termina, se sabe qué hilos había adentro y se puede exigir que hayan
 * terminado.
 *
 * <p>De ahí que la jerarquía de contenedores no se guarde con punteros de padre a hijo, sino que
 * salga de la **pila de ámbitos**: {@link #parent()} es el contenedor que encierra a éste en el hilo
 * que lo abrió. Un árbol que se deduce del anidamiento no puede desincronizarse con él.
 *
 * <p>{@link #threads()} es abstracto a propósito: cómo se guardan los hilos depende de la subclase
 * --{@link SharedThreadContainer} usa un conjunto concurrente-- y esta clase no elige por ella.
 */
public abstract class ThreadContainer extends StackableScope {

    /**
     * @param shared si el contenedor **no** pertenece a un hilo en particular
     */
    protected ThreadContainer(boolean shared) {
        super(shared);
    }

    /** El nombre, o `null` si no tiene. */
    public String name() {
        return null;
    }

    /** El contenedor que encierra a éste, o `null` si es de nivel superior. */
    public ThreadContainer parent() {
        return ThreadContainers.parent(this);
    }

    /** Los contenedores anidados directamente en éste. */
    public final Stream<ThreadContainer> children() {
        return ThreadContainers.children(this);
    }

    /**
     * Cuántos hilos tiene.
     *
     * <p>Se cuenta recorriendo {@link #threads()} y no con un contador aparte, y es deliberado: un
     * contador se desincroniza --un hilo que muere sin avisar lo deja alto para siempre-- mientras que
     * contar lo que hay no puede mentir. El costo es recorrer; el contrato del JDK ya dice que el
     * número es una estimación.
     */
    public long threadCount() {
        return this.threads().count();
    }

    /** Los hilos de este contenedor. */
    public abstract Stream<Thread> threads();

    /** Aviso de que un hilo arrancó. Las subclases lo redefinen para anotarlo. */
    protected void onStart(Thread thread) {
    }

    /** Aviso de que un hilo terminó. */
    protected void onExit(Thread thread) {
    }

    /**
     * Registra un hilo que arrancó.
     *
     * <p>`final` --como en el JDK-- porque separa dos cosas que no conviene mezclar: éste es el punto
     * de entrada que la VM usa, y {@link #onStart} es el gancho que la subclase redefine. Si `add`
     * fuera redefinible, una subclase podría quedarse con el aviso sin llamar al de arriba.
     */
    public final void add(Thread thread) {
        this.onStart(thread);
    }

    /** Da de baja un hilo que terminó. */
    public final void remove(Thread thread) {
        this.onExit(thread);
    }

    /** Las ligaduras de {@link java.lang.ScopedValue} vigentes al abrirse este contenedor. */
    public ScopedValueContainer.BindingsSnapshot scopedValueBindings() {
        return null;
    }

    public String toString() {
        String n = this.name();
        String base = n != null ? n : this.getClass().getName();
        return base + "@" + Integer.toHexString(System.identityHashCode(this));
    }
}
