package org.xml.sax.helpers;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.AttributeList;

// KajiLibrary's org.xml.sax.helpers.AttributeListImpl -- the SAX1 attribute list, keepable and
// mutable.
//
// It exists for the same reason as LocatorImpl: the AttributeList a SAX1 parser passes to
// startElement is valid only while that call lasts, and a handler that wants to keep the
// attributes has to copy them. `new AttributeListImpl(atts)` is that copy.
//
// The other use is the mirror of the previous one: code that has to *produce* startElement events
// needs an AttributeList to hand over, and building it with addAttribute/clear is easier than
// implementing the interface every time.
//
// Three parallel lists instead of a list of triples, because that is the shape the getters want:
// getName(i)/getType(i)/getValue(i) are three independent lookups by the same index. The SAX2
// replacement, AttributesImpl, packs everything into one flat String[] with a stride of five; they
// are the same idea with different arithmetic.
//
// The lookup by name (getType(String)/getValue(String)) is a linear walk of the names and returns
// the *first* match. SAX1 had no notion of namespaces, so here a name is the literal name of the
// attribute as it was written, with the prefix attached.
//
// This class is deprecated in the JDK, together with the whole SAX1 layer; it is here because the
// contract still lists it and because ParserAdapter needs something of this shape.
public class AttributeListImpl implements AttributeList {

    List<String> names = new ArrayList<String>();
    List<String> types = new ArrayList<String>();
    List<String> values = new ArrayList<String>();

    public AttributeListImpl() {
    }

    // The copy constructor described above.
    public AttributeListImpl(AttributeList atts) {
        setAttributeList(atts);
    }

    ////////////////////////////////////////////////////////////////////
    // Construction
    ////////////////////////////////////////////////////////////////////

    // It replaces everything with a copy of `atts`. It reads getLength() once and then walks it, so
    // it is a fixed copy even of a list that is about to change.
    public void setAttributeList(AttributeList atts) {
        int count = atts.getLength();

        clear();

        for (int i = 0; i < count; i++) {
            addAttribute(atts.getName(i), atts.getType(i), atts.getValue(i));
        }
    }

    public void addAttribute(String name, String type, String value) {
        names.add(name);
        types.add(type);
        values.add(value);
    }

    // It removes the first attribute with this name, if there is any; a name that is not there is
    // not an error, there is simply nothing to do.
    public void removeAttribute(String name) {
        int i = names.indexOf(name);
        if (i >= 0) {
            names.remove(i);
            types.remove(i);
            values.remove(i);
        }
    }

    public void clear() {
        names.clear();
        types.clear();
        values.clear();
    }

    ////////////////////////////////////////////////////////////////////
    // AttributeList
    ////////////////////////////////////////////////////////////////////

    public int getLength() {
        return names.size();
    }

    // The getters by index answer null for an index out of range instead of throwing, which is what
    // the SAX1 contract asks for.
    public String getName(int i) {
        if (i < 0 || i >= names.size()) {
            return null;
        }
        return names.get(i);
    }

    public String getType(int i) {
        if (i < 0 || i >= types.size()) {
            return null;
        }
        return types.get(i);
    }

    public String getValue(int i) {
        if (i < 0 || i >= values.size()) {
            return null;
        }
        return values.get(i);
    }

    public String getType(String name) {
        return getType(names.indexOf(name));
    }

    public String getValue(String name) {
        return getValue(names.indexOf(name));
    }
}
