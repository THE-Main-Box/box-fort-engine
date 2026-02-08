package official.sketchBook.engine.util_related.helper.body;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import official.sketchBook.engine.liquid_related.model.ILiquid;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.enumerators.CollisionLayers;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import static official.sketchBook.engine.util_related.helper.CollisionBitImplantation.apply;

public class LiquidBodyCreatorHelper {

    public static void createLiquidFixture(LiquidRegion region, Body body, ILiquid liquid) {
        //Seta a posição da body no centro da criação de mundo,
        // para que a posição relativa das fixtures seja relativa a geração de mundo
        body.setTransform(0, 0, 0);

        // Usa BodyCreatorHelper para criar a shape
        PolygonShape shape = BodyCreatorHelper.createBoxShape(
            region.getWidth(),
            region.getHeight(),
            region.getX(),
            region.getY()
        );

        // Cria FixtureDef como sensor
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;     //Não iremos usar essa body para física, então teremos ela como sensor
        fixtureDef.density = 0;         //Não iremos usar densidade
        fixtureDef.friction = 0;        //Não iremos usar fricção
        fixtureDef.restitution = 0;     //Muito menos restituição

        // Aplica bits de colisão
        apply(
            fixtureDef,
            CollisionLayers.LIQUID.bit(),               // Category
            CollisionLayers.LIQUID_SUBMERGEABLE.bit()   //mask
        );

        //Criamos a fixture em si
        Fixture fixture = body.createFixture(fixtureDef);

        // Aplicamos a userData na fixture para que possamos detectar os dados
        fixture.setUserData(
            new GameObjectTag(
                ObjectType.LIQUID,
                liquid
            )
        );

        //Liberamos o shape
        shape.dispose();
    }
}
