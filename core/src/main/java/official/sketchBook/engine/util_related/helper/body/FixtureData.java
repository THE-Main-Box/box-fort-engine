package official.sketchBook.engine.util_related.helper.body;

public class FixtureData {

    /// Propriedades a serem criadas da fixture
    public final float
        density,                    //Densidade da fixture
        restitution,                //O quanto de movimento que ela mantém no sentido oposto, após a colisão
        friction;                   //Quantidade de fricção

    /// Dimensões e coordenadas
    public final float
        globalOffsetX,              //Coordenada de referência global ao offset natural da fixture no eixo X
        globalOffsetY,              //Coordenada de referência global ao offset natural da fixture no eixo Y
        offsetX,                    //Coordenada de offset da fixture do eixo X
        offsetY,                    //Coordenada de offset da fixture do eixo Y
        radius,                     //Raio da fixture, caso seja um círculo
        width,                      //Dimensão da largura
        height;                     //Dimensão da altura

    /// Dados de colisão
    public final short
        categoryBit,                //Categoria que essa fixture se encaixa (quem ela é na colisão)
        maskBit;                    //Máscara de colisão da fixture (com quem ela pode colidir)

    /// Flag de auxílio
    private final boolean
        isCircle,
        isSensor;

    public FixtureData(
        float density,
        float restitution,
        float friction,
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
        this.density = density;
        this.restitution = restitution;
        this.friction = friction;

        this.globalOffsetX = globalOffsetX;
        this.globalOffsetY = globalOffsetY;

        this.offsetX = offsetX;
        this.offsetY = offsetY;

        this.radius = radius;

        this.width = width;
        this.height = height;

        this.categoryBit = (short) categoryBit;
        this.maskBit = (short) maskBit;

        this.isCircle = isCircle;
        this.isSensor = isSensor;
    }

    public boolean isSensor() {
        return isSensor;
    }

    /// É um círculo apenas caso tenhamos valores de raio, mas não de
    public boolean isCircle() {
        return isCircle && radius != 0;
    }

    /// É um retângulo caso não sejamos um círculo, e caso tenhamos dados de largura e altura
    public boolean isRectangle() {
        return !isCircle && hasWidthAndHeight();
    }

    public boolean isCapsule() {
        return isCircle && hasWidthAndHeight();
    }

    private boolean hasWidthAndHeight() {
        return width != 0 && height != 0;
    }

}
