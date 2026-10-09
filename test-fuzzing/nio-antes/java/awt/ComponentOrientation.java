package java.awt;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Which way things read: left to right or the other way.
 *
 * <p>Three constants over three bits (this note said two). The interesting one is {@code UNKNOWN}:
 * it is not a third direction, it is "not known", and that is why it answers the same as {@code
 * LEFT_TO_RIGHT} to both questions. The difference only shows when comparing by identity, which is
 * exactly what someone deciding whether to ask the user instead of guessing does.
 *
 * <p>The list of right-to-left languages is written by hand and has five codes: Arabic, Hebrew,
 * Farsi and Urdu, plus the old Hebrew code. That last one is not redundant: ISO changed "iw" to
 * "he" in 1989 and {@code Locale} keeps both, so a locale built with the old code has to give the
 * same result.
 */
public final class ComponentOrientation implements java.io.Serializable {

    private static final long serialVersionUID = -4113291392143563828L;

    private static final int UNK_BIT = 1;

    private static final int HORIZ_BIT = 2;

    private static final int LTR_BIT = 4;

    public static final ComponentOrientation LEFT_TO_RIGHT =
            new ComponentOrientation(HORIZ_BIT | LTR_BIT);

    public static final ComponentOrientation RIGHT_TO_LEFT =
            new ComponentOrientation(HORIZ_BIT);

    /** Not known. It answers like LEFT_TO_RIGHT; telling it apart needs an identity comparison. */
    public static final ComponentOrientation UNKNOWN =
            new ComponentOrientation(HORIZ_BIT | LTR_BIT | UNK_BIT);

    private int orientation;

    private ComponentOrientation(int value) {
        orientation = value;
    }

    public boolean isHorizontal() {
        return (orientation & HORIZ_BIT) != 0;
    }

    public boolean isLeftToRight() {
        return (orientation & LTR_BIT) != 0;
    }

    public static ComponentOrientation getOrientation(Locale locale) {
        String lang = locale.getLanguage();
        if ("iw".equals(lang) || "he".equals(lang) || "ar".equals(lang)
                || "fa".equals(lang) || "ur".equals(lang)) {
            return RIGHT_TO_LEFT;
        } else {
            return LEFT_TO_RIGHT;
        }
    }

    /**
     * The orientation the resource bundle declares, or its locale's if it declares none.
     *
     * <p>Swallowing the exception is on purpose and it is in the JDK: if the "Orientation" key is
     * missing, or is there but is not a ComponentOrientation, the right answer is to fall back to
     * the locale, not to propagate. The resource bundle was written by a translator and need not be
     * correct.
     */
    public static ComponentOrientation getOrientation(ResourceBundle bdl) {
        ComponentOrientation result = null;

        try {
            result = (ComponentOrientation) bdl.getObject("Orientation");
        } catch (Exception e) {
            result = null;
        }

        if (result == null) {
            result = getOrientation(bdl.getLocale());
        }
        if (result == null) {
            result = getOrientation(Locale.getDefault());
        }
        return result;
    }
}
