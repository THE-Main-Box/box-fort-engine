package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.components_related.objects.RoomTileGroundDetection;
import official.sketchBook.engine.components_related.objects.TransformComponent;

public interface GroundInteractableObjectII {
    TransformComponent getTransformC();
    RoomTileGroundDetection getRoomGroundDetectC();
    boolean isOnGround();
}
