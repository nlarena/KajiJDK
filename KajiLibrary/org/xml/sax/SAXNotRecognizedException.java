package org.xml.sax;

import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.SAXNotRecognizedException -- la lanzan getFeature/setFeature/
// getProperty/setProperty cuando el lector nunca *oyo hablar* del nombre por el que se le
// pregunta. La distincion con SAXNotSupportedException es toda la razon de tener dos clases:
// "no reconocida" quiere decir que el URI es desconocido, "no soportada" quiere decir que se lo
// conoce pero ahora mismo no se le puede dar el valor pedido (tipicamente porque el analisis ya
// esta en marcha).
public class SAXNotRecognizedException extends SAXException {

    static final long serialVersionUID = 5440506620509557213L;

    public SAXNotRecognizedException() {
        super();
    }

    public SAXNotRecognizedException(String message) {
        super(message);
    }
}
