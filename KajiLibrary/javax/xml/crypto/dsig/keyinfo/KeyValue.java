package javax.xml.crypto.dsig.keyinfo;

import java.security.KeyException;
import java.security.PublicKey;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyValue -- la clave publica, en limpio.
 *
 * <p>Los numeros de la clave publica escritos en el documento: el modulo y el exponente de una RSA,
 * los parametros de una DSA, el punto de una EC.
 *
 * <p>Es lo <b>opuesto</b> a {@link KeyName} en cuanto a confianza. Validar una firma con la clave que
 * la propia firma trae no prueba nada: quien la falsifique pone la suya. Usarlo asi es el error mas
 * comun de XML-DSig, y es facil de cometer porque hace que todo "funcione".
 *
 * <p>Sirve para dos cosas legitimas: comparar la clave del documento contra una que uno conoce, y
 * transportar una clave por un canal donde la confianza ya se establecio de otra forma.
 *
 * <p>Los tres URI de tipo nombran las tres familias que el estandar define.
 */
public interface KeyValue extends XMLStructure {

    /** Una clave DSA. */
    static final String DSA_TYPE = "http://www.w3.org/2000/09/xmldsig#DSAKeyValue";

    /** Una clave RSA. */
    static final String RSA_TYPE = "http://www.w3.org/2000/09/xmldsig#RSAKeyValue";

    /** Una clave de curva eliptica. */
    static final String EC_TYPE = "http://www.w3.org/2009/xmldsig11#ECKeyValue";

    /**
     * La clave publica.
     *
     * @throws KeyException si los numeros del documento no forman una clave, o si su algoritmo no
     *     esta soportado
     */
    PublicKey getPublicKey() throws KeyException;
}
