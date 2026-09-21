package org.xml.sax.helpers;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.XMLReaderFactory -- "find me an XMLReader", the SAX2
// replacement for ParserFactory.
//
// It looks in two places, in this order, and keeps the first one that answers:
//
//   1. the system property `org.xml.sax.driver`, which names a class directly;
//   2. the service provider mechanism, that is an org.xml.sax.XMLReader entry declared by some
//      jar on the classpath.
//
// A stock JDK has a third step, a hard-coded fallback to its bundled Xerces
// (com.sun.org.apache.xerces.internal.parsers.SAXParser). **KajiLibrary does not have that
// fallback, because KajiLibrary brings no XML parser.** With neither the property set nor a
// provider declared, createXMLReader() throws SAXException, which is exactly what the JDK does when
// its own fallback is not there. This is the only place in these two packages where a behaviour of
// the JDK is on purpose not reproduced, and the reason is that reproducing it would mean naming a
// class that does not exist here -- a lie that would fail on the call, not on the declaration.
//
// All the rest of org.xml.sax and org.xml.sax.helpers works with no parser: the handler interfaces
// are declarations, and the helpers (AttributesImpl, NamespaceSupport, XMLFilterImpl,
// ParserAdapter, XMLReaderAdapter) operate on events regardless of who produced them. Point this
// factory at a driver class and the whole layer works.
//
// Both methods report any failure as a SAXException with the original as its cause -- unlike
// ParserFactory, which lets five different checked exceptions out. That is the improvement of
// SAX2: one single exception type to catch.
public final class XMLReaderFactory {

    private static final String property = "org.xml.sax.driver";

    // Not instantiated: everything here is static.
    private XMLReaderFactory() {
    }

    // An XMLReader found with the search the comment of the class describes.
    public static XMLReader createXMLReader() throws SAXException {
        String className = null;
        ClassLoader loader = classLoader();

        // 1. The system property.
        try {
            className = System.getProperty(property);
        } catch (RuntimeException e) {
            // A restricted environment may refuse to answer. It is not an error: we keep looking.
        }

        if (className != null) {
            return loadClass(loader, className);
        }

        // 2. A declared service provider, if there is one.
        XMLReader reader = findServiceProvider(XMLReader.class, loader);
        if (reader != null) {
            return reader;
        }

        // 3. Here there is no step 3. See the comment of the class.
        throw new SAXException("Can't create XMLReader: no value for the "
                               + property + " system property, and no "
                               + "org.xml.sax.XMLReader service provider is "
                               + "declared on the classpath. KajiLibrary "
                               + "bundles no XML parser of its own.");
    }

    // The reader with exactly this class name.
    public static XMLReader createXMLReader(String className)
            throws SAXException {
        return loadClass(classLoader(), className);
    }

    // The context class loader when there is one, and otherwise the loader that loaded SAX itself.
    // A driver on the classpath of the application is invisible to the latter, and that is why the
    // context one is tried first.
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

    // Load, instantiate and cast, turning the five ways of failing into one single SAXException
    // that still carries the original as its cause.
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

    // The first declared provider, or null if there is none. A provider that blows up while being
    // built is a real failure and is reported; there being no provider is not.
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
