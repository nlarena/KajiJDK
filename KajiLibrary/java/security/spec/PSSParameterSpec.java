package java.security.spec;

// Los parametros de RSASSA-PSS (PKCS#1 v2.1): que hash, que MGF, con que parametros, cuanta sal y
// que byte de cola.
//
// PSS existe porque el padding viejo de PKCS#1 v1.5 es deterministico y su seguridad nunca se pudo
// demostrar; PSS es probabilistico —de ahi la sal— y tiene una prueba de seguridad. El precio es que
// hay cinco parametros que **tienen que coincidir exactamente** entre quien firma y quien verifica,
// y ninguno viaja dentro de la firma. Un desacuerdo en cualquiera de ellos no da un error claro: da
// una firma valida que no verifica.
//
// El largo de la sal es el que mas se equivoca. El default de esta clase es 20 —el tamaño de SHA-1,
// por herencia— mientras que la practica actual es usar el tamaño del digest elegido. Con SHA-256 y
// sal de 20 la firma es legal y no verifica contra un verificador que espera 32.
public class PSSParameterSpec implements AlgorithmParameterSpec {

    // El unico valor de trailer que PKCS#1 define: el byte 0xBC al final del bloque codificado.
    public static final int TRAILER_FIELD_BC = 1;

    // Los valores historicos, todos SHA-1. Se mantiene como estaba porque es el default de
    // compatibilidad, no porque sea la eleccion recomendada.
    public static final PSSParameterSpec DEFAULT =
        new PSSParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, 20, TRAILER_FIELD_BC);

    private final String mdName;
    private final String mgfName;
    private final AlgorithmParameterSpec mgfSpec;
    private final int saltLen;
    private final int trailerField;

    public PSSParameterSpec(String mdName, String mgfName, AlgorithmParameterSpec mgfSpec,
                            int saltLen, int trailerField) {
        if (mdName == null) {
            throw new NullPointerException("digest algorithm is null");
        }
        if (mgfName == null) {
            throw new NullPointerException("mask generation function algorithm is null");
        }
        if (saltLen < 0) {
            throw new IllegalArgumentException("negative saltLen value: " + saltLen);
        }
        if (trailerField < 0) {
            throw new IllegalArgumentException("negative trailerField: " + trailerField);
        }
        this.mdName = mdName;
        this.mgfName = mgfName;
        this.mgfSpec = mgfSpec;
        this.saltLen = saltLen;
        this.trailerField = trailerField;
    }

    // Solo el largo de sal, con el resto en los valores historicos de SHA-1.
    public PSSParameterSpec(int saltLen) {
        if (saltLen < 0) {
            throw new IllegalArgumentException("negative saltLen value: " + saltLen);
        }
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.saltLen = saltLen;
        this.trailerField = TRAILER_FIELD_BC;
    }

    public String getDigestAlgorithm() {
        return this.mdName;
    }

    public String getMGFAlgorithm() {
        return this.mgfName;
    }

    // Los parametros del MGF, o null si no se dieron.
    public AlgorithmParameterSpec getMGFParameters() {
        return this.mgfSpec;
    }

    public int getSaltLength() {
        return this.saltLen;
    }

    public int getTrailerField() {
        return this.trailerField;
    }

    // El campo "maskGenAlgorithm" imprime la **spec** del MGF y no su nombre —dice
    // "MGF1ParameterSpec[hashAlgorithm=SHA-1]" y no "MGF1"— y llega a decir "null" si no hay spec.
    // Es raro y es lo que hace el JDK: se replica tal cual porque `toString` de estas clases termina
    // en logs que la gente compara entre implementaciones.
    @Override
    public String toString() {
        return "PSSParameterSpec[hashAlgorithm=" + this.mdName
            + ", maskGenAlgorithm=" + this.mgfSpec
            + ", saltLength=" + this.saltLen
            + ", trailerField=" + this.trailerField
            + "]";
    }
}
