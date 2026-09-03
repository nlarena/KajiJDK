package java.util.concurrent.locks;

// La primitiva de bloqueo y despertar sobre la que el JDK construye todo `java.util.concurrent`:
// `park()` duerme al hilo actual, `unpark(t)` lo despierta. Lo que la hace utilizable --y lo que
// la distingue de `wait`/`notify`-- es el **permiso**: un `unpark` que llega antes del `park` no
// se pierde, se guarda, y el `park` siguiente lo consume y retorna en el acto. Por eso quien
// llama puede comprobar su condicion y despues dormirse sin ventana entre las dos cosas.
//
// # Que sostiene esto en KajiJDK
//
// `park` y `unpark` son **nativos de verdad**: la VM los intercepta en `invokestatic` (son
// operaciones del planificador, no llamadas de hoja) y mantiene el permiso por hilo. No es una
// emulacion ni un bucle de espera activa. `java/ParkTest.java` lo comprueba con las tres
// substancias de hilos, incluida la paralela real.
//
// # Las esperas con plazo
//
// `parkNanos` y `parkUntil` **estan**, y estan bien: el plazo lo lleva la VM, no un rodeo de este
// lado. Este archivo decia lo contrario --que las cuatro sobrecargas no se podian escribir con
// honestidad-- y era cierto mientras el unico intrinseco fuera `park()` a secas. Lo que se hizo fue
// levantar el bloqueo: la VM tiene ahora un `park` con plazo, con el **mismo** permiso que el
// `park()` sin plazo.
//
// Que compartan el permiso es lo que importa, y es exactamente lo que hacia imposible emularlo
// desde Java: un `Object.wait(ms)` de este lado no lo despertaria un `unpark`, y un `unpark` no
// consumiria esa espera. Serian dos sistemas de permisos, y `unpark(t)` --que la VM se queda--
// tocaria solo uno. Adentro de la VM hay uno solo.
//
// **El plazo se mide en el reloj de opcodes**, como toda espera con plazo de esta VM
// (`Thread.sleep`, `Object.wait(ms)`, `join(ms)`): aca no hay reloj de pared. Un `parkNanos` no
// espera nanosegundos de verdad sino su equivalente en instrucciones ejecutadas. Es una propiedad de
// la VM entera y no de este metodo, pero conviene tenerla presente antes de usar el plazo para medir
// algo. Lo que si se cumple, y es lo que a un `Lock` le importa: la espera **termina**, la termina
// tambien un `unpark`, y un permiso que llego antes la saltea.
//
// `park(Object blocker)` **si** esta, y bloquea correctamente, pero **no registra el
// bloqueador**: la VM se queda la llamada antes de que corra una sola instruccion del cuerpo, asi
// que no hay donde anotarlo. `setCurrentBlocker`/`getBlocker` son un par honesto y completo entre
// ellos --lo que uno guarda es lo que el otro devuelve--; lo que falta es el efecto lateral de
// `park(Object)`, y esta dicho aca en vez de simulado.
//
// # Un defecto de fidelidad de la VM, que conviene saber antes de usar esto
//
// Interrumpir un hilo estacionado en `park()` **lanza `InterruptedException`** en vez de hacer
// que `park` retorne con la bandera de interrupcion puesta. La excepcion ademas es indeclarada
// (`park` no tiene `throws`). Repro en `scratchpad/zzlocks/ParkIntr.java`: el JDK real devuelve
// 1, nuestra VM mata al hilo. Por eso `AbstractQueuedSynchronizer` **no** se apoya en `park`
// para su cola: usa el monitor de cada nodo, que si lanza donde corresponde.
public final class LockSupport {

    // Los bloqueadores anotados con `setCurrentBlocker`, por hilo. Un mapa y no un campo de
    // `Thread` porque `Thread` no tiene ese campo y este paquete no lo puede agregar. La entrada
    // se **borra** cuando el bloqueador vuelve a `null`, que es lo que hace todo llamador
    // razonable al salir de la espera, asi que el mapa no crece con los hilos muertos.
    private static final java.util.HashMap<Thread, Object> BLOQUEADORES =
            new java.util.HashMap<Thread, Object>();

    private LockSupport() {
    }

    /**
     * Duerme al hilo actual hasta que lo despierten con {@link #unpark}, salvo que ya tenga un
     * permiso guardado, en cuyo caso lo consume y retorna en el acto.
     *
     * <p>Puede retornar **sin razon** (un despertar espurio), como en el JDK: quien llama tiene
     * que comprobar su condicion en un bucle, nunca suponer que un retorno significa algo.
     */
    public static native void park();

    /**
     * Igual que {@link #park()}, con un objeto que dice *por que* se esta esperando.
     *
     * <p>El bloqueador es solo para diagnostico y **este no lo registra** (la VM se queda la
     * llamada; ver el encabezado de la clase). El bloqueo en si es identico al de `park()`.
     */
    public static native void park(Object blocker);

    /**
     * Le da un permiso a `thread`: si esta estacionado, lo despierta; si no, el permiso queda
     * guardado y su proximo {@link #park()} retorna en el acto.
     *
     * <p>`unpark(null)`, o sobre un hilo que no arranco o que ya termino, no hace nada.
     */
    public static native void unpark(Thread thread);

    /**
     * Duerme al hilo actual hasta que lo despierten con {@link #unpark} o hasta que pasen `nanos`,
     * lo que ocurra primero.
     *
     * <p>Consume el permiso si ya habia uno, igual que {@link #park()}. Un `nanos` cero o negativo
     * retorna en el acto **sin** consumirlo: es lo que hace el JDK -- `parkNanos(0)` no es una
     * espera de cero, es no esperar.
     *
     * <p>Ver el encabezado de la clase sobre en que unidades corre el plazo.
     */
    public static native void parkNanos(long nanos);

    /**
     * Igual que {@link #parkNanos(long)}, con un objeto que dice *por que* se esta esperando.
     *
     * <p>El bloqueador es solo para diagnostico y **este no lo registra**, por lo mismo que
     * {@link #park(Object)}: la VM se queda la llamada antes de que corra una instruccion del
     * cuerpo. El bloqueo y el plazo son identicos a los de la otra forma.
     */
    public static native void parkNanos(Object blocker, long nanos);

    /**
     * Duerme al hilo actual hasta que lo despierten o hasta ese instante absoluto, en milisegundos
     * desde la epoca -- la misma escala de {@link System#currentTimeMillis}.
     *
     * <p>Esta escrito sobre {@link #parkNanos(long)} y no es un nativo aparte, y eso es a proposito:
     * un plazo absoluto **es** un plazo relativo calculado una vez. Escribirlo asi deja una sola
     * espera con plazo en la VM en vez de dos que podrian divergir.
     *
     * <p>Un plazo ya vencido retorna en el acto sin consumir el permiso.
     */
    public static void parkUntil(long deadline) {
        long resta = deadline - System.currentTimeMillis();
        if (resta > 0L) {
            LockSupport.parkNanos(resta * 1000000L);
        }
    }

    /**
     * Igual que {@link #parkUntil(long)}, con un bloqueador de diagnostico.
     *
     * <p>Ver {@link #park(Object)}: el bloqueador no se registra.
     */
    public static void parkUntil(Object blocker, long deadline) {
        long resta = deadline - System.currentTimeMillis();
        if (resta > 0L) {
            LockSupport.parkNanos(blocker, resta * 1000000L);
        }
    }

    /**
     * Anota el bloqueador del hilo actual. `null` lo borra.
     *
     * <p>Es lo que un `Lock` hace antes de estacionarse para que un volcado de hilos diga sobre
     * que se esta esperando.
     */
    public static void setCurrentBlocker(Object blocker) {
        Thread yo = Thread.currentThread();
        synchronized (BLOQUEADORES) {
            if (blocker == null) {
                BLOQUEADORES.remove(yo);
            } else {
                BLOQUEADORES.put(yo, blocker);
            }
        }
    }

    /**
     * El bloqueador anotado para `thread`, o `null` si no hay ninguno.
     *
     * <p>Es una **foto** y solo sirve para diagnosticar: para cuando la respuesta llegue, el hilo
     * puede haber dejado de esperar. El javadoc del JDK dice lo mismo.
     *
     * @throws NullPointerException si `thread` es `null`
     */
    public static Object getBlocker(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        Object b;
        synchronized (BLOQUEADORES) {
            b = BLOQUEADORES.get(thread);
        }
        return b;
    }
}
