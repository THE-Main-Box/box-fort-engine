package official.sketchBook.engine.world_gen.util;

import com.badlogic.gdx.utils.IntMap;

/**
 * Registro global de resolvers de gera??o por camada, indexados pelo mesmo
 * int que PhysicalPlayableRoom.layerGenerationStyle usa (-1 = n?o gerar,
 * fora do registro tamb?m = n?o gerar; 0+ = estilo espec?fico).
 * <p>
 * Populado do lado do jogo via register(...), consultado pelo engine sem
 * conhecer as implementa??es concretas ? mesmo padr?o de
 * SaveDataInstanceRegistry (registro est?tico global, chaves conhecidas
 * de fora).
 */
public class LayerGenerationRegistry {

    public static final LayerGenerationRegistry GLOBAL = new LayerGenerationRegistry();

    private final IntMap<LayerGenerationResolver> resolvers = new IntMap<>();

    public void register(int style, LayerGenerationResolver resolver) {
        if (resolvers.containsKey(style)) {
            throw new IllegalArgumentException("j? existe um resolver registrado para o estilo " + style);
        }
        resolvers.put(style, resolver);
    }

    /**
     * @return o resolver do estilo informado, ou null se n?o houver nenhum
     *         registrado (estilo -1 nunca ter? resolver ? ? o pr?prio sinal
     *         de "n?o gerar", ent?o nem deve chegar aqui na pr?tica).
     */
    public LayerGenerationResolver get(int style) {
        return resolvers.get(style);
    }

    public boolean has(int style) {
        return resolvers.containsKey(style);
    }
}
