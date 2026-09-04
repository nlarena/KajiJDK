import java.security.DEREncodable;
import java.security.PEMRecord;

// Prueba de comportamiento de java.security.PEMRecord. Corre igual en la VM real y en la nuestra:
// run() devuelve -1 si todo pasa, o el indice de la primera comprobacion que fallo.
//
// En el JDK real PEMRecord es API de vista previa, asi que alla hay que compilar y correr con
// --enable-preview --release 25. Aca no: la clase es normal.
//
// Lo esperado de toString() se ARMA con System.lineSeparator() en vez de escribirse a mano. No es
// comodidad: el JDK separa con el fin de linea de la plataforma, asi que el texto correcto es
// distinto en Windows y en Linux, y una constante con "\n" adentro haria fallar la prueba en la
// mitad de las maquinas por algo que esta bien.
public class PemTest {

    static int marca;

    static final String SEP = System.lineSeparator();

    static String repetir(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append((char) ('A' + (i % 26)));
        }
        return b.toString();
    }

    // El bloque que se espera para un contenido ya partido en lineas.
    static String bloque(String tipo, String[] lineas) {
        StringBuilder b = new StringBuilder();
        b.append("-----BEGIN ").append(tipo).append("-----").append(SEP);
        for (int i = 0; i < lineas.length; i++) {
            b.append(lineas[i]).append(SEP);
        }
        return b.append("-----END ").append(tipo).append("-----").append(SEP).toString();
    }

    public static int run() {
        marca = 0;

        byte[] delante = new byte[] { 1, 2 };
        PEMRecord r = new PEMRecord("CERTIFICATE", "QUJD", delante);

        if (!"CERTIFICATE".equals(r.type())) return marca;
        marca++;                                                    // 1

        if (!"QUJD".equals(r.content())) return marca;
        marca++;                                                    // 2

        if (r.leadingData() == null || r.leadingData().length != 2) return marca;
        marca++;                                                    // 3

        // No se copia: el registro guarda el arreglo que le dieron. Es la semantica del JDK y la
        // prueba la fija para que nadie la "arregle" sin darse cuenta.
        delante[0] = 9;
        if (r.leadingData()[0] != 9) return marca;
        marca++;                                                    // 4

        // El constructor de dos argumentos deja leadingData en null, que no es un arreglo vacio.
        PEMRecord dos = new PEMRecord("CERTIFICATE", "QUJD");
        if (dos.leadingData() != null) return marca;
        marca++;                                                    // 5

        // Es un DEREncodable: es el tipo por el que la API de PEM lo toma.
        if (!(dos instanceof DEREncodable)) return marca;
        marca++;                                                    // 6

        // ---- toString: el bloque armado ----

        if (!bloque("CERTIFICATE", new String[] { "QUJD" }).equals(dos.toString())) return marca;
        marca++;                                                    // 7

        // Un contenido vacio igual da una linea vacia en el medio.
        if (!bloque("X", new String[] { "" }).equals(new PEMRecord("X", "").toString())) return marca;
        marca++;                                                    // 8

        // Justo por debajo del corte: una sola linea.
        String c63 = repetir(63);
        if (!bloque("X", new String[] { c63 }).equals(new PEMRecord("X", c63).toString())) return marca;
        marca++;                                                    // 9

        // Justo en el corte: sigue siendo una sola linea, no dos con una vacia al final.
        String c64 = repetir(64);
        if (!bloque("X", new String[] { c64 }).equals(new PEMRecord("X", c64).toString())) return marca;
        marca++;                                                    // 10

        // Uno mas: dos lineas, la segunda de un caracter.
        String c65 = repetir(65);
        if (!bloque("X", new String[] { c65.substring(0, 64), c65.substring(64) })
                .equals(new PEMRecord("X", c65).toString())) return marca;
        marca++;                                                    // 11

        String c128 = repetir(128);
        if (!bloque("X", new String[] { c128.substring(0, 64), c128.substring(64) })
                .equals(new PEMRecord("X", c128).toString())) return marca;
        marca++;                                                    // 12

        // leadingData no sale en el bloque: es lo que habia ANTES del bloque.
        if (!dos.toString().equals(r.toString())) return marca;
        marca++;                                                    // 13

        // ---- validacion del tipo ----

        if (!npe(null, "QQ==", "\"type\" cannot be null.")) return marca;
        marca++;                                                    // 14

        if (!npe("X", null, "\"content\" cannot be null.")) return marca;
        marca++;                                                    // 15

        // El tipo es la etiqueta sola: nada de sintaxis de PEM ya armada.
        if (!rechaza("-X")) return marca;
        marca++;                                                    // 16

        if (!rechaza("BEGIN X")) return marca;
        marca++;                                                    // 17

        if (!rechaza("END X")) return marca;
        marca++;                                                    // 18

        // Pero no valida mas que eso: minusculas y etiquetas inventadas pasan.
        if (!"cert".equals(new PEMRecord("cert", "Q").type())) return marca;
        marca++;                                                    // 19

        // "BEGIN" sin el espacio no es el prefijo prohibido.
        if (!"BEGINX".equals(new PEMRecord("BEGINX", "Q").type())) return marca;
        marca++;                                                    // 20

        // ---- igualdad de record ----

        // Con leadingData null en los dos, los componentes alcanzan.
        if (!new PEMRecord("X", "Q").equals(new PEMRecord("X", "Q"))) return marca;
        marca++;                                                    // 21

        if (new PEMRecord("X", "Q").equals(new PEMRecord("X", "R"))) return marca;
        marca++;                                                    // 22

        // Con arreglos, la comparacion es por referencia: dos arreglos iguales pero distintos dan
        // registros distintos. Sorprende y es lo que hace el JDK.
        if (new PEMRecord("X", "Q", new byte[] { 1 })
                .equals(new PEMRecord("X", "Q", new byte[] { 1 }))) return marca;
        marca++;                                                    // 23

        byte[] mismo = new byte[] { 1 };
        if (!new PEMRecord("X", "Q", mismo).equals(new PEMRecord("X", "Q", mismo))) return marca;
        marca++;                                                    // 24

        return -1;
    }

    static boolean npe(String tipo, String contenido, String mensaje) {
        try {
            new PEMRecord(tipo, contenido);
            return false;
        } catch (NullPointerException e) {
            return mensaje.equals(e.getMessage());
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean rechaza(String tipo) {
        try {
            new PEMRecord(tipo, "Q");
            return false;
        } catch (IllegalArgumentException e) {
            return "PEM syntax labels found.  Only the PEM type identifier is allowed"
                    .equals(e.getMessage());
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
