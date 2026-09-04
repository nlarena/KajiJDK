package javax.imageio.metadata;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadataController -- pide al usuario que complete los
 * metadatos.
 *
 * <p>El equivalente de {@code javax.imageio.IIOParamController} para metadatos, con el mismo contrato:
 * modifica el objeto que recibe, y si devuelve false lo tiene que dejar <b>como estaba</b>.
 *
 * <p>Sirve para pedirle al usuario el titulo, el autor o la descripcion de una imagen antes de
 * guardarla, sin que el codigo que escribe la imagen sepa nada de interfaces.
 */
public interface IIOMetadataController {

    /**
     * Completa esos metadatos.
     *
     * @return si el usuario acepto; false los deja intactos
     */
    boolean activate(IIOMetadata metadata);
}
