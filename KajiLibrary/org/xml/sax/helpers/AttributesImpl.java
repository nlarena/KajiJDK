package org.xml.sax.helpers;

import org.xml.sax.Attributes;

// KajiLibrary's org.xml.sax.helpers.AttributesImpl -- the implementation of {@link Attributes}
// that serves for the two things needed: **freezing** the attributes a parser lent during
// `startElement`, and **building** them by hand when one generates events oneself.
//
// The storage is a single `String[]` with five slots per attribute --uri, local name, qualified
// name, type, value-- instead of an array of objects. It is the JDK's representation and it is
// worth understanding why: the attributes of an element are typically zero, one or two, and at
// that scale an object per attribute costs more in allocations and pointer chasing than it saves
// in clarity. The price is that every index goes multiplied by five, so attribute `i` lives in
// `data[i*5 .. i*5+4]`.
//
// **The asymmetry of out-of-range indices belongs to the contract, it is not an oversight.** The
// `getXxx(int)` return `null` --an index that does not exist simply has no value--, but the
// `setXxx(int)` and `removeAttribute(int)` throw `ArrayIndexOutOfBoundsException`: writing into a
// position that does not exist is a program error and keeping quiet about it would lose the datum.
//
// The lookups by name are linear. With the number of attributes a real element has, that is faster
// than any map, and it avoids maintaining an index that would have to be rebuilt on every
// `setQName`.
public class AttributesImpl implements Attributes {

    // Number of attributes; the array may be longer.
    int length;

    // uri, localName, qName, type, value for each attribute, in that order.
    String data[];

    // An empty list, ready for `addAttribute`.
    public AttributesImpl() {
        length = 0;
        data = null;
    }

    // An independent copy of `atts`. This is the constructor that solves the classic SAX bug: the
    // object that arrives at `startElement` stops being valid when the call ends, and this copy
    // does not.
    public AttributesImpl(Attributes atts) {
        setAttributes(atts);
    }

    public int getLength() {
        return length;
    }

    public String getURI(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5];
        } else {
            return null;
        }
    }

    public String getLocalName(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 1];
        } else {
            return null;
        }
    }

    public String getQName(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 2];
        } else {
            return null;
        }
    }

    public String getType(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 3];
        } else {
            return null;
        }
    }

    public String getValue(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 4];
        } else {
            return null;
        }
    }

    // Lookup by (URI, local name). -1 if it is not there.
    public int getIndex(String uri, String localName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i + 1].equals(localName)) {
                return i / 5;
            }
        }
        return -1;
    }

    // Lookup by qualified name. -1 if it is not there.
    public int getIndex(String qName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i + 2].equals(qName)) {
                return i / 5;
            }
        }
        return -1;
    }

    public String getType(String uri, String localName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i + 1].equals(localName)) {
                return data[i + 3];
            }
        }
        return null;
    }

    public String getType(String qName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i + 2].equals(qName)) {
                return data[i + 3];
            }
        }
        return null;
    }

    public String getValue(String uri, String localName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i + 1].equals(localName)) {
                return data[i + 4];
            }
        }
        return null;
    }

    public String getValue(String qName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i + 2].equals(qName)) {
                return data[i + 4];
            }
        }
        return null;
    }

    // It empties the list. It also nulls out the used slots, not only `length`: otherwise the list
    // would go on holding strings nobody looks at any more.
    public void clear() {
        if (data != null) {
            for (int i = 0; i < (length * 5); i++) {
                data[i] = null;
            }
        }
        length = 0;
    }

    // It replaces the contents with a copy of those of `atts`.
    public void setAttributes(Attributes atts) {
        clear();
        length = atts.getLength();
        if (length > 0) {
            data = new String[length * 5];
            for (int i = 0; i < length; i++) {
                data[i * 5] = atts.getURI(i);
                data[i * 5 + 1] = atts.getLocalName(i);
                data[i * 5 + 2] = atts.getQName(i);
                data[i * 5 + 3] = atts.getType(i);
                data[i * 5 + 4] = atts.getValue(i);
            }
        }
    }

    // It appends at the end. It does not check for duplicates: XML forbids them, but whoever
    // generates the events is responsible for that, not this list.
    public void addAttribute(String uri, String localName, String qName,
                             String type, String value) {
        ensureCapacity(length + 1);
        data[length * 5] = uri;
        data[length * 5 + 1] = localName;
        data[length * 5 + 2] = qName;
        data[length * 5 + 3] = type;
        data[length * 5 + 4] = value;
        length++;
    }

    // It rewrites the five fields of attribute `index`.
    public void setAttribute(int index, String uri, String localName,
                             String qName, String type, String value) {
        if (index >= 0 && index < length) {
            data[index * 5] = uri;
            data[index * 5 + 1] = localName;
            data[index * 5 + 2] = qName;
            data[index * 5 + 3] = type;
            data[index * 5 + 4] = value;
        } else {
            badIndex(index);
        }
    }

    // It removes attribute `index`, shifting the ones behind one position. The indices of the
    // following ones change, which is what is to be expected in a list.
    public void removeAttribute(int index) {
        if (index >= 0 && index < length) {
            if (index < length - 1) {
                System.arraycopy(data, (index + 1) * 5, data, index * 5,
                                 (length - index - 1) * 5);
            }
            index = (length - 1) * 5;
            data[index++] = null;
            data[index++] = null;
            data[index++] = null;
            data[index++] = null;
            data[index] = null;
            length--;
        } else {
            badIndex(index);
        }
    }

    public void setURI(int index, String uri) {
        if (index >= 0 && index < length) {
            data[index * 5] = uri;
        } else {
            badIndex(index);
        }
    }

    public void setLocalName(int index, String localName) {
        if (index >= 0 && index < length) {
            data[index * 5 + 1] = localName;
        } else {
            badIndex(index);
        }
    }

    public void setQName(int index, String qName) {
        if (index >= 0 && index < length) {
            data[index * 5 + 2] = qName;
        } else {
            badIndex(index);
        }
    }

    public void setType(int index, String type) {
        if (index >= 0 && index < length) {
            data[index * 5 + 3] = type;
        } else {
            badIndex(index);
        }
    }

    public void setValue(int index, String value) {
        if (index >= 0 && index < length) {
            data[index * 5 + 4] = value;
        } else {
            badIndex(index);
        }
    }

    // It grows to double from a floor of 25 slots (five attributes), which is what the JDK chose:
    // the vast majority of elements fit in the first block and never copy again.
    private void ensureCapacity(int n) {
        if (n <= 0) {
            return;
        }
        int max;
        if (data == null || data.length == 0) {
            max = 25;
        } else if (data.length >= n * 5) {
            return;
        } else {
            max = data.length;
        }
        while (max < n * 5) {
            max *= 2;
        }
        String newData[] = new String[max];
        if (length > 0) {
            System.arraycopy(data, 0, newData, 0, length * 5);
        }
        data = newData;
    }

    private void badIndex(int index) {
        String msg = "Attempt to modify attribute at illegal index: " + index;
        throw new ArrayIndexOutOfBoundsException(msg);
    }
}
