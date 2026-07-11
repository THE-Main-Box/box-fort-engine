package official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.objects.TransformComponent;

public interface RenderableObjectII {
    /// Obtém o indice da ordem de renderização deste objeto
    int getRenderIndex();

    /// Atualiza o que precisa ser atualizado para poder realizar o render
    void updateVisuals(float delta);

    /// Executa a renderização
    void render(SpriteBatch batch);

    /// Determinado pela lógica interna do objeto, se pode ser renderizado ou não
    boolean canRender();



    void disposeGraphics();
}
