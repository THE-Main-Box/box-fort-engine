package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid;

import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;

public interface PhysicalLiquidInteractableObjectII extends LIOBase{

    PhysicsComponent getPhysicsC();
    MovementComponent getMoveC();

}
