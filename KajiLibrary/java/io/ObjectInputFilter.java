package java.io;

import java.util.function.Predicate;

/**
 * KajiLibrary's java.io.ObjectInputFilter -- la politica que decide que clases puede reconstruir un
 * flujo de objetos.
 *
 * <p>Existe porque deserializar es, por definicion, dejar que unos bytes de afuera elijan que
 * constructores corren. Un filtro es donde se pone el limite: se lo consulta **antes** de resolver
 * cada clase, y su respuesta es {@code ALLOWED}, {@code REJECTED} o {@code UNDECIDED}.
 *
 * <p>{@code UNDECIDED} no es "no se": es "no opino, que decida el siguiente". Tener tres respuestas
 * y no dos es lo que permite componer filtros -- uno que solo sabe de una clase puede decir lo suyo
 * y callarse del resto sin autorizar nada por omision.
 *
 * <h2>Como se instala, y lo unico que falta</h2>
 *
 * <p>Con {@link ObjectInputStream#setObjectInputFilter}, y desde ahi se lo consulta de verdad: una
 * vez por cada clase que el flujo esta por armar, y con el largo cuando lo que viene es un arreglo.
 * Un {@code REJECTED} corta la lectura con {@link InvalidClassException} **antes** de que se
 * construya nada.
 *
 * <p><strong>{@code ObjectInputFilter.Config} no esta.</strong> Esa clase fija el filtro
 * <em>global</em>, el que se consulta cuando el flujo no trae uno propio, y su pieza central es
 * {@code createFilter(String)}: un lenguaje de patrones con comodines de paquete y limites
 * (`maxarray`, `maxdepth`, `maxrefs`, `maxbytes`) cuya semantica exacta es justamente lo que decide
 * que pasa y que no. Un analizador que se equivoque en un comodin deja entrar lo que el que lo
 * escribio creia haber cerrado, y no hay forma de darse cuenta mirando el resultado. Se declara la
 * ausencia en vez de aproximar la gramatica; mientras tanto el filtro por flujo, que es explicito,
 * si esta y si se cumple.
 */
public interface ObjectInputFilter {

    /**
     * La decision sobre un objeto del flujo.
     *
     * <p>Se lo llama una vez por clase a resolver y tambien --con {@link FilterInfo#serialClass()}
     * en {@code null}-- para los limites de tamanio del flujo, que es como un filtro puede cortar un
     * arreglo de mil millones de elementos sin saber de que clase es.
     */
    Status checkInput(FilterInfo filterInfo);

    /**
     * Un filtro que aprueba lo que cumpla `predicate` y contesta `otherStatus` para el resto.
     *
     * <p><strong>El predicado ve la clase tal cual viene, arreglos incluidos.</strong> Un predicado
     * escrito como {@code c -> c == String.class} **no** deja pasar un {@code String[]}: son clases
     * distintas y la que llega es la del arreglo. No se desenvuelve por conveniencia porque el largo
     * de un arreglo es justamente uno de los vectores de ataque, y quien quiera permitirlos tiene
     * que decirlo.
     *
     * <p>Con la clase en `null` --las consultas de tamanio-- devuelve {@code UNDECIDED}: el
     * predicado habla de clases y ahi no hay ninguna sobre la que opinar.
     *
     * @throws NullPointerException si `predicate` u `otherStatus` son `null`
     */
    static ObjectInputFilter allowFilter(Predicate<Class<?>> predicate, Status otherStatus) {
        if (predicate == null || otherStatus == null) {
            throw new NullPointerException();
        }
        return new Filtros.PorPredicado(predicate, Status.ALLOWED, otherStatus);
    }

    /**
     * El espejo de {@link #allowFilter}: rechaza lo que cumpla `predicate`, y contesta `otherStatus`
     * para el resto.
     *
     * <p>Los dos existen porque una lista blanca y una lista negra no son la misma politica escrita
     * al reves: con `allowFilter` lo que no se nombro queda fuera, con `rejectFilter` queda dentro.
     * La diferencia se nota el dia que aparece una clase en la que nadie penso.
     *
     * @throws NullPointerException si `predicate` u `otherStatus` son `null`
     */
    static ObjectInputFilter rejectFilter(Predicate<Class<?>> predicate, Status otherStatus) {
        if (predicate == null || otherStatus == null) {
            throw new NullPointerException();
        }
        return new Filtros.PorPredicado(predicate, Status.REJECTED, otherStatus);
    }

    /**
     * Combina dos filtros: **cualquier** rechazo gana, y si ninguno rechaza alcanza con que uno
     * apruebe.
     *
     * <p>Que el rechazo gane es lo unico que hace componible a la cosa: si aprobar pudiera anular a
     * un rechazo, agregar un filtro podria **abrir** lo que otro cerraba, y nadie podria razonar
     * sobre una politica sin leerla entera.
     *
     * @throws NullPointerException si `filter` es `null`
     */
    static ObjectInputFilter merge(ObjectInputFilter filter, ObjectInputFilter anotherFilter) {
        if (filter == null) {
            throw new NullPointerException();
        }
        return new Filtros.Union(filter, anotherFilter);
    }

    /**
     * Convierte en rechazo el {@code UNDECIDED} que quede sobre una clase concreta.
     *
     * <p>Es la tapa de una lista blanca: sin esto, una clase que ningun filtro nombro sale
     * {@code UNDECIDED}, y el que llama tiene que acordarse de tratar eso como negativo. Envolver la
     * politica hace que la respuesta por omision quede escrita en un lugar en vez de depender de que
     * cada uso la interprete igual.
     *
     * <p>Las consultas sin clase --las de tamanio-- pasan sin tocar: ahi {@code UNDECIDED} significa
     * "este filtro no pone limites", que es una respuesta legitima y no un olvido.
     *
     * @throws NullPointerException si `filter` es `null`
     */
    static ObjectInputFilter rejectUndecidedClass(ObjectInputFilter filter) {
        if (filter == null) {
            throw new NullPointerException();
        }
        return new Filtros.RechazaIndecisos(filter);
    }

    /** Lo que se sabe del objeto que esta por leerse cuando se consulta al filtro. */
    interface FilterInfo {

        /**
         * La clase a resolver, o `null` si esta consulta no es sobre una clase.
         *
         * <p>El `null` es informacion y no un hueco: es como el flujo pregunta por los limites de
         * tamanio --cuantas referencias van, cuantos bytes-- que valen sin importar la clase.
         */
        Class<?> serialClass();

        /** El largo del arreglo por leer, o -1 si esto no es un arreglo. */
        long arrayLength();

        /** Cuan anidado esta el objeto; el de arriba de todo es 1. */
        long depth();

        /** Cuantas referencias lleva leidas el flujo. */
        long references();

        /** Cuantos bytes lleva consumidos el flujo. */
        long streamBytes();
    }

    /** Las tres respuestas posibles. Ver la nota de {@code UNDECIDED} en la cabecera de la clase. */
    enum Status {
        UNDECIDED,
        ALLOWED,
        REJECTED
    }
}
