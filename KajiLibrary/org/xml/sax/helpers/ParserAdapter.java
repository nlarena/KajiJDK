package org.xml.sax.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.ParserAdapter -- un parser SAX1 con cara de SAX2.
//
// Implementa XMLReader (SAX2) por afuera y DocumentHandler (SAX1) por adentro: se registra con
// el parser viejo que envuelve, recibe los eventos viejos y los vuelve a emitir como nuevos. No
// analiza nada por su cuenta; toda la lectura la hace el Parser envuelto.
//
// Toda la razon de ser es lo unico que SAX1 no hace: espacios de nombres. Un parser SAX1 reporta
// `<xsl:template match="/">` con el nombre "xsl:template" y una lista de atributos que todavia
// contiene las declaraciones xmlns, y no sabe nada de lo que significa "xsl". Este adaptador
// corre un NamespaceSupport en paralelo y convierte eso en el (uri, localName, qName) de SAX2
// mas los eventos startPrefixMapping/endPrefixMapping.
//
// Todo el trabajo esta en startElement, y son dos pasadas sobre la lista de atributos a
// proposito. **La primera pasada atiende solo las declaraciones xmlns**, porque una declaracion
// hecha en este elemento aplica al nombre del elemento mismo y a sus otros atributos; resolver
// cualquier nombre antes de que esten todas las declaraciones lo resolveria contra las
// vinculaciones del padre. Recien despues la segunda pasada copia los atributos de verdad con
// las vinculaciones ya definitivas. Hacerlo en una sola pasada es la forma clasica de equivocarse
// con `<a xmlns:p="u" p:x="1">`.
//
// Se reconocen tres features de SAX2, y solo esas tres:
//
//   namespaces          (default true)   hacer el procesamiento, para empezar
//   namespace-prefixes  (default false)  pasar ademas los atributos xmlns hacia SAX2
//   xmlns-uris          (default false)  y darle a esos atributos el espacio de nombres NSDECL
//
// Las dos primeras no pueden estar las dos en false --sin ninguna, el nombre de un elemento no
// tendria ni forma resuelta ni forma cruda--, asi que apagar una prende la otra. Ninguna se
// puede cambiar en medio del analisis; eso lanza SAXNotSupportedException en vez de producir un
// documento analizado bajo dos reglas distintas. No se reconoce ninguna propiedad: no hay nada
// abajo a quien preguntarle.
//
// Un prefijo no declarado se le reporta al ErrorHandler como error recuperable, no se lanza. En
// un *atributo* el adaptador va mas lejos y conserva el atributo, con URI vacio y el nombre
// crudo en el casillero del nombre local, para que un prefijo mal puesto no borre en silencio un
// atributo del documento. Los errores de la segunda pasada se juntan y se reportan despues de
// las pasadas y no en medio del ciclo, asi el manejador ve una lista de atributos consistente.
//
// NOTA DE COMPILACION, y no es cosmetica -- la misma que lleva DefaultHandler.java: el campo
// `contentHandler` y las firmas de `setContentHandler`/`getContentHandler` escriben
// `org.xml.sax.ContentHandler` con nombre completo aunque el `import` de arriba ya lo trae. No es
// redundancia: es el rodeo del bug #466 del informe. Cuando el fuente de
// org/xml/sax/ContentHandler.java entra en la MISMA invocacion que este archivo, nuestro javac
// ignora el import de un solo tipo y resuelve el nombre simple contra java.net.ContentHandler,
// que existe en el arbol y es una clase abstracta, no la interfaz. Y como tampoco comprueba que
// lo que va en un `implements` sea una interfaz (#467), sale un .class que compila, mide bien,
// corre en nuestra VM y muere con IncompatibleClassChangeError en una JVM real. **No lo
// "limpies" a nombre simple.**
public class ParserAdapter implements XMLReader, DocumentHandler {

    private static final String FEATURES = "http://xml.org/sax/features/";
    private static final String NAMESPACES = FEATURES + "namespaces";
    private static final String NAMESPACE_PREFIXES =
        FEATURES + "namespace-prefixes";
    private static final String XMLNS_URIs = FEATURES + "xmlns-uris";

    private NamespaceSupport nsSupport = new NamespaceSupport();
    private AttributeListAdapter attAdapter;

    private boolean parsing = false;
    private String nameParts[] = new String[3];

    private Parser parser = null;
    private AttributesImpl atts = null;

    // Las banderas de features. Los valores por defecto son los de SAX2: procesar espacios de
    // nombres, esconder las declaraciones.
    private boolean namespaces = true;
    private boolean prefixes = false;
    private boolean uris = false;

    Locator locator;
    EntityResolver entityResolver = null;
    DTDHandler dtdHandler = null;
    org.xml.sax.ContentHandler contentHandler = null;
    ErrorHandler errorHandler = null;

    // Envuelve lo que encuentre ParserFactory, es decir la clase que nombra la propiedad de
    // sistema `org.xml.sax.parser`. Cada forma en que eso puede fallar se vuelve aca una
    // SAXException, que es la diferencia entre las convenciones de error de SAX1 y SAX2.
    public ParserAdapter() throws SAXException {
        super();

        String driver = System.getProperty("org.xml.sax.parser");

        try {
            setup(ParserFactory.makeParser());
        } catch (ClassNotFoundException e1) {
            throw new SAXException("Cannot find SAX1 driver class "
                                   + driver, e1);
        } catch (IllegalAccessException e2) {
            throw new SAXException("SAX1 driver class " + driver
                                   + " found but cannot be loaded", e2);
        } catch (InstantiationException e3) {
            throw new SAXException("SAX1 driver class " + driver
                                   + " loaded but cannot be instantiated", e3);
        } catch (ClassCastException e4) {
            throw new SAXException("SAX1 driver class " + driver
                                   + " does not implement org.xml.sax.Parser");
        } catch (NullPointerException e5) {
            throw new SAXException("System property org.xml.sax.parser not"
                                   + " specified");
        }
    }

    public ParserAdapter(Parser parser) {
        super();
        setup(parser);
    }

    private void setup(Parser parser) {
        if (parser == null) {
            throw new NullPointerException("Parser argument must not be null");
        }
        this.parser = parser;
        atts = new AttributesImpl();
        nameParts = new String[3];
        attAdapter = new AttributeListAdapter();
    }

    ////////////////////////////////////////////////////////////////////
    // XMLReader: configuracion
    ////////////////////////////////////////////////////////////////////

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            checkNotParsing("feature", name);
            namespaces = value;
            // Las dos apagadas no significa nada: quien llama no recibiria los nombres en
            // ninguna de las dos formas.
            if (!namespaces && !prefixes) {
                prefixes = true;
            }
        } else if (name.equals(NAMESPACE_PREFIXES)) {
            checkNotParsing("feature", name);
            prefixes = value;
            if (!prefixes && !namespaces) {
                namespaces = true;
            }
        } else if (name.equals(XMLNS_URIs)) {
            checkNotParsing("feature", name);
            uris = value;
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    public boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            return namespaces;
        } else if (name.equals(NAMESPACE_PREFIXES)) {
            return prefixes;
        } else if (name.equals(XMLNS_URIs)) {
            return uris;
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    // Un parser SAX1 no tiene propiedades, asi que no se puede reconocer ninguna. Afirmar otra
    // cosa seria afirmar algo del parser de abajo que esta clase no puede saber.
    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException("Property: " + name);
    }

    public Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException("Property: " + name);
    }

    // Los manejadores se guardan aca y setupParser() se los empuja al parser al momento de
    // analizar, salvo el manejador de documento, que siempre es este adaptador.
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

    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    // La reentrada se rechaza de plano: la pila de espacios de nombres y el buffer de atributos
    // son estado de un analisis, y un segundo parse sobre el mismo adaptador pisotearia al
    // primero.
    public void parse(InputSource input) throws IOException, SAXException {
        if (parsing) {
            throw new SAXException("Parser is already in use");
        }
        setupParser();
        parsing = true;
        try {
            parser.parse(input);
        } finally {
            parsing = false;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // DocumentHandler: los eventos SAX1 que llegan del parser envuelto
    ////////////////////////////////////////////////////////////////////

    // Se guarda ademas de reenviarse, porque makeException() lo necesita para darle posicion a
    // los errores.
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

    // La traduccion en dos pasadas descripta en el comentario de la clase.
    public void startElement(String qName, AttributeList qAtts)
            throws SAXException {
        // Errores encontrados al resolver nombres de atributo, retenidos hasta que terminen las
        // dos pasadas para que el manejador nunca vea una lista de atributos a medio armar.
        List<SAXException> exceptions = null;

        // Con el procesamiento de espacios de nombres apagado no hay nada que resolver: se pasa
        // la lista SAX1 derecho detras de una cara de Attributes, con uri y nombre local vacios.
        if (!namespaces) {
            if (contentHandler != null) {
                attAdapter.setAttributeList(qAtts);
                contentHandler.startElement("", "", canon(qName),
                                            attAdapter);
            }
            return;
        }

        nsSupport.pushContext();
        int length = qAtts.getLength();

        // Pasada uno: solo las declaraciones. Todas y cada una tienen que estar vigentes antes
        // de resolver cualquier nombre de este elemento.
        for (int i = 0; i < length; i++) {
            String attQName = qAtts.getName(i);

            if (!attQName.startsWith("xmlns")) {
                continue;
            }

            String prefix;
            int n = attQName.indexOf(':');

            if (n == -1 && attQName.length() == 5) {
                // Exactamente `xmlns`: el espacio de nombres por defecto.
                prefix = "";
            } else if (n != 5) {
                // Algo como `xmlnsfoo` o `xmlnsf:oo`: empieza con las cinco letras pero no es una
                // declaracion. La norma no habla de estos; SAX los ignora.
                continue;
            } else {
                // `xmlns:foo`.
                prefix = attQName.substring(n + 1);
            }

            String value = qAtts.getValue(i);
            if (!nsSupport.declarePrefix(prefix, value)) {
                // El prefijo era "xml" o "xmlns", que no se pueden redeclarar.
                reportError("Illegal Namespace prefix: " + prefix);
                continue;
            }
            if (contentHandler != null) {
                contentHandler.startPrefixMapping(prefix, value);
            }
        }

        // Pasada dos: los atributos propiamente dichos, resueltos contra las vinculaciones que
        // ahora estan completas.
        atts.clear();
        for (int i = 0; i < length; i++) {
            String attQName = qAtts.getName(i);
            String type = qAtts.getType(i);
            String value = qAtts.getValue(i);

            if (attQName.startsWith("xmlns")) {
                String prefix;
                int n = attQName.indexOf(':');

                if (n == -1 && attQName.length() == 5) {
                    prefix = "";
                } else if (n != 5) {
                    // Al final no era una declaracion; sigue de largo para tratarse mas abajo
                    // como un atributo comun.
                    prefix = null;
                } else {
                    prefix = attQName.substring(n + 1);
                }

                if (prefix != null) {
                    // Una declaracion de verdad. Llega a SAX2 solo si quien llama la pidio, y
                    // recien ahi en el espacio de nombres NSDECL solo si tambien pidio eso.
                    if (prefixes) {
                        if (uris) {
                            atts.addAttribute(NamespaceSupport.NSDECL, prefix,
                                              canon(attQName), type, value);
                        } else {
                            atts.addAttribute("", "", canon(attQName),
                                              type, value);
                        }
                    }
                    continue;
                }
            }

            // Un atributo comun.
            try {
                String attName[] = processName(attQName, true, true);
                atts.addAttribute(attName[0], attName[1], attName[2],
                                  type, value);
            } catch (SAXException e) {
                if (exceptions == null) {
                    exceptions = new ArrayList<SAXException>();
                }
                exceptions.add(e);
                // Se conserva igual el atributo, sin resolver. Descartarlo perderia datos que el
                // documento realmente tiene por culpa de un error de espacio de nombres.
                atts.addAttribute("", attQName, attQName, type, value);
            }
        }

        // Ahora si los errores retenidos, con la lista ya consistente.
        if (exceptions != null && errorHandler != null) {
            for (int i = 0; i < exceptions.size(); i++) {
                SAXException e = exceptions.get(i);
                errorHandler.error((SAXParseException) e);
            }
        }

        if (contentHandler != null) {
            String name[] = processName(qName, false, false);
            contentHandler.startElement(name[0], name[1], name[2], atts);
        }
    }

    // El espejo de startElement: resolver el nombre, emitir endElement, y recien ahi desarmar los
    // prefijos que declaro este elemento; en ese orden, porque endPrefixMapping quiere decir "el
    // mapeo ya termino", es decir despues de que cerro el elemento que lo tenia.
    public void endElement(String qName) throws SAXException {
        if (!namespaces) {
            if (contentHandler != null) {
                contentHandler.endElement("", "", canon(qName));
            }
            return;
        }

        String names[] = processName(qName, false, false);
        if (contentHandler != null) {
            contentHandler.endElement(names[0], names[1], names[2]);
            // Se llama `declared` para no tapar la bandera de feature `prefixes` de mas arriba.
            Enumeration<String> declared = nsSupport.getDeclaredPrefixes();
            while (declared.hasMoreElements()) {
                String prefix = declared.nextElement();
                contentHandler.endPrefixMapping(prefix);
            }
        }
        nsSupport.popContext();
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

    ////////////////////////////////////////////////////////////////////

    // Cablea el parser envuelto para un analisis. El manejador de documento siempre es este
    // adaptador; los otros tres son los de quien llama, y si no estan puestos se dejan como
    // estaban en el parser, para que un parser configurado directamente conserve lo que tenia.
    private void setupParser() {
        // Va con guarda porque setFeature impide que las dos queden en false, asi que llegar aca
        // significa que alguien metio mano por atras de la API publica.
        if (!prefixes && !namespaces) {
            throw new IllegalStateException();
        }

        nsSupport.reset();
        if (uris) {
            nsSupport.setNamespaceDeclUris(true);
        }

        if (entityResolver != null) {
            parser.setEntityResolver(entityResolver);
        }
        if (dtdHandler != null) {
            parser.setDTDHandler(dtdHandler);
        }
        if (errorHandler != null) {
            parser.setErrorHandler(errorHandler);
        }
        parser.setDocumentHandler(this);
    }

    // Resuelve un nombre calificado. Un prefijo sin vincular es un error recuperable, y
    // `useException` dice de que forma quiere enterarse quien llama: los atributos quieren la
    // excepcion para poder conservar el atributo en forma degradada, los nombres de elemento
    // quieren el error reportado y una terna usable de vuelta para que el analisis siga.
    private String[] processName(String qName, boolean isAttribute,
                                 boolean useException) throws SAXException {
        String parts[] = nsSupport.processName(qName, nameParts, isAttribute);
        if (parts == null) {
            if (useException) {
                throw makeException("Undeclared prefix: " + qName);
            }
            reportError("Undeclared prefix: " + qName);
            parts = new String[3];
            parts[0] = parts[1] = "";
            parts[2] = canon(qName);
        }
        return parts;
    }

    // Es recuperable, asi que va a error() y no a fatalError(); sin manejador de errores se
    // descarta, que es la convencion de SAX para un problema del que nadie pidio enterarse.
    void reportError(String message) throws SAXException {
        if (errorHandler != null) {
            errorHandler.error(makeException(message));
        }
    }

    // Con posicion cuando el parser dio un localizador, y explicitamente sin posicion cuando no:
    // -1/-1 y no 0/0, para que nadie lo lea como "linea cero".
    private SAXParseException makeException(String message) {
        if (locator != null) {
            return new SAXParseException(message, locator);
        } else {
            return new SAXParseException(message, null, null, -1, -1);
        }
    }

    // Ver el comentario largo en NamespaceSupport: String.intern() es nativo y la VM de esta
    // casa no lo implementa, asi que se prueba una vez y se cae a devolver la cadena tal cual.
    // El JDK interna todos los nombres que reporta; no es parte del contrato de SAX, pero
    // donde se puede se hace igual.
    private static final boolean PUEDE_INTERNAR = pruebaIntern();

    private static boolean pruebaIntern() {
        try {
            String s = "";
            return s.intern() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String canon(String s) {
        if (PUEDE_INTERNAR) {
            return s.intern();
        }
        return s;
    }

    private void checkNotParsing(String type, String name)
            throws SAXNotSupportedException {
        if (parsing) {
            throw new SAXNotSupportedException("Cannot change " + type + ' '
                                               + name + " while parsing");
        }
    }

    ////////////////////////////////////////////////////////////////////

    // Un AttributeList de SAX1 visto por la cara de Attributes de SAX2, usado solo cuando el
    // procesamiento de espacios de nombres esta apagado. Todo lo que tenga forma de espacio de
    // nombres contesta vacio o ausente, porque con los espacios de nombres apagados no hay con
    // que contestar: getURI y getLocalName dan "", y las dos busquedas por (uri, localName) no
    // encuentran nada.
    final class AttributeListAdapter implements Attributes {

        private AttributeList qAtts;

        AttributeListAdapter() {
        }

        void setAttributeList(AttributeList qAtts) {
            this.qAtts = qAtts;
        }

        public int getLength() {
            return qAtts.getLength();
        }

        public String getURI(int i) {
            return "";
        }

        public String getLocalName(int i) {
            return "";
        }

        public String getQName(int i) {
            return qAtts.getName(i);
        }

        public String getType(int i) {
            return qAtts.getType(i);
        }

        public String getValue(int i) {
            return qAtts.getValue(i);
        }

        public int getIndex(String uri, String localName) {
            return -1;
        }

        public int getIndex(String qName) {
            int max = qAtts.getLength();
            for (int i = 0; i < max; i++) {
                if (qAtts.getName(i).equals(qName)) {
                    return i;
                }
            }
            return -1;
        }

        public String getType(String uri, String localName) {
            return null;
        }

        public String getType(String qName) {
            return qAtts.getType(qName);
        }

        public String getValue(String uri, String localName) {
            return null;
        }

        public String getValue(String qName) {
            return qAtts.getValue(qName);
        }
    }
}
