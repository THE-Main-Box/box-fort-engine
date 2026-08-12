package official.sketchBook.engine.world_gen.util;

import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;

/**
 * Percorre todas as camadas de uma PhysicalPlayableRoom e dispara o
 * LayerGenerationResolver correspondente ao estilo de cada uma, via
 * LayerGenerationRegistry.GLOBAL. Camadas com estilo -1 (ou qualquer
 * estilo sem resolver registrado) s?o ignoradas silenciosamente.
 */
public class RoomGenerator {

    private static final int SKIP_STYLE = -1;

    public static void generate(PhysicalPlayableRoom room) {
        int layerCount = room.getLayerCount();

        for (int layer = 0; layer < layerCount; layer++) {
            int style = room.getLayerGenerationStyle(layer);

            if (style == SKIP_STYLE) continue;

            LayerGenerationResolver resolver = LayerGenerationRegistry.GLOBAL.get(style);

            if (resolver == null) continue; // estilo sem resolver registrado ? ignora, n?o quebra a sala inteira

            resolver.resolve(room, layer);
        }
    }
}
