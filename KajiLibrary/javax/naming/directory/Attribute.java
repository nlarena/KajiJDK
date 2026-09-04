package javax.naming.directory;

import java.io.Serializable;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.Attribute -- un atributo de una entrada del directorio.
 *
 * <p>Un identificador y <b>cero o mas</b> valores. Que sean varios es lo primero que sorprende: en un
 * directorio, {@code telefono} puede tener tres numeros, y no hay nada que distinga "el atributo" de
 * "la lista de valores del atributo". Por eso {@link #get()} sin indice devuelve <b>alguno</b> y no
 * "el" valor.
 *
 * <h2>Ordenado o no</h2>
 *
 * <p>{@link #isOrdered} parte la interfaz en dos comportamientos:
 *
 * <ul>
 *   <li><b>sin orden</b> --lo habitual-- los valores son un conjunto: agregar uno repetido no hace
 *       nada, y las posiciones no significan nada estable;
 *   <li><b>con orden</b> los valores son una lista: se repiten si se los agrega dos veces, y el
 *       indice es parte del dato.
 * </ul>
 *
 * <p>Los metodos con indice existen para el segundo caso. Sobre uno sin orden funcionan igual, pero
 * lo que devuelven no es reproducible entre implementaciones.
 *
 * <h2>Los dos metodos de esquema</h2>
 *
 * <p>{@link #getAttributeDefinition} y {@link #getAttributeSyntaxDefinition} devuelven partes del
 * esquema del directorio: que reglas tiene este atributo y que sintaxis tienen sus valores. Casi
 * ninguna implementacion los soporta, y la que no, lanza {@code OperationNotSupportedException}.
 */
public interface Attribute extends Cloneable, Serializable {

    /** De 1999. Es parte del API: cambiarlo rompe la deserializacion de lo ya guardado. */
    static final long serialVersionUID = 8707690322213556804L;

    /** Todos los valores. */
    NamingEnumeration<?> getAll() throws NamingException;

    /**
     * Alguno de los valores.
     *
     * @throws javax.naming.NoSuchElementException si no tiene ninguno
     */
    Object get() throws NamingException;

    /** Cuantos valores tiene. */
    int size();

    /** El identificador, por ejemplo {@code "cn"}. */
    String getID();

    /** Si tiene ese valor. */
    boolean contains(Object attrVal);

    /**
     * Agrega un valor.
     *
     * @return si el atributo cambio; false en uno sin orden que ya lo tenia
     */
    boolean add(Object attrVal);

    /** Saca ese valor. */
    boolean remove(Object attrval);

    /** Saca todos. */
    void clear();

    /**
     * El esquema de la sintaxis de los valores.
     *
     * @throws javax.naming.OperationNotSupportedException si la implementacion no lo tiene
     */
    DirContext getAttributeSyntaxDefinition() throws NamingException;

    /** El esquema del atributo. */
    DirContext getAttributeDefinition() throws NamingException;

    /** Una copia. */
    Object clone();

    /** Si los valores son una lista y no un conjunto. Ver la nota de la clase. */
    boolean isOrdered();

    /**
     * El valor de esa posicion.
     *
     * @throws IndexOutOfBoundsException si no existe
     */
    Object get(int ix) throws NamingException;

    /** Saca el de esa posicion y lo devuelve. */
    Object remove(int ix);

    /** Inserta en esa posicion. */
    void add(int ix, Object attrVal);

    /** Reemplaza el de esa posicion y devuelve el anterior. */
    Object set(int ix, Object attrVal);
}
