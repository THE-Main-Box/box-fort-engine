package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

public interface HoldInteractableObjectII extends InteractableObjectII {
    float getHoldTimer();

    boolean isTriggerInteract();

    default boolean isHoldInteractable(){
        return getHoldTimer() > 0;
    }
}
