package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;

import static official.sketchBook.game.util_related.constants.PhysicsC.PPM;

public class MovableObjectPhysicsComponent extends PhysicsComponent {

    private final MovableObjectII mob;

    public MovableObjectPhysicsComponent(PhysicalObjectII object) {
        super(object);
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

}
