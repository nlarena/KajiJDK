package javax.naming.ldap;

import java.io.IOException;

import javax.naming.NamingException;

/**
 * Lo que el servidor contesta a un {@link SortControl}: si pudo ordenar, y si no, por que.
 *
 * <h2>Por que hay que mirarlo</h2>
 *
 * <p>Porque un {@link SortControl} <strong>no critico</strong> que el servidor no puede honrar se
 * ignora en silencio, y los resultados llegan sin ordenar. Sin consultar esta respuesta no hay forma
 * de distinguir "ordenado" de "no pudo".
 *
 * <p>{@link #getAttributeID} dice <em>cual</em> atributo dio problema, que es lo unico que permite
 * arreglarlo: el caso tipico es pedir orden por un atributo que el servidor no tiene indexado, o que
 * no existe en su esquema.
 */
public final class SortResponseControl extends BasicControl {

    private static final long serialVersionUID = 5142939176006310877L;

    /** El OID de este control. */
    public static final String OID = "1.2.840.113556.1.4.474";

    private final int resultCode;
    private final String badAttrId;

    /**
     * Lo construye el proveedor a partir de lo que llego.
     *
     * @throws IOException si el valor no se pudo decodificar
     */
    public SortResponseControl(String id, boolean criticality, byte[] value) throws IOException {
        super(id == null ? OID : id, criticality, value);
        byte[] datos = value == null ? new byte[0] : value;
        if (datos.length < 5) {
            this.resultCode = 0;
            this.badAttrId = null;
            return;
        }
        // SEQUENCE { sortResult ENUMERATED, attributeType [0] OPTIONAL }
        int i = 0;
        if ((datos[i] & 0xFF) != 0x30) {
            throw new IOException("el control de orden no tiene la forma esperada");
        }
        i += 2;
        if ((datos[i] & 0xFF) != 0x0A) {
            throw new IOException("falta el resultado del ordenamiento");
        }
        i++;
        int n = datos[i++] & 0xFF;
        int codigo = 0;
        for (int k = 0; k < n; k++) {
            codigo = (codigo << 8) | (datos[i++] & 0xFF);
        }
        this.resultCode = codigo;
        if (i < datos.length && (datos[i] & 0xFF) == 0x80) {
            i++;
            int largo = datos[i++] & 0xFF;
            this.badAttrId = new String(datos, i, largo,
                    java.nio.charset.StandardCharsets.UTF_8);
        } else {
            this.badAttrId = null;
        }
    }

    /** Si el servidor ordeno; {@code false} significa que los resultados vienen sin ordenar. */
    public boolean isSorted() {
        return this.resultCode == 0;
    }

    /** El codigo LDAP del resultado; {@code 0} es exito. */
    public int getResultCode() {
        return this.resultCode;
    }

    /** El atributo que dio problema, o {@code null} si el servidor no lo dijo. */
    public String getAttributeID() {
        return this.badAttrId;
    }

    /**
     * El error como excepcion, o {@code null} si salio bien.
     *
     * <p>Devolverlo en vez de tirarlo es deliberado: el ordenamiento pudo fallar y aun asi los
     * resultados sirven. Quien llama decide si eso lo invalida todo.
     */
    public NamingException getException() {
        if (this.resultCode == 0) {
            return null;
        }
        NamingException e = new NamingException(
                "no se pudo ordenar; codigo LDAP " + String.valueOf(this.resultCode)
                + (this.badAttrId == null ? "" : ", atributo " + this.badAttrId));
        return e;
    }
}
