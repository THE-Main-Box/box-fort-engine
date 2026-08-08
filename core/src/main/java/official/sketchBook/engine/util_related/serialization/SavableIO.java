package official.sketchBook.engine.util_related.serialization;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import official.sketchBook.engine.util_related.path.SerializationPaths;

/**
 * �nico componente de infraestrutura de I/O do sistema. N�O sabe nada
 * sobre Player/Submarine/Node ? s� sabe pegar um path (vindo de uma
 * constante em {@link SerializationPaths}) + um nome de inst�ncia, e
 * ler/escrever JSON naquele lugar.
 * <p>
 * Usa {@code Gdx.files.external} deliberadamente: {@code local} fica
 * dentro/relativo ao jar (somente-leitura ou inacess�vel em build
 * empacotado), e modders precisam conseguir ler E escrever saves sem
 * mexer na instala��o do jogo.
 * <p>
 * A raiz ({@link SerializationPaths#ROOT}) fica centralizada l�, n�o
 * aqui ? este componente s� concatena ROOT + path da categoria + nome,
 * nunca escreve nenhuma string de caminho pr�pria.
 * <p>
 * Uso t�pico:
 * <pre>{@code
 * SaveData data = SavableIO.load(SerializationPaths.Player.PLAYER, "player_0000");
 * SavableIO.save(SerializationPaths.Player.PLAYER, "player_0000", data);
 * }</pre>
 */
public final class SavableIO {

    private SavableIO() {
    }

    /** Retorna null se o arquivo n�o existir ? quem chama decide o que fazer (default, erro, etc). */
    public static SaveData load(String path, String name) {
        FileHandle handle = fileFor(path, name);
        System.out.println(handle);
        if (!handle.exists()) return null;

        return SaveDataJson.fromJson(handle.readString("UTF-8"));
    }

    /** Cria pastas intermedi�rias automaticamente se necess�rio. */
    public static void save(String path, String name, SaveData data) {
        FileHandle handle = fileFor(path, name);
        handle.writeString(SaveDataJson.toJson(data), false, "UTF-8");
    }

    public static boolean exists(String path, String name) {
        return fileFor(path, name).exists();
    }

    public static void delete(String path, String name) {
        FileHandle handle = fileFor(path, name);
        if (handle.exists()) handle.delete();
    }

    private static FileHandle fileFor(String path, String name) {
        String normalizedPath = path.endsWith("/") ? path : path + "/";
        return Gdx.files.external(SerializationPaths.ROOT + normalizedPath + name + ".txt");
    }
}
