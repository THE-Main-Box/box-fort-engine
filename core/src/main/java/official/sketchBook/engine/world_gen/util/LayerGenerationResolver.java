package official.sketchBook.engine.world_gen.util;

import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;

/**
 * Executa a gera??o de UMA camada espec?fica de uma sala, de acordo com o
 * layerGenerationStyle daquela camada. N?o devolve nada ? cada implementa??o
 * ? respons?vel por aplicar seus pr?prios efeitos (criar bodies e registr?-los
 * em room.nativeBodies, instanciar RoomLiquid, etc), porque diferentes estilos
 * de gera??o produzem coisas fundamentalmente diferentes (f?sica de tile vs
 * l?quido vs o que mais vier), e for?ar um tipo de retorno comum acoplaria
 * o contrato ao primeiro caso de uso.
 * <p>
 * Implementa??es vivem do lado do jogo (official.sketchBook.game), nunca
 * aqui no engine ? o engine s? conhece o contrato.
 */
public interface LayerGenerationResolver {

    int EMPTY_TILE_ID = 0;

    /**
     * @param room  sala sendo gerada (f?sica, pois resolvers de gera??o
     *              sempre precisam de World/nativeBodies ou similar)
     * @param layer ?ndice da camada sendo resolvida
     */
    void resolve(PhysicalPlayableRoom room, int layer);
}
