package official.sketchBook.game.util_related.body.world_gen;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

import static official.sketchBook.game.util_related.constants.WorldConstants.TILE_SIZE_PX;

public class TileBodyFactory {

    /// Cria uma body padrão para as tiles
    public static Body createBoxBodyForTiles(
        World world,
        TileBodyType type,
        BodyDef.BodyType bodyType,
        int x,
        int y,
        int rotation,
        int width,
        int height,
        int totalRows,
        short categoryBit,
        short maskBit
    ) {
        //Dimensão padrão para as tiles
        float tileSize = TILE_SIZE_PX;
        //Encontra a posição y invertida em relação ao mundo
        //A coordenada y é invertida no world, portanto precisamos inverter ela para manter coerente
        int inverseYCord = (totalRows - 1) - y;

        /*
         * Tentamos encontrar a coordenada nos dois eixos, x e y.
         * Porém precisamos que essa coordenada seja o centro do objeto
         * e não leve em conta algum offset como no sistema padrão desta engine
         *
         * TODO: Vale considerar o pequeno offset do eixo y,
         *   esse offset para suavização
         *   talvez acabe causando inconsistências,
         *   importante analisar
         */

        //Obtém a posição do mundo no eixo x
        float worldX = (x + width / 2f) * tileSize;
        //Obtém a posição do mundo no eixo y
        float worldY = (inverseYCord - (height / 2f - 0.5f)) * tileSize;

        //  Criamos uma body quadrada padrão para tiles,
        //  se quisermos criar em formatos diferentes,
        //  vale a pena considerar manipular o formato da box

        return BodyCreatorHelper.createBox(
            world,                              //Mundo físico
            new Vector2(                        //Coordenadas
                worldX,
                worldY
            ),
            rotation,                           //  Rotação
            (width * tileSize),                 //  Largura por index e tamanho de tile
            (height * tileSize),                //  Altura por index e tamanho de tile
            bodyType,                           //  Tipo de corpo estático padrão
            type.getDensity(),                  //  Densidade do corpo
            type.getFriction(),                 //  Fricção pro box2d
            type.getRestt(),                    //  Restituição de força de colisão
            categoryBit,                        //  Quem a tile é na colisão
            maskBit                             //  Com quem a tile pode colidir na colisão
        );
    }

}
