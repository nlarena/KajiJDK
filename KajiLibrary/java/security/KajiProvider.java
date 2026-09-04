package java.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// El unico proveedor que KajiLibrary registra de fabrica.
//
// Ofrece **digests y un generador aleatorio**, y solo lo que se puede cumplir de verdad: los
// digests estan escritos de cero en esta biblioteca y verificados contra el JDK y contra los
// vectores de las especificaciones, y el generador es un pase directo al del sistema operativo. No
// hay `Signature`, ni `Cipher`, ni `KeyPairGenerator`: registrar un servicio que no se puede
// cumplir seria peor que no tenerlo, porque `getInstance` devolveria un objeto que despues no hace
// lo que promete.
//
// El generador se llama `OS-PRNG` y no `SHA1PRNG` ni `DRBG` a proposito. Esos dos nombres designan
// construcciones concretas, y devolver otra cosa bajo ese nombre seria mentir sobre que algoritmo
// esta corriendo. `new SecureRandom()` --que es como lo pide casi todo el mundo-- no nombra
// ninguno y por lo tanto funciona.
//
// Se registra **solo si el sistema puede dar entropia**. Un `SecureRandom` que existe y no puede
// entregar bytes es peor que su ausencia: el llamador se entera en el peor momento.
//
// Package-private a proposito: no es API del JDK, y hacerlo publico agregaria a `java.security` un
// nombre que el JDK no tiene.
final class KajiProvider extends Provider {

    KajiProvider() {
        super("Kaji", "1.0", "Kaji digest provider (MD5, SHA-1, SHA-2 family)");

        this.registrar("MD5", "java.security.DigestMD5", new String[] {"1.2.840.113549.2.5"});
        this.registrar("SHA-1", "java.security.DigestSHA1", new String[] {"SHA", "SHA1"});
        this.registrar("SHA-224", "java.security.DigestSHA2", new String[] {"SHA224"});
        this.registrar("SHA-256", "java.security.DigestSHA2", new String[] {"SHA256"});
        this.registrar("SHA-384", "java.security.DigestSHA5", new String[] {"SHA384"});
        this.registrar("SHA-512", "java.security.DigestSHA5", new String[] {"SHA512"});

        if (OsEntropy.available()) {
            this.putService(new OsPrngService(this));
        }
    }

    // El servicio del generador. Construye directo por lo mismo que `ServicioDigest`: la clase es
    // detalle de implementacion de `java.security` y no se alcanza por reflexion.
    private static final class OsPrngService extends Provider.Service {

        OsPrngService(Provider p) {
            super(p, "SecureRandom", "OS-PRNG", "java.security.OsPrngSpi",
                new ArrayList<String>(), new HashMap<String, String>());
        }

        @Override
        public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
            if (constructorParameter != null) {
                // Los parametros solo los entiende un DRBG, y este no lo es. Se rechaza en vez de
                // ignorarlos: quien los pasa esta pidiendo una configuracion que no se va a aplicar.
                throw new NoSuchAlgorithmException(
                    "OS-PRNG does not accept SecureRandomParameters");
            }
            return new OsPrngSpi();
        }
    }

    private void registrar(String algoritmo, String clase, String[] alias) {
        List<String> lista = new ArrayList<String>();
        int i = 0;
        while (i < alias.length) {
            lista.add(alias[i]);
            i = i + 1;
        }
        this.putService(new ServicioDigest(this, algoritmo, clase, lista));
    }

    // Instancia los digests **sin reflexion**.
    //
    // La implementacion base de `Provider.Service` hace `Class.forName(className).newInstance()`, y
    // eso funciona para un proveedor externo cuyas clases son publicas. Las de aca no lo son —son
    // detalle de implementacion de `java.security`— asi que este servicio construye directo. El
    // `getClassName()` sigue diciendo la verdad: es el nombre real de la clase que se va a
    // instanciar, aunque desde afuera no se pueda alcanzar por reflexion.
    private static final class ServicioDigest extends Provider.Service {

        private final String algoritmo;

        ServicioDigest(Provider p, String algoritmo, String clase, List<String> alias) {
            super(p, "MessageDigest", algoritmo, clase, alias, new HashMap<String, String>());
            this.algoritmo = algoritmo;
        }

        @Override
        public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
            if (constructorParameter != null) {
                throw new InvalidParameterException(
                    "constructorParameter not used with MessageDigest engines");
            }
            if (this.algoritmo.equals("MD5")) {
                return new DigestMD5();
            }
            if (this.algoritmo.equals("SHA-1")) {
                return new DigestSHA1();
            }
            if (this.algoritmo.equals("SHA-224")) {
                return DigestSHA2.sha224();
            }
            if (this.algoritmo.equals("SHA-256")) {
                return DigestSHA2.sha256();
            }
            if (this.algoritmo.equals("SHA-384")) {
                return DigestSHA5.sha384();
            }
            if (this.algoritmo.equals("SHA-512")) {
                return DigestSHA5.sha512();
            }
            throw new NoSuchAlgorithmException(this.algoritmo);
        }
    }
}
