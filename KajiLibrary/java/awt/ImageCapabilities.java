package java.awt;

/**
 * Que puede hacer una imagen: si vive en memoria de video y si el sistema puede tirarla cuando se
 * le antoje.
 *
 * <p>{@code isTrueVolatile()} devuelve false y no es una simplificacion: en el JDK la clase base
 * tambien devuelve false siempre, y la respuesta verdadera la da
 * {@code VolatileImage.getCapabilities()}, que sobreescribe. Aca no hay imagenes, pero la clase se
 * escribe entera porque es la que describe la capacidad, no la que la tiene.
 */
public class ImageCapabilities implements Cloneable {

    private boolean accelerated = false;

    public ImageCapabilities(boolean accelerated) {
        this.accelerated = accelerated;
    }

    public boolean isAccelerated() {
        return accelerated;
    }

    public boolean isTrueVolatile() {
        return false;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
