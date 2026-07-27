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
 * - uma única setMassData por frame quando houver dirty state;
 * - sem alteração funcional do comportamento base.
 */
public class PhysicalLiquidInteractionComponent extends LiquidInteractionComponent {

    private static final float MIN_FRACTION_TO_APPLY_FORCE = 0.001f;
    private static final int MAX_POLYGON_VERTICES = 8;
    private static final int MAX_CLIPPED_POLYGON_VERTICES = MAX_POLYGON_VERTICES * 2;

    private final Vector2 fixtureBuoyancyPointBuffer = new Vector2();
    private final Vector2 fixtureVertexBuffer = new Vector2();

    private final float[] worldVertexX = new float[MAX_POLYGON_VERTICES];
    private final float[] worldVertexY = new float[MAX_POLYGON_VERTICES];

    private final float[] clippedX = new float[MAX_CLIPPED_POLYGON_VERTICES];
    private final float[] clippedY = new float[MAX_CLIPPED_POLYGON_VERTICES];

    private final MassData massDataBuffer = new MassData();

    private boolean centerOfMassDirty = false;
    private boolean inertiaDirty = false;
    private boolean massDirty = false;
    private boolean volumeDirty = false;
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
        if (!massDirty && !volumeDirty && !densityDirty && !inertiaDirty && !centerOfMassDirty) {
            return;
        }

        if (massDirty || volumeDirty || densityDirty) {
            objectDensity = (volume > 0f) ? (mass / volume) : Float.MAX_VALUE;
            densityDirty = false;
        }

        if (inertiaDirty || massDirty || volumeDirty || centerOfMassDirty) {
            applyAllMassDataToBody();
            inertiaDirty = false;
        }

        massDirty = false;
        volumeDirty = false;
        centerOfMassDirty = false;
    }

    private float computeApproximateInertia(float mass, float area) {
        return mass * area / (2f * (float) Math.PI);
    }

    /**
     * Uma única chamada a setMassData por atualização, para evitar custo repetido no Box2D.
     * Mantém massa, inércia e centro de massa sincronizados com os valores atuais do componente.
     */
    private void applyAllMassDataToBody() {
        Body body = physicsC.object.getBody();

        massDataBuffer.mass = mass;
        massDataBuffer.I = computeApproximateInertia(mass, volume);
        massDataBuffer.center.set(centerOfMass);

        body.setMassData(massDataBuffer);
    }

    @Override
    public void setMass(float mass) {
        if (mass < 0f) return;
        if (this.mass == mass) return;

        this.mass = mass;
        massDirty = true;

        markUpdateSimulationData();
    }

    @Override
    public void setVolume(float volume) {
        if (volume < 0f) return;
        if (this.volume == volume) return;

        this.volume = volume;
        volumeDirty = true;

        markUpdateSimulationData();
    }

    @Override
    public void updateCenterOfMass(float x, float y) {
        super.updateCenterOfMass(x, y);
        centerOfMassDirty = true;
        markUpdateSimulationData();
    }

    @Override
    public void updateCenterOfMass(Vector2 centerOfMass) {
        super.updateCenterOfMass(centerOfMass);
        centerOfMassDirty = true;
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
