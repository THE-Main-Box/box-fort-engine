package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.util_related.helper.body.FixtureData;

public interface InteractableObjectII {
    /// Chamada de lógica de interação
    void interact();

    /// Se podemos interagir com o objeto
    boolean canInteract();

    FixtureData getTriggerFixData();

}
