package java.awt.color;

/**
 * Un perfil ICC de un espacio RGB con matriz y curvas.
 *
 * <p>Es la forma mas comun de perfil de monitor y la mas simple de aplicar: convertir un color a
 * XYZ es pasarlo por las tres curvas y multiplicarlo por la matriz de 3x3. Los perfiles que en vez
 * de eso traen tablas de interpolacion **no** son de esta clase --{@link ICC_Profile#getInstance}
 * devuelve un `ICC_Profile` a secas-- y por eso los tres metodos de aca pueden prometer una
 * respuesta.
 *
 * <p>La excepcion es {@link #getGamma} contra {@link #getTRC}: una curva se guarda **o** como un
 * numero de gamma **o** como una tabla, nunca como las dos, asi que uno de los dos metodos tira
 * segun como este ese perfil. No es un defecto: es el formato, y preguntarle a un perfil con tabla
 * cual es su gamma no tiene respuesta.
 */
public final class ICC_ProfileRGB extends ICC_Profile {

    private static final long serialVersionUID = 8505067385152579334L;

    /** El componente rojo. */
    public static final int REDCOMPONENT = 0;
    /** El componente verde. */
    public static final int GREENCOMPONENT = 1;
    /** El componente azul. */
    public static final int BLUECOMPONENT = 2;

    ICC_ProfileRGB(byte[] data) {
        super(data);
    }

    /** El punto blanco del medio. En un perfil de monitor sRGB es D65, no D50. */
    public float[] getMediaWhitePoint() {
        return super.getMediaWhitePoint();
    }

    /**
     * La matriz de 3x3 que lleva de RGB lineal a XYZ.
     *
     * <p>`m[fila][columna]`: la columna 0 es lo que aporta el rojo, la 1 el verde y la 2 el azul.
     * Sale de las tres etiquetas de colorante, que es como ICC la guarda -- una columna por
     * etiqueta.
     *
     * @throws ProfileDataException si al perfil le falta alguna de las tres
     */
    public float[][] getMatrix() {
        float[] r = this.getXYZTag(icSigRedColorantTag);
        float[] g = this.getXYZTag(icSigGreenColorantTag);
        float[] b = this.getXYZTag(icSigBlueColorantTag);
        float[][] m = new float[3][3];
        for (int fila = 0; fila < 3; fila++) {
            m[fila][0] = r[fila];
            m[fila][1] = g[fila];
            m[fila][2] = b[fila];
        }
        return m;
    }

    /**
     * La gamma de ese componente.
     *
     * @throws ProfileDataException si la curva es una tabla; ver la nota de la clase
     * @throws IllegalArgumentException si el componente no es uno de los tres
     */
    public float getGamma(int component) {
        return super.getGamma(tagDe(component));
    }

    /**
     * La tabla de ese componente.
     *
     * @throws ProfileDataException si la curva es una gamma; ver la nota de la clase
     * @throws IllegalArgumentException si el componente no es uno de los tres
     */
    public short[] getTRC(int component) {
        return super.getTRC(tagDe(component));
    }

    private static int tagDe(int component) {
        if (component == REDCOMPONENT) {
            return icSigRedTRCTag;
        }
        if (component == GREENCOMPONENT) {
            return icSigGreenTRCTag;
        }
        if (component == BLUECOMPONENT) {
            return icSigBlueTRCTag;
        }
        throw new IllegalArgumentException("Must be Red, Green, or Blue");
    }
}
