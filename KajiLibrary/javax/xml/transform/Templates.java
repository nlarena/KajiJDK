package javax.xml.transform;

import java.util.Properties;

/**
 * KajiLibrary's javax.xml.transform.Templates -- una hoja de estilo ya compilada.
 *
 * <p>Es la respuesta a un problema muy concreto: compilar una hoja de estilo cuesta caro --hay que
 * parsearla, resolver los `import`, armar el arbol de plantillas, compilar los patrones de XPath--
 * y un {@link Transformer} **no es reutilizable en paralelo**, porque tiene estado (los parametros,
 * las propiedades de salida). Sin esta interfaz habria que elegir entre pagar la compilacion en
 * cada transformacion o compartir un objeto que no se puede compartir.
 *
 * <p>`Templates` parte eso en dos: el resultado caro de compilar, **inmutable y seguro entre hilos**,
 * y los transformadores baratos que salen de el. El patron de uso es compilar una vez al arrancar y
 * pedir un {@link Transformer} por cada trabajo.
 *
 * <p>La inmutabilidad es la razon de que {@link #getOutputProperties} devuelva una **copia**: si
 * entregara la tabla interna, cualquiera que la modificara estaria cambiando la hoja de estilo para
 * todos los hilos que la comparten.
 */
public interface Templates {

    /**
     * Un transformador nuevo, listo para usar, con las propiedades de esta hoja de estilo.
     *
     * <p>El objeto que devuelve es de un solo dueño: no se comparte entre hilos. El que se comparte
     * es este `Templates`.
     *
     * @return un transformador recien hecho
     * @throws TransformerConfigurationException si no se puede construir
     */
    Transformer newTransformer() throws TransformerConfigurationException;

    /**
     * Una copia de las propiedades de salida que declara la hoja de estilo.
     *
     * <p>Copia, no vista: ver la nota del encabezado. Las que la hoja no declara aparecen como los
     * valores por omision del metodo de salida en las {@link Properties#defaults} de la tabla, no
     * como entradas propias -- asi se distingue "lo dijo la hoja" de "lo puso la spec", que es
     * justo lo que hay que saber para decidir si conviene pisarlo.
     *
     * @return las propiedades, con las por omision abajo
     */
    Properties getOutputProperties();
}
