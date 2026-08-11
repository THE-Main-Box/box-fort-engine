package official.sketchBook.engine.world_gen;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.ManagedUpdatableObject;
import official.sketchBook.engine.game_object_related.base_game_object.BaseGameObject;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.model.TileModel;
import official.sketchBook.game.util_related.body.world_gen.RoomBodyFactory;

import java.util.List;

import static official.sketchBook.game.util_related.constants.WorldConstants.TILE_SIZE_PX;

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

    /**
     * Limpa completamente os dados de uma sala que está saindo de cena
     */
    public void cleanUpRoom(PlayableRoom roomToClean) {
        if (roomToClean != null) {
            roomToClean.dispose();
        }
    }

    /**
     * Imprime o conteúdo da matriz formatado por linhas e colunas.
     * Útil para debug de posicionamento e verificação de IDs.
     */
    public void printMatrixContent(int[][] grid) {
        if (grid == null || grid.length == 0) {
            System.out.println("A matriz está vazia ou nula.");
            return;
        }

        System.out.println("--- Início do Debug da Matrix (" + grid.length + "x" + grid[0].length + ") ---");

        for (int[] ints : grid) {
            // Início da linha: um colchete para organizar visualmente
            System.out.print("[");

            for (int anInt : ints) {
                // Imprime o valor seguido de um espaço ou tabulação para manter alinhado
                // Use \t (tab) se os IDs tiverem muitos dígitos diferentes
                System.out.print(anInt + ",");
            }

            // Fim da linha: fecha o colchete e pula para a próxima linha do console
            System.out.println("]");
        }

        System.out.println("--- Fim do Debug da Matrix ---");
    }

}
