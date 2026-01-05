package official.sketchBook.engine.util_related.helper;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import static official.sketchBook.game.util_related.constants.PhysicsC.PPM;

public class BodyCreatorHelper {

    /**
     * Criação de uma body Quadrangular
     *
     * @param world       mundo físico a ser instanciado
     * @param width       largura do corpo
     * @param height      altura do corpo
     * @param type        tipo do corpo a ser criado
     * @param restitution restituição de movimento ao colidir
     * @param density     densidade do corpo
     * @param friction    fricção do corpo com outros corpos
     * @param position    posição do corpo
     */
    public static Body createBox(
        World world,
        Vector2 position,
        float width,
        float height,
        BodyDef.BodyType type,
        float density,
        float friction,
        float restitution,
        short category,
        short mask
    ) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = type;
        bodyDef.position.set(                   //Define posição no mundo
            position.x / PPM,
            position.y / PPM
        );

        Body body = world.createBody(           //Instancia uma body
            bodyDef
        );

        PolygonShape shape = createBoxShape(    //Cria um formato em caixa pra body
            width,
            height,
            0,
            0
        );

        body.createFixture(                     //Cria as propriedades da body
            createFixture(                      //Define essas propriedades
                shape,
                density,
                friction,
                restitution,
                category,
                mask
            )
        );

        shape.dispose();


        return body;
    }

    /**
     * Criamos um body com uma fixture circular
     *
     * @param world       mundo físico de onde a body será instanciada
     * @param position    posição na qual a body estará presente (será convertida para metros automaticamente)
     * @param radius      raio da body
     * @param type        tipo do corpo a ser criado
     * @param density     densidade do corpo em questão
     * @param friction    fricção do corpo com o mundo ao redor
     * @param restitution o quanto que a body poderá saltar após uma colisão, uma restituição de movimento
     */
    public static Body createCircle(
        World world,
        Vector2 position,
        float radius,
        BodyDef.BodyType type,
        float density,
        float friction,
        float restitution,
        short category,
        short mask
    ) {
        BodyDef bodyDef = new BodyDef();        //Criamos os valores padrão para o corpo
        bodyDef.type = type;                    //Definimos o tipo do corpo
        bodyDef.position.set(                   //Definimos a posição dentro do world
            position.x / PPM,
            position.y / PPM
        );

        Body body = world.createBody(
            bodyDef                             //Criamos a body
        );

        CircleShape shape = createCircleShape(  //Criamos o formato do corpo
            radius,
            0,
            0
        );

        body.createFixture(                     //Definimos as propriedades da body
            createFixture(                      //Definimos uma propriedade de circulo
                shape,
                density,
                friction,
                restitution,
                category,
                mask
            )
        );

        shape.dispose();

        return body;
    }

    /**
     * Criamos uma shape de círculo
     *
     * @param radius raio do circulo
     * @param rX     posição relativa à body no eixo X
     * @param rY     posição relativa à body no eixo Y
     */
    public static CircleShape createCircleShape(float radius, float rX, float rY) {
        CircleShape shape = new CircleShape();  //Inicia um formato em circulo

        shape.setRadius(                        //Define o raio
            radius / PPM
        );
        shape.setPosition(                      //Define a posição da forma
            new Vector2(
                rX / PPM,
                rY / PPM
            )
        );

        return shape;
    }

    /**
     * Cria um corpo em forma de cápsula (capsule body), que pode ser vertical ou horizontal
     * dependendo da relação entre largura e altura fornecida.
     * <p>
     * A cápsula é formada por dois círculos e dois segmentos de borda conectando-os.
     * - Se largura > altura: a cápsula é horizontal (círculos laterais)
     * - Se altura >= largura: a cápsula é vertical (círculos superior/inferior)
     *
     * @param world       Mundo físico Box2D
     * @param position    Posição central do corpo em pixels
     * @param width       Largura total do corpo em pixels
     * @param height      Altura total do corpo em pixels
     * @param type        Tipo de corpo (estático, dinâmico, cinemático)
     * @param density     Densidade física da fixture
     * @param friction    Fricção da fixture
     * @param restitution Restituição (elasticidade) da fixture
     * @return Um corpo cápsula configurado e pronto para uso
     */
    public static Body createCapsule(
        World world,
        Vector2 position,
        float width,
        float height,
        BodyDef.BodyType type,
        float density,
        float friction,
        float restitution,
        short category,
        short mask
    ) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = type;

        bodyDef.position.set(               //Define a posição
            position.x / PPM,
            position.y / PPM
        );

        Body body = world.createBody(       //Cria uma instancia da body
            bodyDef
        );

        //Caso a largura seja maior que a altura, iremos mudar a orientação da capsule
        boolean isHorizontal = width > height;

        //Dimensões das seções do corpo
        float radius = isHorizontal ?
            height / 2f :
            width / 2f;
        float halfLength = isHorizontal ?
            width / 2f :
            height / 2f;

        // Posição do primeiro circulo em relação ao corpo
        Vector2 circleOffset1 = isHorizontal ?
            new Vector2(
                -halfLength + radius,
                0
            ) :
            new Vector2(
                0,
                -halfLength + radius
            );

        //Posição do segundo circulo em relação ao corpo
        Vector2 circleOffset2 = isHorizontal ?
            new Vector2(
                halfLength - radius,
                0
            ) :
            new Vector2(
                0,
                halfLength - radius
            );

        //Começo da primeira linha
        Vector2 edgeStart1 = isHorizontal ?
            new Vector2(
                (-halfLength + radius) / PPM,
                radius / PPM
            ) :
            new Vector2(
                -radius / PPM,
                (-halfLength + radius) / PPM
            );
        //Final da primeira linha
        Vector2 edgeEnd1 = isHorizontal ?
            new Vector2(
                (halfLength - radius) / PPM,
                radius / PPM
            ) :
            new Vector2(
                -radius / PPM,
                (halfLength - radius) / PPM
            );

        //Começo da segunda linha
        Vector2 edgeStart2 = isHorizontal ?
            new Vector2(
                (-halfLength + radius) / PPM,
                -radius / PPM
            ) :
            new Vector2(
                radius / PPM,
                (-halfLength + radius) / PPM
            );

        //Final da segunda linha
        Vector2 edgeEnd2 = isHorizontal ?
            new Vector2(
                (halfLength - radius) / PPM,
                -radius / PPM
            ) :
            new Vector2(
                radius / PPM,
                (halfLength - radius) / PPM
            );

        // Adiciona o primeiro circulo
        createCircleFixture(
            body,
            radius,
            circleOffset1.x,
            circleOffset1.y,
            density,
            friction,
            restitution,
            category,
            mask
        );
        //Adiciona o segundo circulo
        createCircleFixture(
            body,
            radius,
            circleOffset2.x,
            circleOffset2.y,
            density,
            friction,
            restitution,
            category,
            mask
        );

        //Adiciona primeira reta
        createEdge(
            body,
            edgeStart1,
            edgeEnd1,
            density,
            friction,
            restitution,
            category,
            mask
        );
        //Adiciona segunda reta
        createEdge(
            body,
            edgeStart2,
            edgeEnd2,
            density,
            friction,
            restitution,
            category,
            mask
        );

        return body;
    }


    /**
     * Cria e anexa uma fixture circular a uma body com offset.
     *
     * @param body    Body de destino
     * @param radius  Raio da shape (em pixels)
     * @param offsetX Offset relativo no eixo X (em pixels)
     * @param offsetY Offset relativo no eixo Y (em pixels)
     */
    private static void createCircleFixture(
        Body body,
        float radius,
        float offsetX,
        float offsetY,
        float density,
        float friction,
        float restitution,
        short category,
        short mask
    ) {
        CircleShape shape = createCircleShape(
            radius,
            offsetX,
            offsetY
        );
        body.createFixture(createFixture(
                shape,
                density,
                friction,
                restitution,
                category,
                mask
            )
        );
        shape.dispose();
    }


    private static void createEdge(
        Body body,
        Vector2 start,
        Vector2 end,
        float density,
        float friction,
        float restitution,
        short category,
        short mask
    ) {
        EdgeShape edge = new EdgeShape();
        edge.set(
            start,
            end
        );

        body.createFixture(
            createFixture(
                edge,
                density,
                friction,
                restitution,
                category,
                mask
            )
        );
        edge.dispose();
    }

    /**
     * Cria uma shape de círculo
     *
     * @param height altura da fixture em pixels
     * @param width  largura da fixture em pixels
     * @param rX     posição relativa à body no eixo X
     * @param rY     posição relativa à body no eixo Y
     */
    public static PolygonShape createBoxShape(
        float width,
        float height,
        float rX,
        float rY
    ) {
        PolygonShape shape = new PolygonShape();

        shape.setAsBox(
            (width / 2) / PPM,
            (height / 2) / PPM,
            new Vector2(
                rX / PPM,
                rY / PPM),
            0
        );

        return shape;
    }

    /**
     * Encapsulamento da criação de uma fixture
     *
     * @param friction     Fricção da fixture
     * @param density      Densidade da fixture
     * @param restitution  Restituição da fixture
     * @param shape        Formato do corpo da fixture
     * @param categoryBits O que a fixture é dentro do sistema de colisão
     * @param maskBits     Com o que a fixture pode colidir dentro do sistema de colisão
     */
    public static FixtureDef createFixture(
        Shape shape,
        float density,
        float friction,
        float restitution,
        short categoryBits,
        short maskBits
    ) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = restitution;

        fixtureDef.filter.categoryBits = categoryBits;
        fixtureDef.filter.maskBits = maskBits;

        return fixtureDef;
    }

}
