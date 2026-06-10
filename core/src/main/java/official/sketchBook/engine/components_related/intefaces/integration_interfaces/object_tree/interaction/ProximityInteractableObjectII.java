package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

import java.util.List;

public interface ProximityInteractableObjectII extends InteractableObjectII{
    List<InteractionTriggerer> getNearList();

    default void onTriggererEnter(InteractionTriggerer triggerer) {
        if (!getNearList().contains(triggerer))
            getNearList().add(triggerer);
    }

    default void onTriggererExit(InteractionTriggerer triggerer) {
        getNearList().remove(triggerer);
    }

    default boolean isNear() {
        return !getNearList().isEmpty();
    }

    default boolean isTriggererNear(InteractionTriggerer triggerer) {
        return getNearList().contains(triggerer);
    }
}
