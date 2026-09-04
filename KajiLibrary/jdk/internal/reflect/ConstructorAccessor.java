package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;

/**
 * KajiLibrary's jdk.internal.reflect.ConstructorAccessor — el contrato de "construir una instancia".
 *
 * <p>Lo mismo que {@link MethodAccessor} y por lo mismo: en esta VM la construccion reflexiva es el
 * intrinseco {@code Intrinsic::ConstructorNewInstance}, que aloca el objeto y corre el {@code <init>}
 * desde adentro del interprete. Un accesor no puede ser la maquinaria porque la maquinaria esta abajo
 * del piso de Java; puede ser —y es— la forma con la que se la nombra.
 *
 * <p>El intrinseco esta en el interprete y no en el puente de nativas por una razon que vale la pena
 * dejar dicha: tiene que <strong>alocar</strong>, y el objeto a medio construir queda vivo mientras
 * corre bytecode arbitrario, asi que el GC tiene que verlo como raiz. El puente de nativas no tiene
 * esa vista.
 */
public interface ConstructorAccessor {

    /**
     * Aloca una instancia y corre el constructor con {@code args}.
     *
     * @param args los argumentos del constructor
     * @return la instancia ya construida
     * @throws InstantiationException si la clase no se puede instanciar (abstracta, interfaz)
     * @throws IllegalArgumentException si los argumentos no corresponden
     * @throws InvocationTargetException envolviendo lo que haya tirado el constructor
     */
    Object newInstance(Object[] args)
            throws InstantiationException, IllegalArgumentException, InvocationTargetException;
}
