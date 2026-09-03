package org.xml.sax;

import java.io.IOException;
import java.util.Locale;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.Parser -- la interfaz de lector de SAX1, reemplazada por XMLReader.
//
// Dos cosas la distinguen de XMLReader y las dos explican por que se la reemplazo: no hay
// espacios de nombres (alimenta un DocumentHandler, no un ContentHandler) y no hay mecanismo de
// features y propiedades --setLocale es la unica perilla, y por eso cada parser de SAX1 termino
// con sus propios metodos de configuracion incompatibles.
//
// helpers.XMLReaderAdapter implementa esta interfaz sobre un XMLReader de SAX2;
// helpers.ParserAdapter implementa XMLReader sobre uno de estos. Deprecada en el JDK; ver
// AttributeList por que aca no se escribe @Deprecated.
public interface Parser {

    // Pide los mensajes de error en una region. Lanza SAXException si la region no esta
    // soportada --el unico punto de configuracion que SAX1 llego a estandarizar.
    void setLocale(Locale locale) throws SAXException;

    // Instala el resolvedor de entidades externas; null significa el comportamiento por omision.
    void setEntityResolver(EntityResolver resolver);

    // Instala el manejador de la DTD.
    void setDTDHandler(DTDHandler handler);

    // Instala el manejador del documento. Sin uno, el parser no reporta nada.
    void setDocumentHandler(DocumentHandler handler);

    // Instala el manejador de errores. Sin uno, los errores son silenciosos y los fatales lanzan.
    void setErrorHandler(ErrorHandler handler);

    // Analiza un documento. No es reentrante: un analisis por vez por instancia de Parser.
    void parse(InputSource source) throws SAXException, IOException;

    // Atajo para parse(new InputSource(systemId)).
    void parse(String systemId) throws SAXException, IOException;
}
