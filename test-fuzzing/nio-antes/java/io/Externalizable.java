package java.io;

import java.io.Serializable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

// KajiLibrary's java.io.Externalizable — the opt-out from automatic serialization: a class
// that implements it takes complete responsibility for its own byte format, instead of
// letting the engine walk its fields. The tradeoff is control against safety — the format
// can be far more compact, but nothing is written or restored that the class does not write
// and restore itself, and deserialization calls the public no-arg constructor first.
public interface Externalizable extends Serializable {

    void writeExternal(ObjectOutput out) throws IOException;

    void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;
}
