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


/// Simula interação física com líquidos usando fixtures reais do Box2D — o empuxo e o
/// torque de correção rotacional são emergentes (resolvidos pelo próprio Box2D a partir da
/// força aplicada num ponto de contato calculado pela submersão real de cada fixture).
///
/// ORGANIZAÇÃO DESTA CLASSE:
/// 1. Estado geral (campos, buffers)
/// 2. Ciclo principal (update / updateSimulation)
/// 3. Empuxo por fixture (magnitude + ponto de aplicação ponderado)
/// 4. Centroide da porção submersa (clipping + shoelace, com early-exit)
/// 5. Geometria auxiliar (área, fração de submersão)
/// 6. Drag linear
/// 7. Massa (propagação para MassData da body)
/// 8. Estado de líquido/região
/// 9. Finalização (dispose)
public class PhysicalLiquidInteractionComponent extends LiquidInteractionComponent {
    private static final float MIN_FRACTION_TO_APPLY_FORCE = 0.001f;

    // Capacidade máxima de vértices do polígono recortado: um polígono de N vértices
    // cruzando um plano pode gerar no máximo N+1 vértices no recorte (cada aresta cruzada
    // adiciona 1 ponto de corte, sem remover vértices originais submersos). Box2D limita
    // PolygonShape a 8 vértices, então 16 é uma folga segura sem alocar em runtime.
    private static final int MAX_POLYGON_VERTICES = 8;
    private static final int MAX_CLIPPED_POLYGON_VERTICES = MAX_POLYGON_VERTICES * 2;

    private final Vector2 forceBuffer = new Vector2();
    private final Vector2 aabbMinBuffer = new Vector2();
    private final Vector2 aabbMaxBuffer = new Vector2();

    private final Vector2 fixtureBuoyancyPointBuffer = new Vector2();
    private final Vector2 fixtureVertexBuffer = new Vector2();

    // Buffer dos vértices da fixture em espaço de mundo, preenchido uma única vez por
    // fixture (evita chamar body.getWorldPoint duas vezes: uma para o AABB, outra para o
    // clipping — a mesma passada agora alimenta os dois).
    private final float[] worldVertexX = new float[MAX_POLYGON_VERTICES];
    private final float[] worldVertexY = new float[MAX_POLYGON_VERTICES];

    // Buffers reaproveitados para o polígono recortado.
    private final float[] clippedX = new float[MAX_CLIPPED_POLYGON_VERTICES];
    private final float[] clippedY = new float[MAX_CLIPPED_POLYGON_VERTICES];

    private final MassData massDataBuffer = new MassData();

    // Dirty flags locais ao Physical — MassData é Box2D-específico, não pertence à base.
    // Resolvidas sempre nessa ordem (massa e volume antes de densidade) para que
    // objectDensity = mass/volume nunca fique defasada por 1 frame caso os dois mudem no
    // mesmo tick — relevante em simulações rodando a taxas baixas (ex.: 30 ups).
    private boolean
        inertiaDirty = false,
        massDirty = false,
        volumeDirty = false,
        densityDirty = false;

    private LiquidRegion currentLiquidRegionBuffer;

    private final PhysicsComponent physicsC;

    private boolean disposed = false;

    public PhysicalLiquidInteractionComponent(PhysicalLiquidInteractableObjectII owner) {
        super(owner,owner.getMoveC());
        this.physicsC = owner.getPhysicsC();

//        canInteract = false;
    }

    // ==================================================================
    // 2. CICLO PRINCIPAL
    // ==================================================================

    @Override
    public void update(float delta) {
        updateLiquidState();
        updateSimulation(delta);
    }

    @Override
    protected void updateSimulation(float delta) {
        boolean shouldSimulate = canInteract && !liquidAndRegionMap.isEmpty();

        resolveDirtyPhysicsData();

        // Requer líquido presente; sem isso, o resto do método (empuxo, drag) não roda,
        // mas massa/densidade já foram resolvidas acima independente disso.
        if (!shouldSimulate
            ||
            highestDensityLiquidBuffer == null
            ||
            currentLiquidRegionBuffer == null
        ) return;

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

    // ==================================================================
    // 3. EMPUXO POR FIXTURE (magnitude + ponto de aplicação ponderado)
    // ==================================================================

    /// Passe único sobre as fixtures: acumula a magnitude total de empuxo e, quando o corpo
    /// pode rotacionar, o ponto de aplicação ponderado (centroide real da porção submersa
    /// de cada fixture, pesado pela força que ela contribui). Uma única applyForce no final,
    /// na body, no ponto resultante — Box2D resolve o torque emergente a partir do braço de
    /// alavanca entre esse ponto e o centro de massa.
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
                fixture, surfaceY, liquidDensity, gravity, canRotate, fixtureBuoyancyPointBuffer
            );
            if (buoyancyForceMagnitude <= 0f) continue;

            totalBuoyancyMagnitude += buoyancyForceMagnitude;

            if (canRotate) {
                weightedX += fixtureBuoyancyPointBuffer.x * buoyancyForceMagnitude;
                weightedY += fixtureBuoyancyPointBuffer.y * buoyancyForceMagnitude;
            }
        }

        if (totalBuoyancyMagnitude <= 0f) return;

        forceBuffer.set(0f, totalBuoyancyMagnitude);

        if (canRotate) {
            floatApplicationPoint.set(
                weightedX / totalBuoyancyMagnitude,
                weightedY / totalBuoyancyMagnitude
            );

            body.applyForce(forceBuffer, floatApplicationPoint, true);
        } else {
            body.applyForceToCenter(forceBuffer, true);
        }
    }

    /// Critério de "o que conta como submersível" — hoje é qualquer fixture física
    /// (não-sensor). Ponto único de mudança se esse critério evoluir depois.
    private boolean isSubmersibleFixture(Fixture fixture) {
        return !fixture.isSensor();
    }

    /// Calcula a magnitude de empuxo de uma única fixture e, se canRotate, escreve em
    /// outPoint o centroide da porção submersa (usado como ponto de contribuição ponderada).
    /// Retorna 0f se a fixture está totalmente fora d'água ou se a shape não é suportada.
    ///
    /// Três caminhos, do mais barato ao mais caro:
    ///  - CircleShape: fórmula fechada, sem clipping.
    ///  - PolygonShape totalmente submersa (fração >= 1): usa o centroide real já cacheado
    ///    pelo Box2D via fixture.getMassData() — sem clipping nem shoelace.
    ///  - PolygonShape parcialmente submersa: única passada por vértice calculando AABB e
    ///    populando o buffer de vértices em mundo ao mesmo tempo, seguida de clipping e
    ///    shoelace só nesse subconjunto de fixtures "na casca" da superfície.
    private float computeFixtureBuoyancy(
        Fixture fixture,
        float surfaceY,
        float liquidDensity,
        float gravity,
        boolean canRotate,
        Vector2 outPoint
    ) {
        Shape shape = fixture.getShape();

        if (shape instanceof CircleShape) {
            return computeCircleBuoyancy((CircleShape) shape, fixture, surfaceY, liquidDensity, gravity, canRotate, outPoint);
        }

        if (shape instanceof PolygonShape) {
            return computePolygonBuoyancy((PolygonShape) shape, fixture, surfaceY, liquidDensity, gravity, canRotate, outPoint);
        }

        // Shape não suportada (ex.: EdgeShape) — sem empuxo.
        return 0f;
    }

    private float computeCircleBuoyancy(
        CircleShape circle,
        Fixture fixture,
        float surfaceY,
        float liquidDensity,
        float gravity,
        boolean canRotate,
        Vector2 outPoint
    ) {
        Body body = fixture.getBody();
        Vector2 center = body.getWorldPoint(circle.getPosition());
        float r = circle.getRadius();

        float minY = center.y - r;
        float maxY = center.y + r;

        float fraction = calculateSubmersionFraction(minY, maxY, surfaceY);
        if (fraction < MIN_FRACTION_TO_APPLY_FORCE) return 0f;

        float area = (float) Math.PI * r * r;
        float magnitude = liquidDensity * area * fraction * gravity;

        if (canRotate) {
            // Aproximação: desloca o centro em direção à parte submersa, proporcional a
            // quanto o círculo está afundado (sem recorte exato do segmento circular —
            // a simetria radial do círculo não sofre o cancelamento em X que polígonos
            // simétricos sofriam com a média simples de vértices).
            float depth = surfaceY - center.y;
            float clampedDepth = Math.max(-r, Math.min(r, depth));
            outPoint.set(center.x, center.y + clampedDepth * 0.5f);
        }

        return magnitude;
    }

    private float computePolygonBuoyancy(
        PolygonShape poly,
        Fixture fixture,
        float surfaceY,
        float liquidDensity,
        float gravity,
        boolean canRotate,
        Vector2 outPoint
    ) {
        Body body = fixture.getBody();
        int count = poly.getVertexCount();

        // Passada única: popula worldVertexX/Y e calcula min/maxY ao mesmo tempo — evita
        // chamar getWorldPoint de novo dentro do clipping.
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            poly.getVertex(i, fixtureVertexBuffer);
            Vector2 world = body.getWorldPoint(fixtureVertexBuffer);

            worldVertexX[i] = world.x;
            worldVertexY[i] = world.y;

            if (world.y < minY) minY = world.y;
            if (world.y > maxY) maxY = world.y;
        }

        float fraction = calculateSubmersionFraction(minY, maxY, surfaceY);
        if (fraction < MIN_FRACTION_TO_APPLY_FORCE) return 0f;

        float area = computePolygonAreaFromWorldBuffer(count);
        if (area <= 0f) return 0f;

        float magnitude = liquidDensity * area * fraction * gravity;

        if (!canRotate) return magnitude;

        if (fraction >= 1f) {
            // Totalmente submersa: centroide real já cacheado pelo Box2D, sem clipping.
            outPoint.set(body.getWorldPoint(massDataBuffer.center));
            return magnitude;
        }

        // Parcialmente submersa: única fixture que realmente paga o custo do clipping.
        int clippedCount = buildClippedPolygon(count, surfaceY, clippedX, clippedY);
        boolean found = clippedCount >= 3 && computePolygonCentroid(clippedX, clippedY, clippedCount, outPoint);

        if (!found) {
            // Fallback: recorte degenerado (fixture tangenciando a superfície) — usa o
            // centro do AABB em vez de descartar a contribuição de força dessa fixture.
            outPoint.set((worldVertexX[0] + worldVertexX[count - 1]) * 0.5f, (minY + maxY) * 0.5f);
        }

        return magnitude;
    }

    // ==================================================================
    // 4. CENTROIDE DA PORÇÃO SUBMERSA (clipping + shoelace)
    // ==================================================================

    /// Constrói o polígono recortado (só a parte com depth >= 0, abaixo da superfície) a
    /// partir do buffer de vértices em mundo já populado por computePolygonBuoyancy — sem
    /// nenhuma chamada a getWorldPoint aqui. Clipping de Sutherland-Hodgman especializado
    /// para um único plano de corte horizontal: percorre as N arestas do polígono original,
    /// inserindo pontos de corte interpolados onde a aresta cruza a superfície.
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

    /// Centroide de polígono via fórmula shoelace ponderada por área — não é a média dos
    /// vértices. A média simples cancelaria a assimetria em X para formas simétricas (ex.:
    /// retângulo único rotacionado), o que mantinha o corpo artificialmente "em equilíbrio"
    /// sem braço de alavanca, e portanto sem torque de correção. Retorna false se a área
    /// calculada for degenerada (~0) — recorte quase-linear, fixture tangenciando a
    /// superfície.
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

    // ==================================================================
    // 5. GEOMETRIA AUXILIAR
    // ==================================================================

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

    /// Área da fixture poligonal em metros², via shoelace sobre o buffer de vértices em
    /// mundo já populado — evita recalcular a partir dos vértices locais (o resultado é o
    /// mesmo, já que shoelace é invariante a rotação/translação rígida).
    private float computePolygonAreaFromWorldBuffer(int count) {
        float area = 0f;

        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            area += (worldVertexX[i] * worldVertexY[next]) - (worldVertexX[next] * worldVertexY[i]);
        }

        return Math.abs(area) * 0.5f;
    }

    // ==================================================================
    // 6. DRAG LINEAR
    // ==================================================================

    /// Drag simples proporcional à velocidade linear atual do body, na direção oposta ao
    /// movimento — resistência do líquido, escalada pelo drag do líquido mais resistente
    /// presente.
    private void applyLinearDrag() {
        if (highestDragLiquidBuffer == null) return;

        Body body = physicsC.object.getBody();
        Vector2 velocity = body.getLinearVelocity();

        float dragStrength = highestDragLiquidBuffer.drag;
        if (dragStrength <= 0f) return;

        forceBuffer.set(velocity).scl(-dragStrength * body.getMass()).scl(2);
        body.applyForceToCenter(forceBuffer, true);
    }

    // ==================================================================
    // 7. MASSA (propagação para MassData da body)
    // ==================================================================

    /// Resolve as flags de massa/volume/densidade nessa ordem fixa: massa e volume sempre
    /// são processados antes de densidade. Chamado uma vez por frame, antes de qualquer
    /// cálculo que dependa de objectDensity (empuxo, etc.).
    private void resolveDirtyPhysicsData() {
        if (massDirty) {
            applyMassToBody();
            massDirty = false;
            densityDirty = true;

            inertiaDirty = true;
        }

        if (volumeDirty) {
            volumeDirty = false;
            densityDirty = true;

            inertiaDirty = true;
        }

        if (densityDirty) {
            objectDensity = (volume > 0f) ? (mass / volume) : Float.MAX_VALUE;
            densityDirty = false;
        }

        if(inertiaDirty){
            applyInertiaToBody(mass * volume / 12f);
            inertiaDirty = false;
        }

    }

    private void applyInertiaToBody(float inertia){
        Body body = physicsC.object.getBody();

        massDataBuffer.I = inertia;

        body.setMassData(massDataBuffer);
    }

    /// Aplica a massa atual como MassData manual na body — não usa body.resetMassData()
    /// porque isso derivaria a massa da densidade configurada em cada fixture (via userData
    /// ainda não padronizado o bastante pra isso). Centro de massa e inércia rotacional são
    /// preservados como a body já os tem (lidos antes de sobrescrever); só a massa em si é
    /// trocada, evitando zerar/perder dados que este componente não gerencia.
    private void applyMassToBody() {
        Body body = physicsC.object.getBody();

        massDataBuffer.mass = mass;

        body.setMassData(massDataBuffer);
    }

    /// Define a massa do objeto e marca a flag de dirty local — o recálculo real
    /// (propagação para a body e recálculo de densidade) só acontece na próxima
    /// resolveDirtyPhysicsData(), respeitando a ordem massa → volume → densidade.
    @Override
    public void setMass(float mass) {
        if (mass < 0f) return;
        if (this.mass == mass) return;
        this.mass = mass;
        massDirty = true;
    }

    public float getObjectDensity(){
        return this.objectDensity;
    }

    @Override
    public void setVolume(float volume) {
        if (volume < 0f) return;
        if (this.volume == volume) return;
        this.volume = volume;
        volumeDirty = true;
    }

    // ==================================================================
    // 8. ESTADO DE LÍQUIDO/REGIÃO
    // ==================================================================

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
    // 9. FINALIZAÇÃO
    // ==================================================================

    @Override
    public void dispose() {
        if (disposed) return;
        liquidAndRegionMap.clear();
        disposed = true;
    }
}
