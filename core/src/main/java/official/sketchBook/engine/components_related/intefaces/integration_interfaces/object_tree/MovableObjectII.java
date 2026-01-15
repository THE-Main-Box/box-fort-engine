package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;

public interface MovableObjectII {
    TransformComponent getTransformC();
    MovementComponent getMoveC();
}
