package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.util.Set;

// Los parametros de una **construccion** de camino PKIX: los mismos de la validacion, mas el largo
// maximo.
//
// El largo maximo no es una optimizacion, es un corte de seguridad. Construir un camino es una
// busqueda, y sin techo esa busqueda puede no terminar: un conjunto de certificados cruzados
// —muy comun entre CAs que se firman mutuamente— produce ciclos, y basta con unos pocos para que
// el espacio de caminos posibles explote. El default de 5 sale de que en la practica ninguna cadena
// real pasa de tres o cuatro eslabones.
//
// El -1 significa "sin limite" y hay que leerlo asi y no como "cero": es el unico valor negativo
// aceptado, y por eso el setter rechaza -2 en vez de tratarlo como otro "sin limite".
//
public class PKIXBuilderParameters extends PKIXParameters {

    private int maxPathLength = 5;

    // `targetConstraints` puede ser null, pero conviene no dejarlo: sin un criterio para el
    // certificado del final, el constructor no sabe hacia donde buscar.
    public PKIXBuilderParameters(Set<TrustAnchor> trustAnchors, CertSelector targetConstraints)
            throws InvalidAlgorithmParameterException {
        super(trustAnchors);
        super.setTargetCertConstraints(targetConstraints);
    }

    // Idem, con las anclas sacadas de un almacen. Ver `PKIXParameters(KeyStore)` para que entradas
    // se miran y cuales no.
    public PKIXBuilderParameters(java.security.KeyStore keystore, CertSelector targetConstraints)
            throws java.security.KeyStoreException, InvalidAlgorithmParameterException {
        super(keystore);
        super.setTargetCertConstraints(targetConstraints);
    }

    // El largo maximo de la cadena, sin contar el ancla. -1 quita el limite; 0 fuerza a que el
    // ancla haya firmado directamente el certificado buscado.
    public void setMaxPathLength(int maxPathLength) {
        if (maxPathLength < -1) {
            throw new InvalidParameterException("the maximum path "
                + "length parameter can not be less than -1");
        }
        this.maxPathLength = maxPathLength;
    }

    public int getMaxPathLength() {
        return this.maxPathLength;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        sb.append(super.toString());
        sb.append("  Maximum Path Length: " + this.maxPathLength + "\n");
        sb.append("]\n");
        return sb.toString();
    }
}
