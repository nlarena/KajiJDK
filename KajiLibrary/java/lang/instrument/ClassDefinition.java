package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.ClassDefinition -- una clase y los bytes con los que
 * reemplazarla.
 *
 * <p>Un par inmutable, y nada mas. Existe porque {@code redefineClasses} recibe <b>varias</b>
 * redefiniciones y las aplica juntas: sin un par, harian falta dos arreglos paralelos y un error de
 * indice pondria los bytes de una clase en otra.
 *
 * <p>Que se apliquen juntas no es un detalle. Redefinir dos clases que se llaman entre si de a una
 * dejaria un instante con la version vieja de una y la nueva de la otra, y ahi el programa puede
 * romperse.
 *
 * <p>Los bytes <b>no se copian</b>: se guarda el arreglo que se pasa, y {@link #getDefinitionClassFile}
 * lo devuelve tal cual. Es lo que hace el JDK, y hay que saberlo -- modificar el arreglo despues de
 * construir esto cambia lo que se va a redefinir.
 */
public final class ClassDefinition {

    /** La clase a reemplazar. */
    private final Class<?> definitionClass;

    /** Con que bytes. */
    private final byte[] definitionClassFile;

    /**
     * @param theClass la clase a reemplazar
     * @param theClassFile los bytes del archivo de clase nuevo
     * @throws NullPointerException si alguno es null
     */
    public ClassDefinition(Class<?> theClass, byte[] theClassFile) {
        if (theClass == null) {
            throw new NullPointerException("null passed as 'theClass' in ClassDefinition");
        }
        if (theClassFile == null) {
            throw new NullPointerException("null passed as 'theClassFile' in ClassDefinition");
        }
        this.definitionClass = theClass;
        this.definitionClassFile = theClassFile;
    }

    /** La clase a reemplazar. */
    public Class<?> getDefinitionClass() {
        return this.definitionClass;
    }

    /** Los bytes. El mismo arreglo que se paso; ver la nota de la clase. */
    public byte[] getDefinitionClassFile() {
        return this.definitionClassFile;
    }
}
