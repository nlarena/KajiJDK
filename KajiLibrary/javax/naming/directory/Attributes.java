package javax.naming.directory;

import java.io.Serializable;
import javax.naming.NamingEnumeration;

/**
 * KajiLibrary's javax.naming.directory.Attributes -- el conjunto de atributos de una entrada.
 *
 * <p>Un mapa de identificador a {@link Attribute}, con una particularidad que decide todo lo demas:
 * {@link #isCaseIgnored}. Los directorios LDAP <b>no distinguen mayusculas</b> en los nombres de
 * atributo --{@code cn}, {@code CN} y {@code Cn} son el mismo-- y por eso la coleccion tiene que
 * saber con que regla busca.
 *
 * <p>Eso no es un detalle de comodidad: dos colecciones con la misma informacion y distinta regla se
 * comportan distinto ante la misma consulta, y por eso la regla se fija al construir y no se puede
 * cambiar despues.
 *
 * <p>{@link #put(String, Object)} es un atajo que arma el {@link Attribute} por dentro. Conviene
 * saber que envuelve un <b>solo</b> valor: para un atributo con varios hay que armarlo y usar la otra
 * sobrecarga.
 */
public interface Attributes extends Cloneable, Serializable {

    /** Si los identificadores se comparan sin distinguir mayusculas. Ver la nota de la clase. */
    boolean isCaseIgnored();

    /** Cuantos atributos hay. */
    int size();

    /**
     * El atributo con ese identificador.
     *
     * @return null si no esta
     */
    Attribute get(String attrID);

    /** Todos los atributos. */
    NamingEnumeration<? extends Attribute> getAll();

    /** Solo los identificadores. */
    NamingEnumeration<String> getIDs();

    /**
     * Agrega un atributo de un solo valor.
     *
     * @return el que estaba con ese identificador, o null
     */
    Attribute put(String attrID, Object val);

    /** Agrega un atributo ya armado. */
    Attribute put(Attribute attr);

    /** Lo saca y lo devuelve. */
    Attribute remove(String attrID);

    /** Una copia. */
    Object clone();
}
