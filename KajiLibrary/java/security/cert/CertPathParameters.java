package java.security.cert;

// The parameters of an algorithm of validation or of path building.
//
// It is a marker interface with `clone()`: each algorithm defines its own parameters
// —`PKIXParameters` is the only one the JDK brings— and the only thing in common is that they can
// be copied. The copy is not tidiness: these objects are mutable and the validator keeps them, so
// without it changing the parameters after starting would change the rules halfway through the
// validation.
public interface CertPathParameters extends Cloneable {

    Object clone();
}
