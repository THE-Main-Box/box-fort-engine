package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.movement.JumpComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;

public interface JumpCapableObjectII {

    default void jump(boolean cancelJump){
        getJumpC().jump(cancelJump);
    }

    boolean canJump();
    boolean isOnGround();

    Body getBody();
    PhysicsComponent getPhysicsC();
    MovementComponent getMoveC();
    JumpComponent getJumpC();
}
