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
        applyAcceleration();
        applyDeceleration();
        limitSpeed();

        enforceSpeedAxisConstraints();
    }

    @Override
    public void postUpdate() {

    }

    protected void applyAcceleration() {
        enforceXAcceleration();
        enforceYAcceleration();
    }

    /// Aplica a aceleração na velocidade x suavizada pela weight
    protected void enforceXAcceleration() {
        if (canAccelerateX && canMoveX) {   //Se pudermos acelerar e nos mover
            xSpeedInMeters += xAccelInMeters / weight;
        } else {
            xAccelInMeters = 0;             //Caso não possamos acelerar colocamos a aceleração em 0
        }
    }

    /// Aplica a aceleração na velocidade y suavizada pela weight
    protected void enforceYAcceleration() {
        if (canAccelerateY && canMoveY) {   //Se pudermos nos acelerar e nos mover
            ySpeedInMeters += yAccelInMeters / weight;
        } else {
            yAccelInMeters = 0;             //Caso não possamos acelerar colocamos a aceleração em 0
        }
    }

    /// Limita velocidade
    private void limitSpeed() {
        enforceXSpeedLimit();
        enforceYSpeedLimit();
    }

    /// Limita a velocidade no eixo x
    protected void enforceXSpeedLimit() {
        //Caso não estejamos nos movendo no eixo x não precisamos limitar a velocidade
        if (!isMovingX()) return;

        //A velocidade x tem que estar dentro do limite pré-determinado
        xSpeedInMeters = Math.max(
            -xMaxSpeedInMeters,
            Math.min(
                xSpeedInMeters,
                xMaxSpeedInMeters
            )
        );
    }

    /// Limita a velocidade no eixo y
    protected void enforceYSpeedLimit() {
        //Caso não estejamos nos movendo no eixo y não precisamos limitar a velocidade
        if (!isMovingY()) return;

        //A velocidade y tem que estar dentro do limite pré-determinado
        ySpeedInMeters = Math.max(
            -yMaxSpeedInMeters,
            Math.min(
                ySpeedInMeters,
                yMaxSpeedInMeters
            )
        );
    }

    protected void applyDeceleration() {
        enforceXDeceleration();
        enforceYDeceleration();
    }

    protected void enforceXDeceleration() {
        //Se estiver acelerando podemos ignorar
        if (isAcceleratingX()) return;

        xSpeedInMeters = applyFriction(
            xSpeedInMeters,
            xDeceleration
        );

    }

    protected void enforceYDeceleration() {
        //Se estiver acelerando podemos ignorar
        if (isAcceleratingY()) return;

        ySpeedInMeters = applyFriction(
            ySpeedInMeters,
            yDeceleration
        );

    }

    /// Impede que tenhamos uma velocidade armazenada caso não possamos nos mover
    protected void enforceSpeedAxisConstraints() {
        if (!canMoveX) resetXMovement();
        if (!canMoveY) resetYMovement();
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
