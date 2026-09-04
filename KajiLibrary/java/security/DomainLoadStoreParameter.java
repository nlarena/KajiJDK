package java.security;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// Los parametros para cargar un **dominio** de almacenes: varios `KeyStore` que se manejan como si
// fueran uno.
//
// La idea es de Java 8 y resuelve un problema concreto: una aplicacion que necesita claves de
// varios lugares —un archivo PKCS#12, una tarjeta, un almacen del sistema— terminaba abriendo y
// coordinando cada uno a mano. Un dominio los describe en un archivo de configuracion, cada uno con
// su propia contraseña, y se abren todos juntos.
//
// La URI apunta a esa configuracion y puede llevar un fragmento con el nombre del dominio:
// `file:///etc/keystores.cfg#produccion`.
//
// `getProtectionParameter()` devuelve **null** siempre, y no es un olvido: la proteccion no es una
// sola, hay una por almacen, y estan en el mapa. Devolver la de alguno seria elegir arbitrariamente.
public final class DomainLoadStoreParameter implements KeyStore.LoadStoreParameter {

    private final URI configuration;
    private final Map<String, KeyStore.ProtectionParameter> protectionParams;

    // Las claves del mapa son nombres de almacen dentro del dominio; la clave vacia da la
    // proteccion por default para los que no aparezcan.
    public DomainLoadStoreParameter(URI configuration,
                                    Map<String, KeyStore.ProtectionParameter> protectionParams) {
        if (configuration == null || protectionParams == null) {
            throw new NullPointerException("invalid null input");
        }
        this.configuration = configuration;
        this.protectionParams = Collections.unmodifiableMap(
            new HashMap<String, KeyStore.ProtectionParameter>(protectionParams));
    }

    // La URI del archivo de configuracion, con el nombre del dominio en el fragmento si lo lleva.
    public URI getConfiguration() {
        return this.configuration;
    }

    // Copia inmutable del mapa de protecciones.
    public Map<String, KeyStore.ProtectionParameter> getProtectionParams() {
        return this.protectionParams;
    }

    // Siempre null: la proteccion es por almacen, no del dominio. Ver la nota de la clase.
    public KeyStore.ProtectionParameter getProtectionParameter() {
        return null;
    }
}
