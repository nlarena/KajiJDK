package java.lang.instrument;

import java.security.ProtectionDomain;

/**
 * KajiLibrary's java.lang.instrument.ClassFileTransformer -- reescribe los bytes de una clase antes
 * de que se cargue.
 *
 * <p>Es la pieza que hace posible un perfilador, un rastreador o un inyector de dependencias sin
 * tocar el codigo: la maquina virtual le pasa los bytes de cada clase que va a cargar y usa lo que
 * devuelva.
 *
 * <h2>Devolver null es lo normal</h2>
 *
 * <p>Un transformador se aplica a <b>todas</b> las clases, incluidas las miles de la plataforma.
 * Devolver null significa "no la toco" y es la respuesta correcta para casi todas; devolver una copia
 * sin cambios funciona igual pero cuesta una reescritura por clase.
 *
 * <h2>Los dos metodos, y cual redefinir</h2>
 *
 * <p>Los dos tienen default y llamarse igual, y esa es la parte que confunde. El de <b>seis</b>
 * argumentos --el que recibe el {@code Module}-- es el que la maquina virtual llama; su default
 * delega en el de cinco, que es el que existia antes de los modulos.
 *
 * <p>Asi que redefinir el de cinco alcanza y es lo habitual. Redefinir el de seis solo hace falta
 * cuando el transformador necesita saber en que modulo esta la clase -- por ejemplo para no tocar
 * nada de {@code java.base}.
 *
 * <p>El default de cinco devuelve null: un transformador que no redefine ninguno de los dos no
 * transforma nada, que es lo unico coherente.
 */
public interface ClassFileTransformer {

    /**
     * La version anterior a los modulos.
     *
     * @param loader quien esta cargando la clase; null es el cargador de arranque
     * @param className el nombre interno, con barras y sin {@code .class}
     * @param classBeingRedefined la clase que se esta redefiniendo, o null si es una carga nueva
     * @param protectionDomain de donde viene
     * @param classfileBuffer los bytes actuales
     * @return los bytes nuevos, o null para no tocarla
     * @throws IllegalClassFormatException si los bytes que llegaron no sirven
     */
    default byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                             ProtectionDomain protectionDomain, byte[] classfileBuffer)
        throws IllegalClassFormatException {
        return null;
    }

    /**
     * La version con modulo, que es la que la maquina virtual llama.
     *
     * <p>Por omision delega en la de cinco; ver la nota de la clase.
     *
     * @param module el modulo de la clase
     */
    default byte[] transform(Module module, ClassLoader loader, String className,
                             Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                             byte[] classfileBuffer) throws IllegalClassFormatException {
        return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }
}
