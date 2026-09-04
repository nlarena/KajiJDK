package javax.net.ssl;

/**
 * Quien provee las credenciales propias durante un handshake: el certificado que se presenta y la
 * clave privada que lo respalda.
 *
 * <p>No declara ningun metodo, y eso no es un descuido. Las credenciales dependen del tipo de
 * autenticacion —X.509, Kerberos, PSK— y cada uno necesita preguntas distintas, asi que la interfaz
 * comun no puede tener ninguna. Lo que hace es <strong>marcar</strong>: es el tipo que
 * {@link SSLContext#init} acepta, y quien lo implementa de verdad lo hace a traves de una subinterfaz
 * como {@link X509KeyManager}.
 */
public interface KeyManager {
}
