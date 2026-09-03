package org.xml.sax.ext;

import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.DefaultHandler2 -- `DefaultHandler` mas las tres interfaces de
 * `ext`, todas con cuerpo vacio.
 *
 * <p>Es la misma idea que `DefaultHandler`: un manejador que solo quiere ver los comentarios no
 * deberia tener que escribir los otros veintitantos metodos. Extendiendo esta clase se escribe
 * `comment` y nada mas.
 *
 * <p>El unico metodo que **no** tiene el cuerpo vacio es el `resolveEntity` de dos argumentos, y
 * ahi esta lo interesante de la clase. `DefaultHandler` lo resolvia devolviendo `null` directo;
 * aca se redirige al de cuatro argumentos de {@link EntityResolver2}, pasandole `null` en nombre y
 * baseURI. La consecuencia practica es que una subclase que solo redefina la version de cuatro
 * argumentos tambien queda bien atendida cuando el parser es viejo y llama a la de dos --que es
 * exactamente lo que uno espera al redefinir "el" resolvedor--. Sin ese puente habria que redefinir
 * los dos y mantenerlos sincronizados.
 *
 * <p>Lo mismo al reves no se puede hacer: el de cuatro argumentos no puede delegar en el de dos sin
 * tirar a la basura el nombre y la base, que son justo los datos por los que existe.
 *
 * <p>Que `getExternalSubset` y `resolveEntity` devuelvan `null` no es un stub: es la respuesta que
 * el contrato define para "no sustituyo nada, abrilo por el identificador de sistema". Un manejador
 * por omision que inventara una DTD seria el que estaria mintiendo.
 *
 * <p>Se hereda tambien el `fatalError` de `DefaultHandler`, que relanza en vez de callarse, por la
 * razon que explica alla: un error fatal termina el analisis por definicion y tragarselo dejaria al
 * llamador creyendo que leyo el documento.
 */
public class DefaultHandler2 extends org.xml.sax.helpers.DefaultHandler
        implements LexicalHandler, DeclHandler, EntityResolver2 {

    public DefaultHandler2() {
    }

    ////////////////////////////////////////////////////////////////////
    // LexicalHandler
    ////////////////////////////////////////////////////////////////////

    public void startCDATA() throws SAXException {
    }

    public void endCDATA() throws SAXException {
    }

    public void startDTD(String name, String publicId, String systemId)
            throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    public void startEntity(String name) throws SAXException {
    }

    public void endEntity(String name) throws SAXException {
    }

    public void comment(char ch[], int start, int length) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // DeclHandler
    ////////////////////////////////////////////////////////////////////

    public void attributeDecl(String eName, String aName, String type,
                              String mode, String value) throws SAXException {
    }

    public void elementDecl(String name, String model) throws SAXException {
    }

    public void externalEntityDecl(String name, String publicId, String systemId)
            throws SAXException {
    }

    public void internalEntityDecl(String name, String value) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // EntityResolver2
    ////////////////////////////////////////////////////////////////////

    public InputSource getExternalSubset(String name, String baseURI)
            throws SAXException, IOException {
        return null;
    }

    public InputSource resolveEntity(String name, String publicId,
                                     String baseURI, String systemId)
            throws SAXException, IOException {
        return null;
    }

    /** El puente hacia la version de cuatro argumentos que explica el comentario de la clase. */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        return resolveEntity(null, publicId, null, systemId);
    }
}
