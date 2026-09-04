package java.net;

// Un usuario y una contrasena, para devolverle a un `Authenticator`.
//
// La contrasena se guarda como `char[]` y no como `String`, y eso no es estilo: un `String` es
// inmutable y queda vivo hasta que el recolector lo levante, o sea que la contrasena se queda dando
// vueltas en memoria sin que nadie pueda borrarla. Un arreglo se puede sobreescribir.
//
// El constructor **copia** el arreglo --para que quien lo paso pueda limpiar el suyo-- pero
// `getPassword()` devuelve el interno sin copiar, para que el que lo consume pueda limpiarlo cuando
// termine. La asimetria es del JDK y es deliberada.
//
// Nada omitido: esto es un par de valores.
public final class PasswordAuthentication {

    private final String userName;
    private final char[] password;

    public PasswordAuthentication(String userName, char[] password) {
        this.userName = userName;
        this.password = (char[]) password.clone();
    }

    public String getUserName() {
        return this.userName;
    }

    /** El arreglo interno, no una copia: ver la cabecera. */
    public char[] getPassword() {
        return this.password;
    }
}
