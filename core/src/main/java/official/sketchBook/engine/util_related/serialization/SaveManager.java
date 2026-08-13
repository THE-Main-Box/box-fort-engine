package official.sketchBook.engine.util_related.serialization;

/**
 * Manager central �nico do sistema de save ? equivalente ao
 * {@code WorldSaver} do padr�o validado (OlegDzhuraev/SaveSystem,
 * Unity): um s� ponto de entrada pra "salvar isso em algum lugar" e
 * "carregar isso de algum lugar", sem quem chama precisar montar
 * SaveDataInstanceRegistry + SavableIO na m�o toda vez.
 * <p>
 * Junta as duas metades j� existentes:
 * <ul>
 *   <li>{@link SaveDataInstanceRegistry} ? sabe QUAL DTO usar pra cada
 *   type key (o "quem" registrado no boot)</li>
 *   <li>{@link SavableIO} ? sabe LER/ESCREVER o JSON em disco, dado um
 *   path + nome (o "onde")</li>
 * </ul>
 * <p>
 * Uso t�pico completo, incluindo a parte que o registry sozinho n�o
 * cobre (instanciar com contexto):
 * <pre>{@code
 * SaveManager saves = new SaveManager(registry);
 *
 * // salvar
 * saves.save("player", "player_0000", "player", playerInstance);
 *
 * // carregar: devolve o DTO, N�O o Player ainda
 * PlayerSaveData dto = saves.loadDto("player", "player_0000");
 * Player player = dto.newInstance(worldContext); // contexto aplicado fora do manager
 * }</pre>
 * O SaveManager deliberadamente NUNCA chama {@code newInstance} sozinho
 * ? ele n�o sabe (nem deveria saber) que contexto cada tipo de objeto
 * precisa. Devolver o DTO e deixar quem chama decidir quando/com que
 * contexto instanciar � o que mant�m isso plug-and-play: um Manager
 * central sem acoplamento a World/PlayableRoom/etc.
 */
public class SaveManager {

    private final SaveDataInstanceRegistry registry;

    public SaveManager(SaveDataInstanceRegistry registry) {
        this.registry = registry;
    }

    /**
     * Salva uma inst�ncia de jogo j� existente, atrav�s do DTO
     * registrado sob {@code typeKey}, no caminho (path + nome)
     * informado.
     */
    public <T> void save(String path, String name, String typeKey, T instance) {
        SaveData data = registry.save(typeKey, instance);
        SavableIO.save(path, name, data);
    }

    /**
     * Carrega e popula o DTO correspondente ao arquivo, descobrindo o
     * tipo sozinho via metadado embutido. N�O instancia o objeto de
     * jogo final ? devolve o DTO pra quem chamou decidir com que
     * contexto instanciar. Lan�a se o arquivo n�o existir.
     */
    public <T> SaveDataInstance<T> loadDto(String path, String name) {
        SaveData data = SavableIO.load(path, name);
        if (data == null) {
            throw new IllegalStateException("Arquivo n�o encontrado: " + path + "/" + name);
        }
        return registry.load(data);
    }

    /** Vers�o que devolve null quando o arquivo simplesmente n�o existe (ex: save novo, sem player salvo ainda). */
    public <T> SaveDataInstance<T> loadDtoOrNull(String path, String name) {
        SaveData data = SavableIO.load(path, name);
        if (data == null) return null;
        return registry.load(data);
    }

    /**
     * Atalho: carrega o DTO E j� instancia com o contexto informado,
     * numa chamada s�. Use quando o contexto j� est� dispon�vel no
     * mesmo lugar em que voc� est� pedindo o load (ex: dentro de
     * GameObjectDataManager.setupSystems(), que j� tem World/room em
     * m�os).
     */
    public <T> T loadAndInstantiate(String path, String name) {
        SaveDataInstance<T> dto = loadDto(path, name);
        return dto.newInstance();
    }

    public SaveDataInstanceRegistry getRegistry() {
        return registry;
    }
}
