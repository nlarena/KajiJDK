package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.PostVMInitHook — lo que corre cuando la VM terminó de arrancar.
 *
 * <p>Hay cosas que no se pueden hacer durante la inicialización porque necesitan una VM ya en pie:
 * en el JDK, este gancho es el que arma el soporte de gestión cuando se pidió por línea de comandos.
 * La VM lo invoca por nombre, una vez, después del arranque y antes del `main`.
 *
 * <p>Acá **no hace nada, y no queda pendiente**: no hay agentes de gestión ni JMX que inicializar,
 * así que la lista de cosas por hacer después del arranque está vacía. La clase existe con la forma
 * que el JDK declara, para que ese punto de invocación tenga a quién llamar si algún día lo hay.
 */
public class PostVMInitHook {

    public PostVMInitHook() {
    }

    /** Lo invoca la VM una vez, ya arrancada. */
    public static void run() {
    }
}
