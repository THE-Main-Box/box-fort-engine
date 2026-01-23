package official.sketchBook.engine.components_related.movement;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;

public class MovementComponent implements Component {

    /// Referencia ao objeto capaz de se mover
    private MovableObjectII mob;

    /// Valores de velocidade
    public float
        xSpeed,             //Velocidade do eixo x (m|px/s)
        ySpeed,             //Velocidade do eixo y (m|px/s)
        rSpeed;             //Velocidade de rotação (rad/s)

    /// Limite de velocidade
    public float
        xMaxSpeed,          //Limite de velocidade do eixo X (m|px/s)
        yMaxSpeed,          //Limite de velocidade do eixo y (m|px/s)
        rMaxSpeed;          //Limite de velocidade de rotação (rad/s)

    /// Valores de aceleração (input, sempre vem em pixels, exceto pelo rad que é em radianos)
    public float
        xAccel,             //Aceleração do eixo x (m|px/s)
        yAccel,             //Aceleração do eixo y (m|px/s)
        rAccel;             //Aceleração de rotação (rad/s)

    /// Valores de desaceleração
    public float
        xDeceleration,          //Desaceleração do eixo x (m|px/s)
        yDeceleration,          //Desaceleração do eixo x (m|px/s)
        rDeceleration;          //Desaceleração de rotação (rad/s)

    /// Flags que determinam se podemos nos mover nos eixos respectivos
    public boolean
        canMoveX,           //Se podemos nos mover no eixo x
        canMoveY,           //Se podemos nos mover no eixo y
        gravityAffected,    //Se podemos ser afetados pela gravidade (quem aplica é o componente de física)
        canRotate;          //Se podemos rotacionar usando o componente de movimento

    /// Flags para determinar se podemos manter uma aceleração nos eixos respectivos
    public boolean
        canAccelerateX,         //Se podemos acelerar no eixo x
        canAccelerateY,         //Se podemos acelerar no eixo y
        canAccelerateR;         //Se podemos acelerar a rotação

    /// Flags para determinar se podemos realizar uma desaceleração nos eixos respectivos
    public boolean
        canDeAccelerateX,           //Se podemos desacelerar no eixo x
        canDeAccelerateY,           //Se podemos desacelerar no eixo y
        canDeAccelerateR;           //Se podemos desacelerar a rotação

    /// Esta variavel determina se a velocidade é aplicada no objeto de forma direta
    public final boolean autoApplySpeed;

    private boolean disposed = false;

    public MovementComponent(
        MovableObjectII mob,
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
        boolean autoApplySpeed,
        boolean gravityAffected
    ) {
        this.mob = mob;

        this.xMaxSpeed = xMaxSpeed;
        this.yMaxSpeed = yMaxSpeed;
        this.rMaxSpeed = rMaxSpeed;

        this.xDeceleration = xDeceleration;
        this.yDeceleration = yDeceleration;
        this.rDeceleration = rDeceleration;

        this.canMoveX = canMoveX;
        this.canMoveY = canMoveY;
        this.canRotate = canRotate;

        this.canAccelerateX = canAccelerateX;
        this.canAccelerateY = canAccelerateY;
        this.canAccelerateR = canAccelerateR;

        this.canDeAccelerateX = canDeAccelerateX;
        this.canDeAccelerateY = canDeAccelerateY;
        this.canDeAccelerateR = canDeAccelerateR;

        this.autoApplySpeed = autoApplySpeed;
        this.gravityAffected = gravityAffected;
    }

    @Override
    public void update(float delta) {

        xSpeed = updateAxis(
            xSpeed,
            xAccel,
            xMaxSpeed,
            xDeceleration,
            canMoveX,
            canAccelerateX,
            canDeAccelerateX,
            delta
        );

        ySpeed = updateAxis(
            ySpeed,
            yAccel,
            yMaxSpeed,
            yDeceleration,
            canMoveY,
            canAccelerateY,
            canDeAccelerateY,
            delta
        );

        rSpeed = updateAxis(
            rSpeed,
            rAccel,
            rMaxSpeed,
            rDeceleration,
            canRotate,
            canAccelerateR,
            canDeAccelerateR,
            delta
        );

        if (!canRotate)
            resetRotation();
        if (!canMoveX)
            resetXMovement();
        if (!canMoveY)
            resetYMovement();

        if (autoApplySpeed) {
            applyMovementToMob(delta);
        }
    }

    @Override
    public void postUpdate() {

    }

    protected void applyMovementToMob(float delta) {
        mob.getTransformC().x = (
            mob.getTransformC().x + this.xSpeed * delta
        );

        mob.getTransformC().y = (
            mob.getTransformC().y + this.ySpeed * delta
        );

    }

    private float updateAxis(
        float speed,
        float accel,
        float maxSpeed,
        float deceleration,
        boolean canMove,
        boolean canAccelerate,
        boolean canDeAccelerate,
        float delta
    ) {
        if (!canMove) return 0;


        if (canAccelerate && accel != 0) {
            speed += accel;
        } else if (canDeAccelerate && speed != 0) {
            speed = applyDeceleration(speed, deceleration * delta);
        }

        return clamp(speed, maxSpeed);
    }

    private float clamp(float value, float max) {
        if (value > max) return max;
        return Math.max(value, -max);
    }

    /// Aplica a desaceleração artificial
    private float applyDeceleration(float speed, float deceleration) {
        if (speed == 0 || deceleration == 0) return 0;

        // Se a velocidade é menor que o deceleration, zera
        if (Math.abs(speed) <= deceleration) return 0;

        return speed - deceleration * Math.signum(speed);
    }

    /// Reseta a movimentação no eixo x de aceleração e velocidade
    public void resetXMovement() {
        this.xSpeed = 0;
        this.xAccel = 0;
    }

    /// Reseta a movimentação no eixo y de aceleração e velocidade
    public void resetYMovement() {
        this.ySpeed = 0;
        this.yAccel = 0;
    }

    /// Reseta os valores de movimento de rotação
    public void resetRotation() {
        this.rSpeed = 0;
        this.rAccel = 0;
    }

    /// Verifica se existe aceleração armazenada no eixo x
    public boolean isAcceleratingX() {
        return xAccel != 0;
    }

    /// Verifica se existe aceleração armazenada no eixo y
    public boolean isAcceleratingY() {
        return yAccel != 0;
    }

    /// Verifica se temos aceleração armazenada no eixo de rotação
    public boolean isAcceleratingRotation() {
        return rAccel != 0;
    }

    /// Verifica se existe velocidade armazenada no eixo x
    public boolean isMovingX() {
        return xSpeed != 0;
    }

    /// Verifica se existe velocidade armazenada no eixo y
    public boolean isMovingY() {
        return ySpeed != 0;
    }

    /// Verifica se temos velocidade armazenada no eixo de rotação
    public boolean isRotating() {
        return rSpeed != 0;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    public void nullifyReferences() {
        this.mob = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
