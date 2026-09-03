package java.util.concurrent.locks;

// Un nodo de la cola de espera de un `AbstractQueued(Long)Synchronizer`: **un hilo bloqueado**.
// Clase de paquete y de primer nivel --no anidada-- porque la comparten los dos sincronizadores
// y la `ConditionObject` de cada uno; nada de esto sale al contrato publico.
//
// Hay **dos** monitores en juego y conviene no confundirlos:
//
//   - el monitor interno del sincronizador guarda los enlaces de la cola (`anterior`,
//     `siguiente`, `encolado`, `compartido`) y el `state`;
//   - el monitor **de este nodo** (`synchronized (nodo)`) guarda `liberado`, que es el permiso
//     del nodo, y es donde el hilo duerme.
//
// Esa separacion es la que hace imposible el despertar perdido: quien libera hace
// `synchronized (n) { n.liberado = true; n.notifyAll(); }` y quien espera comprueba `liberado`
// **bajo el mismo monitor** antes de dormirse. Si la senial llego primero, la bandera ya esta
// puesta y el hilo no llega a dormirse; si llega despues, el `notifyAll` lo encuentra dormido.
// No hay ventana entre las dos cosas porque las dos pasan adentro del mismo monitor.
//
// Y por eso el nodo se bloquea con `Object.wait()` y no con `LockSupport.park()`, aunque el JDK
// use lo segundo: `wait` tiene forma **con plazo** y lanza `InterruptedException` donde el
// contrato dice que hay que lanzarla. `park` de nuestra VM no tiene plazo, y ademas *lanza*
// `InterruptedException` en vez de retornar (ver el encabezado de `LockSupport`).
final class SyncWaiter {

    // El hilo que espera en este nodo; `null` una vez que salio de la cola.
    Thread hilo;

    // Enlaces de la cola FIFO. Los guarda el monitor interno del sincronizador.
    SyncWaiter siguiente;
    SyncWaiter anterior;

    // Si el nodo espera en modo compartido (`acquireShared`) o exclusivo (`acquire`).
    boolean compartido;

    // Si todavia esta en la cola. Sirve para que un desencolado repetido --el que hace un
    // `tryAcquireNanos` que vence justo cuando lo estaban por atender-- no rompa los enlaces.
    boolean encolado;

    // El permiso del nodo. Lo guarda el monitor **de este nodo**.
    boolean liberado;
}
