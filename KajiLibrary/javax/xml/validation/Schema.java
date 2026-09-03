package javax.xml.validation;

/**
 * KajiLibrary's javax.xml.validation.Schema -- un esquema ya compilado.
 *
 * <p>Representa un conjunto de reglas --XML Schema, RELAX NG, lo que sea-- <b>ya leido y
 * verificado</b>. La clase tiene dos metodos y ninguno valida: los dos fabrican algo que valida.
 *
 * <h2>Por que la separacion en tres</h2>
 *
 * <p>{@code SchemaFactory} lee el esquema, {@code Schema} lo guarda compilado, y
 * {@link Validator} valida <b>un</b> documento. Podria ser una sola clase con un metodo
 * {@code validate(esquema, documento)}, y seria mucho mas lento: compilar un esquema es caro y
 * validar contra uno ya compilado es barato. La separacion hace que ese costo se pague una vez.
 *
 * <p>De ahi sale la regla de uso que importa: un {@code Schema} es <b>inmutable y compartible entre
 * hilos</b>; un {@link Validator} no. Lo que se guarda en un campo estatico es el esquema, y el
 * validador se fabrica en cada uso -- al reves de lo que uno haria por costumbre.
 *
 * <h2>Las dos formas de validar</h2>
 *
 * <p>{@link #newValidator} valida algo que ya existe: un arbol, un archivo, un flujo.
 * {@link #newValidatorHandler} valida <b>mientras</b> se lee, enchufandose en una cadena SAX. La
 * segunda no necesita tener el documento entero en memoria, y ademas puede ir pasandole el contenido
 * ya validado a otro manejador.
 */
public abstract class Schema {

    /** Para las subclases. */
    protected Schema() {
    }

    /**
     * Un validador para documentos que ya existen.
     *
     * <p>Uno nuevo por cada uso, o al menos uno por hilo: ver la nota de la clase.
     */
    public abstract Validator newValidator();

    /** Un validador que se enchufa en una cadena SAX. */
    public abstract ValidatorHandler newValidatorHandler();
}
