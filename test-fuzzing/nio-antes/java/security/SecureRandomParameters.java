package java.security;

// It marks the parameters of a `SecureRandom`.
//
// Empty, and for the same reason as `AlgorithmParameterSpec`: the only thing the API needs is to be
// able to pass "the parameters of this generator" through a common type. The only concrete set the
// JDK brings is that of `DrbgParameters`.
public interface SecureRandomParameters {
}
