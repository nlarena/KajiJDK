package javax.xml.crypto.dsig;

import javax.xml.crypto.XMLCryptoContext;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignContext -- the context of a <b>signing</b> operation.
 *
 * <p>It adds no method to {@link XMLCryptoContext}. It is a marker type, and that is its whole
 * function: {@code sign} receives one of these and {@code validate} receives an {@link
 * XMLValidateContext}, so confusing them does not compile.
 *
 * <p>It seems excessive until one thinks of what is inside: the {@code KeySelector}. A signing
 * context carries the <b>private</b> key and a validation one the public key. Passing the signing
 * one to a validation --or the other way round-- would be a silent error with serious consequences,
 * and the type makes it impossible.
 */
public interface XMLSignContext extends XMLCryptoContext {
}
