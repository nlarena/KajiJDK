package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.XMLCryptoContext -- el contexto de una operacion criptografica XML.
 *
 * <p>Junta todo lo que firmar o validar necesita saber y que no esta en el documento: con que clave
 * ({@link KeySelector}), como resolver las referencias ({@link URIDereferencer}), contra que base se
 * resuelve lo relativo, y que prefijos usar al escribir.
 *
 * <h2>Dos mapas, no uno</h2>
 *
 * <p>{@link #setProperty} y {@link #put} parecen lo mismo y no lo son:
 *
 * <ul>
 *   <li>las <b>propiedades</b> tienen clave {@code String} y las define la implementacion: son
 *       configuracion;
 *   <li>el mapa de {@link #put} tiene clave {@code Object} y es para que quien usa el API pase
 *       informacion suya de un lado a otro -- entre un {@code URIDereferencer} propio y un
 *       {@code KeySelector} propio, por ejemplo.
 * </ul>
 *
 * <p>Estan separados para que la configuracion de la implementacion y los datos de la aplicacion no
 * se pisen por elegir el mismo nombre.
 *
 * <h2>Los prefijos de espacio de nombres</h2>
 *
 * <p>{@link #putNamespacePrefix} solo afecta a lo que se <b>escribe</b>. Al leer no importa: los
 * prefijos del documento son los que son. Sirve para que la firma que se genera se lea, y para que
 * coincida con la de otra herramienta si hace falta compararlas.
 */
public interface XMLCryptoContext {

    /** Contra que se resuelve lo relativo. */
    String getBaseURI();

    /** Ver {@link #getBaseURI}. */
    void setBaseURI(String baseURI);

    /** Con que clave. */
    KeySelector getKeySelector();

    /** Ver {@link #getKeySelector}. */
    void setKeySelector(KeySelector ks);

    /** Como se resuelven las referencias. Ver {@link URIDereferencer}. */
    URIDereferencer getURIDereferencer();

    /** Ver {@link #getURIDereferencer}. */
    void setURIDereferencer(URIDereferencer dereferencer);

    /**
     * El prefijo que se usa para ese espacio de nombres.
     *
     * @param defaultPrefix que devolver si no hay ninguno registrado
     */
    String getNamespacePrefix(String namespaceURI, String defaultPrefix);

    /**
     * Lo registra.
     *
     * @return el que estaba, o null
     */
    String putNamespacePrefix(String namespaceURI, String prefix);

    /** El prefijo por omision al escribir. */
    String getDefaultNamespacePrefix();

    /** Ver {@link #getDefaultNamespacePrefix}. */
    void setDefaultNamespacePrefix(String defaultPrefix);

    /**
     * Una propiedad de la implementacion.
     *
     * @return el valor que estaba, o null
     */
    Object setProperty(String name, Object value);

    /** Ver {@link #setProperty}. */
    Object getProperty(String name);

    /** Un dato de la aplicacion. Ver la nota de la clase sobre los dos mapas. */
    Object get(Object key);

    /** Ver {@link #get}. */
    Object put(Object key, Object value);
}
