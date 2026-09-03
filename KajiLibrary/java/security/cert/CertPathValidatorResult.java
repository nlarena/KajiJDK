package java.security.cert;

// El resultado de validar un camino, cuando la validacion salio bien.
//
// Que exista un resultado y no un boolean es lo importante: una validacion exitosa produce datos
// que hacen falta despues —el ancla en la que termino confiando, la clave publica del sujeto— y
// tirarlos obligaria a recalcularlos. Una validacion **fallida** no devuelve esto: lanza
// `CertPathValidatorException`.
public interface CertPathValidatorResult extends Cloneable {

    Object clone();
}
