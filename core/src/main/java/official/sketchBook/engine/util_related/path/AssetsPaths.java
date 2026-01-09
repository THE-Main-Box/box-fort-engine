package official.sketchBook.engine.util_related.path;

public class AssetsPaths {
    public static class BasePath {
        public static final String ATLASES = "atlases/";
        public static final String FONTS = "fonts/";
        public static final String MAPS = "maps/";
        public static final String MUSIC = "music/";
        public static final String SHADERS = "shaders/";
        public static final String SOUNDS = "sounds/";
        public static final String TEXTURES = "textures/";
    }

    public static class SpriteSheetPath {
        public static final String BASE_SS_PATH = BasePath.TEXTURES + "raw/" + "sprite_sheet/";
        public static final String ENTITY_SS_PATH = BASE_SS_PATH + "entities/";
        public static final String TILE_SS_PATH = BASE_SS_PATH + "tiles/";

    }


}
