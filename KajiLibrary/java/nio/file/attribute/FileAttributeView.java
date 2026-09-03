package java.nio.file.attribute;

// Una vista de atributos de un **archivo**, por oposicion a `FileStoreAttributeView`, que es la del
// volumen. No agrega nada a `AttributeView`: la division existe solo para que el sistema de tipos
// distinga las dos familias, y por eso `Files.getFileAttributeView` puede pedir `Class<V extends
// FileAttributeView>` y rechazar en compilacion una vista de volumen.
public interface FileAttributeView extends AttributeView {
}
