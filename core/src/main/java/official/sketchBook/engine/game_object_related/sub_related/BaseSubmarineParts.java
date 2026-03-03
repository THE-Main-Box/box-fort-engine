package official.sketchBook.engine.game_object_related.sub_related;

import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.Disposable;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;

import java.util.ArrayList;
import java.util.List;

public class BaseSubmarineParts implements Disposable {

    /// Id de identificação
    public final int id;
    /// Tag para facilitar leitura e identificação
    public final String tag;
    /// Lista de fixtures para criar as sessões para a body do sub
    public final List<FixtureDef> fixtureDataList;

    private boolean disposed = false;

    public BaseSubmarineParts(int id, String tag) {
        this.id = id;
        this.tag = tag;
        this.fixtureDataList = new ArrayList<>();
    }

    /**
     * Adiciona uma "FixtureDef" na lista para podermos criar ela futuramente
     * @param partRelativeOffsetX offset em relação à grid da body no eixo X
     * @param partRelativeOffsetY offset em relação à grid da body no eixo Y
     * @param offsetX offset em relação a posição relativa da grid da body no eixo X
     * @param offsetY offset em relação a posição relativa da grid da body no eixo Y
     * @param width largura a ser gerada futuramente, passa em pixels
     * @param height altura a ser gerada futuramente, passa em pixels
     * @param density densidade da parte
     * @param friction fricção da parte
     * @param restitution restituição da parte
     * @param isSensor se essa parte é um sensor
     * @param categoryBit quem essa parte é no quesito de colisão
     * @param maskBit com quem essa parte pode colidir
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

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        if(disposed) return;

        fixtureDataList.clear();

        disposed = true;
    }
}
