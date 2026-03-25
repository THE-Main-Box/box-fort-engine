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
        //Criamos a body
        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.KinematicBody;

        bodyDef.position.set(
            transformC.x / PPM,
            transformC.y / PPM
        );

        Body internal = world.createBody(bodyDef);

        //Para cada parte interna
        for (int i = 0; i < parts.size(); i++) {
            SubmarinePart part = parts.get(i);

            //Para cada dado de fixture que devemos criar
            for (int j = 0; j < part.fixtureDefList.size(); j++) {
                FixtureDef def = part.fixtureDefList.get(j);

                //Criamos a fixture, passando a def atual como parâmetro
                Fixture internalFix =  internal.createFixture(def);

                internalFix.setUserData(
                    new GameObjectTag(
                        ObjectType.VEHICLE,
                        part
                    )
                );

                //Liberamos a shape
                def.shape.dispose();
            }

            //Aproveitamos e calculamos os limites da body

            //Por isso é muito importante manter esta sempre o mais simples possível,
            // melhor ainda se for um quadrado ou retângulo
            SubmarinePart.calculateAndStoreBounds(part);

            //Se o calculo foi bem sucedido
            if (!part.isBoundsCalculated()) continue;

            //Criamos o sensor interno delimitando os limites internos da parte
            createInternalAreaSensor(internal, part);

        }

        internal.setUserData(
            new GameObjectTag(
                ObjectType.VEHICLE,
                node
            )
        );
        return internal;
    }

    /// Criamos o sensor que determina qual é a area interna do sub
    public static void createInternalAreaSensor(Body body, SubmarinePart part) {

        // Convertemos as margens de pixels para metros
        // Cada lado pode ter uma margem diferente, permitindo áreas internas assimétricas
        // Ex: uma seção de acoplagem pode ter margem 0 no lado da conexão
        float marginLeft  = part.internalMarginLeft  / PPM;
        float marginRight = part.internalMarginRight / PPM;
        float marginUp    = part.internalMarginUp    / PPM;
        float marginDown  = part.internalMarginDown  / PPM;

        // Calculamos os limites reais da área interna após aplicar as margens
        // Min cresce com a margem esquerda/baixo, Max diminui com a margem direita/cima
        float minX = part.internalMinX + marginLeft;
        float maxX = part.internalMaxX - marginRight;
        float minY = part.internalMinY + marginDown;
        float maxY = part.internalMaxY - marginUp;

        // Dimensões e centro do sensor resultante
        float width   = maxX - minX;
        float height  = maxY - minY;
        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;

        Shape sensorShape = BodyCreatorHelper.createBoxShape(
            width   * PPM,
            height  * PPM,
            centerX * PPM,
            centerY * PPM
        );

        FixtureDef sensorDef = new FixtureDef();
        sensorDef.shape = sensorShape;
        sensorDef.isSensor = true;
        sensorDef.filter.categoryBits = VEHICLE.bit();
        sensorDef.filter.maskBits = VEHICLE_PASSENGER.bit();

        Fixture sensor = body.createFixture(sensorDef);
        sensor.setUserData(new GameObjectTag(ObjectType.DYNAMIC_DRY_AREA, part));
        sensorShape.dispose();
    }
}
