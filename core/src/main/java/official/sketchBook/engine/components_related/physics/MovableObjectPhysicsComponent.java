package official.sketchBook.engine.components_related.physics;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.objects.AxisData;

public class MovableObjectPhysicsComponent extends PhysicsComponent {

    private MovableObjectII mob;

    private AxisData
        xAxis,
        yAxis,
        rAxis;

    private float defaultGravityScale = 0;
    private boolean gravityWasAffected = true;

    /// Flag de constraint
    public boolean
        autoConstraintR = true,
        autoApplyMovement = true;

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

        this.xAxis = new AxisData();
        this.yAxis = new AxisData();
        this.rAxis = new AxisData();
    }

    private void updateAxisReferences(){
        if(mob.getMoveC() == null) return;

        this.xAxis.set(mob.getMoveC().dataComponent.xAxis);
        this.yAxis.set(mob.getMoveC().dataComponent.yAxis);
        this.rAxis.set(mob.getMoveC().dataComponent.rAxis);
    }

    public void update(float deltaTime) {
        super.update(deltaTime);

        updateAxisReferences();

        if (autoApplyMovement) {
            applyMovement();
        }
        constraintMovementAxis();
        constraintRotation();
        constraintGravity();

    }

    protected void applyMovement() {
        applyMovementToBodyByImpulse();
    }

    public void applyMovementToBodyByImpulse() {
        //Aplica a movimentação no corpo
        applyImpulseForSpeed(
            xAxis.velocity,
            yAxis.velocity,

            xAxis.maxVel,
            yAxis.maxVel
        );
    }

    @Override
    public void postUpdate() {
        super.postUpdate();

        //Limita a velocidade do corpo físico
        limitVelocity(
            xAxis.maxVel,
            yAxis.maxVel
        );
    }

    @Override
    public void nullifyReferences() {
        super.nullifyReferences();
        this.mob = null;
    }

    private void constraintRotation() {
        if (!rAxis.canMove) {
            object.getBody().setAngularVelocity(0);
            object.getBody().setFixedRotation(true);
        } else {
            object.getBody().setFixedRotation(false);

            if (autoConstraintR)
                object.getBody().setAngularVelocity(
                    rAxis.velocity
                );
        }
    }


    /// Impede a movimentação nos eixos
    private void constraintMovementAxis() {
        if (!xAxis.canMove && !yAxis.canMove) {
            stopMovement();
        } else if (!xAxis.canMove) {
            stopMovementX();
        } else if (!yAxis.canMove) {
            stopMovementY();
        }
    }

    private void constraintGravity() {
        boolean shouldAffectGravity = mob.getMoveC().dataComponent.gravityAffected;

        if (!shouldAffectGravity) {
            if (gravityWasAffected) {
                defaultGravityScale = mob.getMoveC().dataComponent.gravityScale; // backup lógico
                object.getBody().setGravityScale(0);
                gravityWasAffected = false;
            }
            return;
        }

        // Gravidade ligada
        if (!gravityWasAffected) {
            object.getBody().setGravityScale(defaultGravityScale);
            gravityWasAffected = true;
        }

        float targetGravity = mob.getMoveC().dataComponent.gravityScale;
        if (object.getBody().getGravityScale() != targetGravity) {
            object.getBody().setGravityScale(targetGravity);
        }
    }


}
