package official.sketchBook.engine.util_related.path;

/**
 * Paths relativos usados pelo sistema de save (SavableIO/SaveManager).
 * <p>
 * Mesma inten��o do {@link AssetsPaths}: strings de path centralizadas
 * AQUI, classes reais s� referenciam constantes ? nunca escrevem uma
 * string de caminho na m�o.
 * <p>
 * Diferen�a chave em rela��o ao AssetsPaths: estes paths s�o
 * relativos a {@code Gdx.files.external} (pasta de dados do usu�rio,
 * FORA do jar), n�o a {@code Gdx.files.local} (dentro/relativo ao
 * jar, onde os assets normais vivem). Save � dado de runtime do
 * jogador, n�o asset do jogo nem c�digo de engine/game ? por isso tem
 * raiz pr�pria, paralela a "sketchBook/", em vez de aninhada dentro
 * dela junto com assets.
 * <p>
 * {@link #ROOT} � a �nica coisa que {@code SavableIO} precisa conhecer
 * pra resolver qualquer path daqui pra baixo ? category paths
 * (SUBMARINE_NODE, PLAYER, etc) s�o relativos a ROOT, nunca absolutos.
 */
public class SerializationPaths {

    /**
     * Raiz de TODO o sistema de save, fora do jar. Isolada de
     * "sketchBook/" (que continua sendo s� c�digo/engine/game) e de
     * qualquer pasta de assets ? save n�o � nenhuma das duas coisas.
     */
    public static final String ROOT = "serialized/";

    public static class Player {
        public static final String PLAYER = ROOT + "player/";
    }

    public static class Vehicle {
        private static final String VEHICLES = ROOT + "vehicles/";

        public static final String SUBMARINE = VEHICLES + "submarine/";
        public static final String SUBMARINE_NODE = SUBMARINE + "nodes/";
        public static final String SUBMARINE_PART = SUBMARINE + "parts/";
    }

    public static class World {
        public static final String SAVE_INDEX = ROOT + "saves/";
    }
}
