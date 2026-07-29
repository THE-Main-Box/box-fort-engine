package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.PhysicalLiquidInteractableObjectII;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.*;

/**
 * Simula interação física com líquidos usando fixtures reais do Box2D.
 * Otimizações principais:
 * - cache por fixture para polígonos;
 * - cálculo manual de ponto em mundo (sem getWorldPoint em loop);
 * - área de polígono cacheada;
 * - uma única setMassData por atualização quando houver dirty state;
 * - inércia extraída da geometria real das fixtures via Box2D (probe de
 *   densidade temporária), escalada pela massa manual definida externamente;
 * - sem dependência de delta na física de massa/inércia (inércia não é uma
 *   grandeza integrada no tempo, é uma propriedade intrínseca do corpo).
 */
public class PhysicalLiquidInteractionComponent extends LiquidInteractionComponent {

    private static final float MIN_FRACTION_TO_APPLY_FORCE = 0.001f;
    private static final int MAX_POLYGON_VERTICES = 8;
    private static final int MAX_CLIPPED_POLYGON_VERTICES = MAX_POLYGON_VERTICES * 2;

    /**
     * Densidade uniforme temporária usada apenas para extrair, via
     * body.resetMassData(), a distribuição geométrica relativa (o "shape"
     * da inércia) a partir das fixtures reais. As fixtures de produção
     * mantêm density = 0 sempre — massa é controlada manualmente.
     */
    private static final float MASS_PROBE_DENSITY = 1f;

    private final Vector2 fixtureBuoyancyPointBuffer = new Vector2();
    private final Vector2 fixtureVertexBuffer = new Vector2();

    private final float[] worldVertexX = new float[MAX_POLYGON_VERTICES];
    private final float[] worldVertexY = new float[MAX_POLYGON_VERTICES];

    private final float[] clippedX = new float[MAX_CLIPPED_POLYGON_VERTICES];
    private final float[] clippedY = new float[MAX_CLIPPED_POLYGON_VERTICES];

    private final MassData massDataBuffer = new MassData();

    /**
     * Único gatilho para reaplicar massa/inércia/centro no Box2D.
     * Ativado apenas por mudanças que de fato alteram o comportamento
     * físico do corpo: massa e centro de massa. Volume NÃO ativa isso —
     * volume só alimenta objectDensity e a força de flutuabilidade, que já
     * lê o campo `volume` diretamente sem precisar tocar o body.
     */
    private boolean physicsMassDataDirty = false;

    /**
     * Gatilho separado, mais barato: só recalcula objectDensity
     * (mass / volume). Nunca chama resetMassData/setMassData.
     */
    private boolean densityDirty = false;

    private LiquidRegion currentLiquidRegionBuffer;

    private final PhysicsComponent physicsC;
    private boolean disposed = false;

    /**
     * Cache por fixture.
     * Cada polygon shape guarda vértices locais + área.
     * Isso evita chamar getVertex() e recalcular área todo frame.
     */
    private static final class FixtureCache {
        final float[] localX;
        final float[] localY;
        final int count;
        final float area;

        FixtureCache(float[] localX, float[] localY, int count, float area) {
            this.localX = localX;
            this.localY = localY;
            this.count = count;
            this.area = area;
        }
    }

    private final IdentityHashMap<Fixture, FixtureCache> fixtureCache =
        new IdentityHashMap<Fixture, FixtureCache>();

    public PhysicalLiquidInteractionComponent(PhysicalLiquidInteractableObjectII owner) {
        super(owner, owner.getMoveC());
        this.physicsC = owner.getPhysicsC();
    }

    // ============================================================
    // CICLO PRINCIPAL
    // ============================================================

    @Override
    public void update(float delta) {
        updateLiquidState();
        updateSimulation(delta);
    }

    @Override
    protected void updateSimulation(float delta) {
        boolean shouldSimulate = canInteract && !liquidAndRegionMap.isEmpty();

        applyChange(shouldSimulate);
        resolveDirtyPhysicsData();

        if (!shouldSimulate
            || highestDensityLiquidBuffer == null
            || currentLiquidRegionBuffer == null) {
            return;
        }

        applyBuoyancyForEachFixture();
        applyLinearDrag();

        owner.inLiquidUpdate();
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    float originalRotationDamping;

    @Override
    protected void onLiquidEnter() {
    }

    @Override
    protected void onLiquidExit() {
    }

    // ============================================================
    // EMPUXO POR FIXTURE
    // ============================================================

    private void applyBuoyancyForEachFixture() {
        Body body = physicsC.object.getBody();

        float surfaceY = toMeters(
            currentLiquidRegionBuffer.getY() + currentLiquidRegionBuffer.getHeight()
        );
        float liquidDensity = highestDensityLiquidBuffer.density;
        float gravity = Math.abs(body.getWorld().getGravity().y);
        boolean canRotate = !body.isFixedRotation();

        Array<Fixture> fixtures = body.getFixtureList();

        float totalBuoyancyMagnitude = 0f;
        float weightedX = 0f;
        float weightedY = 0f;

        for (int i = 0; i < fixtures.size; i++) {
            Fixture fixture = fixtures.get(i);
            if (!isSubmersibleFixture(fixture)) continue;

            float buoyancyForceMagnitude = computeFixtureBuoyancy(
                fixture, body, surfaceY, liquidDensity, gravity, canRotate, fixtureBuoyancyPointBuffer
            );

            if (buoyancyForceMagnitude <= 0f) continue;

            totalBuoyancyMagnitude += buoyancyForceMagnitude;

            if (canRotate) {
                weightedX += fixtureBuoyancyPointBuffer.x * buoyancyForceMagnitude;
                weightedY += fixtureBuoyancyPointBuffer.y * buoyancyForceMagnitude;
            }
        }

        if (totalBuoyancyMagnitude <= 0f) return;

        floatEffectValue = totalBuoyancyMagnitude;

        if (canRotate) {
            floatApplicationPoint.set(
                weightedX / totalBuoyancyMagnitude,
                weightedY / totalBuoyancyMagnitude
            );

            body.applyForce(
                0f,
                floatEffectValue,
                floatApplicationPoint.x,
                floatApplicationPoint.y,
                true
            );
        } else {
            body.applyForceToCenter(
                0f,
                floatEffectValue * 20f,
                true
            );
        }
    }

    private boolean isSubmersibleFixture(Fixture fixture) {
        return !fixture.isSensor();
    }

    private float computeFixtureBuoyancy(
        Fixture fixture,
        Body body,
        float surfaceY,
        float liquidDensity,
        float gravity,
        boolean canRotate,
        Vector2 outPoint
    ) {
        Shape shape = fixture.getShape();

        if (shape instanceof CircleShape) {
            return computeCircleBuoyancy(
                (CircleShape) shape,
                body,
                surfaceY,
                liquidDensity,
                gravity,
                canRotate,
                outPoint
            );
        }

        if (shape instanceof PolygonShape) {
            return computePolygonBuoyancy(
                fixture,
                (PolygonShape) shape,
                body,
                surfaceY,
                liquidDensity,
                gravity,
                canRotate,
                outPoint
            );
        }

        return 0f;
    }

    private float computeCircleBuoyancy(
        CircleShape circle,
        Body body,
        float surfaceY,
        float liquidDensity,
        float gravity,
        boolean canRotate,
        Vector2 outPoint
    ) {
        Vector2 bodyPos = body.getPosition();
        float bodyAngle = body.getAngle();

        float localX = circle.getPosition().x;
        float localY = circle.getPosition().y;

        float cos = (float) Math.cos(bodyAngle);
        float sin = (float) Math.sin(bodyAngle);

        float centerX = bodyPos.x + (cos * localX - sin * localY);
        float centerY = bodyPos.y + (sin * localX + cos * localY);

        float r = circle.getRadius();

        float minY = centerY - r;
        float maxY = centerY + r;

        float fraction = calculateSubmersionFraction(minY, maxY, surfaceY);
        if (fraction < MIN_FRACTION_TO_APPLY_FORCE) return 0f;

        float area = (float) Math.PI * r * r;
        float magnitude = liquidDensity * area * fraction * gravity;

        if (canRotate) {
            float depth = surfaceY - centerY;
            float clampedDepth = Math.max(-r, Math.min(r, depth));
            outPoint.set(centerX, centerY + clampedDepth * 0.5f);
        }

        return magnitude;
    }

    private float computePolygonBuoyancy(
        Fixture fixture,
        PolygonShape poly,
        Body body,
        float surfaceY,
        float liquidDensity,
        float gravity,
        boolean canRotate,
        Vector2 outPoint
    ) {
        FixtureCache cache = getOrBuildFixtureCache(fixture, poly);
        int count = cache.count;

        Vector2 bodyPos = body.getPosition();
        float bodyAngle = body.getAngle();
        float cos = (float) Math.cos(bodyAngle);
        float sin = (float) Math.sin(bodyAngle);

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            float lx = cache.localX[i];
            float ly = cache.localY[i];

            float wx = bodyPos.x + (cos * lx - sin * ly);
            float wy = bodyPos.y + (sin * lx + cos * ly);

            worldVertexX[i] = wx;
            worldVertexY[i] = wy;

            if (wy < minY) minY = wy;
            if (wy > maxY) maxY = wy;
        }

        float fraction = calculateSubmersionFraction(minY, maxY, surfaceY);
        if (fraction < MIN_FRACTION_TO_APPLY_FORCE) return 0f;

        float area = cache.area;
        if (area <= 0f) return 0f;

        float magnitude = liquidDensity * area * fraction * gravity;

        if (!canRotate) return magnitude;

        if (fraction >= 1f) {
            outPoint.set(body.getWorldPoint(massDataBuffer.center));
            return magnitude;
        }

        int clippedCount = buildClippedPolygon(count, surfaceY, clippedX, clippedY);
        boolean found = clippedCount >= 3 && computePolygonCentroid(clippedX, clippedY, clippedCount, outPoint);

        if (!found) {
            outPoint.set((worldVertexX[0] + worldVertexX[count - 1]) * 0.5f, (minY + maxY) * 0.5f);
        }

        return magnitude;
    }

    private FixtureCache getOrBuildFixtureCache(Fixture fixture, PolygonShape poly) {
        FixtureCache cache = fixtureCache.get(fixture);
        if (cache != null) return cache;

        int count = poly.getVertexCount();
        float[] xs = new float[count];
        float[] ys = new float[count];

        float area = 0f;

        for (int i = 0; i < count; i++) {
            poly.getVertex(i, fixtureVertexBuffer);
            xs[i] = fixtureVertexBuffer.x;
            ys[i] = fixtureVertexBuffer.y;
        }

        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            area += (xs[i] * ys[next]) - (xs[next] * ys[i]);
        }

        area = Math.abs(area) * 0.5f;

        cache = new FixtureCache(xs, ys, count, area);
        fixtureCache.put(fixture, cache);
        return cache;
    }

    // ============================================================
    // CENTROIDE / CLIPPING
    // ============================================================

    private int buildClippedPolygon(int count, float surfaceY, float[] outX, float[] outY) {
        int written = 0;

        float prevX = worldVertexX[count - 1];
        float prevY = worldVertexY[count - 1];
        float prevDepth = surfaceY - prevY;

        for (int i = 0; i < count; i++) {
            float currX = worldVertexX[i];
            float currY = worldVertexY[i];
            float currDepth = surfaceY - currY;

            boolean crossesSurface = (prevDepth >= 0f) != (currDepth >= 0f);
            if (crossesSurface && written < outX.length) {
                float t = prevDepth / (prevDepth - currDepth);
                outX[written] = prevX + (currX - prevX) * t;
                outY[written] = prevY + (currY - prevY) * t;
                written++;
            }

            if (currDepth >= 0f && written < outX.length) {
                outX[written] = currX;
                outY[written] = currY;
                written++;
            }

            prevX = currX;
            prevY = currY;
            prevDepth = currDepth;
        }

        return written;
    }

    private boolean computePolygonCentroid(float[] xs, float[] ys, int count, Vector2 out) {
        float signedAreaSum = 0f;
        float cxSum = 0f;
        float cySum = 0f;

        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;

            float cross = (xs[i] * ys[next]) - (xs[next] * ys[i]);
            signedAreaSum += cross;
            cxSum += (xs[i] + xs[next]) * cross;
            cySum += (ys[i] + ys[next]) * cross;
        }

        float area = signedAreaSum * 0.5f;
        if (Math.abs(area) < 1e-6f) return false;

        float factor = 1f / (6f * area);
        out.set(cxSum * factor, cySum * factor);
        return true;
    }

    // ============================================================
    // GEOMETRIA AUXILIAR
    // ============================================================

    private float calculateSubmersionFraction(float minY, float maxY, float surfaceY) {
        if (maxY <= surfaceY) return 1f;
        if (minY >= surfaceY) return 0f;

        float effectiveHeight = maxY - minY;
        if (effectiveHeight <= 0f) return 0f;

        float fraction = (surfaceY - minY) / effectiveHeight;
        return fraction < 0f ? 0f : Math.min(fraction, 1f);
    }

    // ============================================================
    // DRAG LINEAR
    // ============================================================

    private void applyLinearDrag() {
        if (highestDragLiquidBuffer == null) return;

        Body body = physicsC.object.getBody();
        float verticalVelocity = body.getLinearVelocity().y;

        float dragStrength = highestDragLiquidBuffer.drag;
        if (dragStrength <= 0f) return;

        floatEffectValue = verticalVelocity * -dragStrength * body.getMass();
        floatEffectValue *= dragStrength;

        body.applyForceToCenter(
            0f,
            floatEffectValue,
            true
        );
    }

    // ============================================================
    // MASSA / DENSIDADE / INÉRCIA
    // ============================================================

    private void resolveDirtyPhysicsData() {
        if (!physicsMassDataDirty && !densityDirty) {
            return;
        }

        if (densityDirty) {
            objectDensity = (volume > 0f) ? (mass / volume) : Float.MAX_VALUE;
            densityDirty = false;
        }

        if (physicsMassDataDirty) {
            applyAllMassDataToBody();
            physicsMassDataDirty = false;
        }
    }

    /**
     * Reaplica massa, inércia e centro de massa no body.
     * Chamado apenas quando physicsMassDataDirty está true — ou seja,
     * apenas quando mass ou centerOfMass realmente mudaram.
     * <p>
     * A inércia é extraída da geometria real das fixtures via Box2D:
     * setamos uma densidade uniforme temporária (MASS_PROBE_DENSITY),
     * chamamos resetMassData() para o Box2D calcular I a partir da forma
     * real do corpo, restauramos density = 0 (as fixtures de produção
     * nunca carregam densidade própria — massa é sempre manual), e então
     * escalamos o I obtido pela razão entre a massa real (mass) e a massa
     * "de prova" calculada pelo probe. Isso preserva a distribuição
     * espacial real do casco sem exigir densidade de fixture em produção.
     */
    private void applyAllMassDataToBody() {
        Body body = physicsC.object.getBody();
        Array<Fixture> fixtures = body.getFixtureList();

        for (int i = 0; i < fixtures.size; i++) {
            Fixture fixture = fixtures.get(i);
            if (fixture.isSensor()) continue;
            fixture.setDensity(MASS_PROBE_DENSITY);
        }

        body.resetMassData();
        MassData probed = body.getMassData();

        // restaura density = 0 imediatamente — foi só uma sonda, não é
        // dado de produção
        for (int i = 0; i < fixtures.size; i++) {
            Fixture fixture = fixtures.get(i);
            if (fixture.isSensor()) continue;
            fixture.setDensity(0f);
        }

        float probedMass = probed.mass;

        if (probedMass <= 0f) {
            // sem fixtures válidas para extrair forma — fallback manual puro,
            // sem inércia derivada de geometria
            massDataBuffer.mass = mass;
            massDataBuffer.I = 0f;
            massDataBuffer.center.set(centerOfMass);
            body.setMassData(massDataBuffer);
            return;
        }

        float massScale = mass / probedMass;

        massDataBuffer.mass = mass;
        massDataBuffer.I = probed.I * massScale;
        massDataBuffer.center.set(centerOfMass);
        body.setMassData(massDataBuffer);
    }

    /**
     * Abaixo desse limiar, tratamos o valor como zero. Evita que reduções
     * repetidas (ex: subtração incremental por frame) deixem mass/volume
     * presos em resíduos de ponto flutuante (ex: 1.34E-7) que nunca são
     * corrigidos porque tecnicamente não são negativos.
     */
    private static final float MASS_VOLUME_EPSILON = 1e-4f;

    @Override
    public void setMass(float mass) {
        // clamp, não rejeição: um valor levemente negativo por erro de
        // acumulação (ex: -0.0000003) deve virar 0, não ser descartado —
        // se descartarmos, o campo fica preso no último valor válido antes
        // do estouro, e chamadores que acham que "zeraram" continuam
        // vendo um resíduo antigo.
        float clamped = (mass < MASS_VOLUME_EPSILON) ? 0.4f : mass;

        if (this.mass == clamped) return;

        this.mass = clamped;
        physicsMassDataDirty = true;
        densityDirty = true;

        markUpdateSimulationData();
    }

    @Override
    public void setVolume(float volume) {
        float clamped = (volume < MASS_VOLUME_EPSILON) ? 0f : volume;

        if (this.volume == clamped) return;

        this.volume = clamped;
        densityDirty = true;

        markUpdateSimulationData();
    }

    @Override
    public void updateCenterOfMass(float x, float y) {
        super.updateCenterOfMass(x, y);
        physicsMassDataDirty = true;
        markUpdateSimulationData();
    }

    @Override
    public void updateCenterOfMass(Vector2 centerOfMass) {
        super.updateCenterOfMass(centerOfMass);
        physicsMassDataDirty = true;
        markUpdateSimulationData();
    }

    // ============================================================
    // ESTADO DE LÍQUIDO / REGIÃO
    // ============================================================

    protected void updateCurrentRegion() {
        if (liquidAndRegionMap.isEmpty()) {
            currentLiquidRegionBuffer = null;
            return;
        }

        if (liquidAndRegionMap.size() == 1) {
            ArrayList<LiquidRegion> onlyList = liquidAndRegionMap.values().iterator().next();
            if (onlyList.size() == 1) {
                currentLiquidRegionBuffer = onlyList.get(0);
                return;
            }
        }

        Vector2 objectCenter = physicsC.object.getBody().getWorldCenter();
        float objectCenterX = toPixels(objectCenter.x);
        float objectCenterY = toPixels(objectCenter.y);

        currentLiquidRegionBuffer = getLiquidRegion(objectCenterX, objectCenterY);
    }

    private LiquidRegion getLiquidRegion(float objectCenterX, float objectCenterY) {
        LiquidRegion closestRegion = null;
        float closestDistance = Float.MAX_VALUE;

        for (Map.Entry<LiquidData, ArrayList<LiquidRegion>> entry : liquidAndRegionMap.entrySet()) {
            ArrayList<LiquidRegion> list = entry.getValue();

            for (int i = 0; i < list.size(); i++) {
                LiquidRegion region = list.get(i);
                if (region == null) continue;

                float regionCenterX = region.getX() + (region.getWidth() * 0.5f);
                float regionCenterY = region.getY() + (region.getHeight() * 0.5f);

                float dx = objectCenterX - regionCenterX;
                float dy = objectCenterY - regionCenterY;

                float distance = (dx * dx) + (dy * dy);

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestRegion = region;
                }
            }
        }

        return closestRegion;
    }

    protected void updateCurrentLiquidData() {
        if (liquidAndRegionMap.isEmpty()) {
            highestDensityLiquidBuffer = null;
            highestDragLiquidBuffer = null;
            return;
        }

        if (liquidAndRegionMap.size() == 1) {
            LiquidData onlyData = liquidAndRegionMap.keySet().iterator().next();
            highestDensityLiquidBuffer = onlyData;
            highestDragLiquidBuffer = onlyData;
            return;
        }

        LiquidData highestDensity = null;
        LiquidData highestDrag = null;

        for (LiquidData data : liquidAndRegionMap.keySet()) {
            if (highestDensity == null) {
                highestDensity = data;
                highestDrag = data;
                continue;
            }

            if (data.density > highestDensity.density) highestDensity = data;
            if (data.drag > highestDrag.drag) highestDrag = data;
        }

        highestDensityLiquidBuffer = highestDensity;
        highestDragLiquidBuffer = highestDrag;
    }

    // ============================================================
    // FINALIZAÇÃO
    // ============================================================

    @Override
    public void dispose() {
        if (disposed) return;
        liquidAndRegionMap.clear();
        fixtureCache.clear();
        disposed = true;
    }
}
