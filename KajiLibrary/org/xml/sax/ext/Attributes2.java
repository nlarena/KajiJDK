package org.xml.sax.ext;

import org.xml.sax.Attributes;

/**
 * KajiLibrary's org.xml.sax.ext.Attributes2 -- `Attributes` mas las dos preguntas que la lista
 * plana no puede contestar: ¿este atributo estaba en la DTD? ¿estaba escrito en el elemento, o lo
 * puso el parser?
 *
 * <p>La segunda importa mas de lo que parece. Un atributo con valor por omision en la DTD aparece
 * en `startElement` exactamente igual que uno escrito a mano, y para leer el documento eso esta
 * bien --el valor efectivo es el mismo--. Pero un serializador que reescriba el documento y
 * emita los dos termina metiendo en el archivo cosas que el autor no escribio, y ademas lo rompe
 * si despues se lo lee sin la DTD. `isSpecified` es la unica forma de distinguirlos.
 *
 * <p>El manejador recibe esto sin pedirlo: el parser pasa un `Attributes` a `startElement` y el
 * codigo hace `instanceof Attributes2` para ver si tiene la informacion extra. No hay una feature
 * que lo prenda. La que sí manda es
 * `http://xml.org/sax/features/use-attributes2`, que un parser reporta como `true` (solo lectura)
 * cuando entrega objetos de este tipo.
 *
 * <p><strong>La asimetria con `Attributes` es del contrato y no un descuido:</strong> alla un
 * nombre que no existe devuelve `null`, aca tira excepcion. La razon es el tipo de retorno: con un
 * `boolean` no hay un tercer valor para decir "no hay tal atributo", y devolver `false` seria
 * mentir --seria afirmar que existe y no fue especificado--. Un indice fuera de rango da
 * `ArrayIndexOutOfBoundsException`; un nombre que no esta, `IllegalArgumentException`.
 *
 * <p>Cuando no hay DTD, `isDeclared` es `false` para todo y `isSpecified` es `true` para todo, que
 * es la respuesta correcta y no un valor de relleno: sin DTD nada esta declarado y todo lo que hay
 * fue escrito.
 */
public interface Attributes2 extends Attributes {

    /**
     * `true` si el atributo fue declarado en la DTD. Un parser no validante que se saltee el
     * subconjunto externo va a decir `false` de atributos que sí estaban declarados alla: esta
     * contestando por lo que leyo, no por lo que el documento tiene.
     */
    boolean isDeclared(int index);

    boolean isDeclared(String qName);

    boolean isDeclared(String uri, String localName);

    /**
     * `false` solo cuando el valor salio de un `#FIXED` o de un valor por omision de la DTD. Un
     * atributo escrito en el elemento da `true` aunque coincida con el valor por omision.
     */
    boolean isSpecified(int index);

    boolean isSpecified(String uri, String localName);

    boolean isSpecified(String qName);
}
