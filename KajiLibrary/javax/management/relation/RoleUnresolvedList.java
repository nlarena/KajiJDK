package javax.management.relation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Una lista de {@link RoleUnresolved}.
 *
 * <h2>Por que extiende {@code ArrayList<Object>} y no {@code ArrayList<RoleUnresolved>}</h2>
 *
 * <p>Por compatibilidad, y la historia se le nota. Nacio antes de los genericos como una lista sin
 * tipo; cuando llegaron, cambiarla a {@code ArrayList<RoleUnresolved>} habria roto todo el codigo que ya
 * la usaba. La salida fue dejarla sobre {@code Object} y agregar sobrecargas tipadas.
 *
 * <h2>El modo "tipado" y el modo "crudo"</h2>
 *
 * <p>De ahi el detalle que hay que conocer: una lista construida con el constructor de
 * {@code List<RoleUnresolved>} queda en modo <strong>tipado</strong> y rechaza cualquier cosa que no sea un
 * {@link RoleUnresolved}; una construida vacia acepta lo que sea hasta que alguien llame a
 * {@link #asList}, que es el metodo que la convierte.
 *
 * <p>Mezclar los dos modos es como se consigue una {@code ClassCastException} desde un lugar que no
 * la menciona.
 */
public class RoleUnresolvedList extends ArrayList<Object> {

    private static final long serialVersionUID = 4054902803091360389L;

    private transient boolean tipada = false;

    /** Vacia, en modo crudo. */
    public RoleUnresolvedList() {
        super();
    }

    /**
     * Vacia con esa capacidad, en modo crudo.
     *
     * @throws IllegalArgumentException si la capacidad es negativa
     */
    public RoleUnresolvedList(int initialCapacity) throws IllegalArgumentException {
        super(initialCapacity);
    }

    /**
     * Con esos elementos, en modo tipado.
     *
     * @throws IllegalArgumentException si la lista es {@code null}
     */
    public RoleUnresolvedList(List<RoleUnresolved> list) throws IllegalArgumentException {
        super(revisar(list));
        this.tipada = true;
    }

    private static List<RoleUnresolved> revisar(List<RoleUnresolved> list) {
        if (list == null) {
            throw new IllegalArgumentException("la lista no puede ser null");
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) {
                throw new IllegalArgumentException("un elemento es null");
            }
        }
        return list;
    }

    /**
     * Esta misma lista, vista como {@code List<RoleUnresolved>}, y la pasa a modo tipado.
     *
     * <p>Es una <strong>vista</strong>, no una copia: cambiarla cambia esta.
     *
     * @throws IllegalArgumentException si ya tiene algo que no es un {@link RoleUnresolved}
     */
    @SuppressWarnings("unchecked")
    public List<RoleUnresolved> asList() {
        if (!this.tipada) {
            for (int i = 0; i < size(); i++) {
                if (!(get(i) instanceof RoleUnresolved)) {
                    throw new IllegalArgumentException(
                            "la lista tiene un elemento que no es un RoleUnresolved");
                }
            }
            this.tipada = true;
        }
        return (List<RoleUnresolved>) (List<?>) this;
    }

    /** Agrega al final. */
    public void add(RoleUnresolved element) throws IllegalArgumentException {
        if (element == null) {
            throw new IllegalArgumentException("el elemento no puede ser null");
        }
        super.add(element);
    }

    /** Inserta en esa posicion. */
    public void add(int index, RoleUnresolved element)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        if (element == null) {
            throw new IllegalArgumentException("el elemento no puede ser null");
        }
        super.add(index, element);
    }

    /** Reemplaza el de esa posicion. */
    public void set(int index, RoleUnresolved element)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        if (element == null) {
            throw new IllegalArgumentException("el elemento no puede ser null");
        }
        super.set(index, element);
    }

    /** Agrega todos al final. */
    public boolean addAll(RoleUnresolvedList list) throws IndexOutOfBoundsException {
        if (list == null) {
            return true;
        }
        return super.addAll(list);
    }

    /** Los inserta en esa posicion. */
    public boolean addAll(int index, RoleUnresolvedList list)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        if (list == null) {
            throw new IllegalArgumentException("la lista no puede ser null");
        }
        return super.addAll(index, list);
    }

    /**
     * Agrega, rechazando lo que no sea un {@link RoleUnresolved} si la lista es tipada.
     *
     * @throws IllegalArgumentException si es tipada y el elemento no corresponde
     */
    public boolean add(Object o) {
        revisarTipo(o);
        return super.add(o);
    }

    /** Inserta, con la misma comprobacion. */
    public void add(int index, Object o) {
        revisarTipo(o);
        super.add(index, o);
    }

    /** Agrega todos, con la misma comprobacion. */
    public boolean addAll(Collection<?> c) {
        for (Object o : c) {
            revisarTipo(o);
        }
        return super.addAll(c);
    }

    /** Los inserta, con la misma comprobacion. */
    public boolean addAll(int index, Collection<?> c) {
        for (Object o : c) {
            revisarTipo(o);
        }
        return super.addAll(index, c);
    }

    /** Reemplaza, con la misma comprobacion. */
    public Object set(int index, Object o) {
        revisarTipo(o);
        return super.set(index, o);
    }

    /**
     * La comprobacion que separa los dos modos.
     *
     * <p>Solo mira cuando la lista es tipada: en modo crudo se acepta cualquier cosa, que es lo que
     * hacia antes de los genericos y lo que el codigo viejo espera.
     */
    private void revisarTipo(Object o) {
        if (this.tipada && !(o instanceof RoleUnresolved)) {
            throw new IllegalArgumentException(
                    "esta lista es de RoleUnresolved y el elemento no lo es");
        }
    }
}
