package org.xml.sax.helpers;

import java.io.IOException;
import java.util.Locale;

import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.XMLReaderAdapter -- un lector SAX2 con cara de SAX1.
//
// Es el espejo de ParserAdapter: aquel hace usable un parser viejo desde codigo nuevo, este hace
// usable un parser nuevo desde codigo viejo. Implementa Parser (SAX1) por afuera y
// ContentHandler (SAX2) por adentro, asi puede registrarse con el XMLReader que envuelve y
// traducir cada evento al pasar.
//
// No analiza nada. Todo lo que sigue rio abajo de parse() es trabajo del XMLReader envuelto;
// esta clase solo tiende un puente entre dos formas del mismo evento.
//
// La traduccion es casi toda borrado, porque SAX1 sabe menos:
//
//   - startElement(uri, localName, qName, Attributes) se vuelve startElement(qName,
//     AttributeList): el URI de espacio de nombres y el nombre local se descartan y lo que ve
//     SAX1 es el nombre calificado, es decir el nombre tal cual esta escrito en el documento.
//   - startPrefixMapping/endPrefixMapping se descartan por completo: SAX1 no tiene esos eventos,
//     y la informacion que llevan sobrevive solo como atributos xmlns.
//   - skippedEntity se descarta por la misma razon.
//
// Para que eso funcione el adaptador reconfigura el lector en setupXMLReader(), y las dos
// features que toca no son opcionales por gusto:
//
//   namespace-prefixes = true    conservar los atributos xmlns en la lista de atributos, ya que
//                                un manejador SAX1 no tiene otra forma de enterarse de los
//                                espacios de nombres. Esta es obligatoria; si el lector se
//                                niega, el analisis falla.
//   namespaces         = false   no gastar en calcular URIs que nadie rio abajo va a ver. Esta
//                                es una optimizacion, asi que una negativa se traga.
//
// setLocale() siempre lanza SAXNotSupportedException: SAX2 dejo de lado la negociacion de
// locale, asi que no hay a quien reenviarla. Contestar otra cosa seria afirmar algo que el
// lector envuelto no puede cumplir.
//
// NOTA DE COMPILACION, y no es cosmetica: `ContentHandler` esta escrito con nombre completo en la
// clausula `implements` de abajo. El javac de esta casa, cuando recibe en la MISMA invocacion el
// fuente de org/xml/sax/ContentHandler.java y este archivo, ignora el `import
// org.xml.sax.ContentHandler` de aca y resuelve el nombre simple contra java.net.ContentHandler,
// que existe y es otra cosa. El .class sale declarando que implementa la interfaz equivocada:
// compila, mide bien, y despues `x instanceof ContentHandler` da false y ningun parser acepta
// esta clase como manejador. Compilando este archivo solo no pasa; el proyecto pide compilar los
// tipos que se referencian entre si en una sola invocacion, asi que la salida es calificar.
// Es el bug #466 del informe, con el repro y la ablacion del disparador; que ademas salga en
// silencio en vez de dar un error de compilacion es el #467.
public class XMLReaderAdapter
        implements Parser, org.xml.sax.ContentHandler {

    XMLReader xmlReader;
    DocumentHandler documentHandler;
    AttributesAdapter qAtts;

    // Envuelve lo que encuentre XMLReaderFactory. En KajiLibrary eso quiere decir el driver que
    // nombra la propiedad de sistema `org.xml.sax.driver`, ya que no viene ningun parser
    // incluido; sin nada configurado, la SAXException sale de la fabrica y se pasa derecho.
    public XMLReaderAdapter() throws SAXException {
        setup(XMLReaderFactory.createXMLReader());
    }

    public XMLReaderAdapter(XMLReader xmlReader) {
        setup(xmlReader);
    }

    private void setup(XMLReader xmlReader) {
        if (xmlReader == null) {
            throw new NullPointerException("XMLReader must not be null");
        }
        this.xmlReader = xmlReader;
        qAtts = new AttributesAdapter();
    }

    ////////////////////////////////////////////////////////////////////
    // Parser (SAX1), la cara hacia afuera
    ////////////////////////////////////////////////////////////////////

    // No existe equivalente en SAX2, asi que lo unico honesto es negarse.
    public void setLocale(Locale locale) throws SAXException {
        throw new SAXNotSupportedException("setLocale not supported");
    }

    // Estos tres pasan derecho: las interfaces no cambian entre SAX1 y SAX2.
    public void setEntityResolver(EntityResolver resolver) {
        xmlReader.setEntityResolver(resolver);
    }

    public void setDTDHandler(DTDHandler handler) {
        xmlReader.setDTDHandler(handler);
    }

    public void setErrorHandler(ErrorHandler handler) {
        xmlReader.setErrorHandler(handler);
    }

    // Este no: el manejador de contenido del lector es *este* adaptador, y el manejador SAX1 se
    // guarda aca para reenviarle.
    public void setDocumentHandler(DocumentHandler handler) {
        documentHandler = handler;
    }

    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    public void parse(InputSource input) throws IOException, SAXException {
        setupXMLReader();
        xmlReader.parse(input);
    }

    // La negociacion de features descripta en el comentario de la clase.
    private void setupXMLReader() throws SAXException {
        // Obligatorio: sin los atributos xmlns un manejador SAX1 esta ciego a los espacios de
        // nombres.
        xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes",
                             true);
        try {
            xmlReader.setFeature("http://xml.org/sax/features/namespaces",
                                 false);
        } catch (SAXException e) {
            // Opcional: esto solo le ahorra trabajo al lector, y un lector que insista en
            // procesar espacios de nombres igual produce los eventos que hacen falta.
        }
        xmlReader.setContentHandler(this);
    }

    ////////////////////////////////////////////////////////////////////
    // ContentHandler (SAX2), la cara hacia adentro
    ////////////////////////////////////////////////////////////////////

    public void setDocumentLocator(Locator locator) {
        if (documentHandler != null) {
            documentHandler.setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        if (documentHandler != null) {
            documentHandler.startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (documentHandler != null) {
            documentHandler.endDocument();
        }
    }

    // Descartado: SAX1 no tiene eventos de mapeo de prefijos. Declarado sin lanzar nada, a
    // diferencia de la interfaz, porque aca no hay nada que pueda fallar.
    public void startPrefixMapping(String prefix, String uri) {
    }

    public void endPrefixMapping(String prefix) {
    }

    // La unica traduccion de verdad: tres partes del nombre se vuelven una, y el Attributes de
    // SAX2 se envuelve en vez de copiarse. La envoltura se reusa entre elementos, asi que un
    // manejador SAX1 que se quede con el AttributeList mas alla del final de startElement esta
    // mirando los atributos del elemento siguiente: la regla de siempre en SAX, y la razon de
    // que AttributeListImpl tenga constructor de copia.
    public void startElement(String uri, String localName,
                             String qName, Attributes atts)
            throws SAXException {
        if (documentHandler != null) {
            qAtts.setAttributes(atts);
            documentHandler.startElement(qName, qAtts);
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.endElement(qName);
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.ignorableWhitespace(ch, start, length);
        }
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.processingInstruction(target, data);
        }
    }

    // Descartado: SAX1 no tiene la nocion de entidad salteada.
    public void skippedEntity(String name) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////

    // Un Attributes de SAX2 visto por la cerradura de AttributeList de SAX1: los nombres son
    // nombres calificados, y las dos busquedas que saben de espacios de nombres directamente no
    // existen para preguntarles.
    final class AttributesAdapter implements AttributeList {

        private Attributes atts;

        AttributesAdapter() {
        }

        void setAttributes(Attributes atts) {
            this.atts = atts;
        }

        public int getLength() {
            return atts.getLength();
        }

        // El nombre calificado, que es lo que SAX1 entiende por "el nombre".
        public String getName(int i) {
            return atts.getQName(i);
        }

        public String getType(int i) {
            return atts.getType(i);
        }

        public String getValue(int i) {
            return atts.getValue(i);
        }

        public String getType(String qName) {
            return atts.getType(qName);
        }

        public String getValue(String qName) {
            return atts.getValue(qName);
        }
    }
}
