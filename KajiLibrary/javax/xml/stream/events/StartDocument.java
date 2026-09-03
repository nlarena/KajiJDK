package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.StartDocument -- el comienzo del documento y lo que dice su
 * declaracion XML.
 *
 * <h2>Los pares {@code xxxSet()} y por que hacen falta</h2>
 *
 * <p>La declaracion {@code <?xml version="1.0" encoding="UTF-8" standalone="yes"?>} tiene dos
 * partes opcionales, y para cada una hay dos preguntas distintas: cual es el valor, y si estaba
 * escrito. {@link #getCharacterEncodingScheme()} contra {@link #encodingSet()},
 * {@link #isStandalone()} contra {@link #standaloneSet()}.
 *
 * <p>No es redundancia. Un documento sin {@code encoding} igual tiene una codificacion --la que el
 * parser detecto, UTF-8 por omision-- y {@link #getCharacterEncodingScheme()} la informa; lo que
 * {@link #encodingSet()} agrega es si esa codificacion venia declarada o fue deducida. Quien
 * reescribe el documento necesita la diferencia para no inventar una declaracion que no estaba, y
 * quien diagnostica un problema de codificacion necesita saber si le estan mintiendo o adivinando.
 *
 * <p>Con {@code standalone} pasa lo mismo y es mas fuerte: el valor por omision es {@code no}, asi
 * que {@link #isStandalone()} devuelve false tanto para un documento que dijo {@code no} como para
 * uno que no dijo nada, y solo {@link #standaloneSet()} los distingue.
 */
public interface StartDocument extends XMLEvent {

    /**
     * De donde salio el documento.
     *
     * @return el identificador de sistema, o la cadena vacia si no se conoce
     */
    String getSystemId();

    /**
     * La codificacion del documento: la declarada, o la que se detecto.
     *
     * @return el nombre de la codificacion
     */
    String getCharacterEncodingScheme();

    /**
     * Si la codificacion venia escrita en la declaracion XML.
     *
     * @return true si el documento la declaraba
     */
    boolean encodingSet();

    /**
     * El valor de {@code standalone}, con false por omision.
     *
     * @return true solo si el documento declaro {@code standalone="yes"}
     */
    boolean isStandalone();

    /**
     * Si {@code standalone} venia escrito en la declaracion XML.
     *
     * @return true si el documento lo declaraba
     */
    boolean standaloneSet();

    /**
     * La version declarada.
     *
     * @return {@code "1.0"} si no hay declaracion, o lo que la declaracion diga
     */
    String getVersion();
}
