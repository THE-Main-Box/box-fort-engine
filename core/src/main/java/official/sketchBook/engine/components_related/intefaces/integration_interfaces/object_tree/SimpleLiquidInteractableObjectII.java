package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;

public interface SimpleLiquidInteractableObjectII {

    void onLiquidExit();
    void onLiquidEnter();

    void inLiquidUpdate();

    TransformComponent getTransformC();
    MovementComponent getMoveC();
    PhysicalMobLiquidInteractionComponent getLiquidInteractionC();
}
