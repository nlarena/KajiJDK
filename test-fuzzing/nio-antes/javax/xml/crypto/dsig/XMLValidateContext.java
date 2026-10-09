package javax.xml.crypto.dsig;

import javax.xml.crypto.XMLCryptoContext;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLValidateContext -- the context of a <b>validation</b>.
 *
 * <p>The mirror of {@link XMLSignContext}, and it adds no methods either. See there for why the
 * split into two types is worthwhile.
 *
 * <p>It is where the defence against external references goes: the {@code URIDereferencer} put on
 * it is the one that decides whether validating this signature is going to go out to the network.
 * See {@code javax.xml.crypto.URIDereferencer}.
 */
public interface XMLValidateContext extends XMLCryptoContext {
}
