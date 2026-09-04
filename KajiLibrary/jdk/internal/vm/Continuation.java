package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.Continuation — una continuación delimitada (Project Loom).
 *
 * <p>Una continuación es una computación que se puede **suspender a la mitad y reanudar después**,
 * quizás en otro hilo. Es el sustrato de los hilos virtuales: suspender uno es guardar su
 * continuación, y reanudarlo es correrla.
 *
 * <h2>Acá es una continuación que nunca se suspende, y eso es un estado legítimo</h2>
 *
 * <p>Suspender exige que la VM levante los cuadros de la pila y los guarde en el montón. Esta VM no
 * sabe hacerlo (ver {@link ContinuationSupport}), así que **toda continuación está permanentemente
 * clavada**. La palabra es del JDK: una continuación *pinned* es una que no se puede suspender ahora.
 *
 * <p>Lo importante es que el JDK **ya define qué pasa en ese caso**, porque en HotSpot también ocurre
 * —dentro de un bloque sincronizado, o con un cuadro nativo en el medio—. Así que no hay que inventar
 * ningún comportamiento: se usa el que ya está especificado.
 *
 * <ul>
 * <li>{@link #run()} corre el objetivo **hasta el final**. Es exactamente lo que pasa cuando ningún
 *     {@code yield} tiene éxito: la computación no se corta, sigue hasta terminar.</li>
 * <li>{@link #yield} devuelve `false`, que es su forma documentada de decir "no se pudo".</li>
 * <li>{@link #isPinned} devuelve `true`, y {@link #onPinned} recibe {@link Pinned#NATIVE}: el motivo
 *     es que hay un cuadro que no se puede levantar.</li>
 * <li>{@link #tryPreempt} devuelve {@link PreemptStatus#PERM_FAIL_UNSUPPORTED} — la constante que el
 *     JDK tiene justo para esto.</li>
 * </ul>
 *
 * <p>Un usuario que llame `run()` y no use `yield` obtiene el resultado correcto. Uno que dependa de
 * suspender recibe un `false` que puede mirar, en vez de una suspensión que no ocurrió.
 *
 * <h2>Lo que queda afuera</h2>
 *
 * <p>{@code getStackTrace()}, los tres {@code stackWalker(...)} y {@code wrapWalk(...)}. Los cinco
 * entregan **los cuadros de la continuación**, que es justamente lo que no existe: sin pila guardada
 * en el montón no hay qué recorrer. Devolver la pila del hilo actual sería peor que no estar — se
 * leería como la de la continuación y apuntaría a otro lado.
 */
public class Continuation {

    /**
     * Por qué una continuación no se pudo suspender.
     *
     * <p>Los cuatro motivos son de la VM y no del programa, y por eso el que llama no puede
     * "arreglarlos": lo único que puede hacer es no contar con la suspensión.
     */
    public enum Pinned {
        /** Hay un cuadro nativo en la pila; no se puede levantar. */
        NATIVE,
        /** Se está adentro de un bloque sincronizado. */
        MONITOR,
        /** Se está en una sección crítica de la VM. */
        CRITICAL_SECTION,
        /** Se estaba desenrollando una excepción. */
        EXCEPTION
    }

    /** El resultado de intentar desalojar una continuación. */
    public enum PreemptStatus {
        /** Se pudo. */
        SUCCESS(null),
        /** No se puede, y no va a poder: esta VM no soporta desalojo. */
        PERM_FAIL_UNSUPPORTED(null),
        /** No se puede porque ya está suspendiéndose. */
        PERM_FAIL_YIELDING(null),
        /** No se puede porque no está montada en ningún hilo. */
        PERM_FAIL_NOT_MOUNTED(null),
        /** Ahora no: está en una sección crítica. */
        TRANSIENT_FAIL_PINNED_CRITICAL_SECTION(Pinned.CRITICAL_SECTION),
        /** Ahora no: hay un cuadro nativo. */
        TRANSIENT_FAIL_PINNED_NATIVE(Pinned.NATIVE),
        /** Ahora no: hay un monitor tomado. */
        TRANSIENT_FAIL_PINNED_MONITOR(Pinned.MONITOR);

        private final Pinned motivo;

        PreemptStatus(Pinned motivo) {
            this.motivo = motivo;
        }

        /**
         * El motivo de clavado, o `null` si el fallo no fue por estar clavada.
         *
         * <p>Los `PERM_FAIL_*` devuelven `null` y los `TRANSIENT_FAIL_PINNED_*` el motivo. La
         * diferencia es la que le importa a quien reintenta: un fallo transitorio puede desaparecer,
         * uno permanente no.
         */
        public Pinned pinned() {
            return this.motivo;
        }
    }

    // La pila de continuaciones montadas en el hilo actual. Un `ThreadLocal` y no un campo de
    // `Thread`, por lo mismo que en `StackableScope`: no se puede tocar `Thread` desde este paquete y
    // la semantica es identica.
    private static final ThreadLocal<Continuation> MONTADA = new ThreadLocal<Continuation>();

    private final ContinuationScope scope;
    private final Runnable target;
    private Continuation padre;
    private boolean terminada;

    public Continuation(ContinuationScope scope, Runnable target) {
        this.scope = scope;
        this.target = target;
    }

    /** El ámbito que delimita esta continuación. */
    public ContinuationScope getScope() {
        return this.scope;
    }

    /** La continuación que la encierra en este hilo, o `null`. */
    public Continuation getParent() {
        return this.padre;
    }

    /** Si ya terminó. */
    public boolean isDone() {
        return this.terminada;
    }

    /**
     * Si fue desalojada.
     *
     * <p>Siempre `false`: el desalojo necesita suspender, y acá no se puede. Es consistente con que
     * {@link #tryPreempt} nunca devuelva {@link PreemptStatus#SUCCESS}.
     */
    public boolean isPreempted() {
        return false;
    }

    /**
     * Corre la continuación hasta que se suspenda o termine — acá, siempre hasta que termine.
     *
     * <p>Es `final` como en el JDK: el ciclo de montar, correr y desmontar no se puede redefinir a
     * medias sin romper la invariante de qué continuación está montada en el hilo. Lo que sí se
     * redefine son los ganchos {@link #onContinue} y {@link #onPinned}.
     *
     * <p>El desmontaje va en un `finally`: si el objetivo tira, la continuación tiene que salir de la
     * pila del hilo igual, o el hilo queda creyendo que sigue adentro de algo que ya explotó.
     *
     * @throws IllegalStateException si ya terminó
     */
    public final void run() {
        if (this.terminada) {
            throw new IllegalStateException("esta continuacion ya termino");
        }
        this.padre = Continuation.MONTADA.get();
        Continuation.MONTADA.set(this);
        this.onContinue();
        try {
            if (this.target != null) {
                this.target.run();
            }
        } finally {
            Continuation.MONTADA.set(this.padre);
            this.terminada = true;
        }
    }

    /** Aviso de que la continuación arranca o se reanuda. Para redefinir. */
    protected void onContinue() {
    }

    /** Aviso de que un {@link #yield} falló por estar clavada. Para redefinir. */
    protected void onPinned(Pinned reason) {
    }

    /**
     * Intenta suspender la continuación del ámbito indicado.
     *
     * <p>Devuelve `false` **siempre** en esta VM, y avisa por {@link #onPinned} con
     * {@link Pinned#NATIVE}. El `false` no es un error: es la respuesta que el contrato define para
     * cuando no se pudo, y la que ocurre en HotSpot cada vez que hay un cuadro nativo en el medio.
     */
    public static boolean yield(ContinuationScope scope) {
        Continuation actual = Continuation.MONTADA.get();
        if (actual != null) {
            actual.onPinned(Pinned.NATIVE);
        }
        return false;
    }

    /**
     * Si la continuación de ese ámbito está clavada.
     *
     * <p>Siempre `true`. Preguntarlo antes de intentar suspender es el uso normal, y acá la respuesta
     * evita el intento.
     */
    public static boolean isPinned(ContinuationScope scope) {
        return true;
    }

    /** La continuación montada de ese ámbito en el hilo actual, o `null`. */
    public static Continuation getCurrentContinuation(ContinuationScope scope) {
        Continuation c = Continuation.MONTADA.get();
        while (c != null) {
            if (scope == null || scope.equals(c.scope)) {
                return c;
            }
            c = c.padre;
        }
        return null;
    }

    /**
     * Intenta desalojar la continuación que corre en ese hilo.
     *
     * @return siempre {@link PreemptStatus#PERM_FAIL_UNSUPPORTED} en esta VM
     */
    public PreemptStatus tryPreempt(Thread thread) {
        return PreemptStatus.PERM_FAIL_UNSUPPORTED;
    }

    /**
     * Impide suspender hasta el {@link #unpin} correspondiente.
     *
     * <p>No hace nada, y es lo correcto: donde nada se puede suspender, no hay nada que impedir.
     *
     * <p><strong>Acá no es `native`, y el JDK sí lo declara así.</strong> Un `native` sin
     * implementación registrada en esta VM no tira una excepción — voltea el proceso. Un método vacío
     * hace lo mismo que el del JDK haría (nada observable); uno `native` mataría al que lo llame.
     */
    public static void pin() {
    }

    /** Lo simétrico de {@link #pin}, y por lo mismo tampoco hace nada. */
    public static void unpin() {
    }
}
