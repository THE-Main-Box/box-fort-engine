package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.VehiclePassenger;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.game_object_related.vehicle.VehicleSection;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class VehiclePassengerPhysicsComponent extends MovableObjectPhysicsComponent implements Component {

    private VehicleSection currentSection;

    private VehiclePassenger passenger;

    private MovementComponent moveC;

    private float
        lastSubVelX = 0f,
        lastSubVelY = 0f;

    private boolean
        initialized = false,
        disposed = false;

    // Constantes (evita recriação/lookup)
    private static final float CORRECTION_THRESHOLD = 0.001f;

    public boolean
        autoCorrect = true,
        autoApplySubMovement = true;

    public VehiclePassengerPhysicsComponent(
        VehiclePassenger object,
        int categoryBit,
        int maskBit,
        float density,
        float frict,
        float rest
    ) {
        super(
            object,
            categoryBit,
            maskBit,
            density,
            frict,
            rest
        );

        this.passenger = object;
        this.moveC = object.getMoveC();
    }

    @Override
    public void update(float delta) {
        super.update(delta);
    }

    @Override
    protected void applyMovement() {
        if (currentSection == null || !autoApplySubMovement) {
            super.applyMovement();
            return;
        }

        updateVelBuffer();

        final float subVelX = currentSection.getVelX();
        final float subVelY = currentSection.getVelY();

        // Velocidade relativa atual do jogador em relação ao sub
        final float relVelX = tmpVel.x - subVelX;
        final float relVelY = tmpVel.y - subVelY;

        // Velocidade desejada relativa ao sub
        final float desiredRelX = limitAndConvertSpeedToMeters(
            moveC.xSpeed, moveC.xMaxSpeed, relVelX
        );
        final float desiredRelY = limitAndConvertSpeedToMeters(
            moveC.ySpeed, moveC.yMaxSpeed, relVelY
        );

        final float impulseX = desiredRelX != 0 ? desiredRelX - relVelX : 0;
        final float impulseY = desiredRelY != 0 ? desiredRelY - relVelY : 0;

        // --- OTIMIZAÇÃO: evita aplicar impulso nulo ---
        if (impulseX == 0f && impulseY == 0f) return;

        tmpVel.set(impulseX, impulseY);

        final float mass = object.getBody().getMass();
        applyImpulse(tmpVel.scl(mass));
    }

    @Override
    public void postUpdate() {
        super.postUpdate();
    }

    @Override
    public void syncObjectToBodyPos() {
        super.syncObjectToBodyPos();
        synObjectPositionToVehicle();
    }

    private void synObjectPositionToVehicle() {
        if (this.currentSection == null || !autoCorrect) return;

        final Body body = object.getBody();

        final float subVelX = currentSection.getVelX();
        final float subVelY = currentSection.getVelY();

        if (!initialized) {
            lastSubVelX = subVelX;
            lastSubVelY = subVelY;
            initialized = true;
            return;
        }

        final float deltaSubX = subVelX - lastSubVelX;
        final float deltaSubY = subVelY - lastSubVelY;

        final boolean hasDelta =
            Math.abs(deltaSubX) > CORRECTION_THRESHOLD ||
                Math.abs(deltaSubY) > CORRECTION_THRESHOLD;

        // --- OTIMIZAÇÃO: se não mudou nada, sai cedo ---
        if (!hasDelta) {
            lastSubVelX = subVelX;
            lastSubVelY = subVelY;
            return;
        }

        // --- VELOCIDADE ---
        final Vector2 vel = body.getLinearVelocity();

        final float relativeVelX = vel.x - lastSubVelX;
        final float relativeVelY = vel.y - lastSubVelY;

        body.setLinearVelocity(
            relativeVelX + subVelX,
            relativeVelY + subVelY
        );

        // --- POSIÇÃO ---
        final Vector2 pos = body.getPosition();

        body.setTransform(
            pos.x + deltaSubX * deltaTime,
            pos.y + deltaSubY * deltaTime,
            body.getAngle()
        );

        // --- update estado ---
        lastSubVelX = subVelX;
        lastSubVelY = subVelY;
    }

    @Override
    public void dispose() {
        if (disposed) return;

        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        super.nullifyReferences();
        this.currentSection = null;
        this.moveC = null;
        this.passenger = null;
    }

    public void setCurrentSection(VehicleSection section) {
        if (this.currentSection == section) return;

        this.currentSection = section;

        this.initialized = false;

        this.lastSubVelX = 0f;
        this.lastSubVelY = 0f;
    }

    public VehicleSection getCurrentSection() {
        return currentSection;
    }

    public boolean isInsideVehicle() {
        return currentSection != null;
    }
}
