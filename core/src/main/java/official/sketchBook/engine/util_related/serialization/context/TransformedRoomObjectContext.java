package official.sketchBook.engine.util_related.serialization.context;

import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

/**
 * Espelha o degrau {@code AnimatedRenderableRoomGameObject(...)} ?
 * soma os 9 campos de transform (x, y, z, rotation, width, height,
 * scaleX, scaleY, mirrorX, mirrorY) ao que {@link RoomObjectContext}
 * j� carrega.
 * <p>
 * Usado por QUALQUER classe de jogo que desça at� aqui na heran�a real
 * ? hoje Player � o �nico exemplo, mas inimigos e itens futuros
 * tamb�m v�o precisar exatamente disso, sem repetir os 9 campos em
 * cada Context novo.
 * <p>
 * Os valores de transform aqui s�o os que o DTO leu de {@code
 * loadFields} (ou seja, s�o ESTADO salvo ? posi��o/tamanho da
 * inst�ncia espec�fica), n�o defaults ? por isso vivem no Context (que
 * � montado DEPOIS de loadFields, no momento de newInstance), n�o
 * como constantes fixas.
 */
public class TransformedRoomObjectContext extends RoomObjectContext {

    public final float x, y, z, rotation, width, height, scaleX, scaleY;
    public final boolean mirrorX, mirrorY;

    public TransformedRoomObjectContext(
        BaseGameObjectDataManager worldDataManager,
        PlayableRoom ownerRoom,
        RoomObjectScope roomScope,
        float x,
        float y,
        float z,
        float rotation,
        float width,
        float height,
        float scaleX,
        float scaleY,
        boolean mirrorX,
        boolean mirrorY
    ) {
        super(worldDataManager, ownerRoom, roomScope);
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.width = width;
        this.height = height;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.mirrorX = mirrorX;
        this.mirrorY = mirrorY;
    }
}
