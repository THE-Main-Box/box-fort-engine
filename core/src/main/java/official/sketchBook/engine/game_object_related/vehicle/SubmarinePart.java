package official.sketchBook.engine.game_object_related.vehicle;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;
import java.util.List;

public class SubmarinePart implements Disposable {

    /// Id de identificação
    public final int id;
    /// Tag para facilitar leitura e identificação
    public final String tag;

    private VehicleSection section;

    /// Lista de fixtures para criar as sessões para a body do sub
    public final List<FixtureData> fixtureDataList;
    public final List<Fixture> internalFixtureList;

    public float
        liquidMass,
        baseMass,
        internalMarginLeft,
        internalMarginRight,
        internalMarginUp,
        internalMarginDown,
        internalMinX,
        internalMinY,
        internalMaxX,
        internalMaxY;

    /// Flags auxiliares
    private boolean
        boundsCalculated = false,   //Se calculamos as dimensões internas
        disposed = false;

    public SubmarinePart(int id, String tag) {
        this.id = id;
        this.tag = tag;
        this.fixtureDataList = new ArrayList<>();
        this.internalFixtureList = new ArrayList<>();
    }

    /// Calcula a parte interna do sub, passamos uma pequena margem como limite simples
    public static void calculateAndStoreBounds(SubmarinePart part) {
        if (part.isBoundsCalculated()) return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        Vector2 vertex = new Vector2();

        for (int j = 0; j < part.internalFixtureList.size(); j++) {
            Fixture fix = part.internalFixtureList.get(j);
            if (fix.isSensor()) continue;

            Shape shape = fix.getShape();

            if (shape instanceof PolygonShape) {
                PolygonShape poly = (PolygonShape) shape;

                for (int i = 0; i < poly.getVertexCount(); i++) {
                    poly.getVertex(i, vertex);

                    if (vertex.x < minX) minX = vertex.x;
                    if (vertex.y < minY) minY = vertex.y;
                    if (vertex.x > maxX) maxX = vertex.x;
                    if (vertex.y > maxY) maxY = vertex.y;
                }
            } else if (shape instanceof CircleShape) {
                CircleShape circle = (CircleShape) shape;

                Vector2 pos = circle.getPosition();
                float r = circle.getRadius();

                float cMinX = pos.x - r;
                float cMaxX = pos.x + r;
                float cMinY = pos.y - r;
                float cMaxY = pos.y + r;

                if (cMinX < minX) minX = cMinX;
                if (cMinY < minY) minY = cMinY;
                if (cMaxX > maxX) maxX = cMaxX;
                if (cMaxY > maxY) maxY = cMaxY;
            }
            // EdgeShape ignorado
        }

        if (minX == Float.MAX_VALUE) return;

        part.internalMinX = minX;
        part.internalMinY = minY;
        part.internalMaxX = maxX;
        part.internalMaxY = maxY;

        part.boundsCalculated = true;
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
                0,
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

    public float getTotalMass() {
        return baseMass + liquidMass;
    }

    public VehicleSection getSection() {
        return section;
    }

    public void setSection(SubmarineNode node) {
        if (this.section == node || node == null || this.section != null) return;
        this.section = node;
    }
}
