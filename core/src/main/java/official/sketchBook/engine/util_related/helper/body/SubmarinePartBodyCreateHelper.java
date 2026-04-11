package official.sketchBook.engine.util_related.helper.body;

import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.objects.TransformComponent;
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
            float width = part.internalMaxX - part.internalMinX;            //Largura
            float height = part.internalMaxY - part.internalMinY;           //Altura
            float centerX = (part.internalMinX + part.internalMaxX) / 2f;   //Centro no eixo x
            float centerY = (part.internalMinY + part.internalMaxY) / 2f;   //Centro no eixo y

            //Criamos o shape do corpo, sempre o mais simples possível
            Shape externalShape = BodyCreatorHelper.createBoxShape(
                width * PPM,
                height * PPM,
                centerX * PPM,
                centerY * PPM
            );

            //Determinamo algumas coisas importantes para o bom funcionamento da body externa
            FixtureDef externalDef = new FixtureDef();      //Determinamos sua existencia
            externalDef.shape = externalShape;              //Aplicamos seu formato

            /*Como é um corpo que irá interagir com o mundo físico,
             * não pode ser um sensor*/
            externalDef.isSensor = false;

            //O que este é
            externalDef.filter.categoryBits = (short) (
                VEHICLE.bit()
                    |
                    LIQUID_SUBMERGEABLE.bit()
            );

            //Com o que pode interagir
            externalDef.filter.maskBits = (short) (
                ENVIRONMENT.bit()
                    |
                    PROJECTILES.bit()
                    |
                    LIQUID.bit()
            );

            //Adicionamos ao rastreamento de fixture externa a fixture que acabamos de criar
            Fixture externalFix = external.createFixture(externalDef);

            externalFix.setUserData(
                new GameObjectTag(
                    ObjectType.VEHICLE,
                    part
                )
            );

            //Liberamos o uso da shape
            externalShape.dispose();
        }

        //Setamos os dados do corpo com os dados do node, o objeto a quem esta body pertence
        external.setUserData(
            new GameObjectTag(
                ObjectType.VEHICLE,
                node
            )
        );

        return external;

    }

    /// Criamos as bodies internas
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

            for (int j = 0; j < part.fixtureDataList.size(); j++) {
                FixtureData data = part.fixtureDataList.get(j);

                // CAPSULE (não retorna Shape)
                if (data.isCapsule()) {

                    int before = internal.getFixtureList().size;

                    BodyCreatorHelper.createCapsuleFixture(
                        internal,
                        data.width,
                        data.height,
                        (data.offsetX + data.globalOffsetX),
                        (data.offsetY + data.globalOffsetY),
                        data.density,
                        data.friction,
                        data.restitution,
                        data.categoryBit,
                        data.maskBit
                    );

                    // captura fixtures criadas
                    for (int k = before; k < internal.getFixtureList().size; k++) {
                        Fixture fix = internal.getFixtureList().get(k);

                        fix.setSensor(data.isSensor());

                        fix.setUserData(
                            new GameObjectTag(ObjectType.VEHICLE, part)
                        );

                        part.internalFixtureList.add(fix);
                    }

                    continue;
                }

                // CIRCLE / RECTANGLE
                Shape shape = null;

                if (data.isCircle()) {
                    shape = BodyCreatorHelper.createCircleShape(
                        data.radius,
                        (data.offsetX + data.globalOffsetX),
                        (data.offsetY + data.globalOffsetY)
                    );
                } else if (data.isRectangle()) {
                    shape = BodyCreatorHelper.createBoxShape(
                        data.width,
                        data.height,
                        (data.offsetX + data.globalOffsetX),
                        (data.offsetY + data.globalOffsetY)
                    );
                }

                if (shape == null) continue;

                FixtureDef def = BodyCreatorHelper.createFixture(
                    shape,
                    data.density,
                    data.friction,
                    data.restitution,
                    data.categoryBit,
                    data.maskBit
                );

                def.isSensor = data.isSensor();

                Fixture fix = internal.createFixture(def);

                part.internalFixtureList.add(fix);

                fix.setUserData(
                    new GameObjectTag(ObjectType.VEHICLE, part)
                );

                shape.dispose();
            }

            SubmarinePart.calculateAndStoreBounds(part);

            if (!part.isBoundsCalculated()) continue;

            createInternalAreaSensor(internal, part);
        }

        internal.setUserData(
            new GameObjectTag(ObjectType.VEHICLE, node)
        );

        return internal;
    }

    /// Criamos o sensor que determina qual é a area interna do sub
    public static void createInternalAreaSensor(Body body, SubmarinePart part) {

        float toInteriorMargin = 2f;

        // fallback seguro: usa bounds já calculado (independente do formato original)
        float marginLeft = (toInteriorMargin + part.internalMarginLeft) / PPM;
        float marginRight = (toInteriorMargin + part.internalMarginRight) / PPM;
        float marginUp = (toInteriorMargin + part.internalMarginUp) / PPM;
        float marginDown = (toInteriorMargin + part.internalMarginDown) / PPM;

        float minX = part.internalMinX + marginLeft;
        float maxX = part.internalMaxX - marginRight;
        float minY = part.internalMinY + marginDown;
        float maxY = part.internalMaxY - marginUp;

        float width = maxX - minX;
        float height = maxY - minY;
        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;

        Shape shape = BodyCreatorHelper.createBoxShape(
            width * PPM,
            height * PPM,
            centerX * PPM,
            centerY * PPM
        );

        FixtureDef def = new FixtureDef();
        def.shape = shape;
        def.isSensor = true;
        def.filter.categoryBits = VEHICLE.bit();
        def.filter.maskBits = VEHICLE_PASSENGER.bit();

        Fixture sensor = body.createFixture(def);

        sensor.setUserData(
            new GameObjectTag(ObjectType.DYNAMIC_DRY_AREA, part)
        );

        shape.dispose();
    }
}
