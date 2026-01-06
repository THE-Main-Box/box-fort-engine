package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;

public class MovementComponent implements Component {

    /// Referencia ao objeto capaz de se mover
    private final MovableObjectII mob;

    /// Valores de velocidade em metros
    private float xSpeed, ySpeed;

    /// Limite de velocidade em metros
    private float xMaxSpeed, yMaxSpeed;

    /// Valores de aceleração em metros
    private float xAccel, yAccel;

    /// Valores de desaceleração em metros
    private float xDeceleration, yDeceleration;

    /// Flags que determinam se podemos nos mover nos eixos respectivos
    private boolean canMoveX, canMoveY;

    /// Flags para determinar se podemos manter uma aceleração nos eixos respectivos
    private boolean canAccelerateX, canAccelerateY;

    /// se devemos deixar o sistema aplicar a velocidade no objeto automaticamente
    private final boolean autoApplySpeed;

    /// Valor de suavização de movimentação
    private float weight;

    private boolean disposed = false;

    public MovementComponent(
        MovableObjectII mob,
        float xMaxSpeed,
        float yMaxSpeed,
        float xDeceleration,
        float yDeceleration,
        boolean canMoveX,
        boolean canMoveY,
        boolean canAccelerateX,
        boolean canAccelerateY,
        boolean autoApplySpeed,
        float weight
    ) {
        this.mob = mob;

        this.xMaxSpeed = xMaxSpeed;
        this.yMaxSpeed = yMaxSpeed;

        this.xDeceleration = xDeceleration;
        this.yDeceleration = yDeceleration;

        this.canMoveX = canMoveX;
        this.canMoveY = canMoveY;

        this.canAccelerateX = canAccelerateX;
        this.canAccelerateY = canAccelerateY;

        this.autoApplySpeed = autoApplySpeed;

        this.weight = weight;
    }

    @Override
    public void update(float delta) {
        if (!canMoveX && !canMoveY) {
            return;
        }

        updateXAxis(delta);
        updateYAxis(delta);

        if (autoApplySpeed) {
            applyMovementToMob(delta);
        }
    }

    @Override
    public void postUpdate() {

    }

    public void applyMovementToMob(float delta) {
        mob.getTransformC().setX(
            mob.getTransformC().getX() + this.xSpeed * delta
        );

        mob.getTransformC().setY(
            mob.getTransformC().getY() + this.ySpeed * delta
        );

    }

    /// Atualização interna da movimentação do eixo x
    private void updateXAxis(float delta) {
        // 1. Constraint Mestra (Guard Clause)
        if (!canMoveX) {
            resetXMovement();
            return;
        }

        // 2. Fluxo de Aceleração
        if (canAccelerateX && isAcceleratingX()) {
            xSpeed += xAccel / weight;
            applyXSpeedClamp(); // Limita imediatamente após alteração
            return; // Sai da função pois já acelerou, não precisa de fricção
        }

        // 3. Fluxo de Deceleração (Inércia)
        // Se chegou aqui, ou canAccelerateX é false ou não há aceleração vindo do input
        xAccel = 0;
        if (isMovingX()) {
            xSpeed = applyFriction(
                xSpeed,
                xDeceleration * delta
            );
        }

        // 4. Constraint Final
        applyXSpeedClamp();
    }

    private void applyXSpeedClamp() {
        if (xSpeed > xMaxSpeed) {
            xSpeed = xMaxSpeed;
        } else if (xSpeed < -xMaxSpeed) {
            xSpeed = -xMaxSpeed;
        }
    }

    private void updateYAxis(float delta) {
        // 1. Constraint Mestra
        if (!canMoveY) {
            resetYMovement();
            return;
        }

        // 2. Fluxo de Aceleração
        if (canAccelerateY && isAcceleratingY()) {
            ySpeed += yAccel / weight;
            applyYSpeedClamp(); // Limita imediatamente após alteração
            return; // Sai da função pois já acelerou, não precisa de fricção
        }

        // 3. Fluxo de Deceleração (Inércia)
        // Se chegou aqui, ou canAccelerateX é false ou não há aceleração vindo do input
        yAccel = 0;
        if (isMovingY()) {
            ySpeed = applyFriction(
                ySpeed,
                yDeceleration * delta
            );
        }

        // 4. Constraint Final
        applyYSpeedClamp();

    }

    private void applyYSpeedClamp() {
        if (ySpeed > yMaxSpeed) {
            ySpeed = yMaxSpeed;
        }
        else if (ySpeed < -yMaxSpeed) {
            ySpeed = -yMaxSpeed;
        }
    }

    /// Aplica a desaceleração
    private float applyFriction(float speed, float deceleration) {
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

    /// Verifica se existe aceleração armazenada no eixo x
    public boolean isAcceleratingX() {
        return xAccel != 0;
    }

    /// Verifica se existe aceleração armazenada no eixo y
    public boolean isAcceleratingY() {
        return yAccel != 0;
    }

    /// Verifica se existe velocidade armazenada no eixo x
    public boolean isMovingX() {
        return xSpeed != 0;
    }

    /// Verifica se existe velocidade armazenada no eixo y
    public boolean isMovingY() {
        return ySpeed != 0;
    }


    public float getxSpeed() {
        return xSpeed;
    }

    public void setxSpeed(float xSpeed) {
        this.xSpeed = xSpeed;
    }

    public float getySpeed() {
        return ySpeed;
    }

    public void setySpeed(float ySpeed) {
        this.ySpeed = ySpeed;
    }

    public float getxMaxSpeed() {
        return xMaxSpeed;
    }

    public void setxMaxSpeed(float xMaxSpeed) {
        this.xMaxSpeed = xMaxSpeed;
    }

    public float getyMaxSpeed() {
        return yMaxSpeed;
    }

    public void setyMaxSpeed(float yMaxSpeed) {
        this.yMaxSpeed = yMaxSpeed;
    }

    public float getxAccel() {
        return xAccel;
    }

    public void setxAccel(float xAccel) {
        this.xAccel = xAccel;
    }

    public float getyAccel() {
        return yAccel;
    }

    public void setyAccel(float yAccel) {
        this.yAccel = yAccel;
    }

    public float getxDeceleration() {
        return xDeceleration;
    }

    public void setxDeceleration(float xDeceleration) {
        this.xDeceleration = xDeceleration;
    }

    public float getyDeceleration() {
        return yDeceleration;
    }

    public void setyDeceleration(float yDeceleration) {
        this.yDeceleration = yDeceleration;
    }

    public boolean isCanMoveX() {
        return canMoveX;
    }

    public void setCanMoveX(boolean canMoveX) {
        this.canMoveX = canMoveX;
    }

    public boolean isCanMoveY() {
        return canMoveY;
    }

    public void setCanMoveY(boolean canMoveY) {
        this.canMoveY = canMoveY;
    }

    public boolean isCanAccelerateX() {
        return canAccelerateX;
    }

    public void setCanAccelerateX(boolean canAccelerateX) {
        this.canAccelerateX = canAccelerateX;
    }

    public boolean isCanAccelerateY() {
        return canAccelerateY;
    }

    public void setCanAccelerateY(boolean canAccelerateY) {
        this.canAccelerateY = canAccelerateY;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
