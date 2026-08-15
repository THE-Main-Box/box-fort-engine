package official.sketchBook.engine.util_related.path;

public class SerializationPaths {
    public static final String ROOT = "dws_files/";
    public static final String SAVE_ROOT = "save_files/";

    public static class Blueprints {
        public static final String BP_ROOT = "blueprint_files/";
        public static final String BP_ROOMS = BP_ROOT + "def_rooms";

        public static class Vehicles {
            public static final String BP_VEHICLES_ROOT = BP_ROOT + "vehicles_bp/";
            public static final String BP_SUB = BP_VEHICLES_ROOT + "submarines/";
            public static final String BP_SUB_PARTS = BP_SUB + "parts/";
        }
    }

    /**
     * Raiz do save atual — pasta própria do jogador, contém subpastas por
     * categoria de entidade savable. Sempre resolvido via método, nunca
     * concatenado à mão fora daqui (evita "getCurrentSaveFilePath() +
     * "/vehicles"" escrito solto em código consumidor).
     */
    public static String getCurrentSaveFilePath() {
        return SAVE_ROOT + World.CURRENT_SAVE_INDEX;
    }

    /**
     * Categorias de dado dentro do save atual. Cada categoria nova
     * (vehicles, player, rooms_state, etc) ganha uma constante aqui — o
     * padrão de composição (getCurrentSaveFilePath() + categoria) fica
     * centralizado, então adicionar uma categoria nova não exige mudar
     * nada fora deste arquivo.
     */
    public static class SaveCategories {
        public static String entities() {
            return getCurrentSaveFilePath() + "/entt";
        }

        public static String vehicles() {
            return getCurrentSaveFilePath() + "/vehicles";
        }
    }

    public static class World {
        public static int CURRENT_SAVE_INDEX = 0;
    }
}
