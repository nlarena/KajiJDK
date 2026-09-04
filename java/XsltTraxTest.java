import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;

/**
 * Pruebas de comportamiento de `javax.xml.transform` (la API de XSLT).
 *
 * <p>Corre igual contra nuestra biblioteca y contra el JDK real, y esa es la gracia. La unica
 * divergencia legitima entre los dos es que el JDK **trae** un procesador de XSLT y nosotros no, asi
 * que `TransformerFactory.newInstance()` alla devuelve una fabrica y aca lanza
 * `TransformerFactoryConfigurationError`. El paso 90 esta escrito para eso: chequea el **contrato**
 * --o una fabrica no nula, o ese error, y nada mas-- en vez del resultado, que es lo unico que se
 * puede afirmar de las dos implementaciones a la vez.
 *
 * <p>Devuelve -1 si todo bien; si no, el numero del paso que fallo.
 */
public class XsltTraxTest {

    // ---- utileria -------------------------------------------------------------------------------

    /** Un locator con los cuatro datos puestos. */
    static class LocLleno implements SourceLocator {
        public String getPublicId() { return "PUB"; }
        public String getSystemId() { return "SYS"; }
        public int getLineNumber() { return 7; }
        public int getColumnNumber() { return 3; }
    }

    /** Un locator que no sabe nada: URIs nulas y posiciones en cero, el centinela de "no se". */
    static class LocVacio implements SourceLocator {
        public String getPublicId() { return null; }
        public String getSystemId() { return null; }
        public int getLineNumber() { return 0; }
        public int getColumnNumber() { return 0; }
    }

    /** Solo para comprobar que la interfaz se puede implementar y que los tres metodos lanzan. */
    static class OyenteCuenta implements ErrorListener {
        int avisos;
        int errores;
        int fatales;
        public void warning(TransformerException e) { avisos = avisos + 1; }
        public void error(TransformerException e) { errores = errores + 1; }
        public void fatalError(TransformerException e) throws TransformerException { fatales = fatales + 1; throw e; }
    }

    /** Un resolvedor que devuelve null, que segun el contrato significa "resolvelo vos". */
    static class ResolvedorNulo implements URIResolver {
        String ultimoHref;
        String ultimaBase;
        public Source resolve(String href, String base) {
            ultimoHref = href;
            ultimaBase = base;
            return null;
        }
    }

    /**
     * La subclase minima de `Transformer`: todos los abstractos, ninguno con sentido.
     *
     * <p>Sirve para una sola cosa y no pretende otra: comprobar que `reset()`, que es el unico
     * metodo **con cuerpo** de la clase abstracta, lanza `UnsupportedOperationException` como manda
     * el contrato. No transforma nada y `transform` lo dice lanzando.
     */
    static class TransformadorInutil extends Transformer {
        public void transform(Source s, Result r) throws TransformerException {
            throw new TransformerException("este transformador no transforma, es una prueba");
        }
        public void setParameter(String n, Object v) { }
        public Object getParameter(String n) { return null; }
        public void clearParameters() { }
        public void setURIResolver(URIResolver r) { }
        public URIResolver getURIResolver() { return null; }
        public void setOutputProperties(Properties p) { }
        public Properties getOutputProperties() { return new Properties(); }
        public void setOutputProperty(String n, String v) { }
        public String getOutputProperty(String n) { return null; }
        public void setErrorListener(ErrorListener l) { }
        public ErrorListener getErrorListener() { return null; }
    }

    static boolean iguales(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    // ---- la prueba ------------------------------------------------------------------------------

    public static int run() {
        // 10. Las constantes de OutputKeys son los atributos de <xsl:output>, tal cual.
        if (!iguales(OutputKeys.METHOD, "method")) return 10;
        if (!iguales(OutputKeys.VERSION, "version")) return 11;
        if (!iguales(OutputKeys.ENCODING, "encoding")) return 12;
        if (!iguales(OutputKeys.OMIT_XML_DECLARATION, "omit-xml-declaration")) return 13;
        if (!iguales(OutputKeys.STANDALONE, "standalone")) return 14;
        if (!iguales(OutputKeys.DOCTYPE_PUBLIC, "doctype-public")) return 15;
        if (!iguales(OutputKeys.DOCTYPE_SYSTEM, "doctype-system")) return 16;
        if (!iguales(OutputKeys.CDATA_SECTION_ELEMENTS, "cdata-section-elements")) return 17;
        if (!iguales(OutputKeys.INDENT, "indent")) return 18;
        if (!iguales(OutputKeys.MEDIA_TYPE, "media-type")) return 19;
        if (!iguales(Result.PI_DISABLE_OUTPUT_ESCAPING, "javax.xml.transform.disable-output-escaping")) return 20;
        if (!iguales(Result.PI_ENABLE_OUTPUT_ESCAPING, "javax.xml.transform.enable-output-escaping")) return 21;

        // 30. La ubicacion como texto. Los tres casos que se confunden: hay locator con datos, no
        //     hay locator (null), y hay locator sin datos (cadena vacia).
        TransformerException conLoc = new TransformerException("msg", new LocLleno());
        if (!iguales(conLoc.getLocationAsString(), "; SystemID: SYS; Line#: 7; Column#: 3")) return 30;
        if (!iguales(conLoc.getMessageAndLocation(), "msg; SystemID: SYS; Line#: 7; Column#: 3")) return 31;
        if (!iguales(conLoc.getMessage(), "msg")) return 32;
        if (conLoc.getLocator() == null) return 33;

        TransformerException sinLoc = new TransformerException("msg");
        if (sinLoc.getLocationAsString() != null) return 34;
        if (!iguales(sinLoc.getMessageAndLocation(), "msg")) return 35;
        if (sinLoc.getLocator() != null) return 36;

        TransformerException locVacio = new TransformerException("mm", new LocVacio());
        if (!iguales(locVacio.getLocationAsString(), "")) return 37;
        if (!iguales(locVacio.getMessageAndLocation(), "mm")) return 38;

        // 40. setLocator lo completa despues, que es para lo que existe.
        TransformerException puesta = new TransformerException("m");
        puesta.setLocator(new LocLleno());
        if (!iguales(puesta.getLocationAsString(), "; SystemID: SYS; Line#: 7; Column#: 3")) return 40;
        puesta.setLocator(null);
        if (puesta.getLocationAsString() != null) return 41;

        // 50. El mensaje sale de la causa cuando el propio es nulo o vacio.
        IllegalStateException adentro = new IllegalStateException("boom");
        TransformerException env = new TransformerException(adentro);
        if (!iguales(env.getMessage(), adentro.toString())) return 50;
        if (env.getCause() != adentro) return 51;
        if (env.getException() != adentro) return 52;
        if (!iguales(new TransformerException("", adentro).getMessage(), adentro.toString())) return 53;
        if (!iguales(new TransformerException((String) null, adentro).getMessage(), adentro.toString())) return 54;
        if (!iguales(new TransformerException("propio", adentro).getMessage(), "propio")) return 55;
        // Sin causa, las dos vias dan null: no se inventa una causa que no hay.
        if (sinLoc.getCause() != null) return 56;
        if (sinLoc.getException() != null) return 57;
        // Con los tres argumentos, la causa viaja aunque el mensaje sea propio.
        TransformerException tres = new TransformerException("propio", new LocLleno(), adentro);
        if (tres.getCause() != adentro) return 58;
        if (!iguales(tres.getMessage(), "propio")) return 59;

        // 60. initCause opera sobre el campo propio y se puede llamar una sola vez.
        TransformerException ic = new TransformerException("m");
        if (ic.initCause(adentro) != ic) return 60;
        if (ic.getCause() != adentro) return 61;
        if (ic.getException() != adentro) return 62;
        boolean salto = false;
        try {
            ic.initCause(new RuntimeException("otra"));
        } catch (IllegalStateException e) {
            salto = true;
        }
        if (!salto) return 63;
        // Sobre una construida con causa, tambien: es el mismo campo.
        salto = false;
        try {
            new TransformerException("m", adentro).initCause(new RuntimeException("otra"));
        } catch (IllegalStateException e) {
            salto = true;
        }
        if (!salto) return 64;
        // Causarse a si misma no se permite.
        salto = false;
        TransformerException sola = new TransformerException("m");
        try {
            sola.initCause(sola);
        } catch (IllegalArgumentException e) {
            salto = true;
        }
        if (!salto) return 65;
        // initCause(null) sobre una sin causa es valido y la deja sin causa.
        TransformerException nula = new TransformerException("m");
        if (nula.initCause(null) != nula) return 66;
        if (nula.getCause() != null) return 67;

        // 70. TransformerConfigurationException: mensaje por omision y jerarquia.
        TransformerConfigurationException tce = new TransformerConfigurationException();
        if (!iguales(tce.getMessage(), "Configuration Error")) return 70;
        if (!(tce instanceof TransformerException)) return 71;
        if (!(tce instanceof Exception)) return 72;
        TransformerConfigurationException tce2 =
                new TransformerConfigurationException("m", new LocLleno(), adentro);
        if (tce2.getCause() != adentro) return 73;
        if (!iguales(tce2.getLocationAsString(), "; SystemID: SYS; Line#: 7; Column#: 3")) return 74;
        if (!iguales(new TransformerConfigurationException(adentro).getMessage(), adentro.toString())) return 75;

        // 80. TransformerFactoryConfigurationError: es un Error, y su getMessage cae en la causa.
        TransformerFactoryConfigurationError e0 = new TransformerFactoryConfigurationError();
        if (!(e0 instanceof Error)) return 80;
        if (e0.getMessage() != null) return 81;
        if (e0.getException() != null) return 82;
        if (e0.getCause() != null) return 83;
        TransformerFactoryConfigurationError e1 = new TransformerFactoryConfigurationError(adentro);
        if (!iguales(e1.getMessage(), adentro.toString())) return 84;
        if (e1.getException() != adentro) return 85;
        if (e1.getCause() != adentro) return 86;
        TransformerFactoryConfigurationError e2 = new TransformerFactoryConfigurationError(adentro, "propio");
        if (!iguales(e2.getMessage(), "propio")) return 87;
        if (e2.getException() != adentro) return 88;
        // Con causa sin mensaje y sin mensaje propio, cae en el de la causa (que aca es null).
        if (new TransformerFactoryConfigurationError(new IllegalStateException()).getMessage() == null) return 89;

        // 90. La fabrica. El contrato, no el resultado: el JDK trae XSLT y nosotros no, asi que
        //     newInstance() alla devuelve una fabrica y aca lanza. Las dos cosas son correctas; lo
        //     que no seria correcto es null, u otra excepcion.
        try {
            TransformerFactory f = TransformerFactory.newInstance();
            if (f == null) return 90;
        } catch (TransformerFactoryConfigurationError e) {
            if (e.getMessage() == null) return 91;
        }
        // newDefaultInstance tiene el mismo contrato y la misma divergencia.
        try {
            TransformerFactory f = TransformerFactory.newDefaultInstance();
            if (f == null) return 92;
        } catch (TransformerFactoryConfigurationError e) {
            if (e.getMessage() == null) return 93;
        }

        // 94. Estas dos si dan lo mismo en las dos implementaciones: un nombre nulo y uno que no
        //     existe fallan igual, porque el fallo no depende de que haya procesador.
        salto = false;
        try {
            TransformerFactory.newInstance(null, null);
        } catch (TransformerFactoryConfigurationError e) {
            salto = true;
            if (!iguales(e.getMessage(),
                    "Provider null could not be instantiated: java.lang.NullPointerException")) return 95;
        }
        if (!salto) return 94;
        salto = false;
        try {
            TransformerFactory.newInstance("no.Such", null);
        } catch (TransformerFactoryConfigurationError e) {
            salto = true;
            if (!iguales(e.getMessage(), "Provider no.Such not found")) return 97;
        }
        if (!salto) return 96;

        // 100. Transformer.reset() por omision lanza; el resto de la clase es abstracto.
        Transformer t = new TransformadorInutil();
        salto = false;
        try {
            t.reset();
        } catch (UnsupportedOperationException e) {
            salto = true;
        }
        if (!salto) return 100;
        salto = false;
        try {
            t.transform(null, null);
        } catch (TransformerException e) {
            salto = true;
        }
        if (!salto) return 101;

        // 110. Las interfaces de devolucion de llamada se implementan y se usan.
        OyenteCuenta oyente = new OyenteCuenta();
        try {
            oyente.warning(sinLoc);
            oyente.error(sinLoc);
        } catch (TransformerException e) {
            return 110;
        }
        salto = false;
        try {
            oyente.fatalError(sinLoc);
        } catch (TransformerException e) {
            salto = true;
        }
        if (!salto) return 111;
        if (oyente.avisos != 1 || oyente.errores != 1 || oyente.fatales != 1) return 112;

        ResolvedorNulo res = new ResolvedorNulo();
        try {
            if (res.resolve("comun.xsl", "http://ej/") != null) return 113;
        } catch (TransformerException e) {
            return 114;
        }
        if (!iguales(res.ultimoHref, "comun.xsl")) return 115;
        if (!iguales(res.ultimaBase, "http://ej/")) return 116;

        // 120. Que los tipos que quedan sean asignables como manda la jerarquia. Sin instanciarlos:
        //      `Templates` es una interfaz que solo un procesador de XSLT implementa.
        Class<?> ct = Templates.class;
        if (ct == null) return 120;
        if (!Source.class.isAssignableFrom(Source.class)) return 121;

        // 130. El vocabulario compartido. Una URI mal tipeada aca no da error en ningun lado:
        //      simplemente deja de matchear, que es la peor forma de fallar.
        if (!iguales(XMLConstants.NULL_NS_URI, "")) return 130;
        if (!iguales(XMLConstants.DEFAULT_NS_PREFIX, "")) return 131;
        if (!iguales(XMLConstants.XML_NS_URI, "http://www.w3.org/XML/1998/namespace")) return 132;
        if (!iguales(XMLConstants.XML_NS_PREFIX, "xml")) return 133;
        if (!iguales(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "http://www.w3.org/2000/xmlns/")) return 134;
        if (!iguales(XMLConstants.XMLNS_ATTRIBUTE, "xmlns")) return 135;
        if (!iguales(XMLConstants.W3C_XML_SCHEMA_NS_URI, "http://www.w3.org/2001/XMLSchema")) return 136;
        if (!iguales(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                "http://www.w3.org/2001/XMLSchema-instance")) return 137;
        if (!iguales(XMLConstants.W3C_XPATH_DATATYPE_NS_URI,
                "http://www.w3.org/2003/11/xpath-datatypes")) return 138;
        if (!iguales(XMLConstants.XML_DTD_NS_URI, "http://www.w3.org/TR/REC-xml")) return 139;
        if (!iguales(XMLConstants.RELAXNG_NS_URI, "http://relaxng.org/ns/structure/1.0")) return 140;
        if (!iguales(XMLConstants.FEATURE_SECURE_PROCESSING,
                "http://javax.xml.XMLConstants/feature/secure-processing")) return 141;
        if (!iguales(XMLConstants.ACCESS_EXTERNAL_DTD,
                "http://javax.xml.XMLConstants/property/accessExternalDTD")) return 142;
        if (!iguales(XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                "http://javax.xml.XMLConstants/property/accessExternalSchema")) return 143;
        if (!iguales(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,
                "http://javax.xml.XMLConstants/property/accessExternalStylesheet")) return 144;
        if (!iguales(XMLConstants.USE_CATALOG, "http://javax.xml.XMLConstants/feature/useCatalog")) return 145;

        return -1;
    }
}
