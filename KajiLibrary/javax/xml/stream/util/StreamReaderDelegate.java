package javax.xml.stream.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * KajiLibrary's javax.xml.stream.util.StreamReaderDelegate -- un lector de cursor que reenvia todo
 * a otro.
 *
 * <h2>Para que sirve una clase que no hace nada</h2>
 *
 * <p>{@link XMLStreamReader} tiene cuarenta y pico de metodos. Quien quiera interceptar <b>uno</b>
 * --contar elementos, normalizar el texto al pasar, saltear una rama-- tendria que escribir los
 * otros cuarenta a mano si implementara la interfaz directamente. Esta clase los escribe una vez;
 * la subclase redefine el que le importa y hereda el resto.
 *
 * <p>Es el patron decorador con la parte aburrida ya hecha, y el mismo que
 * {@link java.io.FilterInputStream} usa para los flujos.
 *
 * <h2>El constructor sin argumentos y el {@link #setParent}</h2>
 *
 * <p>Se puede construir sin lector y ponerlo despues. Sirve cuando la subclase necesita calcular
 * algo antes de saber a quien envuelve, o cuando el mismo decorador se reusa sobre lectores
 * sucesivos. El precio es que entre la construccion y el {@code setParent} cualquier llamada revienta
 * con {@link NullPointerException}, que es la falla correcta: usar un decorador sin nada abajo es un
 * error del llamador, no un estado que valga la pena representar.
 *
 * <p>Los metodos no estan sincronizados y el campo del padre no es {@code volatile}: cambiar el
 * padre mientras otro hilo lee no es un uso previsto ni aca ni en el original.
 */
public class StreamReaderDelegate implements XMLStreamReader {

    /** A quien se le reenvia todo. */
    private XMLStreamReader reader;

    /**
     * Un decorador sin lector todavia.
     *
     * <p>Hay que llamar a {@link #setParent} antes de usarlo; ver el encabezado.
     */
    public StreamReaderDelegate() {
    }

    /**
     * Un decorador sobre el lector dado.
     *
     * @param reader el lector de abajo
     */
    public StreamReaderDelegate(XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * Cambia el lector de abajo.
     *
     * @param reader el lector nuevo
     */
    public void setParent(XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * El lector de abajo.
     *
     * @return el lector, o null si todavia no se puso
     */
    public XMLStreamReader getParent() {
        return reader;
    }

    // ---- todo lo demas es reenvio -----------------------------------------------------------

    /** {@inheritDoc} */
    public int next() throws XMLStreamException {
        return reader.next();
    }

    /** {@inheritDoc} */
    public int nextTag() throws XMLStreamException {
        return reader.nextTag();
    }

    /** {@inheritDoc} */
    public String getElementText() throws XMLStreamException {
        return reader.getElementText();
    }

    /** {@inheritDoc} */
    public void require(int type, String namespaceURI, String localName)
            throws XMLStreamException {
        reader.require(type, namespaceURI, localName);
    }

    /** {@inheritDoc} */
    public boolean hasNext() throws XMLStreamException {
        return reader.hasNext();
    }

    /** {@inheritDoc} */
    public void close() throws XMLStreamException {
        reader.close();
    }

    /** {@inheritDoc} */
    public String getNamespaceURI(String prefix) {
        return reader.getNamespaceURI(prefix);
    }

    /** {@inheritDoc} */
    public NamespaceContext getNamespaceContext() {
        return reader.getNamespaceContext();
    }

    /** {@inheritDoc} */
    public boolean isStartElement() {
        return reader.isStartElement();
    }

    /** {@inheritDoc} */
    public boolean isEndElement() {
        return reader.isEndElement();
    }

    /** {@inheritDoc} */
    public boolean isCharacters() {
        return reader.isCharacters();
    }

    /** {@inheritDoc} */
    public boolean isWhiteSpace() {
        return reader.isWhiteSpace();
    }

    /** {@inheritDoc} */
    public String getAttributeValue(String namespaceUri, String localName) {
        return reader.getAttributeValue(namespaceUri, localName);
    }

    /** {@inheritDoc} */
    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    /** {@inheritDoc} */
    public QName getAttributeName(int index) {
        return reader.getAttributeName(index);
    }

    /** {@inheritDoc} */
    public String getAttributePrefix(int index) {
        return reader.getAttributePrefix(index);
    }

    /** {@inheritDoc} */
    public String getAttributeNamespace(int index) {
        return reader.getAttributeNamespace(index);
    }

    /** {@inheritDoc} */
    public String getAttributeLocalName(int index) {
        return reader.getAttributeLocalName(index);
    }

    /** {@inheritDoc} */
    public String getAttributeType(int index) {
        return reader.getAttributeType(index);
    }

    /** {@inheritDoc} */
    public String getAttributeValue(int index) {
        return reader.getAttributeValue(index);
    }

    /** {@inheritDoc} */
    public boolean isAttributeSpecified(int index) {
        return reader.isAttributeSpecified(index);
    }

    /** {@inheritDoc} */
    public int getNamespaceCount() {
        return reader.getNamespaceCount();
    }

    /** {@inheritDoc} */
    public String getNamespacePrefix(int index) {
        return reader.getNamespacePrefix(index);
    }

    /** {@inheritDoc} */
    public String getNamespaceURI(int index) {
        return reader.getNamespaceURI(index);
    }

    /** {@inheritDoc} */
    public int getEventType() {
        return reader.getEventType();
    }

    /** {@inheritDoc} */
    public String getText() {
        return reader.getText();
    }

    /** {@inheritDoc} */
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException {
        return reader.getTextCharacters(sourceStart, target, targetStart, length);
    }

    /** {@inheritDoc} */
    public char[] getTextCharacters() {
        return reader.getTextCharacters();
    }

    /** {@inheritDoc} */
    public int getTextStart() {
        return reader.getTextStart();
    }

    /** {@inheritDoc} */
    public int getTextLength() {
        return reader.getTextLength();
    }

    /** {@inheritDoc} */
    public String getEncoding() {
        return reader.getEncoding();
    }

    /** {@inheritDoc} */
    public boolean hasText() {
        return reader.hasText();
    }

    /** {@inheritDoc} */
    public Location getLocation() {
        return reader.getLocation();
    }

    /** {@inheritDoc} */
    public QName getName() {
        return reader.getName();
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return reader.getLocalName();
    }

    /** {@inheritDoc} */
    public boolean hasName() {
        return reader.hasName();
    }

    /** {@inheritDoc} */
    public String getNamespaceURI() {
        return reader.getNamespaceURI();
    }

    /** {@inheritDoc} */
    public String getPrefix() {
        return reader.getPrefix();
    }

    /** {@inheritDoc} */
    public String getVersion() {
        return reader.getVersion();
    }

    /** {@inheritDoc} */
    public boolean isStandalone() {
        return reader.isStandalone();
    }

    /** {@inheritDoc} */
    public boolean standaloneSet() {
        return reader.standaloneSet();
    }

    /** {@inheritDoc} */
    public String getCharacterEncodingScheme() {
        return reader.getCharacterEncodingScheme();
    }

    /** {@inheritDoc} */
    public String getPITarget() {
        return reader.getPITarget();
    }

    /** {@inheritDoc} */
    public String getPIData() {
        return reader.getPIData();
    }

    /** {@inheritDoc} */
    public Object getProperty(String name) {
        return reader.getProperty(name);
    }
}
