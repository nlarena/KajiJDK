import java.io.IOException;
import java.util.Enumeration;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.NamespaceSupport;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.ext.Attributes2Impl;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2;
import org.xml.sax.ext.Locator2Impl;

// Pruebas de comportamiento de org.xml.sax, org.xml.sax.helpers y org.xml.sax.ext.
//
// El oraculo es el JDK 25: las mismas afirmaciones corridas alla tienen que dar -1. Como
// org.xml.sax vive en el modulo java.xml, una corrida con `java -cp KajiLibrary` carga las clases
// del JDK y no las nuestras, y eso es justo lo que la hace util como oraculo. Para verificar
// nuestro bytecode en una JVM real hace falta `--patch-module java.xml=<dir>`, que es como se
// corre la segunda vuelta.
//
// No se prueba nada que necesite un parser: el arbol no trae ninguno y una prueba que fingiera
// analizar XML estaria midiendo la prueba, no la biblioteca. Lo que si tiene comportamiento propio
// --las listas de atributos, la pila de espacios de nombres, el filtro que reenvia-- se prueba
// entero.
public class SaxTest {

    private static int failures;

    private static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("FALLA: " + what);
        }
    }

    private static void eq(Object got, Object want, String what) {
        boolean ok = (got == null) ? (want == null) : got.equals(want);
        if (!ok) {
            failures++;
            System.out.println("FALLA: " + what + " -- esperaba <" + want + "> y vino <" + got + ">");
        }
    }

    ////////////////////////////////////////////////////////////////////
    // AttributesImpl
    ////////////////////////////////////////////////////////////////////

    private static AttributesImpl threeAttrs() {
        AttributesImpl a = new AttributesImpl();
        a.addAttribute("urn:a", "x", "a:x", "CDATA", "1");
        a.addAttribute("urn:b", "y", "b:y", "ID", "2");
        a.addAttribute("", "z", "z", "CDATA", "3");
        return a;
    }

    private static void attributesTest() {
        AttributesImpl a = threeAttrs();
        eq(String.valueOf(a.getLength()), "3", "AttributesImpl.getLength");

        // Las tres vistas sobre lo mismo.
        eq(String.valueOf(a.getIndex("b:y")), "1", "getIndex(qName)");
        eq(String.valueOf(a.getIndex("urn:b", "y")), "1", "getIndex(uri, local)");
        eq(a.getValue("b:y"), "2", "getValue(qName)");
        eq(a.getValue("urn:b", "y"), "2", "getValue(uri, local)");
        eq(a.getType("urn:b", "y"), "ID", "getType(uri, local)");
        eq(a.getQName(2), "z", "getQName(2)");
        eq(a.getURI(2), "", "getURI sin espacio de nombres es cadena vacia");

        // Lo que no esta: -1 y null, no excepcion.
        eq(String.valueOf(a.getIndex("nope")), "-1", "getIndex de lo que no esta");
        eq(String.valueOf(a.getIndex("urn:q", "q")), "-1", "getIndex(uri,local) de lo que no esta");
        eq(a.getValue("nope"), null, "getValue de lo que no esta");
        eq(a.getType("nope"), null, "getType de lo que no esta");

        // Indice fuera de rango en lectura: null.
        eq(a.getURI(99), null, "getURI fuera de rango");
        eq(a.getValue(-1), null, "getValue con indice negativo");

        // Indice fuera de rango en escritura: excepcion. La asimetria es del contrato.
        boolean threw = false;
        try {
            a.setValue(99, "x");
        } catch (ArrayIndexOutOfBoundsException e) {
            threw = true;
        }
        check(threw, "setValue fuera de rango tira ArrayIndexOutOfBoundsException");

        // La copia es independiente: es para lo que existe el constructor.
        AttributesImpl copy = new AttributesImpl(a);
        a.setValue(0, "cambiado");
        eq(copy.getValue(0), "1", "la copia no ve los cambios del original");
        eq(a.getValue(0), "cambiado", "el original si los ve");

        // removeAttribute corre los de atras.
        AttributesImpl b = threeAttrs();
        b.removeAttribute(0);
        eq(String.valueOf(b.getLength()), "2", "removeAttribute baja el largo");
        eq(b.getQName(0), "b:y", "removeAttribute corre los indices");
        eq(b.getQName(1), "z", "removeAttribute corre los indices (2)");
        eq(String.valueOf(b.getIndex("a:x")), "-1", "el sacado ya no se encuentra");

        // clear deja la lista vacia y reusable.
        b.clear();
        eq(String.valueOf(b.getLength()), "0", "clear vacia");
        b.addAttribute("", "n", "n", "CDATA", "v");
        eq(b.getValue("n"), "v", "clear deja la lista reusable");
    }

    ////////////////////////////////////////////////////////////////////
    // NamespaceSupport
    ////////////////////////////////////////////////////////////////////

    private static void namespacesTest() {
        NamespaceSupport ns = new NamespaceSupport();

        // El prefijo `xml` viene predeclarado por la norma, sin que nadie lo declare.
        eq(ns.getURI("xml"), NamespaceSupport.XMLNS, "el prefijo xml esta predeclarado");

        ns.pushContext();
        check(ns.declarePrefix("a", "urn:a"), "declarePrefix('a')");
        check(ns.declarePrefix("", "urn:def"), "declarePrefix por omision");
        eq(ns.getURI("a"), "urn:a", "getURI en el contexto que lo declaro");

        // Contexto anidado: hereda lo de afuera y agrega lo suyo.
        ns.pushContext();
        check(ns.declarePrefix("b", "urn:b"), "declarePrefix('b') anidado");
        eq(ns.getURI("a"), "urn:a", "el anidado hereda el prefijo de afuera");
        eq(ns.getURI("b"), "urn:b", "el anidado ve el suyo");

        // Y el de afuera **no** ve el de adentro despues de popContext. Es la prueba central.
        ns.popContext();
        eq(ns.getURI("b"), null, "despues de popContext el prefijo de adentro no existe mas");
        eq(ns.getURI("a"), "urn:a", "y el de afuera sigue");

        // processName: el nombre calificado partido en (uri, local, qName).
        String parts[] = new String[3];
        String resolved[] = ns.processName("a:x", parts, false);
        check(resolved != null, "processName resuelve un prefijo declarado");
        if (resolved != null) {
            eq(resolved[0], "urn:a", "processName uri");
            eq(resolved[1], "x", "processName nombre local");
            eq(resolved[2], "a:x", "processName nombre calificado");
        }

        // Un elemento sin prefijo toma el espacio por omision; un atributo sin prefijo, no.
        String elemResolved[] = ns.processName("x", new String[3], false);
        check(elemResolved != null, "processName de elemento sin prefijo");
        if (elemResolved != null) {
            eq(elemResolved[0], "urn:def", "un elemento sin prefijo toma el espacio por omision");
        }
        String attrResolved[] = ns.processName("x", new String[3], true);
        check(attrResolved != null, "processName de atributo sin prefijo");
        if (attrResolved != null) {
            eq(attrResolved[0], "", "un atributo sin prefijo NO toma el espacio por omision");
        }

        // Un prefijo que nadie declaro no se resuelve: null, no una URI inventada.
        eq(ns.processName("zzz:x", new String[3], false), null,
           "processName de un prefijo no declarado da null");

        // La vuelta: de URI a prefijo.
        eq(ns.getPrefix("urn:a"), "a", "getPrefix");
        eq(ns.getPrefix("urn:def"), null,
           "getPrefix nunca devuelve el prefijo por omision (cadena vacia)");

        // getDeclaredPrefixes es solo lo de este contexto; getPrefixes, todo lo visible.
        int declaredCount = 0;
        Enumeration<String> declared = ns.getDeclaredPrefixes();
        while (declared.hasMoreElements()) {
            declared.nextElement();
            declaredCount++;
        }
        eq(String.valueOf(declaredCount), "2", "getDeclaredPrefixes cuenta 'a' y el por omision");

        // reset devuelve todo al estado inicial.
        ns.reset();
        eq(ns.getURI("a"), null, "reset borra las declaraciones");
        eq(ns.getURI("xml"), NamespaceSupport.XMLNS, "reset deja el xml predeclarado");
    }

    ////////////////////////////////////////////////////////////////////
    // XMLFilterImpl
    ////////////////////////////////////////////////////////////////////

    // Un ContentHandler que anota lo que le llega, para ver si el filtro reenvia.
    static class Recorder implements ContentHandler {
        StringBuilder log = new StringBuilder();

        public void setDocumentLocator(Locator l) { log.append("loc;"); }
        public void startDocument() { log.append("startDoc;"); }
        public void endDocument() { log.append("endDoc;"); }
        public void startPrefixMapping(String p, String u) { log.append("pre(" + p + "," + u + ");"); }
        public void endPrefixMapping(String p) { log.append("/pre(" + p + ");"); }
        public void startElement(String u, String l, String q, Attributes a) {
            log.append("start(" + q + "," + a.getLength() + ");");
        }
        public void endElement(String u, String l, String q) { log.append("end(" + q + ");"); }
        public void characters(char ch[], int start, int len) {
            log.append("chars(" + new String(ch, start, len) + ");");
        }
        public void ignorableWhitespace(char ch[], int start, int len) { log.append("ws;"); }
        public void processingInstruction(String t, String d) { log.append("pi(" + t + ");"); }
        public void skippedEntity(String n) { log.append("skip(" + n + ");"); }
    }

    // Lo mismo para los tres eventos de error.
    static class ErrorRecorder implements org.xml.sax.ErrorHandler {
        StringBuilder log = new StringBuilder();

        public void warning(SAXParseException e) { log.append("warning;"); }
        public void error(SAXParseException e) { log.append("error;"); }
        public void fatalError(SAXParseException e) { log.append("fatal;"); }
    }

    private static void filterTest() throws Exception {
        XMLFilterImpl f = new XMLFilterImpl();
        Recorder downstream = new Recorder();
        f.setContentHandler(downstream);

        check(f.getContentHandler() == downstream, "el filtro devuelve el handler que le pusieron");

        f.startDocument();
        f.startPrefixMapping("a", "urn:a");
        f.startElement("urn:a", "x", "a:x", threeAttrs());
        char ch[] = "hola".toCharArray();
        f.characters(ch, 0, 4);
        f.processingInstruction("php", "d");
        f.skippedEntity("ent");
        f.endElement("urn:a", "x", "a:x");
        f.endPrefixMapping("a");
        f.endDocument();

        eq(downstream.log.toString(),
           "startDoc;pre(a,urn:a);start(a:x,3);chars(hola);pi(php);skip(ent);end(a:x);/pre(a);endDoc;",
           "XMLFilterImpl reenvia los eventos al handler de abajo, en orden");

        // Sin handler abajo no explota: se traga los eventos.
        XMLFilterImpl empty = new XMLFilterImpl();
        empty.startDocument();
        empty.endDocument();
        check(true, "un filtro sin handler no tira");

        // Sin padre, parse() tiene que fallar: no hay de donde leer. No puede inventar un parser.
        boolean threw = false;
        try {
            empty.parse(new InputSource("x.xml"));
        } catch (NullPointerException e) {
            threw = true;
        } catch (SAXException e) {
            threw = true;
        }
        check(threw, "parse() sin padre falla en vez de fingir que leyo algo");

        // El resolvedor de entidades tambien se reenvia, y sin uno instalado devuelve null.
        eq(f.resolveEntity("pub", "sys"), null, "resolveEntity sin resolvedor devuelve null");

        // Los tres eventos de error tambien se reenvian. Y aca esta la diferencia con
        // DefaultHandler, que es facil de suponer al reves: el filtro **no** relanza el
        // fatalError, lo pasa para abajo. Relanzar seria decidir por el manejador de abajo, y un
        // filtro no decide: reenvia.
        ErrorRecorder err = new ErrorRecorder();
        f.setErrorHandler(err);
        SAXParseException spe = new SAXParseException("m", null, null, 1, 2);
        f.warning(spe);
        f.error(spe);
        f.fatalError(spe);
        eq(err.log.toString(), "warning;error;fatal;",
           "XMLFilterImpl reenvia los tres eventos de error sin relanzar");

        // Y sin manejador de errores abajo se callan los tres, fatalError incluido.
        XMLFilterImpl silent = new XMLFilterImpl();
        boolean quiet = true;
        try {
            silent.fatalError(spe);
        } catch (SAXException e) {
            quiet = false;
        }
        check(quiet, "sin ErrorHandler abajo, el filtro tampoco relanza el fatalError");
    }

    ////////////////////////////////////////////////////////////////////
    // Attributes2Impl
    ////////////////////////////////////////////////////////////////////

    private static void attributes2Test() {
        Attributes2Impl a = new Attributes2Impl();
        eq(String.valueOf(a.getLength()), "0", "Attributes2Impl arranca vacio");

        // Las banderas por omision de addAttribute: especificado siempre, declarado segun el tipo.
        a.addAttribute("", "x", "x", "CDATA", "1");
        a.addAttribute("", "y", "y", "ID", "2");
        check(a.isSpecified(0), "lo agregado a mano cuenta como especificado");
        check(!a.isDeclared(0), "CDATA no implica declarado");
        check(a.isDeclared(1), "un tipo distinto de CDATA solo puede venir de una declaracion");

        // Las banderas se pueden forzar.
        a.setDeclared(0, true);
        a.setSpecified(1, false);
        check(a.isDeclared(0), "setDeclared");
        check(!a.isSpecified(1), "setSpecified");

        // Las tres formas de preguntar dan lo mismo.
        check(a.isDeclared("x"), "isDeclared(qName)");
        check(a.isDeclared("", "x"), "isDeclared(uri, local)");
        check(!a.isSpecified("y"), "isSpecified(qName)");
        check(!a.isSpecified("", "y"), "isSpecified(uri, local)");

        // Y aca si hay excepcion donde Attributes devolvia null: con un boolean no hay
        // forma de decir "no existe".
        boolean t1 = false;
        try {
            a.isDeclared("nope");
        } catch (IllegalArgumentException e) {
            t1 = true;
        }
        check(t1, "isDeclared de un nombre que no esta tira IllegalArgumentException");

        boolean t2 = false;
        try {
            a.isSpecified("urn:q", "q");
        } catch (IllegalArgumentException e) {
            t2 = true;
        }
        check(t2, "isSpecified(uri,local) de lo que no esta tira IllegalArgumentException");

        boolean t3 = false;
        try {
            a.isDeclared(99);
        } catch (ArrayIndexOutOfBoundsException e) {
            t3 = true;
        }
        check(t3, "isDeclared fuera de rango tira ArrayIndexOutOfBoundsException");

        boolean t4 = false;
        try {
            a.setSpecified(-1, true);
        } catch (ArrayIndexOutOfBoundsException e) {
            t4 = true;
        }
        check(t4, "setSpecified fuera de rango tira ArrayIndexOutOfBoundsException");

        // El constructor de copia desde un Attributes2 copia las banderas de verdad. Esto ejerce
        // la trampa de inicializacion: super(atts) llama al setAttributes de la subclase.
        Attributes2Impl copy = new Attributes2Impl(a);
        eq(String.valueOf(copy.getLength()), "2", "la copia tiene los mismos atributos");
        check(copy.isDeclared(0), "la copia trae la bandera declared forzada");
        check(!copy.isSpecified(1), "la copia trae la bandera specified forzada");
        eq(copy.getValue("y"), "2", "la copia trae los valores");

        // Y desde un Attributes pelado, deduce: todo especificado, declarado segun el tipo.
        Attributes2Impl deduced = new Attributes2Impl(threeAttrs());
        eq(String.valueOf(deduced.getLength()), "3", "la copia desde AttributesImpl");
        check(deduced.isSpecified(0), "sin banderas de origen, todo cuenta como especificado");
        check(!deduced.isDeclared(0), "CDATA -> no declarado");
        check(deduced.isDeclared(1), "ID -> declarado");
        check(!deduced.isDeclared(2), "CDATA -> no declarado (2)");

        // Agregar sobre una copia es el caso donde los arreglos de banderas quedaron del tamano
        // justo y tienen que crecer. Si crecieran mal, el atributo nuevo leeria una bandera ajena.
        Attributes2Impl plusOne = new Attributes2Impl(threeAttrs());
        plusOne.addAttribute("", "w", "w", "NMTOKEN", "4");
        eq(String.valueOf(plusOne.getLength()), "4", "agregar sobre una copia");
        check(plusOne.isDeclared(3), "el agregado sobre una copia trae su propia bandera");
        check(plusOne.isSpecified(3), "y cuenta como especificado");
        check(plusOne.isDeclared(1), "y no piso la bandera del que ya estaba");
        check(!plusOne.isDeclared(0), "ni la del primero");

        // removeAttribute tiene que correr las banderas junto con los datos.
        Attributes2Impl removed = new Attributes2Impl(threeAttrs());
        removed.removeAttribute(0);
        eq(String.valueOf(removed.getLength()), "2", "removeAttribute baja el largo");
        eq(removed.getQName(0), "b:y", "removeAttribute corre los datos");
        check(removed.isDeclared(0), "removeAttribute corre tambien las banderas");
        check(!removed.isDeclared(1), "las banderas siguen alineadas despues de sacar");

        // Sacar el ultimo es el caso de borde del arraycopy.
        Attributes2Impl last = new Attributes2Impl(threeAttrs());
        last.removeAttribute(2);
        eq(String.valueOf(last.getLength()), "2", "sacar el ultimo");
        check(last.isDeclared(1), "sacar el ultimo no toca las banderas de los otros");

        // setAttributes sobre una lista mas larga achica los arreglos.
        Attributes2Impl replaced = new Attributes2Impl(threeAttrs());
        AttributesImpl single = new AttributesImpl();
        single.addAttribute("", "solo", "solo", "CDATA", "v");
        replaced.setAttributes(single);
        eq(String.valueOf(replaced.getLength()), "1", "setAttributes reemplaza el contenido");
        check(replaced.isSpecified(0), "setAttributes rearma las banderas");
        boolean t5 = false;
        try {
            replaced.isDeclared(1);
        } catch (ArrayIndexOutOfBoundsException e) {
            t5 = true;
        }
        check(t5, "las banderas viejas no quedan colgando detras de la lista nueva");

        // Y crecer mas alla del arreglo inicial mantiene todo alineado.
        Attributes2Impl grown = new Attributes2Impl();
        for (int i = 0; i < 40; i++) {
            grown.addAttribute("", "n" + i, "n" + i, (i % 2 == 0) ? "CDATA" : "ID", "v" + i);
        }
        eq(String.valueOf(grown.getLength()), "40", "crece mas alla del bloque inicial");
        boolean aligned = true;
        for (int i = 0; i < 40; i++) {
            if (grown.isDeclared(i) != (i % 2 != 0)) {
                aligned = false;
            }
            if (!grown.isSpecified(i)) {
                aligned = false;
            }
        }
        check(aligned, "las banderas siguen alineadas despues de que el arreglo crecio");

        // Un Attributes2Impl es un Attributes: el manejador lo descubre con instanceof.
        Attributes plain = grown;
        check(plain instanceof Attributes2, "Attributes2Impl se descubre con instanceof Attributes2");
    }

    ////////////////////////////////////////////////////////////////////
    // Locator2Impl
    ////////////////////////////////////////////////////////////////////

    private static void locator2Test() {
        Locator2Impl empty = new Locator2Impl();
        eq(empty.getXMLVersion(), null, "Locator2Impl arranca sin version");
        eq(empty.getEncoding(), null, "Locator2Impl arranca sin codificacion");
        eq(empty.getPublicId(), null, "y sin publicId");
        eq(String.valueOf(empty.getLineNumber()), "0", "y en linea 0");

        empty.setXMLVersion("1.1");
        empty.setEncoding("UTF-8");
        empty.setSystemId("urn:s");
        empty.setLineNumber(7);
        empty.setColumnNumber(3);
        eq(empty.getXMLVersion(), "1.1", "setXMLVersion");
        eq(empty.getEncoding(), "UTF-8", "setEncoding");

        // La copia desde un Locator2 trae los seis campos.
        Locator2Impl fromLocator2 = new Locator2Impl(empty);
        eq(fromLocator2.getXMLVersion(), "1.1", "la copia desde Locator2 trae la version");
        eq(fromLocator2.getEncoding(), "UTF-8", "la copia desde Locator2 trae la codificacion");
        eq(fromLocator2.getSystemId(), "urn:s", "la copia trae el systemId");
        eq(String.valueOf(fromLocator2.getLineNumber()), "7", "la copia trae la linea");
        eq(String.valueOf(fromLocator2.getColumnNumber()), "3", "la copia trae la columna");

        // Y desde un Locator pelado deja version y codificacion en null en vez de inventar.
        LocatorImpl plain = new LocatorImpl();
        plain.setSystemId("urn:p");
        plain.setLineNumber(11);
        Locator2Impl fromPlain = new Locator2Impl(plain);
        eq(fromPlain.getSystemId(), "urn:p", "la copia desde Locator pelado trae los cuatro campos");
        eq(String.valueOf(fromPlain.getLineNumber()), "11", "y la linea");
        eq(fromPlain.getXMLVersion(), null, "un Locator pelado no da version, y no se inventa 1.0");
        eq(fromPlain.getEncoding(), null, "ni codificacion");

        Locator asLocator = fromLocator2;
        check(asLocator instanceof Locator2, "Locator2Impl se descubre con instanceof Locator2");
    }

    ////////////////////////////////////////////////////////////////////
    // DefaultHandler2
    ////////////////////////////////////////////////////////////////////

    // Redefine solo el resolvedor de cuatro argumentos, para ver si el puente lo alcanza.
    static class FourArgOnly extends DefaultHandler2 {
        String seen;

        public InputSource resolveEntity(String name, String publicId,
                                         String baseURI, String systemId)
                throws SAXException, IOException {
            seen = name + "|" + publicId + "|" + baseURI + "|" + systemId;
            return new InputSource("resuelto");
        }
    }

    private static void handler2Test() throws Exception {
        DefaultHandler2 h = new DefaultHandler2();

        // Es las tres interfaces de ext a la vez, y ademas todo lo de DefaultHandler.
        check(h instanceof LexicalHandler, "DefaultHandler2 es LexicalHandler");
        check(h instanceof DeclHandler, "DefaultHandler2 es DeclHandler");
        check(h instanceof EntityResolver2, "DefaultHandler2 es EntityResolver2");
        check(h instanceof ContentHandler, "DefaultHandler2 sigue siendo ContentHandler");

        // Los cuerpos vacios no tiran.
        h.startDTD("doc", "pub", "sys");
        h.startEntity("[dtd]");
        h.startCDATA();
        h.comment("c".toCharArray(), 0, 1);
        h.endCDATA();
        h.endEntity("[dtd]");
        h.endDTD();
        h.elementDecl("e", "ANY");
        h.attributeDecl("e", "a", "CDATA", "#IMPLIED", null);
        h.internalEntityDecl("n", "v");
        h.externalEntityDecl("n", "p", "s");
        h.startElement("", "x", "x", threeAttrs());
        h.characters("t".toCharArray(), 0, 1);
        h.endElement("", "x", "x");
        check(true, "los cuerpos vacios de DefaultHandler2 no tiran");

        // La respuesta por omision del resolvedor es null: 'abrilo vos por el systemId'.
        eq(h.getExternalSubset("doc", "urn:b"), null, "getExternalSubset por omision es null");
        eq(h.resolveEntity("n", "pub", "urn:b", "sys"), null, "resolveEntity(4) por omision es null");
        eq(h.resolveEntity("pub", "sys"), null, "resolveEntity(2) por omision es null");

        // El puente: redefinir solo el de cuatro alcanza para atender al de dos.
        FourArgOnly sub = new FourArgOnly();
        InputSource src = sub.resolveEntity("PUB", "SYS");
        check(src != null, "resolveEntity(2) llega al resolveEntity(4) redefinido");
        if (src != null) {
            eq(src.getSystemId(), "resuelto", "y devuelve lo que el de cuatro devolvio");
        }
        eq(sub.seen, "null|PUB|null|SYS",
           "el puente pasa null en nombre y baseURI, y publicId/systemId en su lugar");

        // fatalError se hereda de DefaultHandler y relanza.
        SAXParseException spe = new SAXParseException("m", null, null, 1, 2);
        h.warning(spe);
        h.error(spe);
        boolean rethrown = false;
        try {
            h.fatalError(spe);
        } catch (SAXException e) {
            rethrown = (e == spe);
        }
        check(rethrown, "DefaultHandler2 hereda el fatalError que relanza");
    }

    ////////////////////////////////////////////////////////////////////

    public static int run() {
        failures = 0;
        try {
            attributesTest();
            namespacesTest();
            filterTest();
            attributes2Test();
            locator2Test();
            handler2Test();
        } catch (Throwable t) {
            failures++;
            System.out.println("FALLA: excepcion inesperada " + t);
        }
        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String args[]) {
        System.out.println("SaxTest -> " + run());
    }
}
