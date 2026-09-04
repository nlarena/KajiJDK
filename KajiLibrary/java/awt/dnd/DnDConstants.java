package java.awt.dnd;

/**
 * Qué se puede hacer con lo que se arrastra: copiarlo, moverlo o enlazarlo.
 *
 * <p>Son **bits**, no valores excluyentes, y ahí está la gracia: el origen declara todo lo que
 * acepta —normalmente copiar o mover— y el destino elige uno. La tecla que el usuario tenga apretada
 * inclina la elección, y por eso arrastrar con Control copia donde arrastrar sin nada movería.
 *
 * <p>La clase no se puede instanciar y es final: son constantes y nada más.
 */
public final class DnDConstants {

    /** Nada; el arrastre no se acepta. */
    public static final int ACTION_NONE = 0x0;

    /** Copiar: el original queda donde estaba. */
    public static final int ACTION_COPY = 0x1;

    /** Mover: el original se saca del origen. */
    public static final int ACTION_MOVE = 0x2;

    /** Cualquiera de las dos; el destino elige. */
    public static final int ACTION_COPY_OR_MOVE = ACTION_COPY | ACTION_MOVE;

    /** Enlazar: se crea una referencia al original. */
    public static final int ACTION_LINK = 0x40000000;

    /** El otro nombre de {@link #ACTION_LINK}. */
    public static final int ACTION_REFERENCE = ACTION_LINK;

    /** No se instancia. */
    private DnDConstants() {
    }
}
