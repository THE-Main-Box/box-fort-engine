package official.sketchBook.engine.world_gen;

import com.badlogic.gdx.utils.Array;
import official.sketchBook.engine.components_related.objects.TimerComponent;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

/**
 * Controla o ciclo de vida de salas que n?o est?o mais ativas (n?o s?o a
 * currentRoom), mas ainda n?o foram descartadas de vez.
 * <p>
 * Quando uma sala deixa de ser a atual, ela n?o sofre dispose imediatamente
 * ? entra aqui com um timer de reten??o. Se o jogador voltar pra ela antes
 * do timer zerar, ela ? resgatada (evict cancelado) sem custo de recria??o.
 * Se o timer zerar sem retorno, sofre dispose real e ? removida.
 * <p>
 * Usa Array em vez de HashMap deliberadamente: o n?mero de salas retidas
 * simultaneamente tende a ser pequeno (poucas unidades), ent?o busca linear
 * por id ? mais barata que hashing, e itera??o por ?ndice evita alocar
 * Iterator/Map.Entry por update ? menos trabalho de GC no longo prazo.
 * <p>
 * N?o ? uma pool no sentido de objetos intercambi?veis (como CustomPool) ?
 * cada sala tem identidade pr?pria (roomId), ent?o aqui s? gerenciamos
 * reten??o/expira??o, nunca reciclagem de inst?ncia.
 */
public class RoomRetentionPool {

    private static final class RetainedRoom {
        final PlayableRoom room;
        final TimerComponent retentionTimer;

        RetainedRoom(PlayableRoom room, float retentionSeconds) {
            this.room = room;
            this.retentionTimer = new TimerComponent(retentionSeconds);
            this.retentionTimer.start();
        }
    }

    private final Array<RetainedRoom> retainedRooms = new Array<>(false, 8);
    private final float defaultRetentionSeconds;

    public RoomRetentionPool(float defaultRetentionSeconds) {
        this.defaultRetentionSeconds = defaultRetentionSeconds;
    }

    /**
     * Marca uma sala como inativa, iniciando seu tempo de reten??o.
     * Se a sala j? estiver retida (ex: marcada duas vezes por engano),
     * ignoramos ? o timer j? em curso n?o ? reiniciado.
     */
    public void retain(PlayableRoom room) {
        retain(room, defaultRetentionSeconds);
    }

    public void retain(PlayableRoom room, float retentionSeconds) {
        if (room == null || isRetained(room.getRoomId())) return;
        retainedRooms.add(new RetainedRoom(room, retentionSeconds));
    }

    /**
     * Resgata uma sala retida pelo id, removendo-a da reten??o (o chamador
     * assume posse dela de volta como sala ativa). Retorna null se a sala
     * n?o estiver retida (j? expirou ou nunca esteve aqui).
     */
    public PlayableRoom reclaim(int roomId) {
        for (int i = 0; i < retainedRooms.size; i++) {
            RetainedRoom retained = retainedRooms.get(i);
            if (retained.room.getRoomId() == roomId) {
                retainedRooms.removeIndex(i);
                return retained.room;
            }
        }
        return null;
    }

    public boolean isRetained(int roomId) {
        for (int i = 0; i < retainedRooms.size; i++) {
            if (retainedRooms.get(i).room.getRoomId() == roomId) return true;
        }
        return false;
    }

    /**
     * Avan?a os timers de reten??o e descarta (dispose real) qualquer sala
     * cujo tempo tenha expirado. Deve ser chamado uma vez por frame/update.
     */
    public void update(float delta) {
        for (int i = retainedRooms.size - 1; i >= 0; i--) {
            RetainedRoom retained = retainedRooms.get(i);
            retained.retentionTimer.update(delta);

            if (retained.retentionTimer.isFinished()) {
                retained.room.dispose();
                retainedRooms.removeIndex(i);
            }
        }
    }

    /**
     * For?a o dispose imediato de todas as salas retidas, ignorando o timer.
     * ?til em shutdown do jogo/mudan?a de n?vel completa.
     */
    public void disposeAllRetained() {
        for (int i = 0; i < retainedRooms.size; i++) {
            retainedRooms.get(i).room.dispose();
        }
        retainedRooms.clear();
    }

    public int getRetainedCount() {
        return retainedRooms.size;
    }
}
