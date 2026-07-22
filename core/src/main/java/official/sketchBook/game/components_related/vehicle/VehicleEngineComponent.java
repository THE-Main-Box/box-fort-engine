package official.sketchBook.game.components_related.vehicle;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WirableConfigurable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WirableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WiringConfig;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;

public class VehicleEngineComponent extends VehicleBaseComponent implements
    WirableObjectII,
    ControllableObjectII,
    WirableConfigurable {

    /// Dire��o local de empuxo do motor — normalizada e imut�vel
    private final Vector2 localThrustDir;

    /// Buffer de dire��o mundial calculada a cada frame — evita aloca��o
    private final Vector2 worldThrustDir = new Vector2();

    /// Buffer do ponto de aplica��o mundial em metros — evita aloca��o
    private final Vector2 worldApplicationPoint = new Vector2();

    /// Deslocamento do motor em rela��o ao centro da body em pixels
    private final float offsetX, offsetY;

    /// For�a m�xima de empuxo em unidades de pixel — convertida pra metros na aplica��o
    private final float maxForce;

    /// Limites de pot�ncia — definem a capacidade de movimento do motor
    private final float minPower;   // 0 = sem reverso, -1 = reverso total
    private final float maxPower;   // 0 = sem frente, 1 = frente total

    /// Pot�ncia atual — aproxima-se do targetPower gradualmente
    private float power;

    /// Pot�ncia alvo — definida pela config do grupo
    private float targetPower;

    /// Taxa de acelera��o por segundo — quanto de pot�ncia ganha/perde por segundo
    private final float accelerationRate;

    /// Se o motor est� ligado
    private boolean active;

    /// Se o motor est� quebrado
    private boolean broken;

    /// Transform do motor — atualizado no postUpdate com posi��o mundial
    private final TransformComponent transformC;

    private final Body body;

    /// Config atual do motor — usada pelo grupo de controle
    private VehicleEngineConfig currentConfig;

    /// Flag de visibilidade para o sistema de culling
    private boolean inScreen;

    public VehicleEngineComponent(
        VehicleSection ownerSection,
        Body body,
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
        super(
            "engine",
            "engine",
            ownerSection,
            VehicleComponentType.PHYSICAL_INTERNAL
        );

        // Normalizamos a dire��o local uma �nica vez na cria��o
        this.localThrustDir = new Vector2(localDirX, localDirY).nor();
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.maxForce = maxForce;

        //Normaliza a quantidade de força entre -1 e 1
        this.minPower = MathUtils.clamp(minPower, -1f, 0f);
        this.maxPower = MathUtils.clamp(maxPower, 0f, 1f);
        this.power = MathUtils.clamp(defaultPower, this.minPower, this.maxPower);

        this.targetPower = this.power;

        this.accelerationRate = Math.max(0f, accelerationRate);

        this.active = startActive;
        this.broken = isBroken;
        this.body = body;

        /// Transform inicializado sem dimens�es — ser� atualizado no postUpdate
        this.transformC = new TransformComponent();

        /// Config padr�o criada na instancia do motor
        this.currentConfig = new VehicleEngineConfig(defaultPower);
    }

    @Override
    public void update(float delta) {
        executePropulsion(delta);
    }

    /// Executa a lógica de propulsão
    protected void executePropulsion(float deltaTime) {
        //Obtém o angulo da body anexada
        float bodyAngle = this.body.getAngle();
        //Obtém o cosseno
        float cos = MathUtils.cos(bodyAngle);
        //Obtém outro calculo matematic importante/bullshit
        float sin = MathUtils.sin(bodyAngle);

        // Calculamos o ponto de aplica��o mundial em metros da posição x e y
        float offsetXMeters = toMeters(offsetX);
        float offsetYMeters = toMeters(offsetY);

        //Seta o buffer de aplicação, considerando o angulo, para aplicar uma propulsão coerente
        worldApplicationPoint.set(
            ownerSection.getBody().getPosition().x + (offsetXMeters * cos - offsetYMeters * sin),
            ownerSection.getBody().getPosition().y + (offsetXMeters * sin + offsetYMeters * cos)
        );

        // Atualizamos o transform com a posi��o mundial em pixels — usado para culling e render
        transformC.x = worldApplicationPoint.x * PPM;
        transformC.y = worldApplicationPoint.y * PPM;
        transformC.setRotation(
            ownerSection.getBody().getAngle() * MathUtils.radiansToDegrees
        );

        /// Aproxima power do targetPower gradualmente
        if (power != targetPower) {
            float step = accelerationRate * deltaTime;
            if (Math.abs(targetPower - power) <= step) {
                power = targetPower;
            } else {
                power += Math.signum(targetPower - power) * step;
            }
        }

        // S� aplica for�a se ativo, n�o quebrado, e com pot�ncia relevante
        if (!active || broken || (power == 0f && targetPower == 0f)) return;

        /// Rotacionamos a dire��o local pelo �ngulo atual da body
        worldThrustDir.set(
            localThrustDir.x * cos - localThrustDir.y * sin,
            localThrustDir.x * sin + localThrustDir.y * cos
        );

        // For�a convertida de pixels pra metros, mantendo consist�ncia com o resto do sistema
        float force = (maxForce * power) / PPM;

        // Aplica no ponto de offset — gera torque se deslocado do centro de massa
        ownerSection.getBody().applyForce(
            worldThrustDir.x * force,
            worldThrustDir.y * force,
            worldApplicationPoint.x,
            worldApplicationPoint.y,
            true
        );
    }

    /// Toggle ou set de ativa��o — depende da config atual
    @Override
    public void executeWiringInteraction() {
        if (currentConfig.active == null) {
            this.active = !active;
        } else {
            this.active = currentConfig.active;
        }

        /// Ao desligar, reseta a aceleração para o próximo acionamento partir do zero
        if (!active) {
            power = 0f;
            targetPower = 0f;
        }
    }

    @Override
    public boolean canWireInteract() {
        return !broken;
    }

    /// Aplica a config recebida do grupo antes do acionamento
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

    /// Define a pot�ncia alvo respeitando os limites de capacidade do motor
    public void setPower(float request) {
        this.targetPower = MathUtils.clamp(request, minPower, maxPower);
    }

    @Override
    protected void nullifyReferences() {
        currentConfig = null;
    }

    public float getPower() {
        return power;
    }

    public float getTargetPower() {
        return targetPower;
    }

    public float getMinPower() {
        return minPower;
    }

    public float getMaxPower() {
        return maxPower;
    }

    public float getAccelerationRate() {
        return accelerationRate;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    public float getMaxForce() {
        return maxForce;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public Vector2 getLocalThrustDir() {
        return localThrustDir;
    }

    /// Config do motor — exposta ao jogador via HUD
    /// Determina a pot�ncia e estado de ativa��o ao acionar este motor num grupo
    public static class VehicleEngineConfig implements WiringConfig {

        /// Pot�ncia entre minPower e maxPower do motor — definida pelo jogador
        public final float power;

        /// Estado de ativa��o — null = toggle, true/false = set direto
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
