package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.Source -- de donde sale un documento XML.
 *
 * <p>Es una interfaz **marcadora con identidad**: no dice como leer el documento, solo que hay uno y
 * de donde vino. Quien la implementa elige la forma --un flujo, un arbol DOM, una secuencia de
 * eventos SAX-- y el procesador acepta la que sepa manejar. Sin esta abstraccion, cada API que recibe
 * XML tendria que ofrecer una sobrecarga por cada representacion.
 *
 * <p>El identificador de sistema es lo unico comun a todas: la URI base contra la cual resolver las
 * referencias relativas del documento. Sin ella, un `&lt;xsl:include href="comun.xsl"/&gt;` no se
 * puede seguir.
 */
public interface Source {

    /** La URI base del documento. */
    void setSystemId(String systemId);

    String getSystemId();

    /**
     * Si esta fuente no tiene nada.
     *
     * <p>Existe para distinguir "vacia" de "nula": pasar una fuente vacia es valido y significa
     * "sin documento", que no es lo mismo que un error de programacion. Por omision dice que no,
     * porque una implementacion que no lo sepa contestar mejor que no invente.
     */
    default boolean isEmpty() {
        return false;
    }
}
