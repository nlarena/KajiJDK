package java.security;

// La mitad publicable de un par asimetrico.
//
// No agrega ni un metodo sobre `AsymmetricKey`, y no le hace falta: lo unico que aporta es **el
// tipo**. Que `Signature.initVerify` pida una `PublicKey` y `Signature.initSign` una `PrivateKey`
// es lo que hace que confundirlas sea un error de compilacion y no una vulnerabilidad.
public interface PublicKey extends AsymmetricKey {

    long serialVersionUID = 7187392471159151072L;
}
