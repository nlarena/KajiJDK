package java.security.spec;

// Marca los parametros **transparentes** de un algoritmo criptografico.
//
// No declara ni un metodo, y esa es toda la idea: la contraparte opaca es
// `java.security.AlgorithmParameters`, que guarda los parametros codificados y solo sabe
// devolverlos como bytes. Un `AlgorithmParameterSpec` es la version que el programa puede leer
// campo por campo — el modulo y el exponente de RSA, la curva de EC — y la interfaz existe para
// poder pasar cualquiera de esas por un parametro comun sin que el receptor sepa cual es.
public interface AlgorithmParameterSpec {
}
