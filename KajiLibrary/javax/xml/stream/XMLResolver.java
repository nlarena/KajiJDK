package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLResolver -- quien decide de donde sale una entidad externa.
 *
 * <p>Un documento puede referirse a otro archivo --un DTD, una entidad externa-- por una URI, y el
 * parser tiene que ir a buscarlo. Este es el punto donde la aplicacion se mete en esa busqueda, y
 * las dos razones para hacerlo son distintas:
 *
 * <ul>
 *   <li><b>servir de un catalogo local</b>, para no salir a la red a bajar un DTD que ya se tiene, y
 *   <li><b>no servir nada</b>, que es la defensa contra XXE: un documento hostil que declara una
 *       entidad apuntando a un archivo del servidor se la hace leer al parser y se la lleva en la
 *       respuesta. Un resolver que devuelve vacio para todo lo que no reconozca corta eso de raiz.
 * </ul>
 *
 * <p>El valor de retorno es {@code Object} y no un tipo util porque las implementaciones aceptan
 * varias formas de lo mismo --un {@link java.io.InputStream}, un {@link java.io.Reader}, un
 * {@link javax.xml.stream.XMLStreamReader}, un {@link javax.xml.transform.Source}-- y la spec no
 * quiso elegir. Que tipos acepta cada parser es cosa suya.
 */
public interface XMLResolver {

    /**
     * Resuelve una entidad externa.
     *
     * @param publicID el identificador publico declarado, o null si no hay
     * @param systemID el identificador de sistema declarado
     * @param baseURI la URI del documento que la referencia, para resolver la relativa
     * @param namespace el espacio de nombres de la entidad, si aplica
     * @return el contenido, en alguna de las formas que acepte el parser, o null para que resuelva
     *     el por omision
     * @throws XMLStreamException si la entidad no se puede resolver y eso tiene que cortar
     */
    Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
            throws XMLStreamException;
}
