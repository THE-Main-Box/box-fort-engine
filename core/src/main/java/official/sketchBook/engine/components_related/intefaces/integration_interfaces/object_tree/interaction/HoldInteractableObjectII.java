package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

public interface HoldInteractableObjectII extends InteractableObjectII {
    void interactOnHold();

    float getHoldTime();

    boolean isTriggerInteract();

    default boolean isHoldInteractable(){
        return getHoldTime() > 0;
    }
}
