package org.xml.sax.helpers;

import org.xml.sax.Parser;

// KajiLibrary's org.xml.sax.helpers.ParserFactory -- "give me the SAX1 parser the system property
// names", and nothing else.
//
// It was SAX1's way of avoiding a compile-time dependency on a concrete parser: the name of the
// class lives in the system property `org.xml.sax.parser`, this class loads it by reflection and
// casts it to Parser. Each way of failing is a different checked exception, and they are all in
// the throws clause instead of wrapped:
//
//   NullPointerException      the property is not even set
//   ClassNotFoundException    it names a class that is not on the classpath
//   IllegalAccessException    the class or its no-argument constructor is not accessible
//   InstantiationException    it is abstract, or has no no-argument constructor
//   ClassCastException        it loaded, but it is not an org.xml.sax.Parser
//
// Note that a NullPointerException in a throws clause is odd and is on purpose: SAX1 chose to
// signal "not configured" with an unchecked exception and to document it. Having it in the clause
// is part of the contract, not an ornament.
//
// This class is deprecated in the JDK together with the rest of SAX1; XMLReaderFactory is its
// SAX2 replacement. In this library, just as in a stock JDK with no parser configured, both are
// mechanisms that have nothing to find: KajiLibrary brings no XML parser, so makeParser() throws
// NullPointerException unless the caller points the property at a class of their own. That is the
// right answer and not a stub -- the job of the factory is finding somebody else's parser, and
// search, it does.
public class ParserFactory {

    // Not instantiated: everything here is static.
    private ParserFactory() {
    }

    // The parser the system property `org.xml.sax.parser` names.
    public static Parser makeParser()
            throws ClassNotFoundException, IllegalAccessException,
                   InstantiationException, NullPointerException,
                   ClassCastException {
        String className = System.getProperty("org.xml.sax.parser");
        if (className == null) {
            throw new NullPointerException("No value for sax.parser property");
        } else {
            return makeParser(className);
        }
    }

    // The parser with exactly this class name, loaded with this class's own loader so that a driver
    // sitting next to SAX itself is found.
    public static Parser makeParser(String className)
            throws ClassNotFoundException, IllegalAccessException,
                   InstantiationException, ClassCastException {
        ClassLoader loader = ParserFactory.class.getClassLoader();
        Class<?> c;
        if (loader == null) {
            c = Class.forName(className);
        } else {
            c = loader.loadClass(className);
        }
        // The cast is what turns "I loaded something" into "I loaded a Parser", and its failing is
        // one of the documented outcomes.
        return (Parser) c.newInstance();
    }
}
