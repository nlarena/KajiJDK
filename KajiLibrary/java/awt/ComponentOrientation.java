package java.awt;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * En que sentido se leen las cosas: de izquierda a derecha o al reves.
 *
 * <p>Son tres constantes y dos bits. Lo interesante es {@code UNKNOWN}: no es un tercer sentido,
 * es "no se sabe", y por eso contesta lo mismo que {@code LEFT_TO_RIGHT} a las dos preguntas. La
 * diferencia se ve solo comparando por identidad, que es justo lo que hace quien quiera decidir si
 * preguntarle al usuario en vez de adivinar.
 *
 * <p>La lista de idiomas que leen de derecha a izquierda esta escrita a mano y son cinco: arabe,
 * hebreo, farsi y urdu, mas el codigo viejo del hebreo. Ese ultimo no es redundante: ISO cambio
 * "iw" por "he" en 1989 y {@code Locale} conserva los dos, asi que un locale armado con el codigo
 * viejo tiene que dar el mismo resultado.
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

    /** No se sabe. Contesta igual que LEFT_TO_RIGHT; para distinguirlo hay que comparar identidad. */
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
     * El sentido que declare el paquete de recursos, o el de su locale si no declara ninguno.
     *
     * <p>El {@code catch} sin cuerpo es a proposito y esta en el JDK: si la clave "Orientation" no
     * esta, o esta pero no es un ComponentOrientation, la respuesta correcta es caer al locale, no
     * propagar. El paquete de recursos lo escribio un traductor y no tiene por que ser correcto.
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
