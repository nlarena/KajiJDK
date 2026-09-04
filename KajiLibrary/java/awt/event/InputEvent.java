package java.awt.event;

import java.awt.Component;

/**
 * La raíz de los eventos de entrada: teclado y ratón.
 *
 * <p>Lo que agrega es **cuándo** pasó y **qué modificadores** estaban apretados, que es lo que los
 * dos tienen en común y nadie más necesita.
 *
 * <p>Los modificadores están dos veces y conviene entender por qué. Las máscaras viejas
 * ({@code SHIFT_MASK} y compañía) mezclaban en un solo número el estado de las teclas y el del ratón
 * de una forma que hacía imposible distinguir "el botón 1 está apretado" de "Alt está apretado" en
 * algunas combinaciones. Las nuevas ({@code SHIFT_DOWN_MASK}) usan bits que no chocan y dicen
 * exactamente qué estaba apretado en el momento del evento. Las viejas se conservan porque están en
 * la API desde 1.0.
 *
 * <p>{@link #consume} se redefine acá para hacerse pública: en un evento de entrada, consumir es
 * algo que la aplicación hace a propósito para que el sistema no le dé el tratamiento por omisión a
 * una tecla o a un clic.
 */
public abstract class InputEvent extends ComponentEvent {

    private static final long serialVersionUID = -2482525981698309786L;

    /** Alt estaba apretada. */
    public static final int ALT_DOWN_MASK = 512;

    /** AltGr estaba apretada. */
    public static final int ALT_GRAPH_DOWN_MASK = 8192;

    /** AltGr, en la codificación vieja. */
    public static final int ALT_GRAPH_MASK = 32;

    /** Alt, en la codificación vieja. */
    public static final int ALT_MASK = 8;

    /** El botón 1 estaba apretado. */
    public static final int BUTTON1_DOWN_MASK = 1024;

    /** Botón 1, en la codificación vieja. */
    public static final int BUTTON1_MASK = 16;

    /** El botón 2 estaba apretado. */
    public static final int BUTTON2_DOWN_MASK = 2048;

    /** Botón 2, en la codificación vieja. */
    public static final int BUTTON2_MASK = 8;

    /** El botón 3 estaba apretado. */
    public static final int BUTTON3_DOWN_MASK = 4096;

    /** Botón 3, en la codificación vieja. */
    public static final int BUTTON3_MASK = 4;

    /** Control estaba apretada. */
    public static final int CTRL_DOWN_MASK = 128;

    /** Control, en la codificación vieja. */
    public static final int CTRL_MASK = 2;

    /** Meta estaba apretada. */
    public static final int META_DOWN_MASK = 256;

    /** Meta, en la codificación vieja. */
    public static final int META_MASK = 4;

    /** Mayúsculas estaba apretada. */
    public static final int SHIFT_DOWN_MASK = 64;

    /** Mayúsculas, en la codificación vieja. */
    public static final int SHIFT_MASK = 1;

    /** Cuándo pasó, en milisegundos desde la época. */
    long when;

    /** Qué estaba apretado. */
    int modifiers;

    /** Con el componente, el identificador, el momento y los modificadores. */
    InputEvent(Component source, int id, long when, int modifiers) {
        super(source, id);
        this.when = when;
        this.modifiers = modifiers;
    }

    /**
     * La máscara del botón número `button`.
     *
     * @throws IllegalArgumentException si el número no es positivo
     */
    public static int getMaskForButton(int button) {
        if (button <= 0) {
            throw new IllegalArgumentException("button doesn\'t exist " + button);
        }
        if (button == 1) {
            return BUTTON1_DOWN_MASK;
        }
        if (button == 2) {
            return BUTTON2_DOWN_MASK;
        }
        if (button == 3) {
            return BUTTON3_DOWN_MASK;
        }
        // Los botones a partir del cuarto siguen en los bits altos, uno por boton.
        return 1 << (button + 9);
    }

    /**
     * Si Mayúsculas estaba apretada.
     *
     * <p>Mira la máscara **nueva** y no la vieja: es la única que dice sin ambigüedad qué estaba
     * apretado, y es lo que hace el JDK moderno. Un evento armado con las máscaras viejas contesta
     * `false` acá, y eso es correcto — esas máscaras no distinguían teclas de botones.
     */
    public boolean isShiftDown() {
        return (this.modifiers & SHIFT_DOWN_MASK) != 0;
    }

    /** Si Control estaba apretada. */
    public boolean isControlDown() {
        return (this.modifiers & CTRL_DOWN_MASK) != 0;
    }

    /** Si Meta estaba apretada. */
    public boolean isMetaDown() {
        return (this.modifiers & META_DOWN_MASK) != 0;
    }

    /** Si Alt estaba apretada. */
    public boolean isAltDown() {
        return (this.modifiers & ALT_DOWN_MASK) != 0;
    }

    /** Si AltGr estaba apretada. */
    public boolean isAltGraphDown() {
        return (this.modifiers & ALT_GRAPH_DOWN_MASK) != 0;
    }

    /** Cuándo pasó. */
    public long getWhen() {
        return this.when;
    }

    /**
     * Los modificadores en la codificación vieja.
     *
     * @deprecated mezcla teclas con botones de forma ambigua. Usar {@link #getModifiersEx}.
     */
    @Deprecated
    public int getModifiers() {
        return this.modifiers & (JDK_1_3_MODIFIERS | HIGH_MODIFIERS);
    }

    /** Los modificadores en la codificación nueva. */
    public int getModifiersEx() {
        return this.modifiers & ~JDK_1_3_MODIFIERS;
    }

    /** Los bits que usaba la codificación vieja. */
    static final int JDK_1_3_MODIFIERS = SHIFT_DOWN_MASK - 1;

    /** Los bits reservados para los botones a partir del cuarto. */
    static final int HIGH_MODIFIERS = ~((1 << 14) - 1);

    /** Marca que alguien se hizo cargo. */
    public void consume() {
        this.consumed = true;
    }

    /** Si alguien ya se hizo cargo. */
    public boolean isConsumed() {
        return this.consumed;
    }

    /** Los modificadores escritos para una persona, como "Ctrl+Shift". */
    public static String getModifiersExText(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if ((modifiers & META_DOWN_MASK) != 0) {
            sb.append("Meta+");
        }
        if ((modifiers & CTRL_DOWN_MASK) != 0) {
            sb.append("Ctrl+");
        }
        if ((modifiers & ALT_DOWN_MASK) != 0) {
            sb.append("Alt+");
        }
        if ((modifiers & SHIFT_DOWN_MASK) != 0) {
            sb.append("Shift+");
        }
        if ((modifiers & ALT_GRAPH_DOWN_MASK) != 0) {
            sb.append("Alt Graph+");
        }
        for (int b = 1; b <= 3; b++) {
            if ((modifiers & getMaskForButton(b)) != 0) {
                sb.append("Button").append(b).append("+");
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
