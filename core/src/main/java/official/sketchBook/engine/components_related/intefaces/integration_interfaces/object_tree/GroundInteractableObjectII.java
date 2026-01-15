package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.components_related.objects.GroundDetectionComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

public interface GroundInteractableObjectII {
    TransformComponent getTransformC();
    GroundDetectionComponent getGroundDetectC();
    boolean isOnGround();
}
