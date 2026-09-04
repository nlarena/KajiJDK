package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.CompoundControl -- un grupo de perillas que van juntas.
 *
 * <p>No tiene valor propio: es un contenedor. Sirve para que una interfaz pueda agrupar lo que en el
 * dispositivo real esta agrupado -- el ecualizador, con sus bandas; el canal de una consola, con su
 * volumen, su balance y su silencio.
 *
 * <p>Los miembros pueden ser a su vez compuestos, asi que esto es un arbol. Un recorrido tiene que
 * contemplarlo.
 *
 * <p>No hay forma de cambiarle los miembros despues de construirlo, y eso es a proposito: la estructura
 * la fija el dispositivo.
 */
public abstract class CompoundControl extends Control {

    /** Las perillas que agrupa. */
    private final Control[] controls;

    /** @param memberControls las perillas del grupo */
    protected CompoundControl(Type type, Control[] memberControls) {
        super(type);
        this.controls = memberControls;
    }

    /**
     * Las perillas del grupo.
     *
     * <p>Devuelve una copia del arreglo, asi que modificar lo que sale no cambia el control.
     */
    public Control[] getMemberControls() {
        Control[] copy = new Control[this.controls.length];
        System.arraycopy(this.controls, 0, copy, 0, this.controls.length);
        return copy;
    }

    /** El del control, mas los tipos de sus miembros entre corchetes. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        while (i < this.controls.length) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(this.controls[i].getType());
            i = i + 1;
        }
        sb.append(']');
        return super.toString() + " containing " + sb + " controls";
    }

    /**
     * Los tipos de grupo.
     *
     * <p>No trae ninguno predefinido: los grupos que existen dependen enteramente del dispositivo, y
     * nombrar unos pocos habria sido arbitrario.
     */
    public static class Type extends Control.Type {

        /** Protegido: los tipos los define quien provee el mezclador. */
        protected Type(String name) {
            super(name);
        }
    }
}
