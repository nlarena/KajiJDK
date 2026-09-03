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
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.XMLFilterImpl -- un eslabon en una cadena de lectores SAX.
//
// Es un XMLReader que no lee nada: tiene un XMLReader *padre* que si lee, y se para entre ese
// padre y la aplicacion. Hacia abajo parece un manejador (el padre le manda eventos); hacia
// arriba parece un lector (la aplicacion le registra manejadores y le llama parse). Todo lo que
// recibe lo pasa, sin cambiarlo, y todo lo que le preguntan se lo pregunta al padre.
//
// Por si solo eso no hace nada, y esa es justamente la idea: es una clase base. Una subclase
// redefine los dos o tres eventos que le importan, llama a super para el resto, y tiene un
// filtro andando. La forma canonica es
//
//     public void startElement(String uri, String ln, String qn, Attributes a)
//             throws SAXException {
//         super.startElement(uri, ln, qn, rewrite(a));   // reenviar no es opcional
//     }
//
// y el bug clasico es olvidarse de la llamada a super, que borra el evento del flujo en
// silencio. Cada uno de los diecisiete metodos de manejador de aca abajo reenvia; un filtro que
// se come uno se lo come para todo lo que viene rio abajo.
//
// El cableado pasa en setupParse(), que se llama al principio de las dos sobrecargas de
// parse(): el filtro se registra *a si mismo* en el padre como resolvedor de entidades,
// manejador de DTD, manejador de contenido y manejador de errores, pisando lo que hubiera. Asi
// que un manejador puesto directo en el padre se pierde en el momento en que el filtro analiza;
// los manejadores van en el filtro.
//
// Notar el almacenamiento de manejadores en dos niveles que esto genera. setContentHandler()
// sobre el filtro anota el manejador *de la aplicacion*, para reenviarle; el manejador de
// contenido del padre es el filtro mismo. Los getters contestan con el manejador de aplicacion
// anotado, no con lo que el padre tenga en este momento.
//
// Un manejador en null no es un error en ningun momento: cada metodo que reenvia chequea y no
// hace nada cuando no hay nadie escuchando. Eso es lo que hace usable un filtro antes de estar
// cableado del todo.
//
// Las llamadas de feature y propiedad pasan derecho al padre y lanzan
// SAXNotRecognizedException cuando no hay padre, porque sin nadie a quien preguntarle no se
// puede afirmar que ninguna feature sea reconocida.
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
public class XMLFilterImpl
        implements XMLFilter, EntityResolver, DTDHandler,
                   org.xml.sax.ContentHandler, ErrorHandler {

    private XMLReader parent = null;
    private Locator locator = null;
    private EntityResolver entityResolver = null;
    private DTDHandler dtdHandler = null;
    private org.xml.sax.ContentHandler contentHandler = null;
    private ErrorHandler errorHandler = null;

    // Un filtro todavia sin padre; hay que ponerle uno antes de analizar.
    public XMLFilterImpl() {
        super();
    }

    public XMLFilterImpl(XMLReader parent) {
        super();
        setParent(parent);
    }

    ////////////////////////////////////////////////////////////////////
    // XMLFilter
    ////////////////////////////////////////////////////////////////////

    public void setParent(XMLReader parent) {
        this.parent = parent;
    }

    public XMLReader getParent() {
        return parent;
    }

    ////////////////////////////////////////////////////////////////////
    // XMLReader: configuracion, toda delegada
    ////////////////////////////////////////////////////////////////////

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            parent.setFeature(name, value);
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    public boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            return parent.getFeature(name);
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            parent.setProperty(name, value);
        } else {
            throw new SAXNotRecognizedException("Property: " + name);
        }
    }

    public Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            return parent.getProperty(name);
        } else {
            throw new SAXNotRecognizedException("Property: " + name);
        }
    }

    // Estos cuatro anotan los manejadores de la aplicacion. A proposito *no* se reenvian al
    // padre: los manejadores del padre los pone en `this` setupParse().
    public void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setDTDHandler(DTDHandler handler) {
        dtdHandler = handler;
    }

    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    public void setContentHandler(org.xml.sax.ContentHandler handler) {
        contentHandler = handler;
    }

    public org.xml.sax.ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setErrorHandler(ErrorHandler handler) {
        errorHandler = handler;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    // Analizar es tarea del padre; todo lo que hace esto es interponerse antes. Un padre en null
    // da una NullPointerException, que es honesto: no hay con que analizar.
    public void parse(InputSource input) throws SAXException, IOException {
        setupParse();
        parent.parse(input);
    }

    public void parse(String systemId) throws SAXException, IOException {
        parse(new InputSource(systemId));
    }

    ////////////////////////////////////////////////////////////////////
    // EntityResolver
    ////////////////////////////////////////////////////////////////////

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        if (entityResolver != null) {
            return entityResolver.resolveEntity(publicId, systemId);
        } else {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // DTDHandler
    ////////////////////////////////////////////////////////////////////

    public void notationDecl(String name, String publicId, String systemId)
            throws SAXException {
        if (dtdHandler != null) {
            dtdHandler.notationDecl(name, publicId, systemId);
        }
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName)
            throws SAXException {
        if (dtdHandler != null) {
            dtdHandler.unparsedEntityDecl(name, publicId, systemId,
                                          notationName);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // ContentHandler: los once que no hay que olvidarse
    ////////////////////////////////////////////////////////////////////

    // Se guarda tambien aca, para que una subclase pueda preguntar donde esta sin interceptar el
    // evento.
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        if (contentHandler != null) {
            contentHandler.setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        if (contentHandler != null) {
            contentHandler.startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (contentHandler != null) {
            contentHandler.endDocument();
        }
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.startPrefixMapping(prefix, uri);
        }
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (contentHandler != null) {
            contentHandler.endPrefixMapping(prefix);
        }
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes atts) throws SAXException {
        if (contentHandler != null) {
            contentHandler.startElement(uri, localName, qName, atts);
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.endElement(uri, localName, qName);
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.ignorableWhitespace(ch, start, length);
        }
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.processingInstruction(target, data);
        }
    }

    public void skippedEntity(String name) throws SAXException {
        if (contentHandler != null) {
            contentHandler.skippedEntity(name);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // ErrorHandler
    ////////////////////////////////////////////////////////////////////

    public void warning(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.warning(e);
        }
    }

    public void error(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.error(e);
        }
    }

    // Notar que sin manejador de errores esto vuelve en silencio incluso ante un error fatal: un
    // XMLFilterImpl es un conducto, no una politica. El relanzado vive en DefaultHandler.
    public void fatalError(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.fatalError(e);
        }
    }

    ////////////////////////////////////////////////////////////////////

    // Interpone este filtro entre el padre y la aplicacion, reemplazando lo que el padre tuviera
    // registrado. Ver el comentario de la clase: los manejadores puestos directo en el padre no
    // sobreviven a esto.
    private void setupParse() {
        parent.setEntityResolver(this);
        parent.setDTDHandler(this);
        parent.setContentHandler(this);
        parent.setErrorHandler(this);
    }
}
