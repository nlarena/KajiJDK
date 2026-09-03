package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.MessageProp -- que proteccion se pide, y que se obtuvo.
 *
 * <p>El mismo objeto viaja en las dos direcciones, y ahi esta lo que hay que entender: al
 * <b>enviar</b>, quien llama lo llena para pedir --calidad de proteccion y si quiere cifrado--; al
 * <b>recibir</b>, la implementacion lo llena para contar que paso de verdad.
 *
 * <p>Reusar un objeto para las dos cosas ahorra una clase y tiene una consecuencia practica: un
 * {@code MessageProp} que se pasa a {@code unwrap} vuelve <b>modificado</b>, y reusarlo despues para
 * enviar arrastra lo que quedo adentro.
 *
 * <h2>Los cuatro estados suplementarios</h2>
 *
 * <p>{@link #isDuplicateToken}, {@link #isOldToken}, {@link #isUnseqToken} y {@link #isGapToken} son
 * avisos sobre el <b>orden</b> de los mensajes, no sobre su contenido. Y son avisos y no errores a
 * proposito: el mensaje se descifro bien y es autentico, lo que pasa es que llego dos veces, o
 * tarde, o antes que otro. Que hacer con eso depende de la aplicacion --sobre UDP un desorden es
 * normal, sobre una sesion es un ataque de repeticion-- y por eso la biblioteca informa en vez de
 * decidir.
 *
 * <p>Quien no los mira se pierde la deteccion de repeticion entera, que es el error clasico de este
 * API.
 */
public class MessageProp {

    private int qop;

    private boolean privacyState;

    private boolean duplicate = false;

    private boolean old = false;

    private boolean unseq = false;

    private boolean gap = false;

    private int minorStatus = 0;

    private String minorString = null;

    /**
     * Con la calidad por omision.
     *
     * @param privState si se pide cifrado ademas de integridad
     */
    public MessageProp(boolean privState) {
        this(0, privState);
    }

    /**
     * @param qop la calidad de proteccion; 0 es la por omision del mecanismo
     * @param privState si se pide cifrado ademas de integridad
     */
    public MessageProp(int qop, boolean privState) {
        this.qop = qop;
        this.privacyState = privState;
    }

    /** La calidad de proteccion. */
    public int getQOP() {
        return this.qop;
    }

    /** Si hay cifrado y no solo integridad. */
    public boolean getPrivacy() {
        return this.privacyState;
    }

    /** Ver {@link #getQOP}. */
    public void setQOP(int qop) {
        this.qop = qop;
    }

    /** Ver {@link #getPrivacy}. */
    public void setPrivacy(boolean privState) {
        this.privacyState = privState;
    }

    /** El token ya se habia recibido. Ver la nota de la clase. */
    public boolean isDuplicateToken() {
        return this.duplicate;
    }

    /** El token es demasiado viejo para saber si es duplicado. */
    public boolean isOldToken() {
        return this.old;
    }

    /** Llego despues de uno posterior. */
    public boolean isUnseqToken() {
        return this.unseq;
    }

    /** Falto al menos un token anterior. */
    public boolean isGapToken() {
        return this.gap;
    }

    /** El codigo del mecanismo, o 0. */
    public int getMinorStatus() {
        return this.minorStatus;
    }

    /** Lo que dijo el mecanismo, o null. */
    public String getMinorString() {
        return this.minorString;
    }

    /**
     * Los cuatro estados de una vez.
     *
     * <p>Es la implementacion la que lo llama, no quien usa el API.
     */
    public void setSupplementaryStates(boolean duplicate, boolean old, boolean unseq, boolean gap,
                                       int minorStatus, String minorString) {
        this.duplicate = duplicate;
        this.old = old;
        this.unseq = unseq;
        this.gap = gap;
        this.minorStatus = minorStatus;
        this.minorString = minorString;
    }
}
