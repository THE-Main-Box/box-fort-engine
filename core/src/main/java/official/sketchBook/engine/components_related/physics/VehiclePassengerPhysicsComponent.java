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
    private static final float CORRECTION_THRESHOLD = 0.01f;

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
    public void postUpdate() {
        if (currentSection != null) {
            synObjectPositionToVehicle();
        }
        super.postUpdate();
    }

    private void synObjectPositionToVehicle() {

        final VehicleSection section = this.currentSection;
        if (section == null) return;

        final Body body = object.getBody();

        // --- leitura única ---
        final float subVelX = section.getVelX();
        final float subVelY = section.getVelY();

        if (!initialized) {
            lastSubVelX = subVelX;
            lastSubVelY = subVelY;
            initialized = true;
            return;
        }

        // --- velocidade ---
        final Vector2 vel = body.getLinearVelocity();

        final float relativeVelX = vel.x - lastSubVelX;

        final float relativeVelY = vel.y - lastSubVelY;

        final float moveVelX = moveC.xSpeed / PPM;
        final float moveVelY = moveC.ySpeed / PPM;

        final float newVelX = relativeVelX + subVelX;
        final float newVelY = relativeVelY + subVelY;

        body.setLinearVelocity(
            newVelX,
            newVelY
        );

        System.out.println(body.getLinearVelocity().x + " | " + newVelX);

        // --- correção ---
        final float correctionX = (subVelX - lastSubVelX) * deltaTime;
        final float correctionY = (subVelY - lastSubVelY) * deltaTime;

        if ((correctionX > CORRECTION_THRESHOLD || correctionX < -CORRECTION_THRESHOLD) ||
            (correctionY > CORRECTION_THRESHOLD || correctionY < -CORRECTION_THRESHOLD)) {

            final Vector2 pos = body.getPosition();

            body.setTransform(
                pos.x + correctionX,
                pos.y + correctionY,
                body.getAngle()
            );
        }

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
