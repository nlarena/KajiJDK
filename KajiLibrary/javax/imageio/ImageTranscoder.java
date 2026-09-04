package javax.imageio;

import javax.imageio.metadata.IIOMetadata;

/**
 * KajiLibrary's javax.imageio.ImageTranscoder -- traduce metadatos de un formato a otro.
 *
 * <p>Convertir el <b>pixel</b> de PNG a JPEG es facil: se decodifica y se codifica. Lo que se pierde
 * en el camino son los metadatos, porque cada formato los guarda a su manera. Esta interfaz es como se
 * conservan.
 *
 * <p>Los dos metodos toman los metadatos del formato de origen y devuelven los equivalentes en el de
 * destino. La traduccion pasa por el <b>formato estandar</b> de {@code javax.imageio.metadata}: el de
 * origen se expresa en el arbol comun, y de ahi el de destino toma lo que entiende.
 *
 * <p>Lo que el destino no sepa expresar se pierde, y no hay forma de que no sea asi. Devolver null es
 * valido y significa "de esto no puedo traducir nada".
 *
 * <p>Los dos metodos existen porque hay metadatos del <b>flujo</b> --que valen para todas las imagenes
 * de un archivo con varias-- y metadatos de <b>cada imagen</b>.
 */
public interface ImageTranscoder {

    /**
     * Traduce los metadatos del flujo.
     *
     * @param inData los del formato de origen
     * @param param los parametros de escritura, o null
     * @return los del formato de destino, o null si no se puede traducir nada
     */
    IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param);

    /**
     * Traduce los metadatos de una imagen.
     *
     * @param imageType de que tipo va a ser la imagen escrita
     * @return los del formato de destino, o null
     */
    IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType,
                                     ImageWriteParam param);
}
