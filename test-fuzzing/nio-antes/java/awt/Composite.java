package java.awt;

import java.awt.image.ColorModel;

/**
 * How what is drawn is blended with what was already there.
 *
 * <p>Without compositing, drawing is replacing. With compositing, drawing is an operation between
 * two images: the one being painted and the one already there. From that come transparency, alpha
 * clipping and all the blend modes.
 *
 * <p>The object describes the rule; the work is done by a {@link CompositeContext}, which is
 * requested once per drawing operation with the pixel formats already known.
 */
public interface Composite {

    /**
     * Builds the machine that will blend.
     *
     * @param srcColorModel the format of what is drawn
     * @param dstColorModel the format of what was already there
     * @param hints the quality hints
     */
    CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel,
            RenderingHints hints);
}
