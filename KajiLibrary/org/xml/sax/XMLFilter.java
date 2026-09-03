package org.xml.sax;

import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.XMLFilter -- un XMLReader que saca sus eventos de otro XMLReader en
// vez de sacarlos de un documento.
//
// El truco es que un filtro es *a la vez* el lector con el que habla su cliente y el manejador
// con el que habla su padre. Llamar a parse() en el filtro configura al padre para que le
// reporte al filtro y despues arranca al padre; cada evento llega al filtro, que puede
// descartarlo, reescribirlo o dejarlo pasar antes de entregarselo al manejador del cliente. Los
// filtros se encadenan, asi que una tuberia no es mas que filtros cuyo padre es el filtro
// anterior.
//
// helpers.XMLFilterImpl es la clase base que deja pasar todo: se la extiende y se redefinen los
// pocos eventos que a uno le importan.
public interface XMLFilter extends XMLReader {

    // El lector del que este filtro saca los eventos.
    void setParent(XMLReader parent);

    // El lector padre, o null si no se le puso ninguno.
    XMLReader getParent();
}
