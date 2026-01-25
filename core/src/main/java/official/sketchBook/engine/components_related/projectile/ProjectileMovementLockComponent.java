package official.sketchBook.engine.components_related.projectile;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.util_related.enumerators.Direction;

public class ProjectileMovementLockComponent implements Component {

    /// Flags de configuração
    public boolean
        autoApplyLock,
        stickToAnyCollision,    //Se deve parar de se mover ao colidir de qualquer direção
        stickToUpCollision,     //Se deve parar de se mover ao colidir pra cima
        stickToDownCollision,   //Se deve parar de se mover ao colidir pra baixo
        stickToLeftCollision,   //Se deve parar de se mover ao colidir pra esquerda
        stickToRightCollision;  //Se deve parar de se mover ao colidir pra direita

    /// Flag de estado original
    private final boolean
        gravityAffected,
        canMoveX,
        canMoveY,
        canRotate;

    /// Flag auxiliar
    private boolean stuck;

    private MovementComponent moveC;
    private ProjectileControllerComponent controllerC;

    private boolean disposed = false;

    public ProjectileMovementLockComponent(
        ProjectileControllerComponent controllerC,
        MovementComponent moveC
    ) {
        this.controllerC = controllerC;
        this.moveC = moveC;

        this.gravityAffected = moveC.gravityAffected;
        this.canMoveX = moveC.canMoveX;
        this.canMoveY = moveC.canMoveY;
        this.canRotate = moveC.canRotate;

        this.autoApplyLock = true;
    }

    @Override
    public void update(float delta) {
        if (!autoApplyLock) return;
        updateConstraints();
    }

    @Override
    public void postUpdate() {

    }

    public void updateConstraints() {
        unlockMovement();

        if (controllerC.isColliding()) {
            constraintMovement();
        }

    }

    private void constraintMovement() {
        Direction colDir = controllerC.lastCollisionStartBuffer.lastDirection;

        if (     //Se tivermos batido
            stickToAnyCollision
                ||  //Se tivermos batido pra cima e tivermos uma constraint
                colDir.isUp() && stickToUpCollision
                ||  //Se tivermos batido pra baixo e tivermos uma constraint
                colDir.isDown() && stickToDownCollision
                ||  //Se tivermos batido a esquerda e tivermos uma constraint
                colDir.isLeft() && stickToLeftCollision
                ||//Se tivermos batido a direita e tivermos uma constraint
                colDir.isRight() && stickToRightCollision
        ) {
            lockMovement();
        }

    }

    /// Usa a pipeline de movimento para impedir a movimentação do projétil
    public void lockMovement() {
        //Se já estivermos presos não adianta chamar novamente
        if (stuck) return;

        moveC.canMoveY = false;         //Impede a alteração de velocidade do eixo y
        moveC.canMoveX = false;         //Impede a alteração de velocidade do eixo x
        moveC.canRotate = false;        //Impede a alteração de velocidade de rotação
        moveC.gravityAffected = false;  //Impede que a gravidade afete o projétil

        //Reseta todas as velocidades para que não haja movimento residual
        moveC.resetXMovement();
        moveC.resetYMovement();
        moveC.resetRMovement();

        //Marca que estamos travados sem poder nos mover
        stuck = true;
    }

    /// Usa a pipeline de movimento para permitir novamente que o projétil se mova
    public void unlockMovement() {
        //Se já estivermos livres não adianta chamar novamente
        if (!stuck) return;

        //Passa as configurações originais de movimentação
        moveC.canMoveX = canMoveX;
        moveC.canMoveY = canMoveY;
        moveC.canRotate = canRotate;
        moveC.gravityAffected = gravityAffected;

        //Marca que não estamos mais travados e que podemos nos mover
        stuck = false;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        moveC = null;
        controllerC = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
