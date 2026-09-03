package org.xml.sax.helpers;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.XMLReaderFactory -- "encontrame un XMLReader", el reemplazo
// de SAX2 para ParserFactory.
//
// Busca en dos lugares, en este orden, y se queda con el primero que conteste:
//
//   1. la propiedad de sistema `org.xml.sax.driver`, que nombra una clase directamente;
//   2. el mecanismo de proveedores de servicio, es decir una entrada org.xml.sax.XMLReader
//      declarada por algun jar del classpath.
//
// Un JDK de fabrica tiene un tercer paso, un respaldo hardcodeado a su Xerces incluido
// (com.sun.org.apache.xerces.internal.parsers.SAXParser). **KajiLibrary no tiene ese respaldo,
// porque KajiLibrary no trae ningun parser XML.** Sin la propiedad seteada ni un proveedor
// declarado, createXMLReader() tira SAXException, que es exactamente lo que hace el JDK cuando su
// propio respaldo no esta. Este es el unico lugar de estos dos paquetes donde a proposito no se
// reproduce un comportamiento del JDK, y la razon es que reproducirlo significaria nombrar una
// clase que aca no existe -- una mentira que fallaria en la llamada, no en la declaracion.
//
// Todo el resto de org.xml.sax y org.xml.sax.helpers funciona sin parser: las interfaces de
// manejadores son declaraciones, y los helpers (AttributesImpl, NamespaceSupport, XMLFilterImpl,
// ParserAdapter, XMLReaderAdapter) operan sobre eventos sin importar quien los produjo. Apuntale
// a esta fabrica una clase driver y toda la capa anda.
//
// Los dos metodos reportan cualquier falla como una SAXException con la original como causa -- a
// diferencia de ParserFactory, que deja salir cinco excepciones chequeadas distintas. Esa es la
// mejora de SAX2: un solo tipo de excepcion para atrapar.
public final class XMLReaderFactory {

    private static final String property = "org.xml.sax.driver";

    // No se instancia: aca todo es estatico.
    private XMLReaderFactory() {
    }

    // Un XMLReader encontrado con la busqueda que describe el comentario de la clase.
    public static XMLReader createXMLReader() throws SAXException {
        String className = null;
        ClassLoader loader = classLoader();

        // 1. La propiedad de sistema.
        try {
            className = System.getProperty(property);
        } catch (RuntimeException e) {
            // Un entorno restringido puede negarse a contestar. No es un error: seguimos
            // buscando.
        }

        if (className != null) {
            return loadClass(loader, className);
        }

        // 2. Un proveedor de servicio declarado, si lo hay.
        XMLReader reader = findServiceProvider(XMLReader.class, loader);
        if (reader != null) {
            return reader;
        }

        // 3. Aca no hay paso 3. Ver el comentario de la clase.
        throw new SAXException("Can't create XMLReader: no value for the "
                               + property + " system property, and no "
                               + "org.xml.sax.XMLReader service provider is "
                               + "declared on the classpath. KajiLibrary "
                               + "bundles no XML parser of its own.");
    }

    // El lector con exactamente este nombre de clase.
    public static XMLReader createXMLReader(String className)
            throws SAXException {
        return loadClass(classLoader(), className);
    }

    // El class loader de contexto cuando hay uno, y si no el loader que cargo SAX mismo. Un
    // driver que este en el classpath de la aplicacion es invisible para este ultimo, y por eso
    // el de contexto se prueba primero.
    private static ClassLoader classLoader() {
        ClassLoader loader = null;
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (RuntimeException e) {
            loader = null;
        }
        if (loader == null) {
            loader = XMLReaderFactory.class.getClassLoader();
        }
        return loader;
    }

    // Cargar, instanciar y castear, convirtiendo las cinco maneras de fallar en una sola
    // SAXException que igual lleva la original como causa.
    private static XMLReader loadClass(ClassLoader loader, String className)
            throws SAXException {
        try {
            Class<?> c;
            if (loader == null) {
                c = Class.forName(className);
            } else {
                c = loader.loadClass(className);
            }
            return (XMLReader) c.newInstance();
        } catch (ClassNotFoundException e1) {
            throw new SAXException("SAX2 driver class " + className
                                   + " not found", e1);
        } catch (IllegalAccessException e2) {
            throw new SAXException("SAX2 driver class " + className
                                   + " found but cannot be loaded", e2);
        } catch (InstantiationException e3) {
            throw new SAXException("SAX2 driver class " + className
                                   + " loaded but cannot be instantiated"
                                   + " (no empty public constructor?)", e3);
        } catch (ClassCastException e4) {
            throw new SAXException("SAX2 driver class " + className
                                   + " does not implement XMLReader", e4);
        }
    }

    // El primer proveedor declarado, o null si no hay ninguno. Un proveedor que explota mientras
    // se construye es una falla de verdad y se reporta; que no haya proveedor no lo es.
    private static <T> T findServiceProvider(Class<T> type, ClassLoader loader)
            throws SAXException {
        try {
            ServiceLoader<T> serviceLoader;
            if (loader == null) {
                serviceLoader = ServiceLoader.load(type);
            } else {
                serviceLoader = ServiceLoader.load(type, loader);
            }
            Iterator<T> it = serviceLoader.iterator();
            if (it.hasNext()) {
                return it.next();
            }
            return null;
        } catch (RuntimeException e) {
            throw new SAXException("Can't find or create the "
                                   + type.getName() + " service provider", e);
        }
    }
}
