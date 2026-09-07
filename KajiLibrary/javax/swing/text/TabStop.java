package javax.swing.text;

import java.io.Serializable;

/**
 * Una parada de tabulacion: donde se detiene el texto y como se acomoda contra ese punto.
 *
 * <p>Inmutable, y por eso se puede compartir entre parrafos. La <em>alineacion</em> dice que parte
 * del texto queda en la posicion: a la izquierda es lo comun, y la decimal es la que alinea una
 * columna de numeros por su coma. El <em>guia</em> es lo que se dibuja en el hueco que queda antes
 * de la parada, esos puntitos de un indice.
 */
public class TabStop implements Serializable {

    /** El texto empieza en la parada. */
    public static final int ALIGN_LEFT = 0;

    /** El texto termina en la parada. */
    public static final int ALIGN_RIGHT = 1;

    /** El texto queda centrado en la parada. */
    public static final int ALIGN_CENTER = 2;

    /** La coma decimal queda en la parada. */
    public static final int ALIGN_DECIMAL = 4;

    /** Se dibuja una barra en la parada; el texto sigue de largo. */
    public static final int ALIGN_BAR = 5;

    /** Sin guia. */
    public static final int LEAD_NONE = 0;

    public static final int LEAD_DOTS = 1;

    public static final int LEAD_HYPHENS = 2;

    public static final int LEAD_UNDERLINE = 3;

    public static final int LEAD_THICKLINE = 4;

    public static final int LEAD_EQUALS = 5;

    private int alignment;
    private float position;
    private int leader;

    /** Una parada a la izquierda y sin guia en esa posicion. */
    public TabStop(float pos) {
        this(pos, ALIGN_LEFT, LEAD_NONE);
    }

    public TabStop(float pos, int align, int leader) {
        alignment = align;
        this.leader = leader;
        position = pos;
    }

    public float getPosition() {
        return position;
    }

    public int getAlignment() {
        return alignment;
    }

    public int getLeader() {
        return leader;
    }

    /** Iguales si coinciden en las tres cosas; se usa al comparar juegos de paradas. */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof TabStop) {
            TabStop o = (TabStop) other;
            return ((alignment == o.alignment) && (leader == o.leader)
                    && (position == o.position));
        }
        return false;
    }

    public int hashCode() {
        return alignment ^ leader ^ Math.round(position);
    }

    public String toString() {
        String buf = "";
        if (alignment == ALIGN_RIGHT) {
            buf = "right ";
        } else if (alignment == ALIGN_CENTER) {
            buf = "center ";
        } else if (alignment == ALIGN_DECIMAL) {
            buf = "decimal ";
        } else if (alignment == ALIGN_BAR) {
            buf = "bar ";
        }
        buf = buf + "tab @" + String.valueOf(position);
        if (leader != LEAD_NONE) {
            buf = buf + " (w/leaders)";
        }
        return buf;
    }
}
