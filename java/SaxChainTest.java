import java.io.IOException;
import java.util.Locale;

import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributeListImpl;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderAdapter;

// Comportamiento de la cadena de eventos de SAX: XMLFilterImpl, ParserAdapter, XMLReaderAdapter,
// DefaultHandler, HandlerBase, LocatorImpl, AttributeListImpl y las excepciones.
//
// Como SaxNsAttsTest, esta escrita para dar lo mismo en la VM de esta casa y en el `java` real,
// donde org.xml.sax sale del modulo java.xml y no del classpath, asi que el mismo .class corrido
// alla ejercita el JDK y corrido aca ejercita esta biblioteca.
//
// Los "parsers" de aca no leen XML: son fuentes de eventos fijas. Es todo lo que hace falta para
// probar traductores y reenviadores, que es lo unico que hay en estos dos paquetes.
//
// Devuelve -1 si todo dio igual, y si no la cantidad de diferencias.
public class SaxChainTest {

    static int fallas = 0;

    static void eq(String etiqueta, Object esperado, Object real) {
        boolean ok;
        if (esperado == null) {
            ok = (real == null);
        } else {
            ok = esperado.equals(real);
        }
        if (!ok) {
            fallas++;
            System.out.println("FALLA " + etiqueta + ":\n  esperaba <" + esperado
                               + ">\n  y vino    <" + real + ">");
        }
    }

    static void eqi(String etiqueta, int esperado, int real) {
        if (esperado != real) {
            fallas++;
            System.out.println("FALLA " + etiqueta + ": esperaba " + esperado
                               + " y vino " + real);
        }
    }

    static void verdad(String etiqueta, boolean b) {
        if (!b) {
            fallas++;
            System.out.println("FALLA " + etiqueta);
        }
    }

    public static int run() {
        fallas = 0;
        excepciones();
        fuente();
        localizador();
        listaSax1();
        manejadoresVacios();
        filtro();
        filtroSinPadre();
        adaptadorSax1();
        adaptadorSax1SinNamespaces();
        adaptadorSax2();
        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    ////////////////////////////////////////////////////////////////////
    // Excepciones
    ////////////////////////////////////////////////////////////////////

    static void excepciones() {
        // La regla que se pasa por alto: sin mensaje propio pero con causa, getMessage()
        // contesta con el mensaje de la causa.
        IOException causa = new IOException("disco en llamas");
        SAXException sinMensaje = new SAXException(null, causa);
        eq("getMessage delega en la causa", "disco en llamas",
           sinMensaje.getMessage());

        // Y el mensaje propio siempre le gana al de la causa.
        SAXException conMensaje = new SAXException("propio", causa);
        eq("el mensaje propio gana", "propio", conMensaje.getMessage());

        // El constructor de una sola excepcion NO deja el mensaje en null: Throwable(Throwable)
        // pone el toString() de la causa como mensaje, asi que la delegacion no llega a
        // dispararse por ese camino. Es facil creer lo contrario.
        SAXException envuelta = new SAXException(causa);
        eq("envolver pone el toString de la causa",
           "java.io.IOException: disco en llamas", envuelta.getMessage());

        // Sin nada, null.
        SAXException vacia = new SAXException();
        eq("vacia", null, vacia.getMessage());
        eq("vacia sin excepcion", null, vacia.getException());
        eq("vacia sin causa", null, vacia.getCause());

        SAXException sola = new SAXException("solo");
        eq("solo mensaje", "solo", sola.getMessage());
        eq("solo mensaje sin excepcion", null, sola.getException());

        // getException es la causa, angostada a Exception.
        verdad("getException es la causa", envuelta.getException() == causa);
        verdad("getCause tambien", envuelta.getCause() == causa);

        // toString pega la excepcion envuelta abajo.
        eq("toString con causa",
           "org.xml.sax.SAXException: java.io.IOException: disco en llamas\n"
           + "java.io.IOException: disco en llamas", envuelta.toString());
        eq("toString sin causa", "org.xml.sax.SAXException: solo",
           sola.toString());
        eq("toString vacia", "org.xml.sax.SAXException", vacia.toString());

        // Las dos hijas.
        SAXNotRecognizedException nr = new SAXNotRecognizedException("no la conozco");
        verdad("SAXNotRecognizedException es SAXException",
               nr instanceof SAXException);
        eq("mensaje de nr", "no la conozco", nr.getMessage());
        eq("nr vacia", null, new SAXNotRecognizedException().getMessage());

        SAXNotSupportedException ns = new SAXNotSupportedException("no la banco");
        verdad("SAXNotSupportedException es SAXException",
               ns instanceof SAXException);
        eq("mensaje de ns", "no la banco", ns.getMessage());

        // SAXParseException: los cuatro datos de posicion y el toString con su pegote.
        SAXParseException pe =
            new SAXParseException("mens", "pub", "sys", 3, 7);
        eq("publicId", "pub", pe.getPublicId());
        eq("systemId", "sys", pe.getSystemId());
        eqi("lineNumber", 3, pe.getLineNumber());
        eqi("columnNumber", 7, pe.getColumnNumber());
        eq("toString de parse",
           "org.xml.sax.SAXParseExceptionpublicId: pub; systemId: sys;"
           + " lineNumber: 3; columnNumber: 7; mens", pe.toString());

        // Sin posicion, los -1 no se escriben.
        SAXParseException sinPos =
            new SAXParseException("mens", null, null, -1, -1);
        eq("toString sin posicion", "org.xml.sax.SAXParseException; mens",
           sinPos.toString());
        eqi("linea -1", -1, sinPos.getLineNumber());

        // El constructor que toma un Locator lo copia, no lo guarda.
        LocatorImpl loc = new LocatorImpl();
        loc.setPublicId("lp");
        loc.setSystemId("ls");
        loc.setLineNumber(11);
        loc.setColumnNumber(22);
        SAXParseException conLoc = new SAXParseException("desde locator", loc);
        loc.setLineNumber(99);
        eqi("la excepcion copio la linea", 11, conLoc.getLineNumber());
        eq("y el publicId", "lp", conLoc.getPublicId());

        SAXParseException conLocYCausa =
            new SAXParseException("m", loc, causa);
        verdad("SAXParseException guarda la causa",
               conLocYCausa.getException() == causa);
        verdad("y es SAXException", conLocYCausa instanceof SAXException);
    }

    ////////////////////////////////////////////////////////////////////
    // InputSource
    ////////////////////////////////////////////////////////////////////

    static void fuente() {
        InputSource s = new InputSource();
        eq("vacia sin systemId", null, s.getSystemId());
        verdad("vacia isEmpty", s.isEmpty());

        InputSource porId = new InputSource("http://ejemplo/a.xml");
        eq("systemId", "http://ejemplo/a.xml", porId.getSystemId());
        verdad("con systemId no esta vacia", !porId.isEmpty());

        s.setPublicId("p");
        s.setSystemId("t");
        s.setEncoding("UTF-8");
        eq("publicId", "p", s.getPublicId());
        eq("systemId puesto", "t", s.getSystemId());
        eq("encoding", "UTF-8", s.getEncoding());
        eq("sin byteStream", null, s.getByteStream());
        eq("sin characterStream", null, s.getCharacterStream());

        java.io.ByteArrayInputStream bytes =
            new java.io.ByteArrayInputStream(new byte[] { 60, 97, 47, 62 });
        InputSource porBytes = new InputSource(bytes);
        verdad("byteStream guardado", porBytes.getByteStream() == bytes);
        verdad("con contenido no esta vacia", !porBytes.isEmpty());

        java.io.StringReader chars = new java.io.StringReader("<a/>");
        InputSource porChars = new InputSource(chars);
        verdad("characterStream guardado",
               porChars.getCharacterStream() == chars);
    }

    static void localizador() {
        LocatorImpl a = new LocatorImpl();
        eq("locator vacio publicId", null, a.getPublicId());
        eqi("locator vacio linea", 0, a.getLineNumber());
        eqi("locator vacio columna", 0, a.getColumnNumber());

        a.setPublicId("p");
        a.setSystemId("s");
        a.setLineNumber(4);
        a.setColumnNumber(5);

        // La copia es una foto: cambiar el original no la toca. Es para lo que existe.
        LocatorImpl b = new LocatorImpl(a);
        a.setLineNumber(400);
        eqi("la copia no se movio", 4, b.getLineNumber());
        eq("y copio el systemId", "s", b.getSystemId());
        eqi("el original si", 400, a.getLineNumber());
    }

    ////////////////////////////////////////////////////////////////////
    // AttributeListImpl
    ////////////////////////////////////////////////////////////////////

    static void listaSax1() {
        AttributeListImpl l = new AttributeListImpl();
        eqi("vacia", 0, l.getLength());
        l.addAttribute("id", "ID", "1");
        l.addAttribute("h:href", "CDATA", "2");
        eqi("dos", 2, l.getLength());
        eq("getName", "h:href", l.getName(1));
        eq("getType por indice", "ID", l.getType(0));
        eq("getValue por nombre", "2", l.getValue("h:href"));
        eq("getType por nombre", "ID", l.getType("id"));

        // Ausente da null, y fuera de rango tambien.
        eq("nombre ausente", null, l.getValue("nope"));
        eq("tipo ausente", null, l.getType("nope"));
        eq("indice fuera de rango", null, l.getName(2));
        eq("indice negativo", null, l.getValue(-1));

        AttributeListImpl copia = new AttributeListImpl(l);
        eqi("copia", 2, copia.getLength());
        eq("copia bien", "1", copia.getValue("id"));

        l.removeAttribute("id");
        eqi("removeAttribute", 1, l.getLength());
        eq("y corrio", "h:href", l.getName(0));
        eqi("la copia sigue entera", 2, copia.getLength());

        // Sacar algo que no esta no es un error.
        l.removeAttribute("no-esta");
        eqi("sacar lo que no esta no hace nada", 1, l.getLength());

        l.clear();
        eqi("clear", 0, l.getLength());
    }

    ////////////////////////////////////////////////////////////////////
    // DefaultHandler / HandlerBase
    ////////////////////////////////////////////////////////////////////

    static void manejadoresVacios() {
        try {
            DefaultHandler d = new DefaultHandler();
            eq("DefaultHandler.resolveEntity da null", null,
               d.resolveEntity("p", "s"));

            // warning y error se callan; fatalError relanza. Es la asimetria del contrato.
            d.warning(new SAXParseException("w", null, null, -1, -1));
            d.error(new SAXParseException("e", null, null, -1, -1));

            SAXParseException fatal =
                new SAXParseException("f", null, null, -1, -1);
            int relanzo = 0;
            try {
                d.fatalError(fatal);
            } catch (SAXParseException e) {
                relanzo = (e == fatal) ? 1 : 2;
            }
            eqi("fatalError relanza la misma", 1, relanzo);

            // Y los eventos no hacen nada, pero tienen que existir y no tirar.
            d.setDocumentLocator(new LocatorImpl());
            d.startDocument();
            d.startPrefixMapping("p", "u");
            d.startElement("u", "a", "p:a", new AttributesImpl());
            d.characters(new char[] { 'x' }, 0, 1);
            d.ignorableWhitespace(new char[] { ' ' }, 0, 1);
            d.processingInstruction("t", "d");
            d.skippedEntity("ent");
            d.endElement("u", "a", "p:a");
            d.endPrefixMapping("p");
            d.notationDecl("n", "p", "s");
            d.unparsedEntityDecl("n", "p", "s", "not");
            d.endDocument();

            // HandlerBase es lo mismo para SAX1.
            HandlerBase h = new HandlerBase();
            eq("HandlerBase.resolveEntity da null", null,
               h.resolveEntity("p", "s"));
            h.warning(new SAXParseException("w", null, null, -1, -1));
            int relanzo2 = 0;
            SAXParseException fatal2 =
                new SAXParseException("f", null, null, -1, -1);
            try {
                h.fatalError(fatal2);
            } catch (SAXParseException e) {
                relanzo2 = (e == fatal2) ? 1 : 2;
            }
            eqi("HandlerBase.fatalError relanza", 1, relanzo2);
            h.startElement("a", new AttributeListImpl());
            h.endElement("a");
            h.endDocument();

            // Las cuatro interfaces, de verdad.
            verdad("DefaultHandler es EntityResolver", d instanceof EntityResolver);
            verdad("DefaultHandler es DTDHandler", d instanceof DTDHandler);
            verdad("DefaultHandler es ContentHandler", d instanceof ContentHandler);
            verdad("DefaultHandler es ErrorHandler", d instanceof ErrorHandler);
            verdad("HandlerBase es DocumentHandler", h instanceof DocumentHandler);
        } catch (Exception e) {
            fallas++;
            System.out.println("FALLA manejadoresVacios tiro: " + e);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // XMLFilterImpl
    ////////////////////////////////////////////////////////////////////

    static void filtro() {
        try {
            LectorFalso padre = new LectorFalso();
            XMLFilterImpl f = new XMLFilterImpl(padre);
            Grabador g = new Grabador();
            f.setContentHandler(g);
            f.setErrorHandler(g);
            f.setDTDHandler(g);
            f.setEntityResolver(g);

            verdad("getParent", f.getParent() == padre);
            verdad("getContentHandler", f.getContentHandler() == g);

            // Los features van al padre.
            f.setFeature("http://xml.org/sax/features/namespaces", false);
            verdad("el feature llego al padre", !padre.namespaces);
            verdad("y se lee de vuelta",
                   !f.getFeature("http://xml.org/sax/features/namespaces"));
            f.setProperty("http://ejemplo/p", "v");
            eq("la property llego al padre", "v",
               f.getProperty("http://ejemplo/p"));

            f.parse(new InputSource("da igual"));

            // El filtro se puso a si mismo como manejador del padre.
            verdad("el filtro se interpuso", padre.contentHandler == f);

            // Y todo lo que emitio el padre salio por el otro lado, en orden.
            eq("la cadena reenvia todo", GUION, g.log.toString());

            // Un filtro que redefine un evento y NO llama a super lo borra de la cadena.
            LectorFalso padre2 = new LectorFalso();
            FiltroMudo mudo = new FiltroMudo(padre2);
            Grabador g2 = new Grabador();
            mudo.setContentHandler(g2);
            mudo.parse(new InputSource("x"));
            verdad("el filtro mudo borro los characters",
                   g2.log.toString().indexOf("chars(") < 0);
            verdad("pero lo demas sigue",
                   g2.log.toString().indexOf("startDocument") >= 0);

            // Un filtro sin manejador registrado no tira: simplemente no reenvia.
            LectorFalso padre3 = new LectorFalso();
            XMLFilterImpl pelado = new XMLFilterImpl(padre3);
            pelado.parse(new InputSource("x"));

            // parse(String) arma el InputSource solo.
            LectorFalso padre4 = new LectorFalso();
            XMLFilterImpl f4 = new XMLFilterImpl(padre4);
            Grabador g4 = new Grabador();
            f4.setContentHandler(g4);
            f4.parse("mi-system-id");
            eq("parse(String) armo el InputSource", "mi-system-id",
               padre4.ultimoSystemId);

            // Dos filtros encadenados: todo tiene que llegar igual hasta el final.
            LectorFalso padre5 = new LectorFalso();
            XMLFilterImpl f5a = new XMLFilterImpl(padre5);
            XMLFilterImpl f5b = new XMLFilterImpl(f5a);
            Grabador g5 = new Grabador();
            f5b.setContentHandler(g5);
            f5b.setErrorHandler(g5);
            f5b.setDTDHandler(g5);
            f5b.setEntityResolver(g5);
            f5b.parse(new InputSource("x"));
            eq("dos eslabones no pierden nada", GUION, g5.log.toString());
        } catch (Exception e) {
            fallas++;
            System.out.println("FALLA filtro tiro: " + e);
        }
    }

    static void filtroSinPadre() {
        XMLFilterImpl f = new XMLFilterImpl();
        eq("sin padre", null, f.getParent());

        // Sin padre no hay a quien preguntarle, asi que ningun feature es reconocido.
        int a = 0;
        try {
            f.getFeature("http://xml.org/sax/features/namespaces");
        } catch (SAXNotRecognizedException e) {
            a = 1;
        } catch (SAXException e) {
            a = 2;
        }
        eqi("getFeature sin padre no reconoce", 1, a);

        int b = 0;
        try {
            f.setProperty("x", "y");
        } catch (SAXNotRecognizedException e) {
            b = 1;
        } catch (SAXException e) {
            b = 2;
        }
        eqi("setProperty sin padre no reconoce", 1, b);
    }

    ////////////////////////////////////////////////////////////////////
    // ParserAdapter: SAX1 -> SAX2, que es donde esta la traduccion de verdad
    ////////////////////////////////////////////////////////////////////

    static void adaptadorSax1() {
        try {
            ParserAdapter a = new ParserAdapter(new ParserFalso());
            Grabador g = new Grabador();
            a.setContentHandler(g);
            a.setErrorHandler(g);

            // Los valores por omision de SAX2.
            verdad("namespaces por omision",
                   a.getFeature("http://xml.org/sax/features/namespaces"));
            verdad("namespace-prefixes por omision apagado",
                   !a.getFeature("http://xml.org/sax/features/namespace-prefixes"));

            // No reconoce ninguna property.
            int p = 0;
            try {
                a.getProperty("http://ejemplo/x");
            } catch (SAXNotRecognizedException e) {
                p = 1;
            }
            eqi("ParserAdapter no reconoce properties", 1, p);

            a.parse(new InputSource("x"));

            // El documento de ParserFalso es
            //   <r xmlns="http://d" xmlns:p="http://p" id="1" p:x="2"><p:hijo/></r>
            // y con namespaces prendido y prefixes apagado tiene que salir asi: las
            // declaraciones se convierten en startPrefixMapping y desaparecen de la lista de
            // atributos, `id` queda SIN namespace (es atributo), y `p:x` lo toma de su prefijo.
            eq("traduccion SAX1->SAX2",
               "startDocument|"
               + "prefix(=http://d)|prefix(p=http://p)|"
               + "start(http://d,r,r)[id{}=1,x{http://p}=2]|"
               + "start(http://p,hijo,p:hijo)[]|"
               + "end(http://p,hijo,p:hijo)|"
               + "end(http://d,r,r)|"
               // El orden de endPrefixMapping es el de declaracion, no el inverso: sale
               // como los devuelve getDeclaredPrefixes.
               + "endPrefix()|endPrefix(p)|"
               + "endDocument|",
               g.log.toString());
        } catch (Exception e) {
            fallas++;
            System.out.println("FALLA adaptadorSax1 tiro: " + e);
        }
    }

    static void adaptadorSax1SinNamespaces() {
        try {
            ParserAdapter a = new ParserAdapter(new ParserFalso());
            a.setFeature("http://xml.org/sax/features/namespaces", false);
            // Apagar namespaces prende namespace-prefixes solo: los dos apagados no tiene
            // sentido, porque el nombre no llegaria de ninguna forma.
            verdad("apagar namespaces prende prefixes",
                   a.getFeature("http://xml.org/sax/features/namespace-prefixes"));

            Grabador g = new Grabador();
            a.setContentHandler(g);
            a.parse(new InputSource("x"));

            // Sin procesamiento de namespaces no hay prefixMapping, el uri y el local van
            // vacios, y los xmlns siguen siendo atributos comunes.
            eq("sin namespaces",
               "startDocument|"
               // Los cuatro atributos siguen ahi, pero el adaptador de AttributeList
               // contesta "" a getLocalName y a getURI, que es todo lo que puede saber sin
               // procesar namespaces. El grabador anota local{uri}=valor, de ahi los {}.
               + "start(,,r)[{}=http://d,{}=http://p,{}=1,{}=2]|"
               + "start(,,p:hijo)[]|"
               + "end(,,p:hijo)|"
               + "end(,,r)|"
               + "endDocument|",
               g.log.toString());

            // Cambiar un feature en pleno parse no se puede; aca ya termino, asi que se puede.
            a.setFeature("http://xml.org/sax/features/namespaces", true);
            verdad("se pudo volver a prender",
                   a.getFeature("http://xml.org/sax/features/namespaces"));

            // Un feature desconocido no se reconoce.
            int f = 0;
            try {
                a.setFeature("http://ejemplo/inventado", true);
            } catch (SAXNotRecognizedException e) {
                f = 1;
            }
            eqi("feature inventado", 1, f);
        } catch (Exception e) {
            fallas++;
            System.out.println("FALLA adaptadorSax1SinNamespaces tiro: " + e);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // XMLReaderAdapter: SAX2 -> SAX1
    ////////////////////////////////////////////////////////////////////

    static void adaptadorSax2() {
        try {
            LectorFalso lector = new LectorFalso();
            XMLReaderAdapter a = new XMLReaderAdapter(lector);
            GrabadorSax1 g = new GrabadorSax1();
            a.setDocumentHandler(g);

            // setLocale no tiene equivalente en SAX2 y por eso se niega.
            int l = 0;
            try {
                a.setLocale(Locale.getDefault());
            } catch (SAXNotSupportedException e) {
                l = 1;
            } catch (SAXException e) {
                l = 2;
            }
            eqi("setLocale se niega", 1, l);

            a.parse(new InputSource("x"));

            // Pidio namespace-prefixes: sin eso un manejador SAX1 no ve las declaraciones.
            verdad("pidio namespace-prefixes", lector.prefixes);
            verdad("y se puso de contentHandler", lector.contentHandler == a);

            // Del lado SAX1 llega el qName y se pierden uri y localName; los eventos de
            // prefijo y skippedEntity no existen y se descartan.
            eq("traduccion SAX2->SAX1",
               "startDocument|start(p:a)[q:b=v]|chars(hola)|ws( )|pi(t,d)|"
               + "end(p:a)|endDocument|",
               g.log.toString());
        } catch (Exception e) {
            fallas++;
            System.out.println("FALLA adaptadorSax2 tiro: " + e);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Las fuentes de eventos y los grabadores
    ////////////////////////////////////////////////////////////////////

    // Lo que LectorFalso emite, tal como lo anota Grabador. Sirve de esperado para el filtro.
    static final String GUION =
        "resolveEntity(pu,sy)|notation(n,p,s)|unparsed(n,p,s,no)|"
        + "locator|startDocument|prefix(p=http://p)|"
        + "start(http://p,a,p:a)[b{http://q}=v]|chars(hola)|ws( )|pi(t,d)|"
        + "skipped(ent)|end(http://p,a,p:a)|endPrefix(p)|endDocument|"
        + "warning(w)|error(e)|fatal(f)|";

    // Un XMLReader que no lee nada: emite una secuencia fija de eventos SAX2.
    static class LectorFalso implements XMLReader {
        ContentHandler contentHandler;
        ErrorHandler errorHandler;
        DTDHandler dtdHandler;
        EntityResolver entityResolver;
        boolean namespaces = true;
        boolean prefixes = false;
        String propiedad;
        String ultimoSystemId;

        public boolean getFeature(String name) {
            if (name.endsWith("namespace-prefixes")) {
                return prefixes;
            }
            return namespaces;
        }

        public void setFeature(String name, boolean value) {
            if (name.endsWith("namespace-prefixes")) {
                prefixes = value;
            } else {
                namespaces = value;
            }
        }

        public Object getProperty(String name) {
            return propiedad;
        }

        public void setProperty(String name, Object value) {
            propiedad = (String) value;
        }

        public void setEntityResolver(EntityResolver r) {
            entityResolver = r;
        }

        public EntityResolver getEntityResolver() {
            return entityResolver;
        }

        public void setDTDHandler(DTDHandler h) {
            dtdHandler = h;
        }

        public DTDHandler getDTDHandler() {
            return dtdHandler;
        }

        public void setContentHandler(ContentHandler h) {
            contentHandler = h;
        }

        public ContentHandler getContentHandler() {
            return contentHandler;
        }

        public void setErrorHandler(ErrorHandler h) {
            errorHandler = h;
        }

        public ErrorHandler getErrorHandler() {
            return errorHandler;
        }

        public void parse(String systemId) throws IOException, SAXException {
            parse(new InputSource(systemId));
        }

        public void parse(InputSource in) throws IOException, SAXException {
            ultimoSystemId = in.getSystemId();

            if (entityResolver != null) {
                entityResolver.resolveEntity("pu", "sy");
            }
            if (dtdHandler != null) {
                dtdHandler.notationDecl("n", "p", "s");
                dtdHandler.unparsedEntityDecl("n", "p", "s", "no");
            }
            if (contentHandler != null) {
                LocatorImpl loc = new LocatorImpl();
                loc.setSystemId("sy");
                loc.setLineNumber(1);
                contentHandler.setDocumentLocator(loc);
                contentHandler.startDocument();
                contentHandler.startPrefixMapping("p", "http://p");
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute("http://q", "b", "q:b", "CDATA", "v");
                contentHandler.startElement("http://p", "a", "p:a", atts);
                char[] texto = new char[] { 'h', 'o', 'l', 'a' };
                contentHandler.characters(texto, 0, 4);
                contentHandler.ignorableWhitespace(new char[] { ' ' }, 0, 1);
                contentHandler.processingInstruction("t", "d");
                contentHandler.skippedEntity("ent");
                contentHandler.endElement("http://p", "a", "p:a");
                contentHandler.endPrefixMapping("p");
                contentHandler.endDocument();
            }
            if (errorHandler != null) {
                errorHandler.warning(new SAXParseException("w", null, null, -1, -1));
                errorHandler.error(new SAXParseException("e", null, null, -1, -1));
                errorHandler.fatalError(new SAXParseException("f", null, null, -1, -1));
            }
        }
    }

    // Un Parser SAX1 que no lee nada: emite
    //   <r xmlns="http://d" xmlns:p="http://p" id="1" p:x="2"><p:hijo/></r>
    static class ParserFalso implements Parser {
        DocumentHandler documentHandler;

        public void setLocale(Locale locale) {
        }

        public void setEntityResolver(EntityResolver r) {
        }

        public void setDTDHandler(DTDHandler h) {
        }

        public void setDocumentHandler(DocumentHandler h) {
            documentHandler = h;
        }

        public void setErrorHandler(ErrorHandler h) {
        }

        public void parse(String systemId) throws IOException, SAXException {
            parse(new InputSource(systemId));
        }

        public void parse(InputSource in) throws IOException, SAXException {
            if (documentHandler == null) {
                return;
            }
            documentHandler.startDocument();
            AttributeListImpl atts = new AttributeListImpl();
            atts.addAttribute("xmlns", "CDATA", "http://d");
            atts.addAttribute("xmlns:p", "CDATA", "http://p");
            atts.addAttribute("id", "ID", "1");
            atts.addAttribute("p:x", "CDATA", "2");
            documentHandler.startElement("r", atts);
            documentHandler.startElement("p:hijo", new AttributeListImpl());
            documentHandler.endElement("p:hijo");
            documentHandler.endElement("r");
            documentHandler.endDocument();
        }
    }

    // Anota todo lo que le llega por las cuatro interfaces SAX2.
    static class Grabador implements ContentHandler, ErrorHandler, DTDHandler,
                                     EntityResolver {
        StringBuilder log = new StringBuilder();

        public InputSource resolveEntity(String p, String s) {
            log.append("resolveEntity(").append(p).append(',').append(s)
               .append(")|");
            return null;
        }

        public void notationDecl(String n, String p, String s) {
            log.append("notation(").append(n).append(',').append(p)
               .append(',').append(s).append(")|");
        }

        public void unparsedEntityDecl(String n, String p, String s, String no) {
            log.append("unparsed(").append(n).append(',').append(p)
               .append(',').append(s).append(',').append(no).append(")|");
        }

        public void setDocumentLocator(Locator l) {
            log.append("locator|");
        }

        public void startDocument() {
            log.append("startDocument|");
        }

        public void endDocument() {
            log.append("endDocument|");
        }

        public void startPrefixMapping(String p, String u) {
            log.append("prefix(").append(p).append('=').append(u).append(")|");
        }

        public void endPrefixMapping(String p) {
            log.append("endPrefix(").append(p).append(")|");
        }

        public void startElement(String u, String ln, String qn, Attributes a) {
            log.append("start(").append(u).append(',').append(ln).append(',')
               .append(qn).append(")[");
            for (int i = 0; i < a.getLength(); i++) {
                if (i > 0) {
                    log.append(',');
                }
                log.append(a.getLocalName(i)).append('{').append(a.getURI(i))
                   .append("}=").append(a.getValue(i));
            }
            log.append("]|");
        }

        public void endElement(String u, String ln, String qn) {
            log.append("end(").append(u).append(',').append(ln).append(',')
               .append(qn).append(")|");
        }

        public void characters(char[] ch, int start, int length) {
            log.append("chars(").append(new String(ch, start, length))
               .append(")|");
        }

        public void ignorableWhitespace(char[] ch, int start, int length) {
            log.append("ws(").append(new String(ch, start, length)).append(")|");
        }

        public void processingInstruction(String t, String d) {
            log.append("pi(").append(t).append(',').append(d).append(")|");
        }

        public void skippedEntity(String n) {
            log.append("skipped(").append(n).append(")|");
        }

        public void warning(SAXParseException e) {
            log.append("warning(").append(e.getMessage()).append(")|");
        }

        public void error(SAXParseException e) {
            log.append("error(").append(e.getMessage()).append(")|");
        }

        public void fatalError(SAXParseException e) {
            log.append("fatal(").append(e.getMessage()).append(")|");
        }
    }

    // Lo mismo del lado SAX1.
    static class GrabadorSax1 implements DocumentHandler {
        StringBuilder log = new StringBuilder();

        public void setDocumentLocator(Locator l) {
        }

        public void startDocument() {
            log.append("startDocument|");
        }

        public void endDocument() {
            log.append("endDocument|");
        }

        public void startElement(String name, AttributeList atts) {
            log.append("start(").append(name).append(")[");
            for (int i = 0; i < atts.getLength(); i++) {
                if (i > 0) {
                    log.append(',');
                }
                log.append(atts.getName(i)).append('=').append(atts.getValue(i));
            }
            log.append("]|");
        }

        public void endElement(String name) {
            log.append("end(").append(name).append(")|");
        }

        public void characters(char[] ch, int start, int length) {
            log.append("chars(").append(new String(ch, start, length))
               .append(")|");
        }

        public void ignorableWhitespace(char[] ch, int start, int length) {
            log.append("ws(").append(new String(ch, start, length)).append(")|");
        }

        public void processingInstruction(String t, String d) {
            log.append("pi(").append(t).append(',').append(d).append(")|");
        }
    }

    // Un filtro que se come los characters por no llamar a super: el error clasico, aca a
    // proposito.
    static class FiltroMudo extends XMLFilterImpl {
        FiltroMudo(XMLReader padre) {
            super(padre);
        }

        public void characters(char[] ch, int start, int length) {
        }
    }
}
