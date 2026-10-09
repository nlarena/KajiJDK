package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// The certificate of the old identity API, **obsolete since 1.2**.
//
// Not to be confused with `java.security.cert.Certificate`, which is the one that is used: they are
// two different types with the same simple name and have nothing to do with each other. This one is
// an interface that accompanied `Identity`, with the notion of "guarantor" (who attests) and
// "principal" (about whom) explicit; that one is an abstract class that models a real certificate
// and leaves the format to its subclasses.
//
// It is implemented because it is the type that appears in the signatures of
// `Identity.addCertificate` and `certificates()`, and one cannot be had without the other. All of
// its methods are abstract, so there is nothing that can lie here: whoever implements it decides
// everything.
@Deprecated
public interface Certificate {

    // Who guarantees the union between the principal and their key.
    Principal getGuarantor();

    // Whose the key this certificate certifies is.
    Principal getPrincipal();

    PublicKey getPublicKey();

    void encode(OutputStream stream) throws KeyException, IOException;

    void decode(InputStream stream) throws KeyException, IOException;

    String getFormat();

    // The readable form; `detailed` asks for the long version.
    String toString(boolean detailed);
}
