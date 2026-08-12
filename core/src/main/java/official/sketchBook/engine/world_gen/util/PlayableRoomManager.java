package official.sketchBook.engine.world_gen.util;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.ManagedUpdatableObject;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.util.List;

public class PlayableRoomManager {

    /**
     * Realiza a transição lógica dos objetos de uma sala para outra.
     * Filtra quem deve ser destruído e quem deve ser notificado da mudança.
     */
    public void transitionRoomObjects(
        List<ManagedUpdatableObject> currentActiveObjects,
        PlayableRoom oldRoom,
        PlayableRoom nextRoom
    ) {
        for (int i = currentActiveObjects.size() - 1; i >= 0; i--) {
            ManagedUpdatableObject obj = currentActiveObjects.get(i);

            if (obj instanceof BaseRoomGameObject) {
                handleObjectScope(
                    (BaseRoomGameObject) obj,
                    oldRoom,
                    nextRoom
                );
            }
        }
    }

    /**
     * Define o destino do objeto com base no seu escopo
     */
    private void handleObjectScope(BaseRoomGameObject obj, PlayableRoom oldRoom, PlayableRoom nextRoom) {
        if (obj.roomScope == RoomObjectScope.LOCAL) {
            // Se é local, ele deve sair da pipeline do manager de objetos
            obj.markToDestroy();
        } else if (obj.roomScope == RoomObjectScope.GLOBAL) {
            // Se é global, notificamos a troca e atualizamos a referência
            obj.onRoomSwitch(oldRoom, nextRoom);
        }
    }

}
