package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;

import static official.sketchBook.game.util_related.constants.PhysicsC.PPM;

public class MovableObjectPhysicsComponent extends PhysicsComponent {

    private final MovableObjectII mob;

    public MovableObjectPhysicsComponent(PhysicalObjectII object) {
        super(object);
        this.mob = (MovableObjectII) object;
    }

    public void update(float deltaTime) {
        applyMovementVelocity(
            mob.getMoveC().getxSpeed(),
            mob.getMoveC().getySpeed(),
            mob.getMoveC().getxMaxSpeed(),
            mob.getMoveC().getyMaxSpeed()
        );

    }

    /**
     * Aplica a velocidade do MovementComponent diretamente no corpo do Box2D
     * <p>
     * EXPLICAÇÃO CRÍTICA:
     * - O MovementComponent JÁ faz a desaceleração (fricção)
     * - Não precisamos de impulsos (que conflitam com inércia)
     * - Apenas SINCRONIZAMOS a velocidade do corpo com a velocidade calculada
     *
     * @param xSpeed    velocidade horizontal em píxels (já desacelerada pelo MovementComponent)
     * @param ySpeed    velocidade vertical em píxels (já desacelerada pelo MovementComponent)
     * @param maxXSpeed velocidade máxima horizontal permitida em píxels
     * @param maxYSpeed velocidade máxima vertical permitida em píxels
     */
    public void applyMovementVelocity(float xSpeed, float ySpeed, float maxXSpeed, float maxYSpeed) {
        if (body == null) return;

        // Convertemos os valores do MovementComponent (em píxels) para metros (Box2D)
        float targetX = limitAndConvertSpeedToMeters(xSpeed, maxXSpeed);
        float targetY = limitAndConvertSpeedToMeters(ySpeed, maxYSpeed);

        /// PONTO CRÍTICO: Aplicamos a velocidade DIRETAMENTE, sem impulso
        /// Isso faz o corpo acompanhar exatamente o MovementComponent
        body.setLinearVelocity(targetX, targetY);
    }

    /**
     * Converte velocidade de píxels para metros e a limita pela velocidade máxima
     *
     * @param speedToApply velocidade em píxels
     * @param maxSpeed     velocidade máxima em píxels
     * @return velocidade limitada em metros (para Box2D)
     */
    public final float limitAndConvertSpeedToMeters(float speedToApply, float maxSpeed) {
        // Limita primeiramente em píxels
        float limited = Math.max(-maxSpeed, Math.min(speedToApply, maxSpeed));
        // Depois converte para metros (Box2D trabalha em metros)
        return limited / PPM;
    }
}
