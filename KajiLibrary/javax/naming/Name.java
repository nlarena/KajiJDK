package javax.naming;

import java.util.Enumeration;

/**
 * Un nombre como **secuencia ordenada de componentes**, no como cadena.
 *
 * <p>Esa es toda la idea del tipo. `"a/b/c"` es una cadena; `{"a","b","c"}` es un nombre. La
 * conversion entre las dos formas depende de una sintaxis --que separa, que cita, que escapa-- y
 * es justamente lo que cambia de un espacio de nombres a otro: LDAP separa con coma y de derecha
 * a izquierda, un sistema de archivos con barra y de izquierda a derecha, y un espacio plano no
 * separa nada. Manipular componentes en vez de cadenas es lo que permite escribir codigo que
 * atraviesa espacios de nombres sin saber la sintaxis de ninguno.
 *
 * <p>Las dos implementaciones del paquete son `CompositeName` --sintaxis fija, la que sirve para
 * atravesar varios espacios de nombres-- y `CompoundName` --sintaxis dada por un `Properties`,
 * la que representa un nombre **dentro** de un espacio--.
 *
 * <h2>Dos cosas que sorprenden y son del contrato</h2>
 *
 * <p><strong>Es mutable.</strong> `add`, `addAll` y `remove` cambian el nombre en el lugar y
 * devuelven `this` para poder encadenar. Por eso todo el resto del paquete clona antes de guardar
 * un `Name` (ver los setters de `NamingException`).
 *
 * <p><strong>`compareTo` toma `Object` y no `Name`.</strong> La interfaz es `Comparable<Object>`,
 * que hoy se escribiria `Comparable<Name>`. Quedo asi de la epoca previa a los genericos y no se
 * puede arreglar sin romper a todo el que ya implemento la interfaz. Lo mismo con `remove`, que
 * devuelve `Object` --siempre es un `String`-- y con `clone`, que devuelve `Object`.
 *
 * <p>La interfaz es `Serializable`: la forma serial la define cada implementacion.
 */
public interface Name extends Cloneable, java.io.Serializable, Comparable<Object> {

    long serialVersionUID = -3617482732056931635L;

    Object clone();

    /**
     * Orden entre nombres del **mismo** tipo; tira `ClassCastException` si no lo son.
     *
     * <p>El orden es lexicografico por componentes, y lo normaliza la sintaxis **de este** nombre
     * --no la del otro--: si esta sintaxis ignora mayusculas, la comparacion tambien.
     */
    int compareTo(Object obj);

    int size();

    boolean isEmpty();

    /** Los componentes en orden, del cero al ultimo. Es `Enumeration` por la edad de la API. */
    Enumeration<String> getAll();

    String get(int posn);

    /** Los primeros `posn` componentes, como un nombre nuevo. `posn == size()` da una copia entera. */
    Name getPrefix(int posn);

    /** Del `posn` al final, como un nombre nuevo. `posn == size()` da el nombre vacio. */
    Name getSuffix(int posn);

    boolean startsWith(Name n);

    boolean endsWith(Name n);

    /** Pega `suffix` al final y devuelve `this`, ya modificado. */
    Name addAll(Name suffix) throws InvalidNameException;

    /** Inserta los componentes de `n` a partir de `posn` y devuelve `this`. */
    Name addAll(int posn, Name n) throws InvalidNameException;

    Name add(String comp) throws InvalidNameException;

    Name add(int posn, String comp) throws InvalidNameException;

    /** Saca el componente `posn` y lo devuelve; siempre es un `String`. */
    Object remove(int posn) throws InvalidNameException;
}
