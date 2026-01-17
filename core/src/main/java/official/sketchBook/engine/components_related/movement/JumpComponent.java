package official.sketchBook.engine.components_related.movement;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.JumpCapableObjectII;
import official.sketchBook.engine.components_related.objects.TimerComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class JumpComponent implements Component {

    /// Flag de estado de pulo
    private boolean
        jumping,
        falling,
        jumpedFromGround,
        coyoteConsumed;

    /// Escala de modificação de gravidade, funciona apenas se existir um mundo físico
    private float
        defGravityScale,
        enhancedGravityScale;

    /// Valor de impulso de pulo
    public float
        jumpForce,
        fallSpeedAfterJCancel;

    /// Determina se devemos aplicar uma escala de gravidade diferente
    private boolean enhancedGravity;

    /// Determina se devemos acumular velocidade vertical no pulo ou resetar ela
    public boolean superJump;

    /// Objeto que pode saltar
    private JumpCapableObjectII object;
    /// Componente de movimentação existente
    private MovementComponent moveC;
    /// Componente de física
    private PhysicsComponent physicsC;
    /// Corpo físico necessário para o componente de pulo
    private Body body;

    /// Buffer para dizer se estávamos no chão no último frame
    private boolean prevOnGround;
    /// Buffer para dizer se aterrissamos no chão neste frame
    private boolean landedThisFrame;

    /// Valida se estamos no estado de aterrissagem
    private TimerComponent landBuffer;
    /// Valida se podemos usar o coyoteTime
    private TimerComponent coyoteTimer;
    /// Valida se podemos usar um pulo mesmo após termos saído do chão
    private TimerComponent jumpBufferTimer;

    private boolean disposed = false;

    /**
     * Componente de pulo, os valores de impulso devem ser passados como pixels e serão convertidos para metros
     *
     * @param object                entidade dona do componente
     * @param jumpForce             força de pulo da entidade
     * @param fallSpeedAfterJCancel Força para terminar o pulo, quanto maior o número,
     *                              mais forte o pulo deve ser para poder parar ele
     * @param coyoteTimeTarget      tempo disponível para o jogador pular após começar a cair
     * @param jumpBufferTime        tempo que o jogador tem para entrar no estado de pulo,
     *                              por pressionar o pulo uma única vez
     * @param landBufferTime        tempo de recuperação após aterrar no chão
     * @param defGravityScale       escala de gravidade padrão
     * @param enhancedGravityScale  escala de gravidade a ser aplicada na queda
     * @param superJump             Se devemos acumular velocidade vertical durante o pulo
     */
    public JumpComponent(
        JumpCapableObjectII object,
        float jumpForce,
        float fallSpeedAfterJCancel,
        float coyoteTimeTarget,
        float jumpBufferTime,
        float landBufferTime,
        float defGravityScale,
        float enhancedGravityScale,
        boolean superJump
    ) {
        this.object = object;
        this.body = object.getBody();
        this.moveC = object.getMoveC();
        this.physicsC = object.getPhysicsC();

        this.coyoteTimer = new TimerComponent(coyoteTimeTarget);
        this.landBuffer = new TimerComponent(landBufferTime);
        this.jumpBufferTimer = new TimerComponent(jumpBufferTime);

        //Se a gravidade aplicada for diferente que a gravidade especial
        this.superJump = superJump;

        this.jumpForce = jumpForce / PPM;
        this.fallSpeedAfterJCancel = fallSpeedAfterJCancel / PPM;

        setGravityValues(
            defGravityScale,
            enhancedGravityScale
        );

        this.jumping = false;
        this.falling = false;
        this.jumpedFromGround = false;
        this.coyoteConsumed = false;
    }

    @Override
    public void update(float delta) {
        jumpBufferTimer.update(delta);
        coyoteTimer.update(delta);
        landBuffer.update(delta);
    }

    @Override
    public void postUpdate() {
        updateJump();
        applyEnhancedGravity();

        updateLandedFlag();
    }

    private void updateLandedFlag() {
        boolean currentlyOnGround = object.isOnGround();

        // detecta aterrissagem: só se estava no ar ANTES e caiu (falling)
        landedThisFrame = !prevOnGround && currentlyOnGround;

        prevOnGround = currentlyOnGround;

        if (landedThisFrame) {
            landBuffer.reset();
            landBuffer.start();
        }

        landBuffer.resetByFinished();
    }

    /// Alteramos o valor da escala de gravidade do corpo físico do objeto com base em valores pré-determinados
    private void applyEnhancedGravity() {
        if (!enhancedGravity) return;

        if (jumpedFromGround && falling) {
            if (body.getGravityScale() == defGravityScale) {
                body.setGravityScale(enhancedGravityScale);
            }
        } else if (object.isOnGround()) {
            if (body.getGravityScale() != defGravityScale) {
                body.setGravityScale(defGravityScale);
            }
        }
    }

    private void updateJump() {
        updateJumpingFallingFlags();
        updateJumpedFlag();
        updateCoyoteState();

        updateJumpBasedOnBuffer();
    }

    /// atualiza se o objeto está caindo, ou pulando ou no chão
    private void updateJumpingFallingFlags() {
        boolean onGround = object.isOnGround();
        float vy = physicsC.getTmpVel().y;
        moveC.ySpeed = vy * PPM;

        if (onGround) {
            jumping = false;
            falling = false;
            coyoteConsumed = false;
        } else if (vy > 0) {
            jumping = true;
            falling = false;
        } else if (vy < 0) {
            falling = true;
            jumping = false;
        }
    }

    /// atualiza o estado referido a ter pulado ou não do chão
    private void updateJumpedFlag() {
        if (object.isOnGround() && jumpedFromGround) {
            jumpedFromGround = false;

        } else if (!object.isOnGround() && jumping) {
            jumpedFromGround = true;
        }
    }

    /**
     * Atualiza o estado do coyoteTimer
     * caso o objeto esteja a cair, não tenha pulado e o temporizador ainda não iniciou iniciamos ele,
     * mas isso apenas caso ainda não tenhamos usado o primeiro pulo do coyote
     * isso porque se não fazer isso o temporizador do coyoteTiming irá ser executado várias vezes devido
     * às circunstâncias semelhantes a usar o pulo do coyote e não fazer isso,
     * fazendo o temporizador reiniciar toda a vez que estivermos a cair
     */
    private void updateCoyoteState() {

        if (falling && !jumpedFromGround && !coyoteTimer.isRunning() && !coyoteConsumed) {
            coyoteTimer.reset();
            coyoteTimer.start();

            coyoteConsumed = true;
        }

        //impede que o coyoteTiming exceda seus limites ao resetarmos quando tocamos no chão pulamos oou o tempo acabe
        if (coyoteTimer.isFinished() || object.isOnGround() || jumpedFromGround || coyoteConsumed) {
            coyoteTimer.stop();
            coyoteTimer.reset();
        }
    }

    private void updateJumpBasedOnBuffer() {
        jumpBufferTimer.resetByFinished();

        if (jumpBufferTimer.isRunning()) {
            if (object.canJump()) {
                executeJump(false); // Executa o pulo real
                jumpBufferTimer.stop();
                jumpBufferTimer.reset();
            }
        }
    }

    public void jump(boolean cancel) {
        if (!cancel) {
            if (object.isOnGround()) {//Se estivermos no chão já executamos o pulo
                executeJump(false);
            } else {//Caso precisemos executar um pulo, e não estejamos no chão, preparamos o buffer
                jumpBufferTimer.reset();
                jumpBufferTimer.start();
            }
        } else {
            if (jumping) {// cancelamos o pulo caso estejamos ainda no processo de salto
                executeJump(true);

                //Preparamos o buffer para o próximo uso
                jumpBufferTimer.stop();
                jumpBufferTimer.reset();

            }

        }

    }

    /// Pula ou cancela um pulo
    private void executeJump(boolean cancel) {
        if (!cancel && moveC.canMoveY) {
            if (object.canJump()) {

                //zera a velocidade vertical caso ela seja negativa antes de pular para evitar um pulo fraco
                //ou zeramos para evitar um superPulo sem a intenção
                if (moveC.ySpeed < 0 ||
                    moveC.ySpeed > 0 && !superJump
                ) {
                    body.setLinearVelocity(
                        physicsC.getTmpVel().x,
                        0
                    );
                }

                // Aplica o impulso inicial do pulo
                physicsC.applyTrajectoryImpulse(
                    jumpForce,
                    0
                );

                jumpedFromGround = true;
                jumping = true;
                falling = false;

                coyoteTimer.stop();
                coyoteTimer.reset();
            }
        } else {
            if (jumping && moveC.ySpeed / PPM> fallSpeedAfterJCancel) {
                body.setLinearVelocity(
                    physicsC.getTmpVel().x,
                    fallSpeedAfterJCancel
                );
            }
        }
    }

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        object = null;
        body = null;

        moveC = null;
        physicsC = null;

        landBuffer = null;
        coyoteTimer = null;
        jumpBufferTimer = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public boolean isJumping() {
        return jumping;
    }

    public boolean isFalling() {
        return falling;
    }

    public boolean hasLanded() {
        return landBuffer.isRunning() && !landBuffer.isFinished();
    }

    public boolean isCoyoteJumpAvailable() {
        return coyoteTimer.isRunning();
    }

    public boolean isLandedThisFrame() {
        return landedThisFrame;
    }

    public void setGravityValues(float defGravityScale, float enhancedGravityScale) {
        this.defGravityScale = defGravityScale;
        this.enhancedGravityScale = enhancedGravityScale;
        this.enhancedGravity = defGravityScale != enhancedGravityScale;
    }

    public float getDefGravityScale() {
        return defGravityScale;
    }

    public float getEnhancedGravityScale() {
        return enhancedGravityScale;
    }


}
