package java.security.spec;

// The finite field an elliptic curve lives over.
//
// The interface has a single method and even so it is the one that decides everything: the two
// fields that implement it —`ECFieldFp` (prime) and `ECFieldF2m` (binary)— share nothing but the
// size, because the arithmetic of one looks nothing like the other's. What this type allows is for
// `EllipticCurve` to name either of the two without knowing which one it is.
public interface ECField {

    // The number of bits needed to write an element of the field.
    int getFieldSize();
}
