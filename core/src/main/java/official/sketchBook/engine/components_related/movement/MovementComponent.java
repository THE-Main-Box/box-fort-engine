package official.sketchBook.engine.components_related.movement;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;

public class MovementComponent implements Component {

    /// Referencia ao objeto capaz de se mover
    private MovableObjectII mob;

    public final MovementDataComponent dataComponent;

    /// Esta variavel determina se a velocidade é aplicada no objeto de forma direta
    public final boolean autoApplyMovement;

    private boolean disposed = false;

    public MovementComponent(
        MovableObjectII mob,
        float xMaxMoveSpeed,
        float yMaxMoveSpeed,
        float rMaxMoveSpeed,
        float xMaxSpeed,
        float yMaxSpeed,
        float rMaxSpeed,
        float xDeceleration,
        float yDeceleration,
        float rDeceleration,
        boolean canMoveX,
        boolean canMoveY,
        boolean canRotate,
        boolean canAccelerateX,
        boolean canAccelerateY,
        boolean canAccelerateR,
        boolean canDeAccelerateX,
        boolean canDeAccelerateY,
        boolean canDeAccelerateR,
        boolean autoApplyMovement,
        boolean gravityAffected
    ) {
        this.mob = mob;

        this.dataComponent = new MovementDataComponent(
            xMaxMoveSpeed,
            yMaxMoveSpeed,
            rMaxMoveSpeed,
            xMaxSpeed,
            yMaxSpeed,
            rMaxSpeed,
            xDeceleration,
            yDeceleration,
            rDeceleration,
            canMoveX,
            canMoveY,
            canRotate,
            canAccelerateX,
            canAccelerateY,
            canAccelerateR,
            canDeAccelerateX,
            canDeAccelerateY,
            canDeAccelerateR,
            gravityAffected
        );

        this.autoApplyMovement = autoApplyMovement;
    }

    @Override
    public void update(float delta) {

        dataComponent.updateAndConstraintAllAxis(delta);

        if (autoApplyMovement) {
            applyMovementToMob(delta);
        }
    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void initObject() {

    }

    protected void applyMovementToMob(float delta) {
        mob.getTransformC().x += dataComponent
            .xAxis.velocity * delta
        ;

        mob.getTransformC().y += dataComponent.
            yAxis.velocity * delta;

        mob.getTransformC().rotation += dataComponent.
            rAxis.velocity * delta;

    }

    @Override
    public void dispose() {
        if (disposed) return;

        dataComponent.dispose();

        nullifyReferences();
        disposed = true;
    }

    public void nullifyReferences() {
        this.mob = null;
    }

}
