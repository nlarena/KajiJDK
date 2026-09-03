package java.awt.color;

/**
 * Un perfil ICC de escala de grises: un punto blanco y una sola curva.
 *
 * <p>Un componente en vez de tres, y por eso {@link #getGamma} y {@link #getTRC} no toman
 * argumento. Vale la misma nota que en {@link ICC_ProfileRGB}: uno de los dos tira segun como este
 * guardada la curva en ese perfil.
 */
public final class ICC_ProfileGray extends ICC_Profile {

    private static final long serialVersionUID = -1124721290732002649L;

    ICC_ProfileGray(byte[] data) {
        super(data);
    }

    /** El punto blanco del medio. */
    public float[] getMediaWhitePoint() {
        return super.getMediaWhitePoint();
    }

    /**
     * La gamma de la curva.
     *
     * @throws ProfileDataException si la curva es una tabla
     */
    public float getGamma() {
        return super.getGamma(icSigGrayTRCTag);
    }

    /**
     * La tabla de la curva.
     *
     * @throws ProfileDataException si la curva es una gamma
     */
    public short[] getTRC() {
        return super.getTRC(icSigGrayTRCTag);
    }
}
