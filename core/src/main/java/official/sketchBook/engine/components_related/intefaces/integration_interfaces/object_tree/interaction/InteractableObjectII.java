package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

public interface InteractableObjectII {
    /// Chamada de lógica de interação
    default void interact(InteractionTriggerer triggerer){
        if(canInteract())
            executeInteraction(triggerer);
    }

    /// Se podemos interagir com o objeto, usado externamente para validar se é viável a interação
    boolean canInteract();

    void executeInteraction(InteractionTriggerer triggerer);

    FixtureData getTriggerFixData();

    Vector2 getCoordinatesInMeters();

}
