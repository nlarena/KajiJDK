package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// El certificado de la API vieja de identidades, **obsoleto desde 1.2**.
//
// No confundir con `java.security.cert.Certificate`, que es el que se usa: son dos tipos distintos
// con el mismo nombre simple y no tienen nada que ver entre si. Este es una interfaz que
// acompañaba a `Identity`, con la nocion de "garante" (quien atestigua) y "principal" (sobre
// quien) explicita; aquel es una clase abstracta que modela un certificado real y delega el
// formato en sus subclases.
//
// Se implementa porque es el tipo que aparece en las firmas de `Identity.addCertificate` y
// `certificates()`, y no se puede tener una sin la otra. Todos sus metodos son abstractos, asi que
// no hay nada que pueda mentir aca: quien la implemente decide todo.
@Deprecated
public interface Certificate {

    // Quien garantiza la union entre el principal y su clave.
    Principal getGuarantor();

    // De quien es la clave que este certificado certifica.
    Principal getPrincipal();

    PublicKey getPublicKey();

    void encode(OutputStream stream) throws KeyException, IOException;

    void decode(InputStream stream) throws KeyException, IOException;

    String getFormat();

    // La forma legible; `detailed` pide la version larga.
    String toString(boolean detailed);
}
