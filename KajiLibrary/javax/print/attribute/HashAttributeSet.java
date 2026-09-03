package javax.print.attribute;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * KajiLibrary's javax.print.attribute.HashAttributeSet -- la implementacion de referencia de
 * {@link AttributeSet}, y la unica que trae el paquete.
 *
 * <h2>Es un mapa con la clave metida adentro del valor</h2>
 *
 * <p>Adentro hay un {@code HashMap} de categoria a atributo, y esa eleccion es toda la clase: la
 * regla de "un atributo por categoria" no se implementa, se hereda de que un mapa tiene una entrada
 * por clave. {@code add} es un {@code put} bajo la clave {@code attribute.getCategory()}, asi que
 * meter un segundo atributo de la misma categoria pisa al primero sin que haya que buscarlo.
 *
 * <p>Lo que si hay que decidir es el **valor de retorno**: {@code add} devuelve si el conjunto
 * cambio, y eso no es "si habia algo antes" sino "si lo que hay ahora es distinto de lo que habia".
 * Por eso se compara el atributo nuevo con el viejo con {@code equals} y no se mira si el
 * {@code put} devolvio null. Agregar {@code new Copies(3)} donde ya estaba {@code new Copies(3)}
 * devuelve {@code false} aunque sean dos objetos distintos.
 *
 * <h2>El segundo campo, {@code myInterface}, es lo que hace utiles a las subclases</h2>
 *
 * <p>Cada conjunto recuerda **de que interfaz** tienen que ser sus miembros. Para un
 * {@code HashAttributeSet} pelado es {@code Attribute.class} y no restringe nada; las cuatro
 * subclases ({@link HashDocAttributeSet} y companeras) pasan {@code DocAttribute.class} y demas, y
 * con eso la restriccion de categoria sale gratis: {@code add} pasa por
 * {@link AttributeSetUtilities#verifyAttributeValue} contra esa interfaz y lo que no encaja sale
 * por {@code ClassCastException}.
 *
 * <p>Notar la asimetria, que es del JDK y se replica: {@code add} verifica contra
 * {@code myInterface} (la restriccion de la subclase) pero {@code get}, {@code remove} y
 * {@code containsKey} verifican contra {@code Attribute.class} a secas. O sea que a un
 * {@code HashDocAttributeSet} se le puede **preguntar** por una categoria que nunca podria
 * contener --devuelve null-- pero no se le puede meter.
 *
 * <h2>Igualdad</h2>
 *
 * <p>{@code equals} acepta cualquier {@link AttributeSet}, no solo otro {@code HashAttributeSet}:
 * compara tamano y despues pregunta por cada atributo con {@code containsValue}. Es lo que permite
 * que dos implementaciones distintas de la interfaz se comparen entre si. El hash es la **suma**
 * de los hashes de los atributos, que es lo unico que puede ser porque el orden no esta definido.
 *
 * <h2>Lo que quedo afuera</h2>
 *
 * <p>Los metodos {@code private writeObject}/{@code readObject} de la serializacion. Son la unica
 * parte de esta clase que no se puede escribir con honestidad aca: piden
 * {@code java.io.ObjectOutputStream} y {@code ObjectInputStream}, que KajiLibrary no tiene --de
 * {@code java.io} solo estan las **interfaces** {@code ObjectInput}/{@code ObjectOutput}--. No son
 * API publica y no cuentan en la superficie; el campo del mapa igual se declara
 * {@code transient}, que es la mitad del contrato que si se puede sostener.
 */
public class HashAttributeSet implements AttributeSet, Serializable {

    private static final long serialVersionUID = 5311560590283707917L;

    // La interfaz de la que tienen que ser instancia todos los miembros. Attribute.class para esta
    // clase; una subinterfaz para cada subclase.
    private Class<?> myInterface;

    // transient porque la forma serializada del JDK escribe los atributos uno por uno, no el mapa.
    // Aca no hay quien la escriba (ver la cabecera), pero el campo es el mismo.
    private transient HashMap<Class<?>, Attribute> attrMap = new HashMap<Class<?>, Attribute>();

    /** Un conjunto vacio, sin restriccion de categoria mas alla de ser atributos. */
    public HashAttributeSet() {
        this(Attribute.class);
    }

    /** Con un atributo adentro. NullPointerException si es null. */
    public HashAttributeSet(Attribute attribute) {
        this(attribute, Attribute.class);
    }

    /**
     * Con los atributos del arreglo, agregados en orden desde el indice 0 -- asi que si el arreglo
     * trae dos de la misma categoria gana el ultimo. Un arreglo null da el conjunto vacio.
     */
    public HashAttributeSet(Attribute[] attributes) {
        this(attributes, Attribute.class);
    }

    /** Con los atributos de otro conjunto. Un conjunto null da el conjunto vacio. */
    public HashAttributeSet(AttributeSet attributes) {
        this(attributes, Attribute.class);
    }

    /** Vacio y restringido a `interfaceName`. NullPointerException si `interfaceName` es null. */
    protected HashAttributeSet(Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        this.myInterface = interfaceName;
    }

    /** Con un atributo, restringido. ClassCastException si el atributo no es de la interfaz. */
    protected HashAttributeSet(Attribute attribute, Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        this.myInterface = interfaceName;
        add(attribute);
    }

    /** Con un arreglo, restringido. */
    protected HashAttributeSet(Attribute[] attributes, Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        this.myInterface = interfaceName;
        int n = (attributes == null) ? 0 : attributes.length;
        for (int i = 0; i < n; i++) {
            add(attributes[i]);
        }
    }

    /**
     * Con otro conjunto, restringido.
     *
     * <p>Este es el unico de los cuatro que **no** rechaza un `interfaceName` null, igual que en el
     * JDK: si ademas `attributes` es null o vacio, el conjunto queda armado con `myInterface` en
     * null y el primer `add` revienta con NullPointerException recien ahi. Es una inconsistencia
     * del original, no una simplificacion nuestra, y se replica porque es observable.
     */
    protected HashAttributeSet(AttributeSet attributes, Class<?> interfaceName) {
        this.myInterface = interfaceName;
        if (attributes != null) {
            Attribute[] attribArray = attributes.toArray();
            int n = (attribArray == null) ? 0 : attribArray.length;
            for (int i = 0; i < n; i++) {
                add(attribArray[i]);
            }
        }
    }

    /**
     * El atributo de esa categoria, o null.
     *
     * <p>Verifica contra `Attribute.class`, no contra `myInterface`: se puede preguntar por
     * cualquier categoria de atributo aunque este conjunto no la pueda contener.
     */
    public Attribute get(Class<?> category) {
        return this.attrMap.get(
                AttributeSetUtilities.verifyAttributeCategory(category, Attribute.class));
    }

    /**
     * Agrega, reemplazando al de la misma categoria.
     *
     * <p>Devuelve si el conjunto **cambio**, o sea si el atributo nuevo no es igual al que estaba;
     * no si habia algo. Ver la cabecera.
     */
    public boolean add(Attribute attribute) {
        Object oldAttribute = this.attrMap.put(attribute.getCategory(),
                AttributeSetUtilities.verifyAttributeValue(attribute, this.myInterface));
        return !attribute.equals(oldAttribute);
    }

    /** Saca el de esa categoria. Una categoria null no es un error: no hace nada y da false. */
    public boolean remove(Class<?> category) {
        return category != null
                && AttributeSetUtilities.verifyAttributeCategory(category, Attribute.class) != null
                && this.attrMap.remove(category) != null;
    }

    /**
     * Saca ese atributo.
     *
     * <p>Ojo con la letra chica, que es la del JDK: saca **por categoria**, sin comparar el valor.
     * `remove(new Copies(3))` sobre un conjunto que tiene `new Copies(5)` saca las cinco copias y
     * devuelve true. Un atributo null no es un error: da false.
     */
    public boolean remove(Attribute attribute) {
        return attribute != null && this.attrMap.remove(attribute.getCategory()) != null;
    }

    /** Si hay algo de esa categoria. Una categoria null da false. */
    public boolean containsKey(Class<?> category) {
        return category != null
                && AttributeSetUtilities.verifyAttributeCategory(category, Attribute.class) != null
                && this.attrMap.get(category) != null;
    }

    /** Si ese atributo exacto (por equals) esta. Aca si se compara el valor. */
    public boolean containsValue(Attribute attribute) {
        return attribute != null && attribute.equals(this.attrMap.get(attribute.getCategory()));
    }

    /**
     * Agrega todos, con la misma regla de reemplazo.
     *
     * <p>Devuelve true si cambio por lo menos uno. Si tira a mitad de camino, los que ya entraron
     * quedan adentro: no hay transaccion, igual que en el JDK.
     */
    public boolean addAll(AttributeSet attributes) {
        Attribute[] attrs = attributes.toArray();
        boolean result = false;
        for (int i = 0; i < attrs.length; i++) {
            Attribute newValue =
                    AttributeSetUtilities.verifyAttributeValue(attrs[i], this.myInterface);
            Object oldValue = this.attrMap.put(newValue.getCategory(), newValue);
            result = (!newValue.equals(oldValue)) || result;
        }
        return result;
    }

    public int size() {
        return this.attrMap.size();
    }

    /**
     * Los atributos en un arreglo nuevo, sin orden definido.
     *
     * <p>El JDK escribe `attrMap.values().toArray(attrs)`; aca se recorre con el iterador para no
     * depender de `Collection.toArray(T[])`. Mismo resultado.
     */
    public Attribute[] toArray() {
        Attribute[] attrs = new Attribute[size()];
        int i = 0;
        Iterator<Attribute> it = this.attrMap.values().iterator();
        while (it.hasNext() && i < attrs.length) {
            attrs[i] = it.next();
            i++;
        }
        return attrs;
    }

    public void clear() {
        this.attrMap.clear();
    }

    public boolean isEmpty() {
        return this.attrMap.isEmpty();
    }

    /**
     * Igual a cualquier {@link AttributeSet} con los mismos pares categoria-valor.
     *
     * <p>No pide que el otro sea un HashAttributeSet: pregunta por la interfaz, que es lo que hace
     * que dos implementaciones distintas se puedan comparar.
     */
    public boolean equals(Object object) {
        if (!(object instanceof AttributeSet)) {
            return false;
        }
        AttributeSet aset = (AttributeSet) object;
        if (aset.size() != size()) {
            return false;
        }
        Attribute[] attrs = toArray();
        for (int i = 0; i < attrs.length; i++) {
            if (!aset.containsValue(attrs[i])) {
                return false;
            }
        }
        return true;
    }

    /** La suma de los hashes. Es lo unico consistente con un orden no definido. */
    public int hashCode() {
        int hcode = 0;
        Attribute[] attrs = toArray();
        for (int i = 0; i < attrs.length; i++) {
            hcode += attrs[i].hashCode();
        }
        return hcode;
    }
}
