package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;

import static official.sketchBook.game.util_related.constants.PhysicsC.PPM;

public class PhysicsComponent implements Component {

    /// Objeto dono do componente
    protected PhysicalObjectII object;
    /// Corpo do objeto
    protected Body body;
    /// Componente de transformação do dono, referenciado
    protected TransformComponent transformC;

    /// Buffer da posição do corpo
    protected final Vector2 tmpPos;
    /// Buffer de impulso a ser aplicado no corpo
    protected final Vector2 tmpImpulse;
    /// Buffer de velocidade a ser aplicado no corpo
    protected final Vector2 tmpVel;

    /// Buffer de diferença de largura
    private final float halfWidth;
    /// Buffer de diferença de altura
    private final float halfHeight;

    private boolean disposed = false;

    public PhysicsComponent(PhysicalObjectII object) {
        this.object = object;
        this.body = object.getBody();
        this.transformC = object.getTransformC();

        this.tmpImpulse = new Vector2();
        this.tmpVel = new Vector2();
        this.tmpPos = new Vector2();


        this.halfWidth = transformC.getHalfWidth();
        this.halfHeight = transformC.getHalfHeight();
    }

    /**
     * Aplica um impulso para alcançar uma velocidade em específico
     * (todos os valores precisam ser em píxels já que serão convertidos em metros)
     *
     * @param xSpeed velocidade horizontal em píxels
     * @param maxXSpeed velocidade horizontal máxima permitida em píxels
     * @param ySpeed velocidade vertical em píxels
     * @param maxYSpeed velocidade vertical máxima permitida em píxels
     * */
    public void applyImpulseForSpeed(float xSpeed, float ySpeed, float maxXSpeed, float maxYSpeed) {
        if (body == null) return;

        updateVelBuffer();

        float desiredX = limitAndConvertSpeedToMeters(xSpeed, maxXSpeed, tmpVel.x);
        float desiredY = limitAndConvertSpeedToMeters(ySpeed, maxYSpeed, tmpVel.y);

        tmpVel.set(
            desiredX != 0 ? desiredX - tmpVel.x : 0,
            desiredY != 0 ? desiredY - tmpVel.y : 0
        );

        applyImpulse(tmpVel.scl(body.getMass()));
    }

    //converte os valores de velocidade em pixel para metros, e os limita com base em uma velocidade maxima passada
    public final float limitAndConvertSpeedToMeters(float speedToApply, float maxSpeed, float currentSpeed) {
        if (speedToApply != 0) {
            return Math.max(-maxSpeed / PPM, Math.min(speedToApply / PPM, maxSpeed / PPM));
        }
        return currentSpeed; // Se speed for 0, não altera a velocidade atual
    }

    /// Aplicamos um impulso diretamente no centro do corpo do objeto
    public final void applyImpulse(Vector2 impulse) {
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    /// Coloca o objeto na posição da body
    public final void syncObjectToBodyPos() {
        updatePosBuffer();

        transformC.x = (
            (tmpPos.x * PPM) - halfWidth
        );
        transformC.y =(
            (tmpPos.y * PPM) - halfHeight
        );

        float bodyAngleDeg = body.getAngle() * MathUtils.radiansToDegrees;
        if (Math.abs(transformC.rotation - bodyAngleDeg) > 0.01f) {
            transformC.rotation = bodyAngleDeg;
        }
    }

    public final void rotateBody(float deltaDegrees) {
        if (disposed || body == null) return;

        /// Early exit para valores irrelevantes
        if (Math.abs(deltaDegrees) < 0.0001f) return;

        /// Conversão para radianos e cálculo do novo ângulo
        float deltaRad = deltaDegrees * MathUtils.degreesToRadians;
        float newAngleRad = body.getAngle() + deltaRad;

        /// Normalização do ângulo (sem múltiplos cálculos)
        newAngleRad = normalizeAngle(newAngleRad);

        /// Aplicação com buffer reutilizável
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

    private float normalizeAngle(float angleRad) {
        if (angleRad > MathUtils.PI) return angleRad - MathUtils.PI2;
        if (angleRad < -MathUtils.PI) return angleRad + MathUtils.PI2;
        return angleRad;
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
     * @param direction direção que devemos aplicar
     * @param magnitude força/magnitude que o corpo deve se mover naquela direção
     */
    public final void applyDirectionalImpulse(Vector2 direction, float magnitude) {
        tmpImpulse.set(direction);

        /// Normaliza apenas se necessário (len2 > 1)
        if (direction.len2() > 1) {
            tmpImpulse.nor();
        }

        applyImpulse(tmpImpulse.scl(magnitude * body.getMass()));
    }

    /// Limita a velocidade do corpo usando clamping
    public final void limitVelocity(float maxX, float maxY) {
        updateVelBuffer();

        /// Conversão feita uma vez
        float maxXInMeters = maxX / PPM;
        float maxYInMeters = maxY / PPM;

        float limitedX = Math.max(-maxXInMeters, Math.min(tmpVel.x, maxXInMeters));
        float limitedY = Math.max(-maxYInMeters, Math.min(tmpVel.y, maxYInMeters));

        /// Só aplica se houver diferença significativa
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

    public Vector2 getTmpVel() {
        return tmpVel;
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
        if (disposed || body == null) return;
        syncObjectToBodyPos();

        object.onObjectAndBodyPosSync();

    }

    @Override
    public void dispose() {
        if (body == null) return;

        this.body.getWorld().destroyBody(body);
        this.nullifyReferences();

        disposed = true;
    }

    public void nullifyReferences(){
        this.body = null;
        this.object = null;
        this.transformC = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
