package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

import official.sketchBook.engine.components_related.interact.WiringTriggererManagementComponent;

public interface WiringTrigger extends InteractableObjectII{
    WiringTriggererManagementComponent getGlobalWiringManager();

    @Override
    default void executeInteraction(InteractionTriggerer triggerer) {
        getGlobalWiringManager().trigger(this);
    }
}
