package java.security.cert;

// A criterion for choosing CRLs of a `CertStore`.
//
// It extends `Cloneable` and **redeclares** `clone()` as public, which `Cloneable` alone does not
// do. The reason is of the contract: a `CertStore` keeps the selector it is passed, and if it could
// not copy it, whoever gave it could change it afterwards and alter from behind what a query
// returns.
public interface CRLSelector extends Cloneable {

    // Whether this CRL meets the criterion.
    boolean match(CRL crl);

    Object clone();
}
