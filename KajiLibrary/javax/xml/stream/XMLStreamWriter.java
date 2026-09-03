package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamWriter -- escribir XML llamando un metodo por pieza, sin
 * armar un arbol.
 *
 * <p>Es el reverso de {@link XMLStreamReader} y tiene la misma virtud: el documento se escribe a
 * medida que se genera, asi que la memoria no crece con el tamanio de la salida. Un catalogo de un
 * millon de productos se escribe con un bucle; con DOM habria que tener el millon en memoria antes
 * de escribir el primer byte.
 *
 * <h2>Lo que el escritor no hace por vos</h2>
 *
 * <p>Esto es lo que mas sorprende de la interfaz, y es una decision de diseño explicita: <b>el
 * escritor no verifica que el documento salga bien formado</b>. Nada lo obliga a que cada
 * {@code writeStartElement} tenga su {@code writeEndElement}, y una implementacion tipica no lleva
 * la cuenta. El motivo es el mismo de siempre en StAX: llevarla cuesta en el camino caliente, y
 * quien genera el documento ya sabe que estructura esta generando.
 *
 * <p>Lo que si hace, y hay que tenerlo presente, es **escapar** el texto: {@link #writeCharacters}
 * convierte {@code &} en {@code &amp;} y {@code <} en {@code &lt;}. Por eso hay metodos separados
 * para el texto y para el marcado, y por eso no hay un "escribir esto crudo".
 *
 * <h2>Los prefijos, y el modo reparador</h2>
 *
 * <p>Los espacios de nombres son la parte pesada de escribir XML: hay que declarar el prefijo antes
 * de usarlo y no repetir la declaracion en cada elemento. La interfaz da las dos formas de
 * manejarlo:
 *
 * <ul>
 *   <li>a mano, con {@link #setPrefix} y {@link #writeNamespace}, que es lo que hay que hacer por
 *       omision;
 *   <li>automatico, si la fabrica tiene puesto
 *       {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES}: el escritor inventa y declara los prefijos
 *       que hagan falta.
 * </ul>
 *
 * <p>{@link #setPrefix} tiene una sutileza que se paga cara si se pasa por alto: **declara la
 * intencion, no escribe nada**. La declaracion sale recien con {@link #writeNamespace}. Llamar solo
 * al primero produce un documento con prefijos sin declarar, que es un documento roto.
 *
 * <h2>Que hay escrito aca y que no</h2>
 *
 * <p>Los treinta y dos metodos de la interfaz. Implementacion no hay, por lo mismo que en
 * {@link XMLStreamReader}: un escritor de verdad tiene que resolver escapes segun el contexto,
 * codificacion de salida y espacios de nombres, y esta biblioteca no trae ningun proveedor de StAX.
 * Ver {@link XMLOutputFactory}.
 */
public interface XMLStreamWriter {

    /**
     * Abre un elemento sin calificar.
     *
     * @param localName el nombre
     * @throws XMLStreamException si falla la escritura
     */
    void writeStartElement(String localName) throws XMLStreamException;

    /**
     * Abre un elemento en un espacio de nombres, con el prefijo que corresponda por contexto.
     *
     * @param namespaceURI el espacio de nombres
     * @param localName el nombre local
     * @throws XMLStreamException si falla la escritura
     */
    void writeStartElement(String namespaceURI, String localName) throws XMLStreamException;

    /**
     * Abre un elemento con prefijo, espacio de nombres y nombre local explicitos.
     *
     * @param prefix el prefijo
     * @param localName el nombre local
     * @param namespaceURI el espacio de nombres
     * @throws XMLStreamException si falla la escritura
     */
    void writeStartElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException;

    /**
     * Escribe un elemento vacio, {@code <a/>}, en un espacio de nombres.
     *
     * <p>No hay que cerrarlo: no abre nivel.
     *
     * @param namespaceURI el espacio de nombres
     * @param localName el nombre local
     * @throws XMLStreamException si falla la escritura
     */
    void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException;

    /**
     * Escribe un elemento vacio con prefijo explicito.
     *
     * @param prefix el prefijo
     * @param localName el nombre local
     * @param namespaceURI el espacio de nombres
     * @throws XMLStreamException si falla la escritura
     */
    void writeEmptyElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException;

    /**
     * Escribe un elemento vacio sin calificar.
     *
     * @param localName el nombre
     * @throws XMLStreamException si falla la escritura
     */
    void writeEmptyElement(String localName) throws XMLStreamException;

    /**
     * Cierra el elemento abierto mas reciente.
     *
     * @throws XMLStreamException si falla la escritura
     */
    void writeEndElement() throws XMLStreamException;

    /**
     * Cierra todos los elementos que queden abiertos y termina el documento.
     *
     * @throws XMLStreamException si falla la escritura
     */
    void writeEndDocument() throws XMLStreamException;

    /**
     * Libera lo que el escritor tenga tomado.
     *
     * <p>No cierra el flujo de destino --quien lo abrio lo cierra-- y **no** vuelca lo pendiente:
     * para eso esta {@link #flush()}.
     *
     * @throws XMLStreamException si falla
     */
    void close() throws XMLStreamException;

    /**
     * Vuelca al destino lo que este en el buffer.
     *
     * @throws XMLStreamException si falla la escritura
     */
    void flush() throws XMLStreamException;

    /**
     * Escribe un atributo sin calificar en el elemento abierto.
     *
     * @param localName el nombre
     * @param value el valor, que se escapa
     * @throws XMLStreamException si no hay un elemento abierto o falla la escritura
     */
    void writeAttribute(String localName, String value) throws XMLStreamException;

    /**
     * Escribe un atributo con prefijo y espacio de nombres explicitos.
     *
     * @param prefix el prefijo
     * @param namespaceURI el espacio de nombres
     * @param localName el nombre local
     * @param value el valor, que se escapa
     * @throws XMLStreamException si no hay un elemento abierto o falla la escritura
     */
    void writeAttribute(String prefix, String namespaceURI, String localName, String value)
            throws XMLStreamException;

    /**
     * Escribe un atributo en un espacio de nombres, con el prefijo que corresponda por contexto.
     *
     * @param namespaceURI el espacio de nombres
     * @param localName el nombre local
     * @param value el valor, que se escapa
     * @throws XMLStreamException si no hay un elemento abierto o falla la escritura
     */
    void writeAttribute(String namespaceURI, String localName, String value)
            throws XMLStreamException;

    /**
     * Escribe una declaracion {@code xmlns:prefijo="uri"} en el elemento abierto.
     *
     * @param prefix el prefijo a declarar
     * @param namespaceURI el espacio de nombres
     * @throws XMLStreamException si no hay un elemento abierto o falla la escritura
     */
    void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException;

    /**
     * Escribe la declaracion del espacio de nombres por omision, {@code xmlns="uri"}.
     *
     * @param namespaceURI el espacio de nombres
     * @throws XMLStreamException si no hay un elemento abierto o falla la escritura
     */
    void writeDefaultNamespace(String namespaceURI) throws XMLStreamException;

    /**
     * Escribe un comentario.
     *
     * @param data el contenido, sin los delimitadores
     * @throws XMLStreamException si falla la escritura
     */
    void writeComment(String data) throws XMLStreamException;

    /**
     * Escribe una instruccion de proceso sin datos.
     *
     * @param target el destino
     * @throws XMLStreamException si falla la escritura
     */
    void writeProcessingInstruction(String target) throws XMLStreamException;

    /**
     * Escribe una instruccion de proceso con datos.
     *
     * @param target el destino
     * @param data los datos
     * @throws XMLStreamException si falla la escritura
     */
    void writeProcessingInstruction(String target, String data) throws XMLStreamException;

    /**
     * Escribe una seccion CDATA.
     *
     * <p>La diferencia con {@link #writeCharacters}: el contenido va **sin escapar**, entre
     * {@code <![CDATA[} y {@code ]]>}. Sirve para meter texto lleno de {@code <} --codigo fuente,
     * XML embebido-- sin que quede ilegible.
     *
     * @param data el contenido
     * @throws XMLStreamException si falla la escritura
     */
    void writeCData(String data) throws XMLStreamException;

    /**
     * Escribe una declaracion de tipo de documento entera, tal cual se la da.
     *
     * @param dtd el texto completo del {@code <!DOCTYPE ...>}
     * @throws XMLStreamException si falla la escritura
     */
    void writeDTD(String dtd) throws XMLStreamException;

    /**
     * Escribe una referencia a entidad: {@code &nombre;}.
     *
     * @param name el nombre de la entidad, sin el ampersand ni el punto y coma
     * @throws XMLStreamException si falla la escritura
     */
    void writeEntityRef(String name) throws XMLStreamException;

    /**
     * Escribe la declaracion XML con la version 1.0 y sin codificacion.
     *
     * @throws XMLStreamException si falla la escritura
     */
    void writeStartDocument() throws XMLStreamException;

    /**
     * Escribe la declaracion XML con una version dada.
     *
     * @param version la version
     * @throws XMLStreamException si falla la escritura
     */
    void writeStartDocument(String version) throws XMLStreamException;

    /**
     * Escribe la declaracion XML con codificacion y version.
     *
     * <p>Ojo con esto: el nombre de codificacion que se escribe **es solo texto**. No cambia como se
     * codifica la salida, que quedo fijada cuando se creo el escritor. Escribir {@code UTF-16} en un
     * escritor que emite UTF-8 produce un documento que ninguna herramienta puede leer, y el
     * escritor no avisa.
     *
     * @param encoding el nombre de la codificacion a declarar
     * @param version la version
     * @throws XMLStreamException si falla la escritura
     */
    void writeStartDocument(String encoding, String version) throws XMLStreamException;

    /**
     * Escribe texto, escapando lo que haga falta.
     *
     * @param text el texto
     * @throws XMLStreamException si falla la escritura
     */
    void writeCharacters(String text) throws XMLStreamException;

    /**
     * Escribe texto desde un arreglo, escapando lo que haga falta.
     *
     * @param text el arreglo
     * @param start desde donde
     * @param len cuantos caracteres
     * @throws XMLStreamException si falla la escritura
     */
    void writeCharacters(char[] text, int start, int len) throws XMLStreamException;

    /**
     * El prefijo ligado a un espacio de nombres en el contexto actual, o null.
     *
     * @param uri el espacio de nombres
     * @return el prefijo
     * @throws XMLStreamException si falla
     */
    String getPrefix(String uri) throws XMLStreamException;

    /**
     * Liga un prefijo a un espacio de nombres para lo que se escriba de aca en mas.
     *
     * <p>No escribe la declaracion; ver el encabezado de la clase.
     *
     * @param prefix el prefijo
     * @param uri el espacio de nombres
     * @throws XMLStreamException si falla
     */
    void setPrefix(String prefix, String uri) throws XMLStreamException;

    /**
     * Liga el espacio de nombres por omision, sin escribir la declaracion.
     *
     * @param uri el espacio de nombres
     * @throws XMLStreamException si falla
     */
    void setDefaultNamespace(String uri) throws XMLStreamException;

    /**
     * Reemplaza el contexto de espacios de nombres entero.
     *
     * <p>Solo se puede llamar antes del elemento raiz: cambiar las ligaduras a mitad de camino
     * invalidaria los prefijos ya escritos.
     *
     * @param context el contexto nuevo
     * @throws XMLStreamException si falla o si ya es tarde
     */
    void setNamespaceContext(NamespaceContext context) throws XMLStreamException;

    /**
     * Las ligaduras vigentes.
     *
     * @return el contexto; no es modificable por el llamador
     */
    NamespaceContext getNamespaceContext();

    /**
     * El valor de una propiedad de la implementacion.
     *
     * @param name el nombre de la propiedad; no puede ser null
     * @return el valor
     * @throws IllegalArgumentException si la propiedad no existe
     */
    Object getProperty(String name) throws IllegalArgumentException;
}
