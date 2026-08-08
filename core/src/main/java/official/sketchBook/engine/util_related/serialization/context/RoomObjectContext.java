package official.sketchBook.engine.util_related.serialization.context;

import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

/**
 * Espelha o degrau {@code BaseRoomGameObject(worldDataManager,
 * ownerRoom, roomScope)} ? soma {@code ownerRoom} e {@code roomScope}
 * ao que {@link GameObjectContext} j� carrega.
 * <p>
 * Reutiliz�vel por QUALQUER classe de jogo que estenda
 * BaseRoomGameObject sem precisar de transform (posi��o/tamanho) ?
 * hoje nenhuma classe real do projeto para exatamente aqui (Player j�
 * desce at� {@link TransformedRoomObjectContext}), mas a pr�pria
 * exist�ncia desse n�vel intermedi�rio evita que
 * TransformedRoomObjectContext precise duplicar ownerRoom/roomScope
 * na m�o.
 */
public class RoomObjectContext extends GameObjectContext {

    public final PlayableRoom ownerRoom;
    public final RoomObjectScope roomScope;

    public RoomObjectContext(
        BaseGameObjectDataManager worldDataManager,
        PlayableRoom ownerRoom,
        RoomObjectScope roomScope
    ) {
        super(worldDataManager);
        this.ownerRoom = ownerRoom;
        this.roomScope = roomScope;
    }
}
