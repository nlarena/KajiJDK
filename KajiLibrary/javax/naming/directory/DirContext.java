package javax.naming.directory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.DirContext -- un contexto que ademas tiene atributos.
 *
 * <p>Extiende {@link Context} con lo que distingue a un <b>directorio</b> de un servicio de nombres:
 * cada entrada no solo tiene un nombre y un objeto, sino un conjunto de atributos que se pueden leer,
 * modificar y buscar.
 *
 * <h2>Las dos formas de modificar</h2>
 *
 * <p>{@code modifyAttributes} viene en dos sabores. El que recibe un codigo y unos
 * {@link Attributes} aplica <b>la misma</b> operacion a todos; el que recibe un arreglo de
 * {@link ModificationItem} mezcla operaciones distintas. El segundo es el que hay que usar cuando el
 * cambio tiene que ser atomico y no uniforme.
 *
 * <p>Los tres codigos no son simetricos. {@link #ADD_ATTRIBUTE} agrega valores a los que ya hay,
 * {@link #REPLACE_ATTRIBUTE} tira los viejos y pone los nuevos, y {@link #REMOVE_ATTRIBUTE} saca los
 * valores que se le pasen --o el atributo entero si se le pasa sin valores--. Confundir los dos
 * primeros sobre un atributo de varios valores es la forma clasica de borrar datos sin querer.
 *
 * <h2>Las tres familias de busqueda</h2>
 *
 * <ul>
 *   <li>por <b>atributos de ejemplo</b>: se pasa un {@link Attributes} y se buscan las entradas que
 *       los tengan. Es comodo y solo hace igualdad;
 *   <li>por <b>filtro</b>, con la sintaxis de RFC 2254: mucho mas expresivo --hay o, y, no,
 *       comodines-- y armado a mano es inyectable;
 *   <li>por filtro con <b>argumentos numerados</b>, donde el filtro lleva {@code {0}}, {@code {1}} y
 *       los valores van aparte. Es la version segura de la anterior y es la que conviene usar
 *       siempre que el filtro dependa de una entrada del usuario.
 * </ul>
 *
 * <p>Cada operacion viene con {@link Name} y con {@code String}. La de {@code Name} es la correcta
 * cuando el nombre se compone o se recorre: un {@code String} obliga a pensar en como escapar los
 * separadores del espacio de nombres, y ahi es donde se rompe.
 */
public interface DirContext extends Context {

    /** Agrega valores a los que el atributo ya tiene. */
    public static final int ADD_ATTRIBUTE = 1;

    /** Tira los valores viejos y pone los nuevos. */
    public static final int REPLACE_ATTRIBUTE = 2;

    /** Saca los valores dados, o el atributo entero si no se dan valores. */
    public static final int REMOVE_ATTRIBUTE = 3;

    /** Todos los atributos de esa entrada. */
    Attributes getAttributes(Name name) throws NamingException;

    /** Idem, con el nombre como texto. */
    Attributes getAttributes(String name) throws NamingException;

    /**
     * Solo esos atributos.
     *
     * @param attrIds cuales traer; null son todos
     */
    Attributes getAttributes(Name name, String[] attrIds) throws NamingException;

    /** Idem, con el nombre como texto. */
    Attributes getAttributes(String name, String[] attrIds) throws NamingException;

    /**
     * Aplica la misma operacion a todos esos atributos.
     *
     * @param mod_op una de las tres constantes; ver la nota de la clase
     */
    void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException;

    /** Idem, con el nombre como texto. */
    void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException;

    /** Aplica una lista de modificaciones distintas, todas o ninguna. */
    void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException;

    /** Idem, con el nombre como texto. */
    void modifyAttributes(String name, ModificationItem[] mods) throws NamingException;

    /** Ata un objeto a un nombre, con atributos. */
    void bind(Name name, Object obj, Attributes attrs) throws NamingException;

    /** Idem, con el nombre como texto. */
    void bind(String name, Object obj, Attributes attrs) throws NamingException;

    /** Igual, pisando lo que hubiera. */
    void rebind(Name name, Object obj, Attributes attrs) throws NamingException;

    /** Idem, con el nombre como texto. */
    void rebind(String name, Object obj, Attributes attrs) throws NamingException;

    /** Crea un subcontexto con esos atributos. */
    DirContext createSubcontext(Name name, Attributes attrs) throws NamingException;

    /** Idem, con el nombre como texto. */
    DirContext createSubcontext(String name, Attributes attrs) throws NamingException;

    /** El esquema que gobierna esa entrada. */
    DirContext getSchema(Name name) throws NamingException;

    /** Idem, con el nombre como texto. */
    DirContext getSchema(String name) throws NamingException;

    /** Las definiciones de clase de objeto de esa entrada. */
    DirContext getSchemaClassDefinition(Name name) throws NamingException;

    /** Idem, con el nombre como texto. */
    DirContext getSchemaClassDefinition(String name) throws NamingException;

    /**
     * Busca por atributos de ejemplo.
     *
     * @param matchingAttributes los que la entrada tiene que tener; vacio o null trae todas
     * @param attributesToReturn cuales traer de cada resultado; null son todos
     */
    NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes,
                                           String[] attributesToReturn) throws NamingException;

    /** Idem, con el nombre como texto. */
    NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes,
                                           String[] attributesToReturn) throws NamingException;

    /** Busca por atributos de ejemplo, trayendo todos los atributos. */
    NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes)
        throws NamingException;

    /** Idem, con el nombre como texto. */
    NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes)
        throws NamingException;

    /**
     * Busca por filtro.
     *
     * <p>Ver la nota de la clase: si el filtro depende de algo que escribio una persona, va la
     * version con argumentos numerados.
     */
    NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons)
        throws NamingException;

    /** Idem, con el nombre como texto. */
    NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
        throws NamingException;

    /**
     * Busca por filtro con argumentos numerados.
     *
     * @param filterArgs los valores de {@code {0}}, {@code {1}}, ...; no pasan por el parser
     */
    NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs,
                                           SearchControls cons) throws NamingException;

    /** Idem, con el nombre como texto. */
    NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs,
                                           SearchControls cons) throws NamingException;
}
