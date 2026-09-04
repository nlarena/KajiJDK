// Prueba de comportamiento de java.nio.charset.spi, org.w3c.dom.bootstrap,
// javax.xml.transform.{dom,sax,stax} y org.w3c.dom.ls.
//
// `run()` devuelve -1 si pasa todo, o el indice del primer caso que falla. Los numeros no se
// reciclan: si se agrega un caso, va al final.
//
// Todo lo de aca corre igual en el JDK y en Kaji. Lo que difiere a proposito --que
// `DOMImplementationRegistry` no encuentre ninguna implementacion, porque esta biblioteca no trae
// DOM incluido-- esta en `runKaji()`.

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.xml.sax.InputSource;

public class XmlPkgTest {

    public static int run() {
        // --- javax.xml.transform.dom.DOMSource ---
        DOMSource emptySource = new DOMSource();
        if (!emptySource.isEmpty()) return 0;
        if (emptySource.getNode() != null) return 1;
        if (emptySource.getSystemId() != null) return 2;
        // Un identificador solo ya la hace no vacia, aunque no haya arbol: ver DOMSource#isEmpty.
        emptySource.setSystemId("sid");
        if (emptySource.isEmpty()) return 3;
        if (!"sid".equals(emptySource.getSystemId())) return 4;
        if (!new DOMSource(null).isEmpty()) return 5;
        if (!"http://javax.xml.transform.dom.DOMSource/feature".equals(DOMSource.FEATURE)) return 6;

        // --- javax.xml.transform.dom.DOMResult ---
        DOMResult emptyResult = new DOMResult();
        if (emptyResult.getNode() != null) return 7;
        if (emptyResult.getNextSibling() != null) return 8;
        if (emptyResult.getSystemId() != null) return 9;
        if (!"http://javax.xml.transform.dom.DOMResult/feature".equals(DOMResult.FEATURE)) return 10;
        // Un hermano sin nodo que lo contenga no describe ningun lugar. En el constructor es un
        // argumento incoherente; en el setter, un estado incoherente. Dos excepciones distintas.
        if (!resultCtorRejects()) return 11;
        if (!resultSetterRejects()) return 12;

        // --- javax.xml.transform.sax.SAXSource ---
        SAXSource emptySax = new SAXSource();
        if (!emptySax.isEmpty()) return 13;
        if (emptySax.getInputSource() != null) return 14;
        if (emptySax.getXMLReader() != null) return 15;
        if (emptySax.getSystemId() != null) return 16;
        // Un InputSource recien construido esta tan vacio como ninguno.
        if (!new SAXSource(new InputSource()).isEmpty()) return 17;
        if (new SAXSource(new InputSource("x")).isEmpty()) return 18;
        if (new SAXSource(new InputSource(new StringReader("<a/>"))).isEmpty()) return 19;
        if (!"x".equals(new SAXSource(new InputSource("x")).getSystemId())) return 20;
        // setSystemId sobre una fuente vacia crea el InputSource.
        SAXSource made = new SAXSource();
        made.setSystemId("y");
        if (!"y".equals(made.getSystemId())) return 21;
        if (made.getInputSource() == null) return 22;
        if (made.isEmpty()) return 23;

        // sourceToInputSource: convierte lo que puede y contesta null con lo que no.
        InputSource fromStream = SAXSource.sourceToInputSource(new StreamSource("z"));
        if (fromStream == null || !"z".equals(fromStream.getSystemId())) return 24;
        InputSource fromReader = SAXSource.sourceToInputSource(
            new StreamSource(new StringReader("<a/>"), "sid"));
        if (fromReader == null) return 25;
        if (!"sid".equals(fromReader.getSystemId())) return 26;
        if (fromReader.getCharacterStream() == null) return 27;
        if (fromReader.getByteStream() != null) return 28;
        InputSource fromBytes = SAXSource.sourceToInputSource(
            new StreamSource(new ByteArrayInputStream(new byte[] {60, 97, 47, 62})));
        if (fromBytes == null) return 29;
        if (fromBytes.getByteStream() == null) return 30;
        if (fromBytes.getCharacterStream() != null) return 31;
        InputSource same = new InputSource("w");
        if (SAXSource.sourceToInputSource(new SAXSource(same)) != same) return 32;
        // Un DOMSource no se puede leer por eventos, y eso es una respuesta, no un error.
        if (SAXSource.sourceToInputSource(new DOMSource()) != null) return 33;
        if (SAXSource.sourceToInputSource((Source) null) != null) return 34;

        // --- javax.xml.transform.sax.SAXResult ---
        SAXResult saxResult = new SAXResult();
        if (saxResult.getHandler() != null) return 35;
        if (saxResult.getLexicalHandler() != null) return 36;
        if (saxResult.getSystemId() != null) return 37;
        if (!"http://javax.xml.transform.sax.SAXResult/feature".equals(SAXResult.FEATURE)) return 38;
        if (!"http://javax.xml.transform.sax.SAXTransformerFactory/feature"
                .equals(SAXTransformerFactory.FEATURE)) return 39;
        if (!"http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter"
                .equals(SAXTransformerFactory.FEATURE_XMLFILTER)) return 40;
        if (!"http://javax.xml.transform.sax.SAXSource/feature".equals(SAXSource.FEATURE)) return 41;

        // --- org.w3c.dom.bootstrap ---
        if (!"org.w3c.dom.DOMImplementationSourceList"
                .equals(DOMImplementationRegistry.PROPERTY)) return 42;
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            if (registry == null) return 43;
            // Caracteristicas que nadie soporta: null y lista vacia, no una excepcion.
            if (registry.getDOMImplementation("NoTal 9.9") != null) return 44;
            if (registry.getDOMImplementationList("NoTal 9.9") == null) return 45;
            if (registry.getDOMImplementationList("NoTal 9.9").getLength() != 0) return 46;
            // Fuera de rango es null, no una excepcion: es lo que pide el DOM.
            if (registry.getDOMImplementationList("NoTal 9.9").item(0) != null) return 47;
            try {
                registry.addSource(null);
                return 48;
            } catch (NullPointerException expected) {
                // asi tiene que ser
            }
        } catch (Exception e) {
            return 49;
        }

        // --- javax.xml.transform.stax, sin depender de que haya una implementacion StAX ---
        if (!"http://javax.xml.transform.stax.StAXSource/feature"
                .equals(javax.xml.transform.stax.StAXSource.FEATURE)) return 50;
        if (!"http://javax.xml.transform.stax.StAXResult/feature"
                .equals(javax.xml.transform.stax.StAXResult.FEATURE)) return 51;
        // Null es un argumento invalido, no un estado invalido.
        try {
            new javax.xml.transform.stax.StAXSource((javax.xml.stream.XMLStreamReader) null);
            return 52;
        } catch (IllegalArgumentException expected) {
            // asi tiene que ser
        }
        try {
            new javax.xml.transform.stax.StAXSource((javax.xml.stream.XMLEventReader) null);
            return 53;
        } catch (IllegalArgumentException expected) {
            // asi tiene que ser
        } catch (Exception e) {
            return 54;
        }
        try {
            new javax.xml.transform.stax.StAXResult((javax.xml.stream.XMLStreamWriter) null);
            return 55;
        } catch (IllegalArgumentException expected) {
            // asi tiene que ser
        }
        try {
            new javax.xml.transform.stax.StAXResult((javax.xml.stream.XMLEventWriter) null);
            return 56;
        } catch (IllegalArgumentException expected) {
            // asi tiene que ser
        }

        return -1;
    }

    /**
     * Los casos que <b>no</b> corren en el JDK.
     *
     * <p>Uno solo, y es la diferencia declarada: alla el registro encuentra la implementacion DOM
     * incluida en la plataforma y aca no hay ninguna, asi que {@code getDOMImplementation("XML 3.0")}
     * contesta null. Es una respuesta definida del contrato --"no hay ninguna con esas
     * caracteristicas"-- y no una falla, pero no se puede afirmar en los dos lados.
     *
     * @return el indice del primer caso que falla, o -1
     */
    public static int runKaji() {
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            if (registry.getDOMImplementation("XML 3.0") != null) return 57;
            if (registry.getDOMImplementationList("XML 3.0").getLength() != 0) return 58;
        } catch (Exception e) {
            return 59;
        }
        return -1;
    }

    /** Si el constructor rechaza un hermano que no cuelga del nodo. */
    static boolean resultCtorRejects() {
        try {
            // Sin nodo, cualquier hermano sobra. No hace falta un arbol para probarlo.
            new DOMResult(null, new FakeNode());
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Si el setter rechaza lo mismo, con la otra excepcion. */
    static boolean resultSetterRejects() {
        try {
            new DOMResult().setNextSibling(new FakeNode());
            return false;
        } catch (IllegalStateException expected) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Un nodo suelto, sin padre.
     *
     * <p>Es todo lo que hace falta para probar la validacion de {@link DOMResult}, y evita depender
     * de que haya una implementacion de DOM para construir un documento de verdad -- que es
     * justamente lo que esta biblioteca no tiene.
     */
    static class FakeNode implements org.w3c.dom.Node {
        public String getNodeName() { return "fake"; }
        public String getNodeValue() { return null; }
        public void setNodeValue(String nodeValue) { }
        public short getNodeType() { return ELEMENT_NODE; }
        public org.w3c.dom.Node getParentNode() { return null; }
        public org.w3c.dom.NodeList getChildNodes() { return null; }
        public org.w3c.dom.Node getFirstChild() { return null; }
        public org.w3c.dom.Node getLastChild() { return null; }
        public org.w3c.dom.Node getPreviousSibling() { return null; }
        public org.w3c.dom.Node getNextSibling() { return null; }
        public org.w3c.dom.NamedNodeMap getAttributes() { return null; }
        public org.w3c.dom.Document getOwnerDocument() { return null; }
        public org.w3c.dom.Node insertBefore(org.w3c.dom.Node n, org.w3c.dom.Node r) { return null; }
        public org.w3c.dom.Node replaceChild(org.w3c.dom.Node n, org.w3c.dom.Node o) { return null; }
        public org.w3c.dom.Node removeChild(org.w3c.dom.Node old) { return null; }
        public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) { return null; }
        public boolean hasChildNodes() { return false; }
        public org.w3c.dom.Node cloneNode(boolean deep) { return null; }
        public void normalize() { }
        public boolean isSupported(String feature, String version) { return false; }
        public String getNamespaceURI() { return null; }
        public String getPrefix() { return null; }
        public void setPrefix(String prefix) { }
        public String getLocalName() { return null; }
        public boolean hasAttributes() { return false; }
        public String getBaseURI() { return null; }
        public short compareDocumentPosition(org.w3c.dom.Node other) { return 0; }
        public String getTextContent() { return null; }
        public void setTextContent(String textContent) { }
        public boolean isSameNode(org.w3c.dom.Node other) { return this == other; }
        public String lookupPrefix(String namespaceURI) { return null; }
        public boolean isDefaultNamespace(String namespaceURI) { return false; }
        public String lookupNamespaceURI(String prefix) { return null; }
        public boolean isEqualNode(org.w3c.dom.Node arg) { return this == arg; }
        public Object getFeature(String feature, String version) { return null; }
        public Object setUserData(String key, Object data, org.w3c.dom.UserDataHandler h) {
            return null;
        }
        public Object getUserData(String key) { return null; }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
