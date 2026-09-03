package org.xml.sax;

import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.ErrorHandler -- tres severidades, y la diferencia entre ellas no es
// cosmetica.
//
//   warning    -- algo que el parser quiere mencionar. El analisis sigue.
//   error      -- un error recuperable, en el sentido de XML: una violacion de *validez*. El
//                 documento es invalido pero sigue estando bien formado, asi que el parser puede
//                 seguir, y sigue, reportando eventos. Si la aplicacion quiere frenar, lanza
//                 desde aca.
//   fatalError -- se perdio el buen formato. El parser no debe reportar mas eventos despues de
//                 que esta llamada retorne, asi que una implementacion que vuelve normalmente de
//                 fatalError queda con comportamiento indefinido. Lo honesto es lanzar.
//
// Un lector sin ErrorHandler instalado descarta avisos y errores en silencio y lanza ante los
// errores fatales --por eso instalar uno es lo primero que hace toda aplicacion SAX de verdad.
public interface ErrorHandler {

    // Un aviso no fatal; el analisis sigue.
    void warning(SAXParseException exception) throws SAXException;

    // Un error recuperable (de validez); el analisis sigue salvo que esto lance.
    void error(SAXParseException exception) throws SAXException;

    // El buen formato esta roto; no van a seguir mas eventos. Deberia lanzar.
    void fatalError(SAXParseException exception) throws SAXException;
}
