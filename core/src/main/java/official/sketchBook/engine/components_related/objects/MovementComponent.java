package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;

public class MovementComponent implements Component {

    /// Valores de velocidade em metros
    private float xSpeedInMeters, ySpeedInMeters;

    /// Limite de velocidade em metros
    private float xMaxSpeedInMeters, yMaxSpeedInMeters;

    /// Valores de aceleração em metros
    private float xAccelInMeters, yAccelInMeters;

    /// Valores de desaceleração em metros
    private float xDeceleration, yDeceleration;

    /// Flags que determinam se podemos nos mover nos eixos respectivos
    private boolean canMoveX, canMoveY;

    /// Flags para determinar se podemos manter uma aceleração nos eixos respectivos
    private boolean canAccelerateX, canAccelerateY;

    /// Valor de suavização de movimentação
    private float weight;

    private boolean disposed = false;

    public MovementComponent(
        float xMaxSpeedInMeters,
        float yMaxSpeedInMeters,
        float xDeceleration,
        float yDeceleration,
        boolean canMoveX,
        boolean canMoveY,
        boolean canAccelerateX,
        boolean canAccelerateY,
        float weight
    ) {
        this.xMaxSpeedInMeters = xMaxSpeedInMeters;
        this.yMaxSpeedInMeters = yMaxSpeedInMeters;
        this.xDeceleration = xDeceleration;
        this.yDeceleration = yDeceleration;
        this.canMoveX = canMoveX;
        this.canMoveY = canMoveY;
        this.canAccelerateX = canAccelerateX;
        this.canAccelerateY = canAccelerateY;
        this.weight = weight;
    }

    @Override
    public void update(float delta) {
        updateXAxis();
        updateYAxis();
    }

    @Override
    public void postUpdate() {

    }

    /// Atualização interna da movimentação do eixo x
    private void updateXAxis() {
        // 1. Constraint Mestra (Guard Clause)
        if (!canMoveX) {
            resetXMovement();
            return;
        }

        // 2. Fluxo de Aceleração
        if (canAccelerateX && isAcceleratingX()) {
            xSpeedInMeters += xAccelInMeters / weight;
            applyXSpeedClamp(); // Limita imediatamente após alteração
            return; // Sai da função pois já acelerou, não precisa de fricção
        }

        // 3. Fluxo de Deceleração (Inércia)
        // Se chegou aqui, ou canAccelerateX é false ou não há aceleração vindo do input
        xAccelInMeters = 0;
        if (isMovingX()) {
            xSpeedInMeters = applyFriction(xSpeedInMeters, xDeceleration);
        }

        // 4. Constraint Final
        applyXSpeedClamp();
    }

    private void applyXSpeedClamp() {
        if (xSpeedInMeters > xMaxSpeedInMeters) xSpeedInMeters = xMaxSpeedInMeters;
        else if (xSpeedInMeters < -xMaxSpeedInMeters) xSpeedInMeters = -xMaxSpeedInMeters;
    }

    private void updateYAxis() {
        // 1. Constraint Mestra
        if (!canMoveY) {
            resetYMovement();
            return;
        }

        // 2. Fluxo de Aceleração
        if (canAccelerateY && isAcceleratingY()) {
            ySpeedInMeters += yAccelInMeters / weight;
            applyYSpeedClamp(); // Limita imediatamente após alteração
            return; // Sai da função pois já acelerou, não precisa de fricção
        }

        // 3. Fluxo de Deceleração (Inércia)
        // Se chegou aqui, ou canAccelerateX é false ou não há aceleração vindo do input
        yAccelInMeters = 0;
        if (isMovingY()) {
            ySpeedInMeters = applyFriction(
                ySpeedInMeters,
                yDeceleration
            );
        }

        // 4. Constraint Final
        applyYSpeedClamp();

    }

    private void applyYSpeedClamp() {
        if (ySpeedInMeters > yMaxSpeedInMeters) ySpeedInMeters = yMaxSpeedInMeters;
        else if (ySpeedInMeters < -yMaxSpeedInMeters) ySpeedInMeters = -yMaxSpeedInMeters;
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
        this.xSpeedInMeters = 0;
        this.xAccelInMeters = 0;
    }

    /// Reseta a movimentação no eixo y de aceleração e velocidade
    public void resetYMovement() {
        this.ySpeedInMeters = 0;
        this.yAccelInMeters = 0;
    }

    /// Verifica se existe aceleração armazenada no eixo x
    public boolean isAcceleratingX() {
        return xAccelInMeters != 0;
    }

    /// Verifica se existe aceleração armazenada no eixo y
    public boolean isAcceleratingY() {
        return yAccelInMeters != 0;
    }

    /// Verifica se existe velocidade armazenada no eixo x
    public boolean isMovingX() {
        return xSpeedInMeters != 0;
    }

    /// Verifica se existe velocidade armazenada no eixo y
    public boolean isMovingY() {
        return ySpeedInMeters != 0;
    }


    public float getxSpeedInMeters() {
        return xSpeedInMeters;
    }

    public void setxSpeedInMeters(float xSpeedInMeters) {
        this.xSpeedInMeters = xSpeedInMeters;
    }

    public float getySpeedInMeters() {
        return ySpeedInMeters;
    }

    public void setySpeedInMeters(float ySpeedInMeters) {
        this.ySpeedInMeters = ySpeedInMeters;
    }

    public float getxMaxSpeedInMeters() {
        return xMaxSpeedInMeters;
    }

    public void setxMaxSpeedInMeters(float xMaxSpeedInMeters) {
        this.xMaxSpeedInMeters = xMaxSpeedInMeters;
    }

    public float getyMaxSpeedInMeters() {
        return yMaxSpeedInMeters;
    }

    public void setyMaxSpeedInMeters(float yMaxSpeedInMeters) {
        this.yMaxSpeedInMeters = yMaxSpeedInMeters;
    }

    public float getxAccelInMeters() {
        return xAccelInMeters;
    }

    public void setxAccelInMeters(float xAccelInMeters) {
        this.xAccelInMeters = xAccelInMeters;
    }

    public float getyAccelInMeters() {
        return yAccelInMeters;
    }

    public void setyAccelInMeters(float yAccelInMeters) {
        this.yAccelInMeters = yAccelInMeters;
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
