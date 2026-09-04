package javax.xml.crypto;

import java.io.InputStream;

/**
 * KajiLibrary's javax.xml.crypto.OctetStreamData -- un flujo de bytes, como dato a firmar.
 *
 * <p>La otra mitad de {@link Data}. Lo que se firma de verdad son bytes: toda cadena de
 * transformaciones termina convirtiendo nodos en octetos --con una canonicalizacion-- porque un
 * resumen criptografico no sabe de arboles.
 *
 * <p>Lleva ademas el URI de donde salio y su tipo de contenido, los dos opcionales. Sirven para
 * decidir como interpretarlo cuando el flujo no es XML: una firma puede cubrir una imagen o un
 * archivo binario, y ahi el tipo es lo unico que dice que es.
 *
 * <p>Es un flujo y no un arreglo, asi que <b>se consume</b>: leerlo dos veces no funciona. Es lo
 * correcto para algo que puede ser enorme, y hay que tenerlo presente al depurar una firma que no
 * valida.
 */
public class OctetStreamData implements Data {

    /** El flujo. */
    private final InputStream octetStream;

    /** De donde salio, o null. */
    private final String uri;

    /** Su tipo de contenido, o null. */
    private final String mimeType;

    /** Solo el flujo. */
    public OctetStreamData(InputStream octetStream) {
        this(octetStream, null, null);
    }

    /**
     * Con el origen y el tipo.
     *
     * @param uri de donde salio, o null
     * @param mimeType su tipo de contenido, o null
     * @throws NullPointerException si el flujo es null
     */
    public OctetStreamData(InputStream octetStream, String uri, String mimeType) {
        if (octetStream == null) {
            throw new NullPointerException("octetStream is null");
        }
        this.octetStream = octetStream;
        this.uri = uri;
        this.mimeType = mimeType;
    }

    /** El flujo. Se consume; ver la nota de la clase. */
    public InputStream getOctetStream() {
        return this.octetStream;
    }

    /** De donde salio, o null. */
    public String getURI() {
        return this.uri;
    }

    /** Su tipo de contenido, o null. */
    public String getMimeType() {
        return this.mimeType;
    }
}
