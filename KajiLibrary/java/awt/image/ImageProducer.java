package java.awt.image;

/**
 * La fuente de los píxeles de una imagen.
 *
 * <p>Es el otro extremo de {@link ImageConsumer}. Un productor puede tener varios consumidores a la
 * vez, y cada uno recibe la imagen entera: registrarse no es repartirse el trabajo sino sumarse a la
 * entrega.
 */
public interface ImageProducer {

    /**
     * Suma un consumidor y le empieza a entregar.
     *
     * <p>Registrar dos veces al mismo consumidor no está definido y conviene evitarlo.
     */
    void addConsumer(ImageConsumer ic);

    /** Si ese consumidor está registrado. */
    boolean isConsumer(ImageConsumer ic);

    /** Saca a ese consumidor; si no estaba, no pasa nada. */
    void removeConsumer(ImageConsumer ic);

    /** Lo registra si hace falta y arranca la entrega. */
    void startProduction(ImageConsumer ic);

    /**
     * Pide que los píxeles se vuelvan a mandar de arriba abajo.
     *
     * <p>Es para el consumidor que necesita ese orden y no lo consiguió la primera vez. El productor
     * puede ignorarlo.
     */
    void requestTopDownLeftRightResend(ImageConsumer ic);
}
