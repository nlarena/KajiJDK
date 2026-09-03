package javax.security.auth;

import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

/**
 * KajiLibrary's javax.security.auth.Subject -- quien esta actuando: sus identidades y sus
 * credenciales.
 *
 * <p>Un Subject junta tres conjuntos y la distincion entre ellos es todo el punto de la clase:
 *
 * <ul>
 *   <li><b>principals</b>: las identidades. Un mismo Subject puede ser {@code cn=juan} en X.500,
 *       {@code juan@ACME} en Kerberos y {@code uid=1000} en el sistema, todo a la vez. No hay una
 *       "principal" -- son todas igual de validas y cada servicio mira la que entiende.
 *   <li><b>credenciales publicas</b>: lo que prueba una identidad y se puede mostrar. Un certificado.
 *   <li><b>credenciales privadas</b>: lo que prueba una identidad y <b>no</b> se puede mostrar. Una
 *       clave privada, un ticket. La separacion existe para que se les pueda pedir un permiso
 *       distinto al leerlas.
 * </ul>
 *
 * <h2>Los conjuntos son vistas vivas</h2>
 *
 * <p>{@code getPrincipals()} no devuelve una copia: devuelve el conjunto, y agregar ahi agrega al
 * Subject. Es a proposito -- es como se arma un Subject -- y trae dos comportamientos que sorprenden
 * y que se reproducen tal cual:
 *
 * <ul>
 *   <li>Meter algo que no es un {@code Principal} en el conjunto de principals tira
 *       {@code SecurityException}, no {@code ClassCastException}: el conjunto se defiende en
 *       ejecucion porque los genericos se borran y la lista puede llegar cruda.
 *   <li>El <b>constructor de cuatro</b>, en cambio, <b>no</b> chequea ese tipo. Tambien es del JDK, y
 *       por lo mismo: el constructor copia los elementos sin castearlos, asi que un conjunto crudo
 *       con basura adentro entra sin protestar. La diferencia entre las dos puertas es una rareza
 *       heredada, no un descuido de esta implementacion.
 * </ul>
 *
 * <p>{@code setReadOnly()} es de ida sola: no hay como volver. Sin eso, pasar un Subject a codigo
 * ajeno seria pasarle permiso de agregarse identidades.
 *
 * <h2>current(), callAs() y doAs()</h2>
 *
 * <p>{@code callAs} ata el Subject al hilo mientras corre la accion, y {@code current()} lo devuelve.
 * El JDK usa un {@code ScopedValue}; aca es un {@code ThreadLocal} que se restaura en un
 * {@code finally}, lo que da el mismo comportamiento observable en un hilo -- incluido el anidado,
 * donde el de adentro tapa al de afuera y al salir vuelve el de afuera --. <b>La diferencia
 * anotada</b>: un {@code ScopedValue} se hereda en los hilos que arranca la concurrencia
 * estructurada y un {@code ThreadLocal} no, asi que un hilo lanzado adentro de un {@code callAs} ve
 * {@code null} donde el JDK podria mostrarle el Subject.
 *
 * <p>{@code doAs} y {@code doAsPrivileged} estan desaconsejados en el JDK y marcados para
 * eliminacion, pero <b>siguen funcionando</b> y siguen atando el Subject igual que {@code callAs};
 * lo unico que cambia es como se envuelven las excepciones. {@code getSubject(AccessControlContext)}
 * es el unico que ya no anda: tira {@code UnsupportedOperationException}, porque su contrato era
 * leer el Subject de un contexto de control de acceso y ese mecanismo ya no existe.
 */
public final class Subject implements Serializable {

    private static final long serialVersionUID = -8308522755600156056L;

    // Un ThreadLocal y no un ScopedValue: ver la nota de la clase sobre la diferencia.
    private static final ThreadLocal<Subject> ACTUAL = new ThreadLocal<Subject>();

    // Listas y no conjuntos: el orden de insercion es el que sale por `toString()`, y la igualdad de
    // los elementos la decide `equals` de cada uno, no un hash.
    private final List<Object> principals = new ArrayList<Object>();
    private final List<Object> pubCredentials = new ArrayList<Object>();
    private final List<Object> privCredentials = new ArrayList<Object>();

    private volatile boolean readOnly = false;

    /** Un Subject vacio y modificable. */
    public Subject() {
    }

    /**
     * Un Subject con esos tres conjuntos, copiados.
     *
     * <p>Los conjuntos se copian: cambiarlos despues no cambia el Subject. Los elementos <b>no</b> se
     * chequean -- ver la nota de la clase --, pero ninguno puede ser null.
     *
     * @throws NullPointerException si algun conjunto o algun elemento es null
     */
    public Subject(boolean readOnly, Set<? extends Principal> principals,
            Set<?> pubCredentials, Set<?> privCredentials) {
        copy(principals, this.principals);
        copy(pubCredentials, this.pubCredentials);
        copy(privCredentials, this.privCredentials);
        this.readOnly = readOnly;
    }

    private static void copy(Collection<?> from, List<Object> into) {
        if (from == null) {
            throw new NullPointerException("invalid null input(s)");
        }
        Iterator<?> it = from.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o == null) {
                throw new NullPointerException("invalid null input(s)");
            }
            if (!into.contains(o)) {
                into.add(o);
            }
        }
    }

    /** Congela el Subject. No hay vuelta atras. */
    public void setReadOnly() {
        this.readOnly = true;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    /** Las identidades. Vista viva: agregar aca agrega al Subject. */
    public Set<Principal> getPrincipals() {
        return new SecureSet<Principal>(this, this.principals, true);
    }

    /** Las credenciales publicas. Vista viva. */
    public Set<Object> getPublicCredentials() {
        return new SecureSet<Object>(this, this.pubCredentials, false);
    }

    /** Las credenciales privadas. Vista viva. */
    public Set<Object> getPrivateCredentials() {
        return new SecureSet<Object>(this, this.privCredentials, false);
    }

    /**
     * Las identidades que son de esa clase o de una subclase. <b>Copia</b>, a diferencia de
     * {@link #getPrincipals()}: agregar a lo que sale de aca no agrega al Subject.
     *
     * @throws NullPointerException si la clase es null
     */
    public <T extends Principal> Set<T> getPrincipals(Class<T> c) {
        return filterByClass(this.principals, c);
    }

    /** Las credenciales publicas de esa clase o subclase. Copia. */
    public <T> Set<T> getPublicCredentials(Class<T> c) {
        return filterByClass(this.pubCredentials, c);
    }

    /** Las credenciales privadas de esa clase o subclase. Copia. */
    public <T> Set<T> getPrivateCredentials(Class<T> c) {
        return filterByClass(this.privCredentials, c);
    }

    private static <T> Set<T> filterByClass(List<Object> list, Class<T> c) {
        if (c == null) {
            throw new NullPointerException("invalid null Class provided");
        }
        Set<T> out = new HashSet<T>();
        synchronized (list) {
            int i = 0;
            while (i < list.size()) {
                Object o = list.get(i);
                if (c.isInstance(o)) {
                    out.add(c.cast(o));
                }
                i = i + 1;
            }
        }
        return out;
    }

    /** El Subject atado al hilo actual, o null si no hay ninguno. */
    public static Subject current() {
        return ACTUAL.get();
    }

    /**
     * Corre la accion con este Subject atado al hilo.
     *
     * <p>Envuelve <b>cualquier</b> excepcion de la accion en {@code CompletionException}, las de
     * ejecucion incluidas. Es lo que hace el JDK y hay que tenerlo presente: un
     * {@code IllegalStateException} que adentro se veia sale de aca como otra cosa.
     *
     * @throws NullPointerException si la accion es null
     */
    public static <T> T callAs(Subject subject, Callable<T> action) throws CompletionException {
        if (action == null) {
            throw new NullPointerException();
        }
        Subject anterior = ACTUAL.get();
        ACTUAL.set(subject);
        try {
            return action.call();
        } catch (Exception e) {
            throw new CompletionException(e);
        } finally {
            // En un `finally` y restaurando el anterior --no borrando--: es lo que hace que anidar
            // dos `callAs` deje el de afuera intacto al volver del de adentro.
            restore(anterior);
        }
    }

    /**
     * Corre la accion con este Subject atado al hilo.
     *
     * @deprecated el mecanismo de control de acceso al que pertenecia ya no existe; usar
     *     {@link #callAs}. Sigue funcionando y sigue atando el Subject.
     */
    @Deprecated
    public static <T> T doAs(Subject subject, PrivilegedAction<T> action) {
        if (action == null) {
            throw new NullPointerException("invalid null action provided");
        }
        Subject anterior = ACTUAL.get();
        ACTUAL.set(subject);
        try {
            return action.run();
        } finally {
            restore(anterior);
        }
    }

    /**
     * Idem, para una accion que puede lanzar.
     *
     * <p>Envuelve solo las excepciones <b>declaradas</b>: un {@code RuntimeException} sale tal cual.
     * Es la diferencia con {@link #callAs}, que envuelve todo.
     *
     * @deprecated ver {@link #doAs(Subject, PrivilegedAction)}
     */
    @Deprecated
    public static <T> T doAs(Subject subject, PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException {
        if (action == null) {
            throw new NullPointerException("invalid null action provided");
        }
        Subject anterior = ACTUAL.get();
        ACTUAL.set(subject);
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        } finally {
            restore(anterior);
        }
    }

    /**
     * Igual que {@link #doAs(Subject, PrivilegedAction)}.
     *
     * <p>El {@code AccessControlContext} se ignora, y no es un atajo de esta implementacion: sin
     * gestor de seguridad no hay contexto que combinar, asi que en el JDK tambien deja de importar.
     *
     * @deprecated ver {@link #doAs(Subject, PrivilegedAction)}
     */
    @Deprecated
    public static <T> T doAsPrivileged(Subject subject, PrivilegedAction<T> action,
            AccessControlContext acc) {
        return doAs(subject, action);
    }

    /**
     * Igual que {@link #doAs(Subject, PrivilegedExceptionAction)}.
     *
     * @deprecated ver {@link #doAs(Subject, PrivilegedAction)}
     */
    @Deprecated
    public static <T> T doAsPrivileged(Subject subject, PrivilegedExceptionAction<T> action,
            AccessControlContext acc) throws PrivilegedActionException {
        return doAs(subject, action);
    }

    private static void restore(Subject anterior) {
        if (anterior == null) {
            ACTUAL.remove();
        } else {
            ACTUAL.set(anterior);
        }
    }

    /**
     * No se puede: el contrato de este metodo era leer el Subject de un contexto de control de
     * acceso, y ese mecanismo ya no existe.
     *
     * <p>Lanza en vez de devolver null a proposito. Null se leeria como "no hay ningun Subject
     * actuando", que es una respuesta -- y la equivocada. Usar {@link #current()}.
     *
     * @throws UnsupportedOperationException siempre
     * @deprecated no tiene reemplazo directo; usar {@link #current()}
     */
    @Deprecated
    public static Subject getSubject(AccessControlContext acc) {
        throw new UnsupportedOperationException("getSubject is not supported");
    }

    /** Dos Subjects son el mismo si tienen los mismos tres conjuntos. */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Subject)) {
            return false;
        }
        Subject other = (Subject) o;
        return sameElements(this.principals, other.principals)
            && sameElements(this.pubCredentials, other.pubCredentials)
            && sameElements(this.privCredentials, other.privCredentials);
    }

    private static boolean sameElements(List<Object> a, List<Object> b) {
        if (a.size() != b.size()) {
            return false;
        }
        int i = 0;
        while (i < a.size()) {
            if (!b.contains(a.get(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * XOR de los hash de los elementos de los tres conjuntos.
     *
     * <p>Tiene que ser un XOR y no una suma con posicion: {@code equals} no mira el orden, asi que el
     * hash tampoco puede.
     */
    @Override
    public int hashCode() {
        return hashOf(this.principals) ^ hashOf(this.pubCredentials) ^ hashOf(this.privCredentials);
    }

    private static int hashOf(List<Object> list) {
        int h = 0;
        int i = 0;
        while (i < list.size()) {
            h = h ^ list.get(i).hashCode();
            i = i + 1;
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Subject:\n");
        listEach(sb, this.principals, "\tPrincipal: ");
        listEach(sb, this.pubCredentials, "\tPublic Credential: ");
        listEach(sb, this.privCredentials, "\tPrivate Credential: ");
        return sb.toString();
    }

    private static void listEach(StringBuilder sb, List<Object> list, String etiqueta) {
        int i = 0;
        while (i < list.size()) {
            sb.append(etiqueta).append(list.get(i)).append("\n");
            i = i + 1;
        }
    }

    /**
     * La vista viva de uno de los tres conjuntos.
     *
     * <p>Hace dos cosas que un {@code Set} normal no: rechaza escrituras si el Subject esta
     * congelado, y --en el conjunto de identidades-- rechaza lo que no es un {@code Principal}. Las
     * dos con la excepcion que usa el JDK, que en el segundo caso no es la que uno esperaria.
     */
    private static final class SecureSet<E> extends AbstractSet<E> {

        private final Subject owner;
        private final List<Object> list;
        private final boolean ofPrincipals;

        SecureSet(Subject owner, List<Object> list, boolean ofPrincipals) {
            this.owner = owner;
            this.list = list;
            this.ofPrincipals = ofPrincipals;
        }

        @Override
        public int size() {
            return this.list.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.list.contains(o);
        }

        @Override
        public boolean add(E o) {
            if (this.owner.readOnly) {
                throw new IllegalStateException("Subject is read-only");
            }
            if (o == null) {
                throw new NullPointerException("invalid null input(s)");
            }
            // SecurityException y no ClassCastException: es lo que tira el JDK. El chequeo existe
            // porque el generico se borra y la lista puede llegar cruda.
            if (this.ofPrincipals && !(o instanceof Principal)) {
                throw new SecurityException("attempting to add an object which is not an instance "
                    + "of java.security.Principal to a Subject's Principal Set");
            }
            if (this.list.contains(o)) {
                return false;
            }
            this.list.add(o);
            return true;
        }

        @Override
        public boolean remove(Object o) {
            if (this.owner.readOnly) {
                throw new IllegalStateException("Subject is read-only");
            }
            return this.list.remove(o);
        }

        @Override
        public void clear() {
            if (this.owner.readOnly) {
                throw new IllegalStateException("Subject is read-only");
            }
            this.list.clear();
        }

        @Override
        public Iterator<E> iterator() {
            final Iterator<Object> base = this.list.iterator();
            final Subject owner = this.owner;
            return new Iterator<E>() {
                public boolean hasNext() {
                    return base.hasNext();
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    return (E) base.next();
                }

                public void remove() {
                    // El chequeo va tambien aca y no solo en `remove(Object)`: quitar por el
                    // iterador es la otra puerta al mismo conjunto, y dejarla abierta haria que
                    // `setReadOnly()` no sirviera para nada.
                    if (owner.readOnly) {
                        throw new IllegalStateException("Subject is read-only");
                    }
                    base.remove();
                }
            };
        }
    }
}
