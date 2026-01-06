package official.sketchBook.game.util_related.enumerators;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;

/**
 * Essa classe contém as propriedades dos corpos das tiles que iremos criar
 */
public enum TileType {
    /*
     *   Bloco inexistente.
     *   Suas propriedades estão por padrão zeradas ou false
     * pois não iremos criar nenhuma body para ele.
     *   IGNORE ESSE BLOCO NA HORA DA CRIAÇÃO DA BODY DE UMA SALA
     */
    EMPTY(
        0,
        false,
        0f,
        0,
        0,
        NONE.bit(),
        NONE.bit(),
        false
    ),

    /*
     *  Bloco sólido padrão.
     *  Possui fricção baixa,
     * mas para objetos que consideram o box2d como fonte de verdade para movimentação
     * irão considerar um atrito baixo nesse bloco.
     *  Densidade é irrelevante.
     *  Resituição completamente irrelevante.
     *  Marcamos no sistema de física como sendo parte do ambiente,
     * ou seja servirá de apoio para outros corpos
     *  Pode interagir com objetos sólidos como entidades, projéteis,
     * mas também pode interagir com sensores.
     *  Pode ser fundida com outras do mesmo tipo
     */
    BLOCK(
        1,
        true,
        0.8f,
        0,
        0,
        ENVIRONMENT.bit(),
        (
            ENTITIES.bit() |
                PROJECTILES.bit() |
                SENSOR.bit()
        ),
        true
    );

    /// Id com o qual podemos identificar a tile
    private final int id;
    /// Se o bloco é sólido, ou seja se pode interagir com outros blocos sólidos
    private final boolean solid;

    /// Constante de fricção
    private final float friction;
    /// Constante de densidade do objeto
    private final float density;
    /// Constante de restituição
    private final float restt;

    /// Determina quem a tile é
    private final short categoryBit;
    /// Determina com quem a tile pode interagir físicamente
    private final short maskBit;

    private final boolean isMergeable;

    TileType(
        int id,
        boolean solid,
        float friction,
        float density,
        float restt,
        float categoryBit,
        float maskBit,
        boolean isMergeable
    ) {
        this.id = id;
        this.solid = solid;
        this.friction = friction;
        this.density = density;
        this.restt = restt;

        this.maskBit = (short) maskBit;
        this.categoryBit = (short) categoryBit;

        this.isMergeable = isMergeable;
    }

    public boolean isMergeable() {
        return isMergeable;
    }

    public short getMaskBit() {
        return maskBit;
    }

    public short getCategoryBit() {
        return categoryBit;
    }

    public float getDensity() {
        return density;
    }

    public float getRestt() {
        return restt;
    }

    public float getFriction() {
        return friction;
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public static TileType fromId(int id) {
        for (TileType type : values()) {
            if (type.id == id) return type;
        }
        return EMPTY;
    }

}
