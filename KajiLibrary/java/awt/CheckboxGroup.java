package java.awt;

import java.io.Serializable;

/**
 * Convierte un grupo de {@link Checkbox} en botones de radio: exactamente uno marcado a la vez.
 *
 * <p>AWT no tiene una clase de botón de radio. Tiene esto: la misma casilla de siempre, con un grupo
 * que se encarga de que marcar una desmarque a la anterior. Es una decisión de diseño discutible
 * —el widget cambia de forma según si tiene grupo o no— pero es la que hay.
 *
 * <p>Una vez que hay algo marcado, el grupo **no se puede vaciar** desde la interfaz: apretar la
 * casilla marcada no la desmarca. Por programa sí, pasando `null` a {@link #setSelectedCheckbox}.
 */
public class CheckboxGroup implements Serializable {

    private static final long serialVersionUID = 3729780091441768983L;

    /** La casilla marcada, o `null` si no hay ninguna. */
    Checkbox selectedCheckbox;

    /** Un grupo vacío. */
    public CheckboxGroup() {
    }

    /**
     * La casilla marcada.
     *
     * @return la casilla, o `null` si no hay ninguna
     */
    public Checkbox getSelectedCheckbox() {
        return this.selectedCheckbox;
    }

    /**
     * La casilla marcada.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getSelectedCheckbox}.
     */
    @Deprecated
    public Checkbox getCurrent() {
        return this.selectedCheckbox;
    }

    /**
     * Marca esa casilla y desmarca la que estuviera.
     *
     * <p>Una casilla que pertenece a **otro** grupo se ignora: aceptarla dejaría a dos grupos
     * creyendo que la mandan ellos.
     *
     * @param box la casilla a marcar, o `null` para dejar el grupo sin nada marcado
     */
    public synchronized void setSelectedCheckbox(Checkbox box) {
        if (box != null && box.group != this) {
            return;
        }
        Checkbox anterior = this.selectedCheckbox;
        this.selectedCheckbox = box;
        if (anterior != null && anterior != box) {
            anterior.setStateInternal(false);
        }
        if (box != null) {
            box.setStateInternal(true);
        }
    }

    /**
     * Marca esa casilla.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #setSelectedCheckbox}.
     */
    @Deprecated
    public synchronized void setCurrent(Checkbox box) {
        this.setSelectedCheckbox(box);
    }

    public String toString() {
        return this.getClass().getName() + "[selectedCheckbox=" + this.selectedCheckbox + "]";
    }
}
