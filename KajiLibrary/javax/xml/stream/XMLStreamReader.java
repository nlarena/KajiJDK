package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamReader -- el modelo de cursor de StAX: el que **tira** del
 * documento en vez de recibirlo empujado.
 *
 * <p>Es la diferencia de fondo con SAX y la razon de ser de todo el paquete. En SAX el parser manda:
 * llama a los metodos del handler cuando quiere, y la aplicacion, si necesita acordarse de donde
 * estaba, se lo tiene que anotar en campos --una maquina de estados escrita a mano en cada uso--.
 * Aca manda la aplicacion: llama a {@link #next()} cuando esta lista, y la posicion en el documento
 * es la posicion en su propio codigo. Un bucle {@code while (r.hasNext())} con un {@code switch}
 * adentro reemplaza al handler entero.
 *
 * <p>Eso ademas hace posibles dos cosas que en SAX cuestan mucho: **parar** en la mitad --y no leer
 * el resto-- y **combinar** dos documentos leyendo un poco de cada uno.
 *
 * <h2>Un solo objeto que cambia de contenido</h2>
 *
 * <p>La regla que hay que tener presente: el lector **es** el evento. {@code getLocalName()} no
 * devuelve el nombre de "un" elemento sino el del elemento en que esta parado el cursor ahora, y
 * despues del proximo {@code next()} devuelve otra cosa. Nada de lo que sale de aca --ni siquiera el
 * {@code char[]} de {@link #getTextCharacters()}-- se puede guardar para despues.
 *
 * <p>De ahi sale toda la ganancia de rendimiento del modelo de cursor: cero objetos por evento. Y de
 * ahi sale tambien la existencia del otro modelo, el de {@link XMLEventReader}, para cuando lo que
 * hace falta es justamente guardar.
 *
 * <p>La otra consecuencia, menos obvia: **cada metodo es valido solo en ciertos estados**. Pedir
 * {@link #getName()} parado en un {@link XMLStreamConstants#CHARACTERS} es un error del llamador y
 * levanta {@link IllegalStateException}, no null. Cada metodo dice abajo donde vale.
 *
 * <h2>Que hay escrito aca y que no, y por que</h2>
 *
 * <p>La interfaz esta completa: los cuarenta y cinco metodos, con sus estados validos y sus
 * excepciones. Lo que no hay en esta biblioteca es una **implementacion**, y no por falta de ganas:
 * un {@code XMLStreamReader} de verdad es un parser de XML entero --tokenizador, manejo de
 * entidades, resolucion de espacios de nombres, decodificacion segun la declaracion XML-- y eso es
 * un proyecto aparte, no un miembro de esta interfaz.
 *
 * <p>La tentacion a evitar es la contraria a la de una fabrica: una fabrica que no encuentra parser
 * puede fallar honestamente, pero un lector que devolviera eventos inventados le mentiria al
 * llamador sin que nada se rompa. Un documento que no se leyo nunca y que igual produjo eventos es
 * el peor de los resultados posibles, asi que no hay ningun lector de mentira en esta biblioteca.
 * Ver {@link XMLInputFactory} para el camino que si esta.
 */
public interface XMLStreamReader extends XMLStreamConstants {

    /**
     * El valor de una propiedad de la implementacion.
     *
     * @param name el nombre de la propiedad; no puede ser null
     * @return el valor
     * @throws IllegalArgumentException si {@code name} es null
     */
    Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Avanza al proximo evento y devuelve su tipo.
     *
     * <p>Este es el metodo que define el modelo: nada pasa hasta que el llamador lo pide.
     *
     * @return uno de los tipos de {@link XMLStreamConstants}
     * @throws XMLStreamException si el documento esta mal formado o falla la lectura
     * @throws java.util.NoSuchElementException si ya no hay mas eventos
     */
    int next() throws XMLStreamException;

    /**
     * Verifica que el cursor este donde el llamador cree, y si no, corta.
     *
     * <p>Es una asercion con forma de metodo, y sirve para que un error de estructura salte en el
     * lugar donde se noto y no cinco eventos despues, cuando ya no se sabe de donde vino. Un null en
     * cualquiera de los dos ultimos parametros significa "no me importa este".
     *
     * @param type el tipo de evento esperado
     * @param namespaceURI el espacio de nombres esperado, o null para no chequearlo
     * @param localName el nombre local esperado, o null para no chequearlo
     * @throws XMLStreamException si el evento actual no coincide
     */
    void require(int type, String namespaceURI, String localName) throws XMLStreamException;

    /**
     * El texto de un elemento que solo contiene texto, dejando el cursor en su cierre.
     *
     * <p>Atajo para el caso mas comun de todos --{@code <precio>12.50</precio>}-- que sin esto son
     * cinco lineas de bucle. Falla si el elemento tiene hijos, que es justamente lo que hace que
     * valga la pena: no devuelve el texto de un elemento con estructura como si no la tuviera.
     *
     * <p>Vale solo parado en un {@link XMLStreamConstants#START_ELEMENT}.
     *
     * @return el texto entre la apertura y el cierre
     * @throws XMLStreamException si el cursor no esta en una apertura o el elemento no es de solo
     *     texto
     */
    String getElementText() throws XMLStreamException;

    /**
     * Saltea espacio en blanco, comentarios e instrucciones de proceso hasta la proxima etiqueta.
     *
     * <p>Lo que hace legible el recorrido de un documento indentado: sin esto, cada salto de linea
     * del archivo es un evento de texto que hay que descartar a mano.
     *
     * @return {@link XMLStreamConstants#START_ELEMENT} o {@link XMLStreamConstants#END_ELEMENT}
     * @throws XMLStreamException si encuentra algo que no sea salteable ni una etiqueta
     */
    int nextTag() throws XMLStreamException;

    /**
     * Si queda al menos un evento por leer.
     *
     * @return true si {@link #next()} tiene algo que devolver
     * @throws XMLStreamException si falla la lectura
     */
    boolean hasNext() throws XMLStreamException;

    /**
     * Libera lo que el lector tenga tomado.
     *
     * <p>No cierra el {@link java.io.InputStream} ni el {@link java.io.Reader} de origen: quien lo
     * abrio lo cierra. Esa regla evita que un lector le cierre por abajo el flujo a quien lo estaba
     * compartiendo.
     *
     * @throws XMLStreamException si falla
     */
    void close() throws XMLStreamException;

    /**
     * El espacio de nombres ligado a un prefijo en la posicion actual.
     *
     * @param prefix el prefijo; la cadena vacia pregunta por el de omision
     * @return el URI, o null si el prefijo no esta ligado
     */
    String getNamespaceURI(String prefix);

    /**
     * Si el cursor esta en una apertura de elemento.
     *
     * @return true si el evento actual es {@link XMLStreamConstants#START_ELEMENT}
     */
    boolean isStartElement();

    /**
     * Si el cursor esta en un cierre de elemento.
     *
     * @return true si el evento actual es {@link XMLStreamConstants#END_ELEMENT}
     */
    boolean isEndElement();

    /**
     * Si el cursor esta en texto.
     *
     * @return true si el evento actual es {@link XMLStreamConstants#CHARACTERS}
     */
    boolean isCharacters();

    /**
     * Si el evento actual es texto y es todo espacio en blanco.
     *
     * @return true si es espacio
     */
    boolean isWhiteSpace();

    /**
     * El valor de un atributo del elemento actual, buscado por nombre.
     *
     * <p>Vale en {@link XMLStreamConstants#START_ELEMENT} y {@link XMLStreamConstants#ATTRIBUTE}.
     *
     * @param namespaceURI el espacio de nombres del atributo, o null para no mirarlo
     * @param localName el nombre local del atributo
     * @return el valor, o null si el atributo no esta
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    String getAttributeValue(String namespaceURI, String localName);

    /**
     * Cuantos atributos tiene el elemento actual.
     *
     * <p>No cuenta las declaraciones de espacio de nombres: {@code xmlns:a="..."} no es un atributo
     * a estos efectos, y por eso hay un {@link #getNamespaceCount()} aparte.
     *
     * @return la cantidad
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    int getAttributeCount();

    /**
     * El nombre calificado del atributo numero {@code index}.
     *
     * @param index el indice, desde 0
     * @return el nombre
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    QName getAttributeName(int index);

    /**
     * El espacio de nombres del atributo numero {@code index}, o null si no tiene.
     *
     * @param index el indice, desde 0
     * @return el espacio de nombres
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    String getAttributeNamespace(int index);

    /**
     * El nombre local del atributo numero {@code index}.
     *
     * @param index el indice, desde 0
     * @return el nombre local
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    String getAttributeLocalName(int index);

    /**
     * El prefijo del atributo numero {@code index}, o null si no tiene.
     *
     * @param index el indice, desde 0
     * @return el prefijo
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    String getAttributePrefix(int index);

    /**
     * El tipo declarado del atributo --{@code CDATA}, {@code ID}, {@code IDREF}...-- segun el DTD.
     *
     * <p>Sin DTD son todos {@code CDATA}, que es lo mismo que decir "texto y nada mas".
     *
     * @param index el indice, desde 0
     * @return el tipo
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    String getAttributeType(int index);

    /**
     * El valor del atributo numero {@code index}.
     *
     * @param index el indice, desde 0
     * @return el valor
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    String getAttributeValue(int index);

    /**
     * Si el atributo estaba escrito en el documento o lo puso el DTD por omision.
     *
     * <p>La distincion importa para volver a escribir el documento: un atributo que vino de un valor
     * por omision no hace falta escribirlo, y escribirlo cambia el documento.
     *
     * @param index el indice, desde 0
     * @return true si estaba escrito
     * @throws IllegalStateException si el cursor no esta en un estado donde haya atributos
     */
    boolean isAttributeSpecified(int index);

    /**
     * Cuantas declaraciones de espacio de nombres hay en este evento.
     *
     * <p>Solo las declaradas **en este elemento**, no las heredadas: para las heredadas esta
     * {@link #getNamespaceContext()}.
     *
     * <p>Vale en {@link XMLStreamConstants#START_ELEMENT}, {@link XMLStreamConstants#END_ELEMENT} y
     * {@link XMLStreamConstants#NAMESPACE}.
     *
     * @return la cantidad
     * @throws IllegalStateException si el cursor no esta en uno de esos estados
     */
    int getNamespaceCount();

    /**
     * El prefijo de la declaracion numero {@code index}, o null si es la del espacio por omision.
     *
     * @param index el indice, desde 0
     * @return el prefijo
     * @throws IllegalStateException si el cursor no esta en un estado con declaraciones
     */
    String getNamespacePrefix(int index);

    /**
     * El URI de la declaracion numero {@code index}.
     *
     * @param index el indice, desde 0
     * @return el espacio de nombres
     * @throws IllegalStateException si el cursor no esta en un estado con declaraciones
     */
    String getNamespaceURI(int index);

    /**
     * Las ligaduras prefijo/URI que valen en la posicion actual, heredadas incluidas.
     *
     * <p>El contexto es del lector, no del evento: cambia a medida que el cursor avanza, y no se
     * puede guardar para consultarlo despues.
     *
     * @return el contexto
     */
    NamespaceContext getNamespaceContext();

    /**
     * El tipo del evento actual.
     *
     * @return uno de los tipos de {@link XMLStreamConstants}
     */
    int getEventType();

    /**
     * El texto del evento actual, como {@link String}.
     *
     * <p>Vale en texto, CDATA, comentario, espacio, referencia a entidad y DTD.
     *
     * @return el texto
     * @throws IllegalStateException si el evento actual no lleva texto
     */
    String getText();

    /**
     * El mismo texto, sin copiarlo a un {@link String}.
     *
     * <p>El arreglo es **del lector** y vale hasta el proximo {@link #next()}: hay que leerlo entre
     * {@link #getTextStart()} y {@link #getTextLength()}, no entero, y no hay que guardarlo.
     * Existe para el codigo que procesa texto grande y no quiere alocar una cadena por evento.
     *
     * @return el buffer interno
     * @throws IllegalStateException si el evento actual no lleva texto
     */
    char[] getTextCharacters();

    /**
     * Copia parte del texto al arreglo del llamador.
     *
     * <p>La variante segura de {@link #getTextCharacters()}: lo que se copia es del llamador y dura
     * lo que el quiera.
     *
     * @param sourceStart desde que caracter del texto
     * @param target adonde copiar
     * @param targetStart desde que posicion del destino
     * @param length cuantos caracteres como maximo
     * @return cuantos se copiaron
     * @throws XMLStreamException si falla la lectura
     * @throws IndexOutOfBoundsException si los indices no entran en el destino
     * @throws IllegalStateException si el evento actual no lleva texto
     */
    int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException;

    /**
     * Desde que posicion del arreglo de {@link #getTextCharacters()} empieza el texto.
     *
     * @return el desplazamiento
     * @throws IllegalStateException si el evento actual no lleva texto
     */
    int getTextStart();

    /**
     * Cuantos caracteres del arreglo de {@link #getTextCharacters()} son el texto.
     *
     * @return el largo
     * @throws IllegalStateException si el evento actual no lleva texto
     */
    int getTextLength();

    /**
     * La codificacion del documento, si se pudo determinar.
     *
     * <p>Es la que el parser **dedujo o le dijeron**, y puede no ser la de la declaracion XML: para
     * esa esta {@link #getCharacterEncodingScheme()}.
     *
     * @return el nombre de la codificacion, o null
     */
    String getEncoding();

    /**
     * Si el evento actual lleva texto.
     *
     * @return true en texto, CDATA, comentario, espacio, referencia a entidad y DTD
     */
    boolean hasText();

    /**
     * Donde, en la entrada, esta el evento actual.
     *
     * @return la ubicacion; nunca null, aunque puede no tener numeros
     */
    Location getLocation();

    /**
     * El nombre calificado del elemento actual.
     *
     * <p>Vale solo en apertura y cierre de elemento; en cualquier otro estado es un error del
     * llamador.
     *
     * @return el nombre
     * @throws IllegalStateException si el evento actual no tiene nombre
     */
    QName getName();

    /**
     * El nombre local del elemento actual, o el nombre de la entidad en una referencia.
     *
     * @return el nombre local
     * @throws IllegalStateException si el evento actual no tiene nombre
     */
    String getLocalName();

    /**
     * Si el evento actual tiene nombre.
     *
     * @return true en apertura y cierre de elemento
     */
    boolean hasName();

    /**
     * El espacio de nombres del elemento actual, o null si no tiene.
     *
     * @return el espacio de nombres
     */
    String getNamespaceURI();

    /**
     * El prefijo del elemento actual, o null si no tiene.
     *
     * @return el prefijo
     */
    String getPrefix();

    /**
     * La version declarada en la declaracion XML, o null si no habia.
     *
     * @return la version
     */
    String getVersion();

    /**
     * El valor de {@code standalone} de la declaracion XML.
     *
     * <p>Devuelve false tanto si decia {@code no} como si no habia declaracion; para distinguirlos
     * hay que preguntarle a {@link #standaloneSet()}.
     *
     * @return true si el documento se declaro standalone
     */
    boolean isStandalone();

    /**
     * Si la declaracion XML traia {@code standalone}.
     *
     * @return true si estaba escrito
     */
    boolean standaloneSet();

    /**
     * La codificacion **declarada** en la declaracion XML, o null si no habia.
     *
     * @return el nombre de la codificacion
     */
    String getCharacterEncodingScheme();

    /**
     * El destino de la instruccion de proceso actual.
     *
     * @return el destino, o null si el evento no es una instruccion de proceso
     */
    String getPITarget();

    /**
     * Los datos de la instruccion de proceso actual.
     *
     * @return los datos, o null si el evento no es una instruccion de proceso
     */
    String getPIData();
}
