package java.awt;

/**
 * How much of what is behind shows through what is painted: opaque, masked or blended.
 *
 * <p>It is one of the few {@code java.awt} interfaces that mention no type of the window system:
 * three integers and one method. {@code Color}, {@code MultipleGradientPaint} and {@code
 * TexturePaint} implement it and cannot be declared without it.
 */
public interface Transparency {

    /** Everything painted covers the background completely. */
    int OPAQUE = 1;

    /**
     * Each pixel covers completely or not at all: there are no halfway points. It is what a GIF
     * does with its transparent colour, and that is why it is worth telling apart from TRANSLUCENT:
     * whoever composites can skip blending and just copy or not copy.
     */
    int BITMASK = 2;

    /** Each pixel has its own alpha and has to be blended with the background. */
    int TRANSLUCENT = 3;

    int getTransparency();
}
