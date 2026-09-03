package org.xml.sax.helpers;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// KajiLibrary's org.xml.sax.helpers.DefaultHandler -- la clase base de SAX2 que la gente
// realmente extiende.
//
// Implementa EntityResolver, DTDHandler, ContentHandler y ErrorHandler con cuerpos vacios, asi
// que el manejador tipico de "solo me importan startElement y characters" son dos metodos y no
// diecisiete. Se le pasa una misma instancia a setContentHandler, setErrorHandler, setDTDHandler y
// setEntityResolver y el parser queda enteramente conectado.
//
// Es la contraparte de SAX2 del viejo org.xml.sax.HandlerBase, y se diferencia de el exactamente
// en los lugares donde SAX2 se diferencia de SAX1: ContentHandler en vez de DocumentHandler, con
// lo que aca hay startPrefixMapping/endPrefixMapping/skippedEntity que HandlerBase ni conoce, y
// startElement lleva (uri, localName, qName, Attributes) en vez de un nombre pelado y un
// AttributeList.
//
// Dos de las respuestas por omision son decisiones, no huecos:
//
//   - resolveEntity devuelve null, que significa "sin sustitucion, abri vos mismo el system id".
//   - warning() y error() vuelven calladas, asi que los problemas recuperables se aceptan en
//     silencio; solo fatalError() tira, relanzando lo que le dieron. Un error fatal termina el
//     analisis por definicion, y tragarselo dejaria a quien llama creyendo que el documento se
//     leyo.
//
// A diferencia de HandlerBase.resolveEntity, esta conserva IOException en su clausula throws,
// porque una subclase que abre un archivo o una URL para contestar necesita algun lugar donde
// poner la falla.
//
// ContentHandler.declaration() no se redefine aca: es un metodo default de la interfaz cuyo
// cuerpo por omision ya no hace nada, que es la misma respuesta que daria esta clase.
// Redefinirlo con un cuerpo vacio agregaria un miembro que el JDK no declara en esta clase.
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
public class DefaultHandler
        implements EntityResolver, DTDHandler,
                   org.xml.sax.ContentHandler, ErrorHandler {

    public DefaultHandler() {
    }

    ////////////////////////////////////////////////////////////////////
    // EntityResolver
    ////////////////////////////////////////////////////////////////////

    public InputSource resolveEntity(String publicId, String systemId)
            throws IOException, SAXException {
        return null;
    }

    ////////////////////////////////////////////////////////////////////
    // DTDHandler
    ////////////////////////////////////////////////////////////////////

    public void notationDecl(String name, String publicId, String systemId)
            throws SAXException {
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName)
            throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // ContentHandler
    ////////////////////////////////////////////////////////////////////

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // ErrorHandler
    ////////////////////////////////////////////////////////////////////

    public void warning(SAXParseException e) throws SAXException {
    }

    public void error(SAXParseException e) throws SAXException {
    }

    // El que no se queda callado, por la razon que da el comentario de la clase.
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
