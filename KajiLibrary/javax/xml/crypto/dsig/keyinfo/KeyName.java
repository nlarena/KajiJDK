package javax.xml.crypto.dsig.keyinfo;

import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyName -- la clave, nombrada.
 *
 * <p>Una cadena, y nada mas. Es la forma <b>correcta</b> de usar un {@link KeyInfo}: no trae la clave
 * sino un nombre que quien valida busca en su propio almacen.
 *
 * <p>Eso invierte la relacion de confianza y por eso funciona: la clave la elige quien valida entre
 * las que ya tiene, y el documento solo dice cual. Un nombre que no esta en el almacen hace fallar la
 * validacion, que es exactamente lo que se quiere.
 *
 * <p>El formato del nombre no lo define el estandar: puede ser un identificador, un correo, un
 * nombre distinguido. Las dos partes tienen que acordarlo por afuera.
 */
public interface KeyName extends XMLStructure {

    /** El nombre de la clave. */
    String getName();
}
