package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;

// Una clave publica de curva de Montgomery: la coordenada u, sin v.
public interface XECPublicKey extends XECKey, PublicKey {

    BigInteger getU();

    // Aca el choque entre `XECKey` y `AsymmetricKey` es entre dos metodos de **la misma firma**, no
    // covariantes: hay que declararlo igual, porque uno es abstracto y el otro default.
    @Override
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
