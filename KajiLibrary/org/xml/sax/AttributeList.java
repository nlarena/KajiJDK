package org.xml.sax;

// KajiLibrary's org.xml.sax.AttributeList -- the attribute list of SAX1, replaced by
// Attributes.
//
// It is kept because the SAX1 shapes (DocumentHandler, Parser) still name it, and because the
// helpers.ParserAdapter bridge exists precisely to turn one of these into an Attributes. The
// difference with Attributes is that here there are no namespaces at all: an attribute has one
// single name, the one that was written in the document, prefix and all.
//
// The same rule of validity as in Attributes holds: it only serves inside the call to
// startElement; to keep it, it has to be copied with helpers.AttributeListImpl.
//
// It is deprecated in the JDK. Here it carries no @Deprecated: the annotation is metadata and not
// a member, so it does not enter the contract this library is measured against, and leaving it
// out keeps the file compiling on the frozen javac without depending on how annotations are
// retained.
public interface AttributeList {

    // The number of attributes in the list.
    int getLength();

    // The name of the attribute at `index`, or null if the index is out of range.
    String getName(int i);

    // The type of the attribute at `index` ("CDATA" and the rest), or null if it is out of range.
    String getType(int i);

    // The value of the attribute at `index`, or null if it is out of range.
    String getValue(int i);

    // The type of the attribute with that name, or null if there is no such attribute.
    String getType(String name);

    // The value of the attribute with that name, or null if there is no such attribute.
    String getValue(String name);
}
