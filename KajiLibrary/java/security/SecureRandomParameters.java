package java.security;

// Marca los parametros de un `SecureRandom`.
//
// Vacia, y por el mismo motivo que `AlgorithmParameterSpec`: lo unico que necesita el API es poder
// pasar "los parametros de este generador" por un tipo comun. El unico juego concreto que trae el
// JDK son los de `DrbgParameters`.
public interface SecureRandomParameters {
}
