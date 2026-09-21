package org.xml.sax;

import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.SAXNotRecognizedException -- getFeature/setFeature/getProperty/
// setProperty throw it when the reader never *heard of* the name it is asked about. The
// distinction with SAXNotSupportedException is the whole reason for having two classes: "not
// recognised" means the URI is unknown, "not supported" means it is known but right now the value
// asked for cannot be given (typically because the analysis is already under way).
public class SAXNotRecognizedException extends SAXException {

    static final long serialVersionUID = 5440506620509557213L;

    public SAXNotRecognizedException() {
        super();
    }

    public SAXNotRecognizedException(String message) {
        super(message);
    }
}
