package official.sketchBook.engine.game_object_related.vehicle;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;

import java.util.ArrayList;
import java.util.List;

public class BaseSubmarinePart implements Disposable {

    /// Id de identificação
    public final int id;
    /// Tag para facilitar leitura e identificação
    public final String tag;
    /// Lista de fixtures para criar as sessões para a body do sub
    public final List<FixtureDef> fixtureDataList;

    public float
        internalMargin,
        internalMinX,
        internalMinY,
        internalMaxX,
        internalMaxY;

    /// Flags auxiliares
    private boolean
        boundsCalculated = false,   //Se calculamos as dimensões internas
        disposed = false;

    public BaseSubmarinePart(int id, String tag) {
        this.id = id;
        this.tag = tag;
        this.fixtureDataList = new ArrayList<>();
    }

    /// Calcula a parte interna do sub, passamos uma pequena margem como limite simples
    public static void calculateAndStoreBounds(BaseSubmarinePart part) {
        if (part.isBoundsCalculated()) return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        Vector2 vertex = new Vector2();

        FixtureDef def;
        //Para cada fixture da parte atual
        for (int j = 0; j < part.fixtureDataList.size(); j++) {
            def = part.fixtureDataList.get(j);
            if (def.isSensor) continue;

            PolygonShape shape = (PolygonShape) def.shape;

            for (int i = 0; i < shape.getVertexCount(); i++) {
                shape.getVertex(i, vertex);
                if (vertex.x < minX) minX = vertex.x;
                if (vertex.y < minY) minY = vertex.y;
                if (vertex.x > maxX) maxX = vertex.x;
                if (vertex.y > maxY) maxY = vertex.y;
            }
        }

        part.internalMinX = minX;
        part.internalMinY = minY;
        part.internalMaxX = maxX;
        part.internalMaxY = maxY;

        part.boundsCalculated = true;
    }

    /**
     * Adiciona uma "FixtureDef" na lista para podermos criar ela futuramente
     *
     * @param partRelativeOffsetX offset em relação à grid da body no eixo X
     * @param partRelativeOffsetY offset em relação à grid da body no eixo Y
     * @param offsetX             offset em relação a posição relativa da grid da body no eixo X
     * @param offsetY             offset em relação a posição relativa da grid da body no eixo Y
     * @param width               largura a ser gerada futuramente, passa em pixels
     * @param height              altura a ser gerada futuramente, passa em pixels
     * @param density             densidade da parte
     * @param friction            fricção da parte
     * @param restitution         restituição da parte
     * @param isSensor            se essa parte é um sensor
     * @param categoryBit         quem essa parte é no quesito de colisão
     * @param maskBit             com quem essa parte pode colidir
     */
    public void addBoxFixture(
        float partRelativeOffsetX,
        float partRelativeOffsetY,
        float offsetX,
        float offsetY,
        float width,
        float height,
        float density,
        float friction,
        float restitution,
        boolean isSensor,
        short categoryBit,
        short maskBit
    ) {

        Shape boxShape = BodyCreatorHelper.createBoxShape(
            width,
            height,
            offsetX + partRelativeOffsetX,
            offsetY + partRelativeOffsetY
        );

        FixtureDef def = BodyCreatorHelper.createFixture(
            boxShape,
            density,
            friction,
            restitution,
            categoryBit,
            maskBit
        );

        def.isSensor = isSensor;
        fixtureDataList.add(def);
    }

    public boolean isBoundsCalculated() {
        return boundsCalculated;
    }

    @Override
    public void dispose() {
        if (disposed) return;

        fixtureDataList.clear();

        disposed = true;
    }
}
