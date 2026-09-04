package java.security;

// La mitad secreta de un par asimetrico.
//
// A KajiLibrary subset: en el JDK esta interfaz tambien extiende
// `javax.security.auth.Destroyable`, que aporta `destroy()` e `isDestroyed()`. Ese paquete no
// existe en esta biblioteca, asi que el supertipo no se declara. La omision se nota: sin
// `destroy()` no hay forma estandar de pedirle a una implementacion que borre el material secreto
// de memoria antes de soltarlo. El dia que exista `javax.security.auth`, se agrega y nada de lo
// que hay aca cambia.
public interface PrivateKey extends AsymmetricKey {

    long serialVersionUID = 6034044314589513430L;
}
