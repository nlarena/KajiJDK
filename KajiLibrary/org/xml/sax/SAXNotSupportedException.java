package org.xml.sax;

import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.SAXNotSupportedException -- the name is recognised, but the value
// asked for cannot be delivered: a read-only property, a feature that cannot be flipped while an
// analysis is running, a value of the wrong type. See SAXNotRecognizedException for the other half
// of the pair.
public class SAXNotSupportedException extends SAXException {

    static final long serialVersionUID = -1422818934641823846L;

    public SAXNotSupportedException() {
        super();
    }

    public SAXNotSupportedException(String message) {
        super(message);
    }
}
