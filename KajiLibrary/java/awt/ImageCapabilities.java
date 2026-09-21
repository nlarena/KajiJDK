package java.awt;

/**
 * What an image can do: whether it lives in video memory and whether the system can throw it away
 * when it pleases.
 *
 * <p>{@code isTrueVolatile()} returns false and it is not a simplification: in the JDK the base
 * class also always returns false, and the true answer comes from the capabilities a volatile image
 * hands out, which override it. The class describes the capability, it does not hold one. (This
 * note said there are no images here; {@code BufferedImage} and {@code VolatileImage} exist.)
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
