package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.CDATASection -- una seccion `&lt;![CDATA[...]]&gt;`.
 *
 * <p>No agrega **ningun** miembro sobre `Text`, y esta bien que asi sea: para el modelo de datos un
 * CDATA es texto y nada mas. Lo unico que cambia es la **serializacion** --el texto sale sin escapar
 * `&amp;` ni `&lt;`-- y eso no es una operacion sobre el nodo. La interfaz existe para que
 * `getNodeType()` pueda distinguirlo y para que el serializador sepa como escribirlo.
 *
 * <p>La consecuencia practica es que `normalize()` puede juntar un CDATA con el `Text` de al lado y
 * perder la distincion, que es exactamente lo que la norma permite.
 */
public interface CDATASection extends Text {
}
