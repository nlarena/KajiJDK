import java.security.KeyStore;
import java.security.PKCS12Attribute;

// Prueba de comportamiento de java.security.PKCS12Attribute. Corre igual en la VM real y en la
// nuestra: run() devuelve -1 si todo pasa, o el indice de la primera comprobacion que fallo.
//
// Las tiras hexadecimales que se comparan salieron de correr el JDK 25 real, no de nuestra
// implementacion: son la especificacion aca, no una transcripcion de lo que nos dio.
public class P12Test {

    static int marca;

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            s.append(Character.forDigit((b[i] >> 4) & 0xf, 16));
            s.append(Character.forDigit(b[i] & 0xf, 16));
        }
        return s.toString();
    }

    // Codifica, compara los bytes contra los del JDK, relee esos bytes y comprueba que el nombre y
    // el valor vuelvan enteros. Devuelve true si todo el viaje de ida y vuelta cierra.
    static boolean viaje(String name, String value, String esperado, String valorDeVuelta) {
        PKCS12Attribute a = new PKCS12Attribute(name, value);
        if (!esperado.equals(hex(a.getEncoded()))) {
            return false;
        }
        if (!name.equals(a.getName()) || !value.equals(a.getValue())) {
            return false;
        }
        PKCS12Attribute b = new PKCS12Attribute(a.getEncoded());
        if (!name.equals(b.getName()) || !valorDeVuelta.equals(b.getValue())) {
            return false;
        }
        return a.equals(b) && a.hashCode() == b.hashCode();
    }

    public static int run() {
        marca = 0;

        // Un OID largo con un texto: el caso comun, emailAddress.
        if (!viaje("1.2.840.113549.1.9.1", "test@example.com",
                "301f06092a864886f70d01090131120c1074657374406578616d706c652e636f6d",
                "test@example.com")) return marca;
        marca++;                                                    // 1

        // Pares hexadecimales: va como OCTET STRING, no como texto.
        if (!viaje("1.3.6.1.4.1.42.2.11", "01:02:03",
                "301106082b060104012a020b31050403010203", "01:02:03")) return marca;
        marca++;                                                    // 2

        // Varios valores entre corchetes: el SET lleva dos UTF8String.
        if (!viaje("2.5.4.3", "[uno, dos]",
                "30110603550403310a0c03756e6f0c03646f73", "[uno, dos]")) return marca;
        marca++;                                                    // 3

        // Un OID corto.
        if (!viaje("1.2.3", "hola", "300c06022a0331060c04686f6c61", "hola")) return marca;
        marca++;                                                    // 4

        // Primer arco 0: el segundo no puede pasar de 39, y 39 entra justo.
        if (!viaje("0.39.2", "x", "30090602270231030c0178", "x")) return marca;
        marca++;                                                    // 5

        // Primer arco 2: ahi el segundo no tiene tope y el numero combinado ocupa dos bytes.
        if (!viaje("2.999.1", "y", "300a060388370131030c0179", "y")) return marca;
        marca++;                                                    // 6

        // Un solo par hexadecimal NO es un valor hexadecimal: hacen falta dos. "01" es el texto
        // "01" y va como UTF8String (0c 02 30 31).
        PKCS12Attribute unPar = new PKCS12Attribute("1.2.3", "01");
        if (!"300a06022a0331040c023031".equals(hex(unPar.getEncoded()))) return marca;
        marca++;                                                    // 7

        // La rareza heredada: los pares hexadecimales pasan por BigInteger y el cero de la
        // izquierda se pierde. "00:01" son los bytes 01, y al releerlo vuelve como "01".
        PKCS12Attribute cero = new PKCS12Attribute("1.2.3", "00:01");
        if (!"300906022a0331030401 01".replace(" ", "").equals(hex(cero.getEncoded()))) return marca;
        marca++;                                                    // 8

        if (!"01".equals(new PKCS12Attribute(cero.getEncoded()).getValue())) return marca;
        marca++;                                                    // 9

        if (!"1.2.3=00:01".equals(cero.toString())) return marca;
        marca++;                                                    // 10

        // getEncoded() devuelve una copia: pisarla no toca al atributo.
        PKCS12Attribute copia = new PKCS12Attribute("1.2.3", "hola");
        byte[] afuera = copia.getEncoded();
        afuera[0] = 0x7f;
        if (!"300c06022a0331060c04686f6c61".equals(hex(copia.getEncoded()))) return marca;
        marca++;                                                    // 11

        // Y el constructor de bytes tambien copia: mutar el arreglo de entrada no lo cambia.
        byte[] adentro = copia.getEncoded();
        PKCS12Attribute desdeBytes = new PKCS12Attribute(adentro);
        adentro[0] = 0x7f;
        if (!"300c06022a0331060c04686f6c61".equals(hex(desdeBytes.getEncoded()))) return marca;
        marca++;                                                    // 12

        // Un nombre que no es un OID.
        if (!rechaza("mal nombre", "x")) return marca;
        marca++;                                                    // 13

        // Un solo arco no alcanza.
        if (!rechaza("1", "x")) return marca;
        marca++;                                                    // 14

        // Primer arco fuera de 0..2.
        if (!rechaza("3.1", "x")) return marca;
        marca++;                                                    // 15

        // Con primer arco 0 o 1, el segundo no puede llegar a 40.
        if (!rechaza("0.40", "x")) return marca;
        marca++;                                                    // 16

        if (!rechaza("1.40", "x")) return marca;
        marca++;                                                    // 17

        // Nulls: NullPointerException, no IllegalArgumentException.
        boolean npe = false;
        try {
            new PKCS12Attribute(null, "x");
        } catch (NullPointerException e) {
            npe = true;
        } catch (Throwable t) {
            npe = false;
        }
        if (!npe) return marca;
        marca++;                                                    // 18

        npe = false;
        try {
            new PKCS12Attribute("1.2.3", (String) null);
        } catch (NullPointerException e) {
            npe = true;
        } catch (Throwable t) {
            npe = false;
        }
        if (!npe) return marca;
        marca++;                                                    // 19

        npe = false;
        try {
            new PKCS12Attribute((byte[]) null);
        } catch (NullPointerException e) {
            npe = true;
        } catch (Throwable t) {
            npe = false;
        }
        if (!npe) return marca;
        marca++;                                                    // 20

        // Bytes que no son un atributo.
        boolean iae = false;
        try {
            new PKCS12Attribute(new byte[] { 0x30, 0x02, (byte) 0xff, (byte) 0xff });
        } catch (IllegalArgumentException e) {
            iae = true;
        } catch (Throwable t) {
            iae = false;
        }
        if (!iae) return marca;
        marca++;                                                    // 21

        iae = false;
        try {
            new PKCS12Attribute(new byte[0]);
        } catch (IllegalArgumentException e) {
            iae = true;
        } catch (Throwable t) {
            iae = false;
        }
        if (!iae) return marca;
        marca++;                                                    // 22

        // Los bytes que sobran DESPUES del SEQUENCE se aceptan, y quedan dentro de `encoded`. Esta
        // comprobacion se escribio al reves y la corrigio el JDK real: la expectativa era la
        // equivocada, no el JDK.
        byte[] bueno = new PKCS12Attribute("1.2.3", "hola").getEncoded();
        byte[] mas = new byte[bueno.length + 1];
        System.arraycopy(bueno, 0, mas, 0, bueno.length);
        mas[bueno.length] = (byte) 0xab;
        PKCS12Attribute conCola = new PKCS12Attribute(mas);
        if (!"1.2.3".equals(conCola.getName()) || !"hola".equals(conCola.getValue())) return marca;
        marca++;                                                    // 23

        if (!"300c06022a0331060c04686f6c61ab".equals(hex(conCola.getEncoded()))) return marca;
        marca++;                                                    // 24

        // Adentro del SEQUENCE, en cambio, no puede haber un tercer elemento.
        iae = false;
        try {
            new PKCS12Attribute(new byte[] { 0x30, 0x08, 0x06, 0x02, 0x2a, 0x03, 0x31, 0x00,
                    0x05, 0x00 });
        } catch (IllegalArgumentException e) {
            iae = true;
        } catch (Throwable t) {
            iae = false;
        }
        if (!iae) return marca;
        marca++;                                                    // 25

        // Y un SET vacio si es valido: el valor queda en "[]".
        PKCS12Attribute vacio = new PKCS12Attribute(
                new byte[] { 0x30, 0x06, 0x06, 0x02, 0x2a, 0x03, 0x31, 0x00 });
        if (!"1.2.3".equals(vacio.getName()) || !"[]".equals(vacio.getValue())) return marca;
        marca++;                                                    // 26

        // Es un KeyStore.Entry.Attribute: es el tipo por el que un almacen lo devuelve.
        KeyStore.Entry.Attribute comoAtributo = new PKCS12Attribute("1.2.3", "hola");
        if (!"1.2.3".equals(comoAtributo.getName())) return marca;
        marca++;                                                    // 27

        if (!"hola".equals(comoAtributo.getValue())) return marca;
        marca++;                                                    // 28

        // La identidad son los bytes: mismo nombre y valor, mismo atributo; distinto valor, no.
        if (!new PKCS12Attribute("1.2.3", "hola").equals(new PKCS12Attribute("1.2.3", "hola")))
            return marca;
        marca++;                                                    // 29

        if (new PKCS12Attribute("1.2.3", "hola").equals(new PKCS12Attribute("1.2.3", "chau")))
            return marca;
        marca++;                                                    // 30

        if (new PKCS12Attribute("1.2.3", "hola").equals("1.2.3=hola")) return marca;
        marca++;                                                    // 31

        return -1;
    }

    static boolean rechaza(String name, String value) {
        try {
            new PKCS12Attribute(name, value);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
