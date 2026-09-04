package javax.naming.ldap;

import java.io.IOException;

/**
 * Pide que el servidor devuelva los resultados ordenados.
 *
 * <h2>Por que ordenar del lado del servidor</h2>
 *
 * <p>Porque combinado con {@link PagedResultsControl} es la unica forma de que la paginacion
 * signifique algo: sin un orden estable, la "pagina 2" no es un concepto — dos busquedas pueden
 * devolver las mismas entradas en otro orden.
 *
 * <p>Y porque el servidor conoce las reglas de comparacion de cada atributo; ordenar en el cliente
 * usaria las de Java, que no son las mismas. Ver {@link SortKey}.
 */
public final class SortControl extends BasicControl {

    private static final long serialVersionUID = -1965961680233330744L;

    /** El OID de este control. */
    public static final String OID = "1.2.840.113556.1.4.473";

    /** Ascendente por ese atributo. */
    public SortControl(String sortBy, boolean criticality) throws IOException {
        this(new SortKey[] { new SortKey(sortBy) }, criticality);
    }

    /** Ascendente por esos atributos, en ese orden de prioridad. */
    public SortControl(String[] sortBy, boolean criticality) throws IOException {
        this(claves(sortBy), criticality);
    }

    /** Con control completo sobre sentido y regla de comparacion. */
    public SortControl(SortKey[] sortBy, boolean criticality) throws IOException {
        super(OID, criticality, codificar(sortBy));
    }

    private static SortKey[] claves(String[] nombres) {
        SortKey[] out = new SortKey[nombres.length];
        for (int i = 0; i < nombres.length; i++) {
            out[i] = new SortKey(nombres[i]);
        }
        return out;
    }

    /**
     * {@code SEQUENCE OF SEQUENCE { attributeType, [0] orderingRule OPTIONAL,
     * [1] reverseOrder DEFAULT FALSE }}, del RFC 2891.
     *
     * <p>Las etiquetas de contexto {@code 0x80} y {@code 0x81} no son adorno: como los dos campos
     * opcionales podrian confundirse por su tipo, el RFC los distingue por posicion.
     */
    private static byte[] codificar(SortKey[] claves) {
        java.io.ByteArrayOutputStream cuerpo = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < claves.length; i++) {
            SortKey k = claves[i];
            java.io.ByteArrayOutputStream una = new java.io.ByteArrayOutputStream();
            escribir(una, 0x04, texto(k.getAttributeID()));
            if (k.getMatchingRuleID() != null) {
                escribir(una, 0x80, texto(k.getMatchingRuleID()));
            }
            if (!k.isAscending()) {
                escribir(una, 0x81, new byte[] { (byte) 0xFF });
            }
            escribir(cuerpo, 0x30, una.toByteArray());
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        escribir(out, 0x30, cuerpo.toByteArray());
        return out.toByteArray();
    }

    private static byte[] texto(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void escribir(java.io.ByteArrayOutputStream out, int etiqueta, byte[] datos) {
        out.write(etiqueta);
        if (datos.length < 0x80) {
            out.write(datos.length);
        } else {
            // Forma larga: cuantos bytes ocupa el largo, y despues el largo.
            int n = datos.length;
            int bytes = n < 0x100 ? 1 : (n < 0x10000 ? 2 : 3);
            out.write(0x80 | bytes);
            for (int i = bytes - 1; i >= 0; i--) {
                out.write((n >> (8 * i)) & 0xFF);
            }
        }
        out.write(datos, 0, datos.length);
    }
}
