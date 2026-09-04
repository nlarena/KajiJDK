package javax.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Una lista de {@link Attribute}, que hereda de `ArrayList&lt;Object&gt;` y no de
 * `ArrayList&lt;Attribute&gt;`.
 *
 * <p>Esa herencia rara es una cicatriz de compatibilidad, no un descuido. La clase es de 1999 y
 * cuando llegaron los genericos ya habia codigo que metia cualquier cosa adentro; parametrizarla
 * con `Attribute` habria roto ese codigo al recompilarlo. La salida del JDK fue dejarla en `Object`
 * y agregar {@link #asList()}, que es la vista tipada y la que conviene usar.
 *
 * <p>El precio: {@code add(Object)} acepta lo que sea. El JDK marca la lista como "contaminada"
 * cuando eso pasa y {@link #asList()} deja de andar; aca se hace lo mismo.
 */
public class AttributeList extends ArrayList<Object> {

    private static final long serialVersionUID = -4077085769279709076L;

    private transient volatile boolean tipada;

    private transient volatile boolean contaminada;

    /** Vacia. */
    public AttributeList() {
        super();
    }

    /** Vacia, con capacidad reservada. */
    public AttributeList(int initialCapacity) {
        super(initialCapacity);
    }

    /** Copia de otra. */
    public AttributeList(AttributeList list) {
        super(list);
    }

    /** Desde una lista ya tipada; la marca como tipada de entrada. */
    public AttributeList(List<Attribute> list) {
        if (list == null) {
            throw new IllegalArgumentException("Null parameter");
        }
        Iterator<Attribute> it = list.iterator();
        while (it.hasNext()) {
            Attribute a = it.next();
            if (a == null) {
                throw new IllegalArgumentException("Null attribute in list");
            }
            super.add(a);
        }
        tipada = true;
    }

    /**
     * La vista tipada.
     *
     * <p>Es una <b>vista</b>, no una copia: agregar por aca agrega alla. Y a partir de la primera
     * llamada la lista queda marcada como tipada, asi que un `add(Object)` posterior con algo que no
     * sea un `Attribute` es un error.
     *
     * @throws IllegalArgumentException si ya se le metio algo que no es un `Attribute`
     */
    @SuppressWarnings("unchecked")
    public List<Attribute> asList() {
        tipada = true;
        if (contaminada) {
            tipada = false;
            throw new IllegalArgumentException("AttributeList contains non-Attribute objects");
        }
        return (List<Attribute>) (List<?>) this;
    }

    /** Agrega al final. */
    public void add(Attribute object) {
        super.add(object);
    }

    /** Inserta en la posicion dada. */
    public void add(int index, Attribute object) {
        super.add(index, object);
    }

    /** Reemplaza la posicion dada. */
    public void set(int index, Attribute object) {
        super.set(index, object);
    }

    /** Agrega todos al final. */
    public boolean addAll(AttributeList list) {
        return super.addAll(list);
    }

    /** Inserta todos a partir de la posicion dada. */
    public boolean addAll(int index, AttributeList list) {
        return super.addAll(index, list);
    }

    /**
     * @throws IllegalArgumentException si la lista ya se declaro tipada y esto no es un `Attribute`
     */
    public boolean add(Object element) {
        revisar(element);
        return super.add(element);
    }

    /** Ver {@link #add(Object)}. */
    public void add(int index, Object element) {
        revisar(element);
        super.add(index, element);
    }

    /** Ver {@link #add(Object)}. */
    public boolean addAll(Collection<?> c) {
        revisar(c);
        return super.addAll(c);
    }

    /** Ver {@link #add(Object)}. */
    public boolean addAll(int index, Collection<?> c) {
        revisar(c);
        return super.addAll(index, c);
    }

    /** Ver {@link #add(Object)}. */
    public Object set(int index, Object element) {
        revisar(element);
        return super.set(index, element);
    }

    private void revisar(Object x) {
        if (x instanceof Attribute) {
            return;
        }
        if (tipada) {
            throw new IllegalArgumentException("Not an Attribute: " + x);
        }
        contaminada = true;
    }

    private void revisar(Collection<?> c) {
        if (c == null) {
            return;
        }
        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            revisar(it.next());
        }
    }
}
