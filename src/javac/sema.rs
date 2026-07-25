//! El **análisis semántico**: sobre el [`ast`](super::ast), resuelve nombres (variables,
//! tipos, métodos) contra sus declaraciones y hace *type checking*. Es la etapa que acepta
//! un programa válido y rechaza uno con errores de tipos, antes de generar código.
//!
//! Hito **B2** — criterio de éxito: acepta `Add.java` y rechaza un programa mal tipado.
//! (Vacío por ahora — andamiaje.)
