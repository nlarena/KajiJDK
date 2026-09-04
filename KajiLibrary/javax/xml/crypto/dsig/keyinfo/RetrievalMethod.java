package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.Data;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Transform;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.RetrievalMethod -- donde ir a buscar la clave.
 *
 * <p>En vez de traer la clave, trae un URI de donde sacarla, con transformaciones opcionales para
 * extraerla de lo que se encuentre ahi.
 *
 * <p>Es el elemento de {@link KeyInfo} que mas cuidado pide, porque {@link #dereference} <b>va a
 * buscar</b> algo que eligio quien firmo. Sobre una firma de origen desconocido eso es un pedido de
 * red o una lectura de archivo que el programa no pidio -- el mismo problema que
 * {@code URIDereferencer} existe para controlar.
 *
 * <p>Su uso legitimo es adentro de un mismo documento: una firma que apunta al {@code KeyInfo} de
 * otra para no repetir el certificado.
 */
public interface RetrievalMethod extends URIReference, XMLStructure {

    /** Las transformaciones a aplicar a lo que se encuentre. No modificable. */
    List<Transform> getTransforms();

    /** Adonde apunta. */
    String getURI();

    /**
     * Va a buscarlo. Ver la nota de la clase.
     *
     * @throws URIReferenceException si no se puede resolver
     */
    Data dereference(XMLCryptoContext context) throws URIReferenceException;
}
