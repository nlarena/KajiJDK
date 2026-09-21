package java.awt;

import java.io.Serializable;

/**
 * What is asked of a graphics configuration, so that the system picks the one that meets it best.
 *
 * <p>Instead of enumerating the configurations and comparing by hand, one declares what is needed
 * —double buffering yes, stereo no— and the device returns the most suitable one. Each requirement
 * can be required, preferred or unnecessary, and that gradation is what allows ranking candidates
 * instead of just accepting or rejecting them.
 */
public abstract class GraphicsConfigTemplate implements Serializable {

    private static final long serialVersionUID = -8061369279557787079L;

    /** The requirement has to be met. */
    public static final int REQUIRED = 1;

    /** Better if it is met. */
    public static final int PREFERRED = 2;

    /** Better if it is not met. */
    public static final int UNNECESSARY = 3;

    /** For subclasses. */
    public GraphicsConfigTemplate() {
    }

    /**
     * The best of those configurations, or `null` if none will do.
     *
     * @throws NullPointerException if the array is `null`
     */
    public abstract GraphicsConfiguration getBestConfiguration(GraphicsConfiguration[] gc);

    /**
     * Whether that configuration meets the required requirements.
     *
     * @throws NullPointerException if the configuration is `null`
     */
    public abstract boolean isGraphicsConfigSupported(GraphicsConfiguration gc);
}
