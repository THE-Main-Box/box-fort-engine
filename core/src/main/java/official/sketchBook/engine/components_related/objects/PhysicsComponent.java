package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;

import static official.sketchBook.game.util_related.constants.PhysicsC.PPM;

public class PhysicsComponent implements Component {

    /// Objeto dono do componente
    protected PhysicalObjectII object;
    /// Corpo do objeto
    protected Body body;
    /// Componente de transformação
    protected TransformComponent transformC;

    /// Buffer da posição do corpo
    protected final Vector2 tmpPos;

    /// Buffer de impulso a ser aplicado no corpo
    protected final Vector2 tmpImpulse;

    /// Buffer de velocidade a ser aplicado no corpo
    protected final Vector2 tmpVel;

    private boolean disposed = false;

    /// Flag para determinar se o PhysicsComponent deve sincronizar a posição do corpo com o objeto
    /// Desativar isso permite usar Box2D como sistema principal e ignorar MovementComponent
    protected boolean syncPositionFromBody = true;

    public PhysicsComponent(PhysicalObjectII object) {
        this.object = object;
        this.body = object.getBody();
        this.transformC = object.getTransformC();

        this.tmpImpulse = new Vector2();
        this.tmpVel = new Vector2();
        this.tmpPos = new Vector2();
    }

    /// Aplicamos um impulso diretamente no centro do corpo do objeto
    public final void applyImpulse(Vector2 impulse) {
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    /// Sincroniza a posição do corpo com a posição do objeto (transformação)
    /// Use com CUIDADO: desative se quiser usar Box2D como sistema principal de física
    public final void syncBodyObjectPos() {
        if (disposed || body == null || !syncPositionFromBody) return;

        updatePosBuffer();

        transformC.setX(
            (tmpPos.x * PPM) - (transformC.getWidth() / 2f)
        );
        transformC.setY(
            (tmpPos.y * PPM) - (transformC.getHeight() / 2f)
        );

        float bodyAngleDeg = body.getAngle() * MathUtils.radiansToDegrees;
        if (Math.abs(transformC.getRotation() - bodyAngleDeg) > 0.01f) {
            transformC.setRotation(bodyAngleDeg);
        }
    }

    /// Sincroniza a posição do objeto com a posição do corpo
    /// Use quando quiser que o Box2D seja a fonte da verdade de posição
    public final void syncObjectBodyPos() {
        if (disposed || body == null) return;

        updatePosBuffer();

        // Coloca o corpo na mesma posição do objeto
        body.setTransform(
            (transformC.getX() + transformC.getWidth() / 2f) / PPM,
            (transformC.getY() + transformC.getHeight() / 2f) / PPM,
            body.getAngle()
        );
    }
    public final void rotateBody(float deltaDegrees) {
        if (disposed || body == null) return;

        // 1. Otimização: Se o delta for irrelevante, nem processamos para poupar a CPU nativa
        if (Math.abs(deltaDegrees) < 0.0001f) return;

        // 2. Cálculo do ângulo em Radianos (Box2D Nativo)
        float deltaRad = deltaDegrees * MathUtils.degreesToRadians;
        float newAngleRad = body.getAngle() + deltaRad;

        // 3. Normalização (mantém o ângulo entre -PI e PI)
        if (newAngleRad > MathUtils.PI) newAngleRad -= MathUtils.PI2;
        else if (newAngleRad < -MathUtils.PI) newAngleRad += MathUtils.PI2;

        // 4. Aplicação sem alocação
        tmpPos.set(body.getPosition());
        body.setTransform(tmpPos, newAngleRad);
        body.setAngularVelocity(0);
    }

    public final void setBodyRotation(float degrees) {
        if (disposed || body == null) return;

        float normalizedDegrees = degrees % 360f;
        float angleRad = normalizedDegrees * MathUtils.degreesToRadians;

        tmpPos.set(body.getPosition());
        body.setTransform(tmpPos, angleRad);
        body.setAngularVelocity(0);
    }

    public final void applyTrajectoryImpulse(float height, float distance) {
        float gravity = Math.abs(body.getWorld().getGravity().y);
        float mass = body.getMass();

        float initialVelocityY = (float) Math.copySign(Math.sqrt(2 * gravity * Math.abs(height)), height);
        tmpImpulse.set(distance * mass, initialVelocityY * mass);
        applyImpulse(tmpImpulse);
    }

    /**
     * Aplicamos um impulso de acordo com a direção aplicada
     *
     * @param direction direção que devemos aplicar,
     *                  se for maior que 1 nos eixos x e y independentemente de ser negativo ou não,
     *                  normalizamos o vetor
     * @param magnitude força/magnitude que o corpo deve se mover naquela direção
     */
    public final void applyDirectionalImpulse(Vector2 direction, float magnitude) {
        tmpImpulse.set(direction);

        if (direction.len2() > 1) {
            tmpImpulse.nor();
        }
        applyImpulse(tmpImpulse.scl(magnitude * body.getMass()));
    }

    /// Limita a velocidade do corpo usando clamping
    /// Use quando quiser um controle suave da velocidade máxima
    public final void limitVelocity(float maxX, float maxY) {
        updateVelBuffer();

        float maxXInMeters = maxX / PPM;
        float maxYInMeters = maxY / PPM;

        float limitedX = Math.max(-maxXInMeters, Math.min(tmpVel.x, maxXInMeters));
        float limitedY = Math.max(-maxYInMeters, Math.min(tmpVel.y, maxYInMeters));

        if (Math.abs(tmpVel.x - limitedX) > 0.0001f || Math.abs(tmpVel.y - limitedY) > 0.0001f) {
            body.setLinearVelocity(limitedX, limitedY);
        }
    }

    /// Define a velocidade do corpo diretamente (sem impulso)
    /// Use para sistemas de movimento custom (como MovementComponent)
    public final void setVelocity(float vx, float vy) {
        if (disposed || body == null) return;
        body.setLinearVelocity(vx, vy);
    }

    /// Define a velocidade do corpo com conversão de píxels para metros
    /// Use quando tem valores em píxels (como do MovementComponent)
    public final void setVelocityFromPixels(float vxPixels, float vyPixels) {
        if (disposed || body == null) return;
        body.setLinearVelocity(vxPixels / PPM, vyPixels / PPM);
    }

    /// Zera a velocidade do corpo (parada completa)
    public final void stopMovement() {
        if (disposed || body == null) return;
        body.setLinearVelocity(0, 0);
    }

    /// Zera a velocidade angular (parada rotacional)
    public final void stopRotation() {
        if (disposed || body == null) return;
        body.setAngularVelocity(0);
    }

    /// Obtém a velocidade atual do corpo em píxels por segundo
    public final Vector2 getVelocityInPixels() {
        updateVelBuffer();
        tmpVel.scl(PPM);
        return tmpVel;
    }

    /// Habilita/desabilita a sincronização automática de posição do corpo → objeto
    /// Use DESATIVADO se quiser que Box2D seja o sistema principal de física
    public final void setSyncPositionFromBody(boolean sync) {
        this.syncPositionFromBody = sync;
    }

    protected final void updateVelBuffer() {
        tmpVel.set(body.getLinearVelocity());
    }

    protected final void updatePosBuffer() {
        tmpPos.set(body.getPosition());
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {
        syncBodyObjectPos();
        object.onObjectAndBodyPosSync();
    }

    @Override
    public void dispose() {
        if(body == null) return;

        body.getWorld().destroyBody(body);
        body = null;
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
