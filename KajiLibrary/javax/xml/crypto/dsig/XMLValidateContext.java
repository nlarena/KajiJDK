package javax.xml.crypto.dsig;

import javax.xml.crypto.XMLCryptoContext;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLValidateContext -- el contexto de una <b>validacion</b>.
 *
 * <p>El espejo de {@link XMLSignContext}, y tampoco agrega metodos. Ver alla por que la separacion en
 * dos tipos vale la pena.
 *
 * <p>Es donde va la defensa contra referencias externas: el {@code URIDereferencer} que se le ponga
 * es el que decide si validar esta firma va a salir a la red. Ver
 * {@code javax.xml.crypto.URIDereferencer}.
 */
public interface XMLValidateContext extends XMLCryptoContext {
}
