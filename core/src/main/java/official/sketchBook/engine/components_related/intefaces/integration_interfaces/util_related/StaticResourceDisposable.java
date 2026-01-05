package official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related;

public interface StaticResourceDisposable {
    /// Dispose dos recursos estáticos da classe
    /// DEVE ser implementado como uma função STATIC nas subclasses
    static void disposeStaticResources() {
        throw new UnsupportedOperationException(
            "Subclasses que implementam StaticResourceDisposable devem " +
                "implementar disposeStaticResources() como método static"
        );
    }
}
