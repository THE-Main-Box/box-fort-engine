package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;

public class MovementComponent implements Component {

    /// Referencia ao objeto capaz de se mover
    private final MovableObjectII mob;

    /// Valores de velocidade em metros
    public float xSpeed, ySpeed;

    /// Limite de velocidade em metros
    public float xMaxSpeed, yMaxSpeed;

    /// Valores de aceleração em metros
    public float xAccel, yAccel;

    /// Valores de desaceleração em metros
    public float xDeceleration, yDeceleration;

    /// Flags que determinam se podemos nos mover nos eixos respectivos
    public boolean canMoveX, canMoveY;

    /// Flags para determinar se podemos manter uma aceleração nos eixos respectivos
    public boolean canAccelerateX, canAccelerateY;

    /// Flags para determinar se podemos realizar uma desaceleração nos eixos respectivos
    public boolean canDeAccelerateX, canDeAccelerateY;

    /// Esta variavel determina se a velocidade é aplicada no objeto de forma direta
    public final boolean autoApplySpeed;

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
        boolean canDeAccelerateX,
        boolean canDeAccelerateY,
        boolean autoApplySpeed
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

        this.canDeAccelerateX = canDeAccelerateX;
        this.canDeAccelerateY = canDeAccelerateY;

        this.autoApplySpeed = autoApplySpeed;
    }

    @Override
    public void update(float delta) {
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
        mob.getTransformC().x = (
            mob.getTransformC().x + this.xSpeed * delta
        );

        mob.getTransformC().y = (
            mob.getTransformC().y + this.ySpeed * delta
        );

    }

    /// Atualização interna da movimentação do eixo x
    private void updateXAxis(float delta) {
        // Se não pudermos mover no eixo x, resetamos a movimentação do eixo x
        if (!canMoveX) {
            resetXMovement();
            return;
        }

        //Se pudermos acelerar no eixo x
        // e tivermos uma aceleração acumulada
        if (canAccelerateX && isAcceleratingX()) {
            //Aplicamos a aceleração na velocidade
            xSpeed += xAccel;
        } else { // se não pudermos acelerar, ou não tivermos aceleração sendo passada

            //Resetamos a aceleração para impedir que haja um fluxo de movimentação incoerente
            xAccel = 0;

            //Se houver velocidade no eixo x lidamos com a desaceleração
            if (isMovingX() && canDeAccelerateX) {
                //Aplicamos uma desaceleração no eixo x
                xSpeed = applyDeceleration(
                    xSpeed,
                    (xDeceleration * delta)
                );
            }
        }

        //Limitamos a velocidade no eixo x
        applyXSpeedClamp();
    }

    /// Mantém a velocidade do eixo x dentro do limite estabelecido quer seja maior ou menor que 0
    private void applyXSpeedClamp() {
        if (xSpeed > xMaxSpeed) {
            xSpeed = xMaxSpeed;
        } else if (xSpeed < -xMaxSpeed) {
            xSpeed = -xMaxSpeed;
        }
    }

    private void updateYAxis(float delta) {
        // Se não puder se mover no eixo y, resetamos a velocidade e aceleração do eixo
        if (!canMoveY) {
            resetYMovement();
            return;
        }

        // Se pudermos acelerar e tivermos aceleração armazenada no eixo y, aceleramos
        if (canAccelerateY && isAcceleratingY()) {
            ySpeed += yAccel;
        } else { //Caso não possamos acelerar ou não tenhamos aceleração no eixo y, começamos a desacelerar
            yAccel = 0;

            //Importante lembrar que se não houver velocidade não é preciso limitar nada
            if (isMovingY() && canDeAccelerateY) {

                ySpeed = applyDeceleration(
                    ySpeed,
                    (yDeceleration * delta)
                );
            }

        }

        //Limita a velocidade do eixo y
        applyYSpeedClamp();

    }

    /// Mantém a velocidade do eixo y dentro do limite estabelecido quer seja maior ou menor que 0
    private void applyYSpeedClamp() {
        if (ySpeed > yMaxSpeed) {
            ySpeed = yMaxSpeed;
        } else if (ySpeed < -yMaxSpeed) {
            ySpeed = -yMaxSpeed;
        }
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

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
