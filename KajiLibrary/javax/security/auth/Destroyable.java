package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.Destroyable -- algo que guarda un secreto y lo puede borrar.
 *
 * <p>Los dos metodos tienen implementacion por defecto, y las dos elegidas dicen "yo no se hacer
 * eso": {@code destroy()} lanza y {@code isDestroyed()} devuelve false. Eso es a proposito y es el
 * default seguro. Un {@code destroy()} que no hiciera nada y un {@code isDestroyed()} que devolviera
 * true dejarian a quien los llama creyendo que el secreto ya no esta en memoria cuando si esta, que
 * es exactamente el error que esta interfaz existe para evitar.
 *
 * <p>Borrar un secreto en Java tiene un limite que conviene tener presente igual: si el secreto es
 * un {@code String}, no hay forma de borrarlo -- es inmutable y vive hasta que el recolector lo
 * levante. Por eso las contraseñas se pasan como {@code char[]} y no como {@code String}.
 */
public interface Destroyable {

    /**
     * Borra el secreto que guarda este objeto.
     *
     * @throws DestroyFailedException si no se puede -- ver {@link DestroyFailedException}
     */
    default void destroy() throws DestroyFailedException {
        throw new DestroyFailedException();
    }

    /** Si el secreto ya se borro. */
    default boolean isDestroyed() {
        return false;
    }
}
