package java.util.concurrent.locks;

import java.io.Serializable;

// Un sincronizador que puede ser **propiedad de un hilo**. No hace nada por si mismo: solo
// guarda quien lo tiene tomado en modo exclusivo, para que las herramientas de diagnostico
// --y las subclases-- puedan preguntarlo. Es la raiz de la jerarquia: tanto
// `AbstractQueuedSynchronizer` como `AbstractQueuedLongSynchronizer` heredan de aca.
//
// La clase no tiene constructor publico ni metodos publicos a proposito: los dos accesores son
// `protected final`, o sea que solo el codigo de la subclase toca el campo, y ninguna subclase
// puede cambiar como se guarda. El campo es `transient` --como en el JDK--: la propiedad de un
// lock es estado de ejecucion, no algo que tenga sentido serializar.
public abstract class AbstractOwnableSynchronizer implements Serializable {

    // El hilo que tiene el sincronizador en modo exclusivo, o `null` si esta libre.
    private transient Thread exclusiveOwnerThread;

    /** Solo para uso de las subclases. */
    protected AbstractOwnableSynchronizer() {
    }

    /**
     * Deja constancia de que `thread` es el dueno exclusivo. `null` para decir que no hay ninguno.
     *
     * <p>No hay comprobacion de nada: el JDK tampoco la hace. Quien llama es la subclase, y es ella
     * la que sabe si acaba de ganar la carrera por el estado.
     */
    protected final void setExclusiveOwnerThread(Thread thread) {
        this.exclusiveOwnerThread = thread;
    }

    /** El ultimo hilo que se anoto con {@link #setExclusiveOwnerThread}, o `null`. */
    protected final Thread getExclusiveOwnerThread() {
        return this.exclusiveOwnerThread;
    }
}
