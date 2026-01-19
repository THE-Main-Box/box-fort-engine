package official.sketchBook.engine.components_related.movement;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;

public class MovableObjectPhysicsComponent extends PhysicsComponent {

    private MovableObjectII mob;

    public MovableObjectPhysicsComponent(
        PhysicalObjectII object,
        int categoryBit,
        int maskBit,
        float density,
        float frict,
        float rest
    ) {
        super(
            object,
            categoryBit,
            maskBit,
            density,
            frict,
            rest
        );
        this.mob = (MovableObjectII) object;
    }

    public void update(float deltaTime) {
        applyImpulseForSpeed(
            mob.getMoveC().xSpeed,
            mob.getMoveC().ySpeed,
            mob.getMoveC().xMaxSpeed,
            mob.getMoveC().yMaxSpeed
        );
    }

    @Override
    public void postUpdate() {
        super.postUpdate();
        limitVelocity(
            mob.getMoveC().xMaxSpeed,
            mob.getMoveC().yMaxSpeed
        );
    }

    @Override
    public void nullifyReferences() {
        super.nullifyReferences();
        this.mob = null;
    }
}
