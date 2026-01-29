package official.sketchBook.engine.components_related.movement;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;

public class MovableObjectPhysicsComponent extends PhysicsComponent {

    private MovableObjectII mob;

    private float defaultGravityScale = -1;
    private boolean gravityWasAffected = true;
    public boolean autoApplyMovement = true;

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
        if (autoApplyMovement) {
            applyMovementToBody();
        }
        constraintMovementAxis();
        constraintRotation();
        constraintGravity();

    }

    public void applyMovementToBody() {
        //Aplica a movimentação no corpo
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

        //Limita a velocidade do corpo físico
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

    private void constraintRotation() {
        if (!mob.getMoveC().canRotate) {
            object.getBody().setAngularVelocity(0);
            object.getBody().setFixedRotation(true);
        } else {
            object.getBody().setFixedRotation(false);

            object.getBody().setAngularVelocity(
                mob.getMoveC().rSpeed
            );
        }
    }


    /// Impede a movimentação nos eixos
    private void constraintMovementAxis() {
        if (!mob.getMoveC().canMoveX && !mob.getMoveC().canMoveY) {
            stopMovement();
        } else if (!mob.getMoveC().canMoveX) {
            stopMovementX();
        } else if (!mob.getMoveC().canMoveY) {
            stopMovementY();
        }
    }

    /// Impede o efeito da gravidade no objeto caso não possamos nos mover no eixo y
    private void constraintGravity() {
        boolean shouldAffectGravity = mob.getMoveC().gravityAffected;

        // Só executa se mudou de estado
        if (gravityWasAffected != shouldAffectGravity) {
            if (!shouldAffectGravity) {
                defaultGravityScale = object.getBody().getGravityScale();
                object.getBody().setGravityScale(0);
            } else {
                object.getBody().setGravityScale(defaultGravityScale);
                defaultGravityScale = -1;
            }
            gravityWasAffected = shouldAffectGravity;
        }
    }

}
