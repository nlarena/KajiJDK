package javax.xml.crypto.dsig;

import javax.xml.crypto.XMLCryptoContext;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignContext -- el contexto de una operacion de <b>firma</b>.
 *
 * <p>No agrega ningun metodo a {@link XMLCryptoContext}. Es un tipo marcador, y esa es toda su
 * funcion: {@code sign} recibe uno de estos y {@code validate} recibe un
 * {@link XMLValidateContext}, asi que confundirlos no compila.
 *
 * <p>Parece exagerado hasta que se piensa que hay adentro: el {@code KeySelector}. Un contexto de
 * firma lleva la clave <b>privada</b> y uno de validacion la publica. Pasar el de firma a una
 * validacion --o al reves-- seria un error silencioso con consecuencias serias, y el tipo lo hace
 * imposible.
 */
public interface XMLSignContext extends XMLCryptoContext {
}
