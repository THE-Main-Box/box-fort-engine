package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.game_object_related.base_game_object.InteractComponent;

public interface InteractableObjectII {
    /// Chamada de lógica de interação
    void interact();

    /// Se podemos interagir com o objeto
    boolean canInteract();


    InteractComponent getInteractC();
}
