package java.awt;

/**
 * What a chain of drawing buffers can do: whether pages can be flipped, and what is left in the
 * back buffer after flipping.
 *
 * <p>{@code isPageFlipping()} keeps no boolean of its own: it is {@code getFlipContents() != null}.
 * The reason is that the two cannot contradict each other. With a separate field, someone could
 * build a capability that says it flips pages but not what is left afterwards, and that means
 * nothing.
 */
public class BufferCapabilities implements Cloneable {

    private ImageCapabilities frontCaps;

    private ImageCapabilities backCaps;

    private FlipContents flipContents;

    /**
     * The two {@code ImageCapabilities} are required; the {@code FlipContents} is not, and its
     * absence is the way of saying "this chain does not flip pages, it copies".
     */
    public BufferCapabilities(ImageCapabilities frontCaps, ImageCapabilities backCaps,
            FlipContents flipContents) {
        if (frontCaps == null || backCaps == null) {
            throw new IllegalArgumentException("Image capabilities specified cannot be null");
        }
        this.frontCaps = frontCaps;
        this.backCaps = backCaps;
        this.flipContents = flipContents;
    }

    public ImageCapabilities getFrontBufferCapabilities() {
        return frontCaps;
    }

    public ImageCapabilities getBackBufferCapabilities() {
        return backCaps;
    }

    public boolean isPageFlipping() {
        return getFlipContents() != null;
    }

    public FlipContents getFlipContents() {
        return flipContents;
    }

    /**
     * False in the base class. What really knows whether full screen is needed is the concrete
     * implementation of the device, and this class only describes.
     */
    public boolean isFullScreenRequired() {
        return false;
    }

    public boolean isMultiBufferAvailable() {
        return false;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * What is left in the back buffer after flipping.
     *
     * <p>It is an enumeration that predates {@code enum} --that is why it inherits from {@code
     * AttributeValue}-- and the difference matters: {@code PRIOR} and {@code COPIED} are the two
     * useful ones and they are opposites. With PRIOR the back buffer is left with what was being
     * shown (useful for animating over the previous frame); with COPIED it is left with what was
     * just shown.
     */
    public static final class FlipContents extends AttributeValue {

        private static int I_UNDEFINED = 0;

        private static int I_BACKGROUND = 1;

        private static int I_PRIOR = 2;

        private static int I_COPIED = 3;

        private static final String[] NAMES = {"undefined", "background", "prior", "copied"};

        /** What is left is not known: everything has to be redrawn. */
        public static final FlipContents UNDEFINED = new FlipContents(I_UNDEFINED);

        /** It is left painted with the background colour. */
        public static final FlipContents BACKGROUND = new FlipContents(I_BACKGROUND);

        public static final FlipContents PRIOR = new FlipContents(I_PRIOR);

        public static final FlipContents COPIED = new FlipContents(I_COPIED);

        private FlipContents(int type) {
            super(type, NAMES);
        }

        public String toString() {
            return super.toString();
        }

        public int hashCode() {
            return super.hashCode();
        }
    }
}
