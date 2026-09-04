package javax.xml.crypto.dsig.spec;

import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec -- la hoja de estilo de una
 * transformacion XSLT.
 *
 * <p>La transformacion XSLT de XML-DSig aplica una hoja de estilo antes de firmar, y esta clase la
 * lleva.
 *
 * <p>Es la transformacion mas peligrosa del conjunto y vale decirlo: validar una firma que la usa
 * significa <b>ejecutar</b> una hoja de estilo que escribio quien firmo, con todo lo que XSLT puede
 * hacer --leer documentos, en algunas implementaciones invocar codigo--. La especificacion la define
 * y la practica es no aceptarla en firmas de origen desconocido.
 */
public final class XSLTTransformParameterSpec implements TransformParameterSpec {

    /** La hoja de estilo. */
    private final XMLStructure stylesheet;

    /**
     * @param stylesheet la hoja de estilo
     * @throws NullPointerException si es null
     */
    public XSLTTransformParameterSpec(XMLStructure stylesheet) {
        if (stylesheet == null) {
            throw new NullPointerException("stylesheet cannot be null");
        }
        this.stylesheet = stylesheet;
    }

    /** La hoja de estilo. */
    public XMLStructure getStylesheet() {
        return this.stylesheet;
    }
}
