package java.security.spec;

// The name of a standard curve, to ask for it when generating an EC key pair.
//
// Since JDK 11 it is nothing more than a `NamedParameterSpec`: it stayed as a subclass for
// compatibility —there is code that does `instanceof ECGenParameterSpec` to tell "they asked me for
// EC by name" from "they asked me for something else by name"— and because the distinct type still
// documents the intent.
public class ECGenParameterSpec extends NamedParameterSpec {

    public ECGenParameterSpec(String stdName) {
        super(stdName);
    }
}
