package org.xml.sax;

import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.SAXNotSupportedException -- el nombre se reconoce, pero el valor
// que se pide no se puede entregar: una propiedad de solo lectura, una feature que no se puede
// dar vuelta mientras hay un analisis en curso, un valor del tipo equivocado. Ver
// SAXNotRecognizedException para la otra mitad del par.
public class SAXNotSupportedException extends SAXException {

    static final long serialVersionUID = -1422818934641823846L;

    public SAXNotSupportedException() {
        super();
    }

    public SAXNotSupportedException(String message) {
        super(message);
    }
}
