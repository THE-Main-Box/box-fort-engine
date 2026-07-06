package official.sketchBook.game.components_related.vehicle;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WirableObjectII;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class VehicleEngineComponent extends VehicleBaseComponent implements WirableObjectII, ControllableObjectII {

    /// Dire��o local de empuxo do motor — normalizada e imut�vel
    private final Vector2 localThrustDir;

    /// Buffer de dire��o mundial calculada a cada frame — evita aloca��o
    private final Vector2 worldThrustDir = new Vector2();

    /// For�a m�xima de empuxo em unidades de pixel — convertida pra metros na aplica��o
    private final float maxForce;

    /// Pot�ncia atual entre -1.0 (reverso total) e 1.0 (frente total)
    private float power;

    /// Se o motor est� ligado
    private boolean active;

    /// Se o motor est� quebrado
    private boolean broken;

    public VehicleEngineComponent(
        VehicleSection ownerSection,
        float localDirX,
        float localDirY,
        float maxForce,
        float defaultPower,
        boolean startActive
    ) {
        super(
            "engine",
            "engine",
            ownerSection,
            VehicleComponentType.PHYSICAL_INTERNAL
        );

        /// Normalizamos a dire��o local uma �nica vez na cria��o
        this.localThrustDir = new Vector2(localDirX, localDirY).nor();
        this.maxForce = maxForce;
        this.power = MathUtils.clamp(defaultPower, -1f, 1f);
        this.active = startActive;
        this.broken = false;
    }

    @Override
    public void postUpdate() {
        if (!active || broken || power == 0f) return;

        /// Rotacionamos a dire��o local pelo �ngulo atual da body
        /// Isso garante que o empuxo sempre respeite a orienta��o do submarino
        float bodyAngle = ownerSection.getBody().getAngle();

        float cos = MathUtils.cos(bodyAngle);
        float sin = MathUtils.sin(bodyAngle);

        /// Rota��o 2D sem alocar: (x*cos - y*sin, x*sin + y*cos)
        worldThrustDir.set(
            localThrustDir.x * cos - localThrustDir.y * sin,
            localThrustDir.x * sin + localThrustDir.y * cos
        );

        /// For�a convertida de pixels pra metros, mantendo consist�ncia com o resto do sistema
        float force = (maxForce * power) / PPM;

        ownerSection.getBody().applyForceToCenter(
            worldThrustDir.x * force,
            worldThrustDir.y * force,
            true
        );
    }

    /// Toggle de liga/desliga via sistema de fia��o
    @Override
    public void executeWiringInteraction() {
        this.active = !active;
    }

    @Override
    public boolean canWireInteract() {
        return !broken;
    }

    /// Define a pot�ncia entre -1.0 e 1.0
    public void setPower(float power) {
        this.power = MathUtils.clamp(power, -1f, 1f);
    }

    public float getPower() { return power; }
    public boolean isActive() { return active; }
    public boolean isBroken() { return broken; }
    public void setBroken(boolean broken) { this.broken = broken; }
    public float getMaxForce() { return maxForce; }
    public Vector2 getLocalThrustDir() { return localThrustDir; }

    @Override
    protected void nullifyReferences() {
    }
}
