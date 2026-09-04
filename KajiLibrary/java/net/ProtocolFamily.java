package java.net;

// La familia de protocolos de una direccion: IPv4, IPv6, o sockets de dominio Unix.
//
// Un solo metodo, `name()`, y esa es la gracia: la interfaz existe para que las APIs que abren
// canales puedan aceptar familias que no sean las tres estandar sin cambiar sus firmas. El
// enum `StandardProtocolFamily` la implementa; cualquiera puede aportar otra.
public interface ProtocolFamily {

    String name();
}
