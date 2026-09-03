package org.xml.sax;

// KajiLibrary's org.xml.sax.HandlerBase -- la base de SAX1 del estilo "redefini solo lo que te
// importa".
//
// Implementa las cuatro interfaces de manejador de SAX1 a la vez (EntityResolver, DTDHandler,
// DocumentHandler, ErrorHandler) con cuerpos que no hacen nada, asi que quien solo quiere
// startElement escribe un metodo en vez de catorce. Su reemplazo en SAX2 es
// org.xml.sax.helpers.DefaultHandler, que hace el mismo trabajo para ContentHandler; por eso
// esta clase esta deprecada en el JDK, y se conserva aca porque el contrato todavia la lista.
//
// Hay dos comportamientos por omision que vale la pena nombrar porque son decisiones y no
// olvidos:
//
//   - resolveEntity devuelve null, que le dice al parser "abri vos el identificador de sistema",
//     o sea el comportamiento comun cuando nadie interviene.
//   - error() y warning() vuelven calladas, asi que un problema no fatal se acepta en silencio;
//     solo fatalError() lanza, y relanza la excepcion que le pasaron. Esa asimetria es la regla
//     de SAX: un error fatal tiene que frenar el analisis, uno recuperable no.
//
// Notar que aca resolveEntity se declara lanzando solo SAXException y no IOException, a
// diferencia de EntityResolver.resolveEntity, que permite las dos. Achicar el conjunto de
// excepciones lanzadas en una redefinicion es legal, y el JDK lo achica aca; DefaultHandler no.
public class HandlerBase
        implements EntityResolver, DTDHandler, DocumentHandler, ErrorHandler {

    public HandlerBase() {
    }

    // Null significa "sin sustitucion": usar el identificador de sistema tal como vino.
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException {
        return null;
    }

    public void notationDecl(String name, String publicId, String systemId) {
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName) {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startElement(String name, AttributeList attributes)
            throws SAXException {
    }

    public void endElement(String name) throws SAXException {
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

    public void warning(SAXParseException e) throws SAXException {
    }

    public void error(SAXParseException e) throws SAXException {
    }

    // El unico que no se queda callado: un error fatal termina el analisis por definicion, asi
    // que tragarselo dejaria al parser sin nada que hacer y al que llamo sin noticias.
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
