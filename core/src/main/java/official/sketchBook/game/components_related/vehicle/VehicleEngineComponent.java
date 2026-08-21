package official.sketchBook.game.components_related.vehicle;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.*;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;

public class VehicleEngineComponent extends VehicleBaseComponent implements
    WirableObjectII,
    ControllableObjectII,
    SerializableWirableConfigurable {

    public static final String TYPE_KEY = "vehicle_engine";

    /// Dire??o local de empuxo do motor ? normalizada
    private Vector2 localThrustDir;

    /// Buffer de dire??o mundial calculada a cada frame ? evita aloca??o
    private final Vector2 worldThrustDir = new Vector2();

    /// Buffer do ponto de aplica??o mundial em metros ? evita aloca??o
    private final Vector2 worldApplicationPoint = new Vector2();

    /// Deslocamento do motor em rela??o ao centro da body em pixels
    private float offsetX, offsetY;

    /// For?a m?xima de empuxo em unidades de pixel ? convertida pra metros na aplica??o
    private float maxForce;

    /// Limites de pot?ncia ? definem a capacidade de movimento do motor
    private float minPower;   // 0 = sem reverso, -1 = reverso total
    private float maxPower;   // 0 = sem frente, 1 = frente total

    /// Pot?ncia atual ? aproxima-se do targetPower gradualmente
    private float power;

    /// Pot?ncia alvo ? definida pela config do grupo
    private float targetPower;

    /// Taxa de acelera??o por segundo ? quanto de pot?ncia ganha/perde por segundo
    private float accelerationRate;

    /// Se o motor est? ligado
    private boolean active;

    /// Se o motor est? quebrado
    private boolean broken;

    /// Transform do motor ? atualizado no postUpdate com posi??o mundial
    private TransformComponent transformC;

    /// Config atual do motor ? usada pelo grupo de controle
    private VehicleEngineConfig currentConfig;

    /// Flag de visibilidade para o sistema de culling
    private boolean inScreen;

    /// Construtor "vivo" ? usado quando o jogo cria o motor diretamente
    public VehicleEngineComponent(
        String id,
        float localDirX,
        float localDirY,
        float offsetX,
        float offsetY,
        float maxForce,
        float minPower,
        float maxPower,
        float defaultPower,
        float accelerationRate,
        boolean startActive,
        boolean isBroken
    ) {
        super(id, VehicleComponentType.PHYSICAL_INTERNAL);

        this.localThrustDir = new Vector2(localDirX, localDirY).nor();
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.maxForce = maxForce;

        this.minPower = MathUtils.clamp(minPower, -1f, 0f);
        this.maxPower = MathUtils.clamp(maxPower, 0f, 1f);
        this.power = MathUtils.clamp(defaultPower, this.minPower, this.maxPower);
        this.targetPower = this.power;

        this.accelerationRate = Math.max(0f, accelerationRate);

        this.active = startActive;
        this.broken = isBroken;

        this.transformC = new TransformComponent();
        this.currentConfig = new VehicleEngineConfig(defaultPower);
    }

    /// Construtor vazio ? exigido pra reflection (registry). Fica em
    /// estado incompleto de prop?sito at? load() popular os campos.
    public VehicleEngineComponent() {
        this.worldThrustDir.setZero();
        this.transformC = new TransformComponent();
    }

    @Override
    public void update(float delta) {
        executePropulsion(delta);
    }

    protected void executePropulsion(float deltaTime) {
        float bodyAngle = ownerSection.getBody().getAngle();
        float cos = MathUtils.cos(bodyAngle);
        float sin = MathUtils.sin(bodyAngle);

        float offsetXMeters = toMeters(offsetX);
        float offsetYMeters = toMeters(offsetY);

        worldApplicationPoint.set(
            ownerSection.getBody().getPosition().x + (offsetXMeters * cos - offsetYMeters * sin),
            ownerSection.getBody().getPosition().y + (offsetXMeters * sin + offsetYMeters * cos)
        );

        transformC.x = worldApplicationPoint.x * PPM;
        transformC.y = worldApplicationPoint.y * PPM;
        transformC.setRotation(ownerSection.getBody().getAngle() * MathUtils.radiansToDegrees);

        if (power != targetPower) {
            float step = accelerationRate * deltaTime;
            if (Math.abs(targetPower - power) <= step) {
                power = targetPower;
            } else {
                power += Math.signum(targetPower - power) * step;
            }
        }

        if (!active || broken || (power == 0f && targetPower == 0f)) return;

        worldThrustDir.set(
            localThrustDir.x * cos - localThrustDir.y * sin,
            localThrustDir.x * sin + localThrustDir.y * cos
        );

        float force = (maxForce * power) / PPM;

        ownerSection.getBody().applyForce(
            worldThrustDir.x * force,
            worldThrustDir.y * force,
            worldApplicationPoint.x,
            worldApplicationPoint.y,
            true
        );
    }

    @Override
    public void executeWiringInteraction() {
        if (currentConfig.active == null) {
            this.active = !active;
        } else {
            this.active = currentConfig.active;
        }

        if (!active) {
            power = 0f;
            targetPower = 0f;
        }
    }

    @Override
    public SaveData saveWiringConfig(WiringConfig config) {
        if (!(config instanceof VehicleEngineConfig)) return null;
        VehicleEngineConfig cfg = (VehicleEngineConfig) config;

        SaveData data = new SaveData().put("power", cfg.power);
        if (cfg.active != null) {
            data.put("active", cfg.active);
        }
        return data;
    }

    @Override
    public void loadWiringConfig(SaveData data) {
        if (data == null) return;
        float power = data.getFloat("power", 0f);
        Boolean active = data.has("active") ? data.getBoolean("active", false) : null;
        this.currentConfig = new VehicleEngineConfig(power, active);
    }

    @Override
    public boolean canWireInteract() {
        return !broken;
    }

    @Override
    public void setCurrentConfiguration(WiringConfig config) {
        if (!(config instanceof VehicleEngineConfig)) return;
        this.currentConfig = (VehicleEngineConfig) config;
        this.targetPower = MathUtils.clamp(currentConfig.power, minPower, maxPower);
    }

    @Override
    public WiringConfig getCurrentConfiguration() {
        return currentConfig;
    }

    public void setPower(float request) {
        this.targetPower = MathUtils.clamp(request, minPower, maxPower);
    }

    @Override
    protected void nullifyReferences() {
        super.nullifyReferences();
        currentConfig = null;
        localThrustDir = null;
        transformC = null;
    }

    @Override
    public boolean toUpdate() {
        return true;
    }

    public float getPower() { return power; }
    public float getTargetPower() { return targetPower; }
    public float getMinPower() { return minPower; }
    public float getMaxPower() { return maxPower; }
    public float getAccelerationRate() { return accelerationRate; }
    public boolean isActive() { return active; }
    public boolean isBroken() { return broken; }
    public void setBroken(boolean broken) { this.broken = broken; }
    public float getMaxForce() { return maxForce; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public Vector2 getLocalThrustDir() { return localThrustDir; }

    // ============================================================
    // SERIALIZA??O
    // ============================================================

    @Override
    public SaveData toSaveData() {
        return new SaveData()
            .put("id", id)
            .putTypeKey(TYPE_KEY)
            .put("local_dir_x", localThrustDir.x)
            .put("local_dir_y", localThrustDir.y)
            .put("offset_x", offsetX)
            .put("offset_y", offsetY)
            .put("max_force", maxForce)
            .put("min_power", minPower)
            .put("max_power", maxPower)
            .put("power", power)
            .put("acceleration_rate", accelerationRate)
            .put("active", active)
            .put("broken", broken);
    }

    @Override
    public void load(SaveData data) {
        this.id = data.getStringRequired("id");

        float dirX = data.getFloat("local_dir_x", 1f);
        float dirY = data.getFloat("local_dir_y", 0f);
        this.localThrustDir = new Vector2(dirX, dirY).nor();

        this.offsetX = data.getFloat("offset_x", 0f);
        this.offsetY = data.getFloat("offset_y", 0f);
        this.maxForce = data.getFloat("max_force", 0f);
        this.minPower = data.getFloat("min_power", -1f);
        this.maxPower = data.getFloat("max_power", 1f);
        this.power = data.getFloat("power", 0f);
        this.targetPower = this.power;
        this.accelerationRate = data.getFloat("acceleration_rate", 0f);
        this.active = data.getBoolean("active", false);
        this.broken = data.getBoolean("broken", false);

        this.type = VehicleComponentType.PHYSICAL_INTERNAL;
        this.currentConfig = new VehicleEngineConfig(power, active);
    }

    public static class VehicleEngineConfig implements WiringConfig {
        public final float power;
        public final Boolean active;

        public VehicleEngineConfig(float power) {
            this.power = power;
            this.active = null;
        }

        public VehicleEngineConfig(float power, Boolean active) {
            this.power = power;
            this.active = active;
        }
    }
}
