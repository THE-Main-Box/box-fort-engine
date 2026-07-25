package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.PhysicalLiquidInteractableObjectII;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.Map;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toPixels;


public class PhysicalObjectLiquidInteractionComponent extends LiquidInteractionComponent {
    private static final float MIN_FRACTION_TO_APPLY_FORCE = 0.001f;

    private final Vector2 forceBuffer = new Vector2();
    private final Vector2 aabbMinBuffer = new Vector2();
    private final Vector2 aabbMaxBuffer = new Vector2();

    private LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    private LiquidRegion currentLiquidRegionBuffer;

    private final PhysicsComponent physicsC;

    private boolean disposed = false;

    public PhysicalObjectLiquidInteractionComponent(PhysicalLiquidInteractableObjectII owner) {
        super(owner.getMoveC());
        this.physicsC = owner.getPhysicsC();

//        canInteract = false;
    }

    // ==================================================================
    // 2. CICLO PRINCIPAL
    // ==================================================================

    @Override
    public void update(float delta) {
        updateLiquidState();

        if(!canInteract || !inLiquid) return;

        if (currentLiquidRegionBuffer == null) return;
        if (highestDensityLiquidBuffer == null) return;

        applyBuoyancyForEachFixture();
        applyLinearDrag();
    }

    @Override
    protected void updateLiquidState() {
        super.updateLiquidState();

        this.inLiquid = !liquidAndRegionMap.isEmpty();
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    // ==================================================================
    // 3. FLUTUABILIDADE POR FIXTURE (força real, emergente no Box2D)
    // ==================================================================

    /// Itera todas as fixtures físicas (não-sensor) do body e aplica uma força de empuxo
    /// independente em cada uma, no seu próprio centro geométrico — o Box2D resolve torque
    /// automaticamente sempre que a submersão varia entre fixtures.
    private void applyBuoyancyForEachFixture() {
        Body body = physicsC.object.getBody();
        float surfaceY = toMeters(currentLiquidRegionBuffer.getY() + currentLiquidRegionBuffer.getHeight());
        float liquidDensity = highestDensityLiquidBuffer.density;
        float gravity = Math.abs(body.getWorld().getGravity().y);

        Array<Fixture> fixtures = body.getFixtureList();

        for (int i = 0; i < fixtures.size; i++) {
            Fixture fixture = fixtures.get(i);
            if (isSubmersibleFixture(fixture)) {
                applyBuoyancyForFixture(fixture, surfaceY, liquidDensity, gravity);
            }
        }
    }

    /// Critério de "o que conta como submersível" isolado à parte — hoje é qualquer
    /// fixture física (não-sensor). Ponto único de mudança se esse critério evoluir depois
    /// (ex.: uma flag própria por fixture, uma tag específica, etc).
    private boolean isSubmersibleFixture(Fixture fixture) {
        return !fixture.isSensor();
    }

    private void applyBuoyancyForFixture(Fixture fixture, float surfaceY, float liquidDensity, float gravity) {
        computeFixtureAabb(fixture, aabbMinBuffer, aabbMaxBuffer);

        float minY = aabbMinBuffer.y;
        float maxY = aabbMaxBuffer.y;

        float fraction = calculateSubmersionFraction(minY, maxY, surfaceY);
        if (fraction < MIN_FRACTION_TO_APPLY_FORCE) return;

        float area = computeFixtureArea(fixture);
        if (area <= 0f) return;

        float submergedVolume = area * fraction;
        float buoyancyForceMagnitude = liquidDensity * submergedVolume * gravity;

        floatApplicationPoint.set(
            (aabbMinBuffer.x + aabbMaxBuffer.x) * 0.5f,
            (aabbMinBuffer.y + aabbMaxBuffer.y) * 0.5f
        );

        forceBuffer.set(0f, buoyancyForceMagnitude);

        fixture.getBody().applyForce(forceBuffer, floatApplicationPoint, true);
    }

    /// Mesma fórmula do sistema baseado em moveC: fração linear entre minY e maxY da
    /// fixture, comparada contra a superfície — sem clipping geométrico exato.
    private float calculateSubmersionFraction(float minY, float maxY, float surfaceY) {
        if (maxY <= surfaceY) return 1f;
        if (minY >= surfaceY) return 0f;

        float effectiveHeight = maxY - minY;
        if (effectiveHeight <= 0f) return 0f;

        float fraction = (surfaceY - minY) / effectiveHeight;
        return fraction < 0f ? 0f : Math.min(fraction, 1f);
    }

    /// Bounding box da fixture em espaço mundo (metros) — suporta PolygonShape e
    /// CircleShape, os dois tipos já usados no restante do projeto.
    private void computeFixtureAabb(Fixture fixture, Vector2 outMin, Vector2 outMax) {
        Shape shape = fixture.getShape();
        Body body = fixture.getBody();

        if (shape instanceof CircleShape) {
            CircleShape circle = (CircleShape) shape;
            Vector2 worldCenter = body.getWorldPoint(circle.getPosition());
            float r = circle.getRadius();

            outMin.set(worldCenter.x - r, worldCenter.y - r);
            outMax.set(worldCenter.x + r, worldCenter.y + r);
            return;
        }

        if (shape instanceof PolygonShape) {
            PolygonShape poly = (PolygonShape) shape;
            int vertexCount = poly.getVertexCount();

            Vector2 localVertex = new Vector2();
            Vector2 worldVertex;

            poly.getVertex(0, localVertex);
            worldVertex = body.getWorldPoint(localVertex);
            float minX = worldVertex.x, maxX = worldVertex.x;
            float minY = worldVertex.y, maxY = worldVertex.y;

            for (int i = 1; i < vertexCount; i++) {
                poly.getVertex(i, localVertex);
                worldVertex = body.getWorldPoint(localVertex);

                if (worldVertex.x < minX) minX = worldVertex.x;
                if (worldVertex.x > maxX) maxX = worldVertex.x;
                if (worldVertex.y < minY) minY = worldVertex.y;
                if (worldVertex.y > maxY) maxY = worldVertex.y;
            }

            outMin.set(minX, minY);
            outMax.set(maxX, maxY);
            return;
        }

        // Shape não suportado (ex.: EdgeShape) — sem bounds, sem empuxo.
        outMin.set(0f, 0f);
        outMax.set(0f, 0f);
    }

    /// Área da fixture em metros². CircleShape é exato (pi*r²); PolygonShape usa a
    /// fórmula shoelace sobre os vértices locais (não depende de posição/rotação do body).
    private float computeFixtureArea(Fixture fixture) {
        Shape shape = fixture.getShape();

        if (shape instanceof CircleShape) {
            float r = ((CircleShape) shape).getRadius();
            return (float) Math.PI * r * r;
        }

        if (shape instanceof PolygonShape) {
            return computePolygonArea((PolygonShape) shape);
        }

        return 0f;
    }

    /// Fórmula shoelace padrão sobre os vértices locais do polígono.
    private float computePolygonArea(PolygonShape poly) {
        int vertexCount = poly.getVertexCount();
        if (vertexCount < 3) return 0f;

        Vector2 current = new Vector2();
        Vector2 next = new Vector2();

        float area = 0f;

        for (int i = 0; i < vertexCount; i++) {
            poly.getVertex(i, current);
            poly.getVertex((i + 1) % vertexCount, next);

            area += (current.x * next.y) - (next.x * current.y);
        }

        return Math.abs(area) * 0.5f;
    }

    /// Drag simples proporcional à velocidade linear atual do body, na direção oposta ao
    /// movimento — resistência do líquido, escalada pelo drag do líquido mais resistente
    /// presente e por dragMultiplier (aerodinâmica/hidrodinâmica manual do objeto).
    private void applyLinearDrag() {
        if (highestDragLiquidBuffer == null) return;

        Body body = physicsC.object.getBody();
        Vector2 velocity = body.getLinearVelocity();

        float dragStrength = highestDragLiquidBuffer.drag;
        if (dragStrength <= 0f) return;

        forceBuffer.set(velocity).scl(-dragStrength * body.getMass());
        body.applyForceToCenter(forceBuffer, true);
    }

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

        currentLiquidRegionBuffer = closestRegion;
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

    // ==================================================================
    // 6. FINALIZAÇÃO
    // ==================================================================

    @Override
    public void dispose() {
        if (disposed) return;
        liquidAndRegionMap.clear();
        disposed = true;
    }
}
