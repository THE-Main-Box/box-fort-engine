package official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related;

import official.sketchBook.engine.components_related.objects.TransformComponent;

public interface OptmizedRenderableObjectII extends RenderableObjectII{
    /// Valor de se estamos dentro da tela, pode ser omitido caso, mas não é recomendado
    boolean isInScreen();
    /// Função usada pelo manager de objetos renderizáveis para determinar se estamos dentro da tela ou não
    void setInScreen(boolean inScreen);

    TransformComponent getTransformC();
}
