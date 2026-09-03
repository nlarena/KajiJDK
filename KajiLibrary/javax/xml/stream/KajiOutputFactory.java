package javax.xml.stream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * La fabrica de escritura de esta biblioteca.
 *
 * <p>Solo tiene una propiedad, {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES}, y la respeta de
 * verdad: los dos modos estan implementados en {@link KajiStreamWriter}.
 *
 * <p>Un {@link OutputStream} sin codificacion se escribe en UTF-8, que es lo que hace el original y
 * ademas la unica eleccion sensata: es la codificacion por omision de XML.
 */
final class KajiOutputFactory extends XMLOutputFactory {

    private boolean repairing;

    KajiOutputFactory() {
    }

    public XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException {
        if (stream == null) {
            throw new XMLStreamException("el escritor no puede ser null");
        }
        return new KajiStreamWriter(stream, repairing);
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException {
        return createXMLStreamWriter(stream, "UTF-8");
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding)
            throws XMLStreamException {
        if (stream == null) {
            throw new XMLStreamException("el flujo no puede ser null");
        }
        String enc = encoding;
        if (enc == null) {
            enc = "UTF-8";
        }
        try {
            return createXMLStreamWriter(new OutputStreamWriter(stream, enc));
        } catch (UnsupportedEncodingException e) {
            throw new XMLStreamException("no se conoce la codificacion " + enc, e);
        }
    }

    public XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException {
        return createXMLStreamWriter(writerOf(result));
    }

    public XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException {
        return new KajiEventWriter(createXMLStreamWriter(stream));
    }

    public XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException {
        return new KajiEventWriter(createXMLStreamWriter(stream));
    }

    public XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding)
            throws XMLStreamException {
        return new KajiEventWriter(createXMLStreamWriter(stream, encoding));
    }

    public XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException {
        return new KajiEventWriter(createXMLStreamWriter(result));
    }

    public void setProperty(String name, Object value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("el nombre de la propiedad no puede ser null");
        }
        if (name.equals(IS_REPAIRING_NAMESPACES)) {
            if (value instanceof Boolean) {
                repairing = ((Boolean) value).booleanValue();
                return;
            }
            if (value instanceof String) {
                repairing = Boolean.valueOf((String) value).booleanValue();
                return;
            }
            throw new IllegalArgumentException(name + " toma un booleano, y le dieron " + value);
        }
        throw new IllegalArgumentException("propiedad desconocida: " + name);
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        if (name != null && name.equals(IS_REPAIRING_NAMESPACES)) {
            return Boolean.valueOf(repairing);
        }
        throw new IllegalArgumentException("propiedad desconocida: " + name);
    }

    public boolean isPropertySupported(String name) {
        return name != null && name.equals(IS_REPAIRING_NAMESPACES);
    }

    private static Writer writerOf(Result result) throws XMLStreamException {
        if (result == null) {
            throw new XMLStreamException("el resultado no puede ser null");
        }
        if (!(result instanceof StreamResult)) {
            throw new XMLStreamException(
                    "esta biblioteca solo escribe a un StreamResult, y le dieron un "
                            + result.getClass().getName());
        }
        StreamResult r = (StreamResult) result;
        if (r.getWriter() != null) {
            return r.getWriter();
        }
        if (r.getOutputStream() != null) {
            try {
                return new OutputStreamWriter(r.getOutputStream(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new XMLStreamException("no se conoce UTF-8", e);
            }
        }
        String sid = r.getSystemId();
        if (sid == null) {
            throw new XMLStreamException("el StreamResult esta vacio");
        }
        String path = sid;
        if (path.startsWith("file:///")) {
            path = path.substring(8);
        } else if (path.startsWith("file://")) {
            path = path.substring(7);
        } else if (path.startsWith("file:")) {
            path = path.substring(5);
        }
        try {
            return new OutputStreamWriter(new FileOutputStream(new File(path)), "UTF-8");
        } catch (IOException e) {
            throw new XMLStreamException("no se pudo abrir " + sid + " para escribir", e);
        }
    }
}
