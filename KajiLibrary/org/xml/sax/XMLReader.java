package org.xml.sax;

import java.io.IOException;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

// KajiLibrary's org.xml.sax.XMLReader -- el lector de SAX2: el objeto que una aplicacion
// configura y despues manda a recorrer un documento.
//
// Lo que agrega sobre el Parser de SAX1 es el par feature/propiedad. Los dos se indexan por
// cadena con un URI, que fue como SAX evito estandarizar un metodo por perilla y aun asi dejo
// que los proveedores extendieran:
//
//   * una *feature* es un interruptor booleano. Las dos que importan son
//     http://xml.org/sax/features/namespaces (prendida por omision: reporta uri/localName) y
//     .../namespace-prefixes (apagada por omision: reporta ademas los qName y los atributos
//     xmlns). Al menos una de las dos tiene que estar prendida.
//   * una *propiedad* es un objeto cualquiera, por ejemplo .../properties/lexical-handler.
//
// Las dos excepciones son todo el protocolo: SAXNotRecognizedException para un nombre del que el
// lector nunca escucho hablar, SAXNotSupportedException para uno que conoce pero no puede
// atender ahora --leer una propiedad de solo escritura, o cambiar una feature en medio del
// analisis.
//
// parse() es sincronico y no reentrante; el lector se puede reusar para otro documento una vez
// que retorna.
public interface XMLReader {

    // Consulta el valor de una feature.
    boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // Cambia una feature. Algunas son de solo lectura durante un analisis.
    void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // Consulta el valor de una propiedad.
    Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // Cambia una propiedad.
    void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // Instala el resolvedor de entidades externas; null vuelve al comportamiento por omision.
    void setEntityResolver(EntityResolver resolver);

    // El resolvedor de entidades actual, o null.
    EntityResolver getEntityResolver();

    // Instala el manejador de la DTD; null vuelve al de por omision.
    void setDTDHandler(DTDHandler handler);

    // El manejador de la DTD actual, o null.
    DTDHandler getDTDHandler();

    // Instala el manejador de contenido --el que realmente recibe el documento.
    void setContentHandler(ContentHandler handler);

    // El manejador de contenido actual, o null.
    ContentHandler getContentHandler();

    // Instala el manejador de errores; sin uno, los avisos y los errores recuperables se
    // descartan.
    void setErrorHandler(ErrorHandler handler);

    // El manejador de errores actual, o null.
    ErrorHandler getErrorHandler();

    // Analiza un documento, bloqueando hasta terminar. De a uno por lector.
    void parse(InputSource input) throws IOException, SAXException;

    // Atajo para parse(new InputSource(systemId)).
    void parse(String systemId) throws IOException, SAXException;
}
