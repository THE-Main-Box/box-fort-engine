package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.interact.InteractTriggerComponent;

public interface InteractionTriggerer {

    InteractTriggerComponent getTriggerC();

    Vector2 getCoordinatesInMeters();
}
