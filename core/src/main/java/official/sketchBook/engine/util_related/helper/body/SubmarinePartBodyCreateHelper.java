package official.sketchBook.engine.util_related.helper.body;

import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.game_object_related.vehicle.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle.SubmarinePart;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import java.util.List;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class SubmarinePartBodyCreateHelper {

    /// Criamos o corpo externo, o que irá interagir com o mundo físico
    public static Body createExternalBody(
        SubmarineNode node,
        List<SubmarinePart> parts,
        TransformComponent transformC,
        World world
    ) {

        //Criamos a body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(transformC.x / PPM, transformC.y / PPM);

        Body external = world.createBody(bodyDef);

        //Para cada parte existente
        for (int i = 0; i < parts.size(); i++) {
            SubmarinePart part = parts.get(i);
            if (!part.isBoundsCalculated()) continue;

            //A partir das dimensões internas definimos os dados da composição da body
            float width = part.internalMaxX - part.internalMinX;
            float height = part.internalMaxY - part.internalMinY;
            float centerX = (part.internalMinX + part.internalMaxX) / 2f;
            float centerY = (part.internalMinY + part.internalMaxY) / 2f;

            Shape externalShape = BodyCreatorHelper.createBoxShape(
                width * PPM,
                height * PPM,
                centerX * PPM,
                centerY * PPM
            );

            //Determinamo algumas coisas importantes para o bom funcionamento da body externa
            FixtureDef externalDef = new FixtureDef();
            externalDef.shape = externalShape;
            externalDef.isSensor = false;
            externalDef.filter.categoryBits = (short) (VEHICLE.bit() | LIQUID_SUBMERGEABLE.bit());
            externalDef.filter.maskBits = (short) (ENVIRONMENT.bit() | PROJECTILES.bit() | LIQUID.bit());

            //Adicionamos ao rastreamento de fixture externa a fixture que acabamos de criar
            external.createFixture(externalDef);

            externalShape.dispose();
        }

        external.setUserData(
            new GameObjectTag(
                ObjectType.VEHICLE,
                node
            )
        );

        return external;

    }

    /// Criamos as body internas
    public static Body createInternalBody(
        SubmarineNode node,
        List<SubmarinePart> parts,
        TransformComponent transformC,
        World world
    ) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.KinematicBody;
        bodyDef.position.set(transformC.x / PPM, transformC.y / PPM);

        Body internal = world.createBody(bodyDef);

        for (int i = 0; i < parts.size(); i++) {
            SubmarinePart part = parts.get(i);
            for (int j = 0; j < part.fixtureDefList.size(); j++) {
                FixtureDef def = part.fixtureDefList.get(j);

                internal.createFixture(def);

                def.shape.dispose();
            }

            SubmarinePart.calculateAndStoreBounds(part);

            if (!part.isBoundsCalculated()) continue;

            createDryAreaSensor(internal, part);

        }

        internal.setUserData(
            new GameObjectTag(
                ObjectType.VEHICLE,
                node
            )
        );
        return internal;
    }

    public static void createDryAreaSensor(Body body, SubmarinePart part) {
        float margin = part.internalMargin / PPM;

        float width = (part.internalMaxX - part.internalMinX) - margin * 2;
        float height = (part.internalMaxY - part.internalMinY) - margin * 2;
        float centerX = (part.internalMinX + part.internalMaxX) / 2f;
        float centerY = (part.internalMinY + part.internalMaxY) / 2f;

        Shape sensorShape = BodyCreatorHelper.createBoxShape(
            width * PPM,
            height * PPM,
            centerX * PPM,
            centerY * PPM
        );

        FixtureDef sensorDef = new FixtureDef();
        sensorDef.shape = sensorShape;
        sensorDef.isSensor = true;
        sensorDef.filter.categoryBits = VEHICLE.bit();
        sensorDef.filter.maskBits = VEHICLE_PASSENGER.bit();

        Fixture sensor = body.createFixture(sensorDef);
        sensor.setUserData(
            new GameObjectTag(
                ObjectType.DYNAMIC_DRY_AREA,
                part
            )
        );
        sensorShape.dispose();
    }
}
