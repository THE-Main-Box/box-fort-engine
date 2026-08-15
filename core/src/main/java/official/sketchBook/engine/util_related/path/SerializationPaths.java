package official.sketchBook.engine.util_related.path;

/**
 * Paths relativos usados pelo sistema de save (SavableIO/SaveManager).
 * <p>
 * Mesma inten��o do {@link AssetsPaths}: strings de path centralizadas
 * AQUI, classes reais s� referenciam constantes ? nunca escrevem uma
 * string de caminho na m�o.
 * <p>
 * Diferen�a chave em rela��o ao AssetsPaths: estes paths s�o
 * relativos a {@code Gdx.files.external} (pasta de dados do usu�rio do
 * SISTEMA OPERACIONAL, FORA do jar/reposit�rio), n�o a {@code
 * Gdx.files.local} (dentro/relativo ao jar, onde os assets normais
 * vivem). Save � dado de runtime do jogador, n�o asset do jogo nem
 * c�digo de engine/game ? por isso tem raiz pr�pria.
 * <p>
 * PATH F�SICO FINAL (onde os arquivos realmente ficam no disco):
 * {@code Gdx.files.external} resolve pra pasta HOME do usu�rio do SO,
 * independente de onde o projeto/reposit�rio est� no disco:
 * <ul>
 *   <li>Windows: {@code C:\Users\<usuario>\serialized\...}</li>
 *   <li>Linux: {@code /home/<usuario>/serialized/...}</li>
 *   <li>macOS: {@code /Users/<usuario>/serialized/...}</li>
 * </ul>
 * Ou seja, pra Player, o arquivo fica em (Linux, exemplo):
 * {@code /home/<usuario>/serialized/player/player_0.json}
 * <p>
 * (O nome exato da pasta ? {@link #ROOT}, "serialized/" ? � relativo a
 * essa home, n�o ao reposit�rio do jogo.)
 */
public class SerializationPaths {

    /**
     * Raiz de TODO o sistema de save, fora do jar/reposit�rio. Isolada
     * de "sketchBook/" (que continua sendo s� c�digo/engine/game
     * dentro do reposit�rio) ? save n�o � c�digo nem asset, tem
     * localiza��o pr�pria no disco do usu�rio.
     */
    public static final String ROOT = "dws_files/";
    ///Pasta de arquivos de save
    public static final String SAVE_ROOT = "save_files/";

    ///Pasta de arquivos de blueprints
    public static class Blueprints{

        public static final String BP_ROOT = "blueprint_files/";

        public static final String BP_ROOMS = BP_ROOT + "def_rooms";

        public static final String BP_VEHICLES = BP_ROOT + "vehicles_bp";

        public static final String BP_SUB = BP_VEHICLES + "/submarines/";

        public static final String BP_SUB_PARTS = BP_SUB + "parts";

    }

    public static String getCurrentSaveFilePath(){
        return SAVE_ROOT + World.CURRENT_SAVE_INDEX;
    }

    public static class World {
        public static int CURRENT_SAVE_INDEX = 0;
    }
}
