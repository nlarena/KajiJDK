package org.xml.sax;

import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.EntityResolver -- el gancho que le permite a una aplicacion
// interceptar cada entidad externa que el parser esta por buscar (el subconjunto externo de la
// DTD, las entidades externas analizadas) y poner sus propios bytes en su lugar.
//
// Es la interfaz detras de dos practicas muy distintas. La buena: redirigir un identificador
// publico conocido a una copia local del catalogo, para que una compilacion no dependa de que
// una DTD remota este levantada. La defensiva: devolver un InputSource vacio para negar la
// busqueda de plano, que es la mitigacion clasica de XXE --un documento no confiable que nombra
// una entidad de sistema que no deberia poder leer.
//
// Devolver null significa "resolvela como siempre", es decir, abri vos mismo el identificador de
// sistema.
public interface EntityResolver {

    // Devuelve el InputSource que el parser deberia leer en su lugar, o null para dejar que abra
    // el identificador de sistema por su cuenta.
    InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException;
}
