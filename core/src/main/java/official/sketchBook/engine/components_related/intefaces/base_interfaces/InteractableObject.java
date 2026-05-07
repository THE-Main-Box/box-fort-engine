package official.sketchBook.engine.components_related.intefaces.base_interfaces;

public interface InteractableObject {

    ///Chamada de lógica de interação
    void interact();

    ///Se podemos interagir com o objeto
    boolean canInteract();
}
