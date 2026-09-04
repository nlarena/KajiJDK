package java.beans.beancontext;

/**
 * Un objeto que **no es** un {@link BeanContextChild} pero delega en uno.
 *
 * <p>Existe por una restricción de herencia: una clase que ya extiende otra jerarquía no puede
 * además implementar `BeanContextChild` si eso la obligara a un supertipo que no tiene.
 * Implementando esta interfaz devuelve el hijo en el que delega, y el contexto trata a ése como el
 * miembro real.
 *
 * <p>Las interfaces de delegación son excluyentes: un objeto implementa **una**, no dos. Con dos, el
 * contexto no tendría forma de decidir en cuál delegar.
 *
 * <h2>Lo que no está, y por qué</h2>
 *
 * <p>El JDK tiene otras dos de esta familia --`BeanContextChildComponentProxy`, que devuelve un
 * `java.awt.Component`, y `BeanContextContainerProxy`, que devuelve un `java.awt.Container`--.
 * KajiLibrary **no las trae**, y el motivo es concreto: su único método devuelve un tipo que esta
 * biblioteca no tiene. `java.awt` acá llega hasta la geometría y el color; no hay jerarquía de
 * componentes, y una interfaz cuyo único miembro no se puede ni nombrar no es un subconjunto de la
 * API sino una declaración vacía con un nombre prestado. El día que exista `java.awt.Component`, las
 * dos son cuatro líneas cada una.
 */
public interface BeanContextProxy {

    /** El hijo en el que este objeto delega. */
    BeanContextChild getBeanContextProxy();
}
