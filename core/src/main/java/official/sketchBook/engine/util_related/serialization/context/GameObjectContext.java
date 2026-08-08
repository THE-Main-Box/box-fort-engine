package official.sketchBook.engine.util_related.serialization.context;

import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;

/**
 * Espelha o degrau mais baixo da cadeia de construtores de
 * {@code BaseGameObject}: {@code BaseGameObject(worldDataManager)}.
 * <p>
 * Isso N�O muta nem exige mudan�a nenhuma nas classes de jogo reais
 * (BaseGameObject continua abstrata, do jeito que est�) ? � uma
 * �rvore de CONTEXTOS DE INSTANCIA��O, paralela � �rvore de heran�a
 * real, que existe s� pro sistema de save saber o que cada n�vel de
 * SaveDataInstance.executeInstantiation precisa em m�os.
 * <p>
 * Qualquer classe de jogo que pare aqui (hipoteticamente, se algum dia
 * existir um BaseGameObject concreto sem sala/transform) usaria s�
 * isso. Na pr�tica hoje, todo objeto salv�vel do jogo desce pelo menos
 * at� {@link RoomObjectContext}.
 */
public class GameObjectContext {

    public final BaseGameObjectDataManager worldDataManager;

    public GameObjectContext(BaseGameObjectDataManager worldDataManager) {
        this.worldDataManager = worldDataManager;
    }
}
