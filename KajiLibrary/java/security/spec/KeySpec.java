package java.security.spec;

// Marks the **transparent** representation of a key.
//
// The distinction from `java.security.Key` is what orders all of `KeyFactory`: a `Key` is opaque
// —the provider decides what is inside and may keep it in hardware— while a `KeySpec` is material
// the program can look at and build. Converting from one to the other is exactly what `KeyFactory`
// does, and that is why this type has to exist even though it declares nothing. (This note said
// neither type declares anything; `Key` declares three methods.)
public interface KeySpec {
}
