package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.movement.PhysicalMobLiquidInteractionComponent;

public interface LiquidInteractableObjectII extends MovableObjectII{
        PhysicalMobLiquidInteractionComponent getLiquidInteractionC();
}
