package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

public interface WirableObjectII {

    default void interactByWiring(){
        if(canWireInteract())
            executeWiringInteraction();
    }

    void executeWiringInteraction();

    boolean canWireInteract();
}
