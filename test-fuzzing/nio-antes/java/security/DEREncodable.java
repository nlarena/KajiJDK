package java.security;

// It marks what can be written as DER, and therefore what `PEMEncoder` knows how to encode.
//
// It is an empty interface introduced with the PEM support (JDK 25). It does not declare
// `getEncoded()` on purpose: the types that implement it have it already with signatures
// incompatible with each other —`Key` returns it without an exception, `Certificate` throws— and
// unifying them would have broken both. The interface only says "this has a DER form"; whoever
// encodes knows how to get it out of each type.
public interface DEREncodable {
}
