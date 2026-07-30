package official.sketchBook.engine.game_object_related.vehicle_related;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;

public class SubmarinePart implements Disposable {

    /// Id de identificação
    public final int id;
    /// Tag para facilitar leitura e identificação
    public final String tag;

    private SubmarineNode section;

    /// Lista de fixtures para criar as sessões para a body do sub
    public final List<FixtureData> fixtureDataList;
    public final List<Fixture> internalFixtureList;

    private float
        centerX,
        centerY,
        width,
        height,
        volume;

    private float
        internalMinX,
        internalMinY,
        internalMaxX,
        internalMaxY;

    private float
        internalMarginLeft,
        internalMarginRight,
        internalMarginUp,
        internalMarginDown;

    private float
        baseMass;

    /// Flags auxiliares
    private boolean
        boundsCalculated = false,   //Se calculamos as dimensões internas
        disposed = false;

    public boolean massDirty = false;

    public SubmarinePart(int id, String tag) {
        this.id = id;
        this.tag = tag;
        this.fixtureDataList = new ArrayList<>();
        this.internalFixtureList = new ArrayList<>();
    }

    public void setMargins(
        float internalMarginLeft,
        float internalMarginRight,
        float internalMarginUp,
        float internalMarginDown
    ) {
        this.internalMarginDown = internalMarginDown;
        this.internalMarginUp = internalMarginUp;
        this.internalMarginLeft = internalMarginLeft;
        this.internalMarginRight = internalMarginRight;
    }

    /**
     * Adiciona uma "FixtureDef" na lista para podermos criar ela futuramente
     *
     * @param globalOffsetX offset em relação à grid da body no eixo X
     * @param globalOffsetY offset em relação à grid da body no eixo Y
     * @param offsetX       offset em relação a posição relativa da grid da body no eixo X
     * @param offsetY       offset em relação a posição relativa da grid da body no eixo Y
     * @param radius        se for um círculo irá ter um raio
     * @param width         largura a ser gerada futuramente, passa em pixels
     * @param height        altura a ser gerada futuramente, passa em pixels
     * @param isSensor      se essa parte é um sensor
     * @param categoryBit   quem essa parte é no quesito de colisão
     * @param maskBit       com quem essa parte pode colidir
     * @param isCircle      se temos partes circulares na fixture
     */
    public void addInternalFixture(
        float globalOffsetX,
        float globalOffsetY,
        float offsetX,
        float offsetY,
        float radius,
        float width,
        float height,
        int categoryBit,
        int maskBit,
        boolean isCircle,
        boolean isSensor
    ) {
        fixtureDataList.add(
            new FixtureData(
                0,
                0,
                1,
                globalOffsetX,
                globalOffsetY,
                offsetX,
                offsetY,
                radius,
                width,
                height,
                categoryBit,
                maskBit,
                isCircle,
                isSensor
            )
        );
    }

    public void updateBounds() {
        // reset explícito — sem isso, internalMinX/Y ficam em 0f (default do
        // Java) em vez de MAX_VALUE, e uma part sem fixtures válidas "engana"
        // o AABB com bounds falsos ao invés de ficar não-calculada
        internalMinX = Float.MAX_VALUE;
        internalMinY = Float.MAX_VALUE;
        internalMaxX = -Float.MAX_VALUE;
        internalMaxY = -Float.MAX_VALUE;

        boundsCalculated = false;

        SubmarinePartHelper.updateDimensions(this);

        // updateDimensions só marca boundsCalculated = true quando encontrou
        // ao menos uma fixture não-sensor válida (ver checagem internalMinX ==
        // Float.MAX_VALUE dentro dele). Se não achou nenhuma, temos que sair
        // aqui SEM tocar em centerX/centerY/width/height — eles devem
        // permanecer no estado "não calculado" para o node ignorar essa part.
        if (!boundsCalculated) {
            return;
        }

        this.centerX = internalMinX + width / 2;
        this.centerY = internalMinY + height / 2;
    }

    public boolean isBoundsCalculated() {
        return boundsCalculated;
    }

    @Override
    public void dispose() {
        if (disposed) return;

        internalFixtureList.clear();
        fixtureDataList.clear();

        disposed = true;
    }

    public void updateBaseMass(float mass) {
        if (this.baseMass != 0) return;
        this.baseMass = mass;

        massDirty = true;
    }

    public float getBaseMass() {
        return baseMass;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getInternalMinX() {
        return internalMinX;
    }

    public float getInternalMinY() {
        return internalMinY;
    }

    public float getInternalMaxX() {
        return internalMaxX;
    }

    public float getInternalMaxY() {
        return internalMaxY;
    }

    public float getInternalMarginLeft() {
        return internalMarginLeft;
    }

    public float getInternalMarginRight() {
        return internalMarginRight;
    }

    public float getInternalMarginUp() {
        return internalMarginUp;
    }

    public float getInternalMarginDown() {
        return internalMarginDown;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getVolume() {
        return volume;
    }

    /// Massa total da part: estrutural (baseMass) + passageiros atualmente localizados nela.
    public float getTotalMass() {
        return baseMass;
    }

    public void setSection(SubmarineNode node) {
        if (this.section != null) return;
        this.section = node;
    }

    private static class SubmarinePartHelper {

        private static final Vector2 VERTEX_BUFFER_A = new Vector2();
        private static final Vector2 VERTEX_BUFFER_B = new Vector2();

        /**
         * Atualiza:
         * <p>
         * - bounds internos
         * - width
         * - height
         * - volume (m²)
         */
        private static void updateDimensions(SubmarinePart part) {

            float volume = 0f;

            for (int i = 0; i < part.internalFixtureList.size(); i++) {

                Fixture fixture = part.internalFixtureList.get(i);

                if (fixture.isSensor()) {
                    continue;
                }

                Shape shape = fixture.getShape();

                if (shape instanceof PolygonShape) {

                    volume += updatePolygonDimensions(
                        (PolygonShape) shape,
                        part
                    );

                } else if (shape instanceof CircleShape) {

                    volume += updateCircleDimensions(
                        (CircleShape) shape,
                        part
                    );
                }
            }

            if (part.internalMinX == Float.MAX_VALUE) {
                return;
            }

            part.width = part.internalMaxX - part.internalMinX;
            part.height = part.internalMaxY - part.internalMinY;

            part.volume = volume;

            part.boundsCalculated = true;
        }

        /**
         * Atualiza os bounds do polígono e retorna sua área.
         */
        private static float updatePolygonDimensions(
            PolygonShape polygon,
            SubmarinePart part
        ) {
            int count = polygon.getVertexCount();

            float area = 0f;

            polygon.getVertex(
                count - 1,
                VERTEX_BUFFER_A
            );

            for (int i = 0; i < count; i++) {

                polygon.getVertex(
                    i,
                    VERTEX_BUFFER_B
                );

                updateBounds(
                    part,
                    VERTEX_BUFFER_B.x,
                    VERTEX_BUFFER_B.y
                );

                area +=
                    (VERTEX_BUFFER_A.x * VERTEX_BUFFER_B.y)
                        -
                        (VERTEX_BUFFER_B.x * VERTEX_BUFFER_A.y);

                VERTEX_BUFFER_A.set(
                    VERTEX_BUFFER_B
                );
            }

            return Math.abs(area) * 0.5f;
        }

        /**
         * Atualiza os bounds do círculo e retorna sua área.
         */
        private static float updateCircleDimensions(
            CircleShape circle,
            SubmarinePart part
        ) {
            Vector2 position = circle.getPosition();

            float radius = circle.getRadius();

            float minX = position.x - radius;
            float maxX = position.x + radius;

            float minY = position.y - radius;
            float maxY = position.y + radius;

            updateBounds(part, minX, minY);
            updateBounds(part, maxX, maxY);

            return MathUtils.PI * radius * radius;
        }

        /**
         * Atualiza os limites acumulados da part.
         */
        private static void updateBounds(
            SubmarinePart part,
            float x,
            float y
        ) {
            if (x < part.internalMinX) {
                part.internalMinX = x;
            }

            if (x > part.internalMaxX) {
                part.internalMaxX = x;
            }

            if (y < part.internalMinY) {
                part.internalMinY = y;
            }

            if (y > part.internalMaxY) {
                part.internalMaxY = y;
            }
        }

        /// Encontra a SubmarinePart do node cujos bounds contêm a posição global (em
        /// pixels) informada. Estático e leve: recebe apenas a lista de parts e os dados
        /// necessários do node (posição/ângulo do internalBody), sem depender de estado
        /// de instância — pode ser chamado a qualquer momento sem overhead de alocação
        /// (reaproveita nenhum buffer próprio, mas não aloca objetos novos no caminho comum).
        ///
        /// Retorna null se a posição não cair em bounds de nenhuma part calculada (ex:
        /// passageiro momentaneamente fora de qualquer área interna válida).
        public static SubmarinePart findPartContaining(
            List<SubmarinePart> parts,
            float globalPosX,
            float globalPosY,
            float nodeBodyPosXMeters,
            float nodeBodyPosYMeters,
            float nodeBodyAngleRadians
        ) {
            if (parts == null || parts.isEmpty()) return null;

            // Posição global (pixels) convertida para metros, relativa à origem do internalBody
            float relXMeters = toMeters(globalPosX) - nodeBodyPosXMeters;
            float relYMeters = toMeters(globalPosY) - nodeBodyPosYMeters;

            // Desrotaciona pela rotação atual do body — os bounds internos das parts são
            // calculados em espaço LOCAL do body (sem rotação), então precisamos trazer a
            // posição global de volta para esse espaço local antes de comparar contra
            // internalMinX/MaxX/MinY/MaxY.
            float cos = MathUtils.cos(-nodeBodyAngleRadians);
            float sin = MathUtils.sin(-nodeBodyAngleRadians);

            float localX = relXMeters * cos - relYMeters * sin;
            float localY = relXMeters * sin + relYMeters * cos;

            for (int i = 0; i < parts.size(); i++) {
                SubmarinePart part = parts.get(i);
                if (!part.isBoundsCalculated()) continue;

                if (localX >= part.internalMinX && localX <= part.internalMaxX
                    && localY >= part.internalMinY && localY <= part.internalMaxY) {
                    return part;
                }
            }

            return null;
        }
    }
}
