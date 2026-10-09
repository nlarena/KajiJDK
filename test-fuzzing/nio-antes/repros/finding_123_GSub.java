// Repro de #123 - un override covariante se rechaza cuando el subtipado del RETORNO
// viaja por un `extends` PARAMETRIZADO de un tipo del classpath.
//
//   bin\javac.exe --emit -cp "KajiLibrary;KajiLibraryepros" ...finding_123_GA.java
//   (idem GB, GBase; despues GSub)
//   -> error: el retorno de `m` no es compatible con el de `GBase`: GB no es un
//      subtipo de GA
//
// Lo revelador: con la MISMA forma pero jerarquia NO generica (interface CB extends CA)
// el override se acepta. O sea que el chequeo consulta la cadena de supertipos, pero se
// pierde cuando la clausula extends lleva argumentos de tipo.
//
// Es la forma exacta de jakarta: SetAttribute<X,E> extends PluralAttribute<X,Set<E>,E>,
// que deja omitido getModel() en SetJoin, CollectionJoin, ListJoin y MapJoin.
import java.util.Set;
public interface finding_123_GSub<Z, E> extends finding_123_GBase<Z, Set<E>, E> {
    finding_123_GB<? super Z, E> m();
}
