package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.RoomGroundInteractableObject;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.model.TileModel;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

import static official.sketchBook.game.util_related.constants.WorldC.TILE_SIZE_PX;

public class RoomTileGroundDetection implements Component {

    /*
     * Importante lembrar a respeito da resolução da grid que usarmos,
     * se utilizarmos uma grid com dimensões muito grandes, como 16 ou 32,
     * a detecção de tiles pode não ser a melhor opção, pelo menos não em jogos de plataforma,
     * ou em outros que necessitam de uma detecção por tiles precisa
     */

    /// Referência ao objeto que pode existir dentro de uma sala e interagir com as tiles
    private RoomGroundInteractableObject roomObject;

    /// ultima referencia de sala
    private PlayableRoom lastReferencedRoom;

    /// Valor de offset direcional para checagem na grid
    public int
        checkOffsetX,
        checkOffsetY;

    private int
        lastCellX,
        lastCellY;

    /// Referencia ao corpo da tile, pode ser usado para interpretação de lógica independente de box2d
    private TileBodyType currentTileBodyType;

    /// Determina se estamos no chão ou não
    private boolean onGround = false;

    private boolean disposed = false;

    /**
     * @param checkOffsetX usamos para checar uma tile na posição X relativa a posição da tile neste eixo
     * @param checkOffsetY usamos para checar uma tile na posição Y relativa a posição da tile neste eixo,
     *                     porém é importante lembrar que devido a como o sistema de coordenadas está feito,
     *                     se quisermos olhar em baixo do objeto precisamos usar valores negativos e em cima positivos,
     *                     ou seja estamos lidando com coordenadas invertidas
     */
    public RoomTileGroundDetection(
        RoomGroundInteractableObject object,
        int checkOffsetX,
        int checkOffsetY
    ) {
        this.roomObject = object;

        this.checkOffsetX = checkOffsetX;
        this.checkOffsetY = checkOffsetY;
    }

    @Override
    public void update(float delta) {
        checkRoomSanity();
    }

    @Override
    public void postUpdate() {
        detectRoomTileAt();
    }


    private void checkRoomSanity() {
        PlayableRoom current = roomObject.getOwnerRoom();
        if (current != lastReferencedRoom) {
            // Resetamos o cache de células para forçar a nova detecção
            lastCellX = -1;
            lastCellY = -1;
            currentTileBodyType = null;
            lastReferencedRoom = current;
        }
    }

    private void detectRoomTileAt() {
        PlayableRoom currentRoom = roomObject.getOwnerRoom();
        if (currentRoom == null) {
            onGround = false;
            return;
        }

        TransformComponent transformC = roomObject.getTransformC();

        //pegamos as coordenadas e realizamos uma conversão para o que seria a coordenada padrão para a grid
        int currentTileX = (int) (transformC.x + transformC.getHalfWidth()) / TILE_SIZE_PX,
            currentTileY = (int) (transformC.y + transformC.getHalfHeight()) / TILE_SIZE_PX;

        //Tentamos obter a coordenada de observação de tile
        int cellCheckX = currentTileX + checkOffsetX,
            cellCheckY = (currentRoom.gridHeight - 1) - (currentTileY + checkOffsetY);


        //Se a celula que estivermos tentando identificar estiver fora da area,
        // ignoramos, pois não temos uma coordenada decente
        if (cellCheckX < 0 || cellCheckX > currentRoom.gridWidth - 1) return;
        if (cellCheckY < 0 || cellCheckY > currentRoom.gridHeight - 1) return;

        //Se as celulas forem as mesmas não prosseguimos
        if (cellCheckX == lastCellX && cellCheckY == lastCellY) return;

        lastCellX = cellCheckX;
        lastCellY = cellCheckY;

        TileModel checkedCell = currentRoom.tileModelIdMap.get(//Tentamos acessar o dado pela grid
            currentRoom.grid[cellCheckY][cellCheckX]
        );

        if (checkedCell == null) {
            onGround = false;
            currentTileBodyType = null;
            return;
        }

        TileBodyType newTile = TileBodyType.fromId(checkedCell.getBodyId());

        if (newTile != currentTileBodyType) {
            currentTileBodyType = newTile;
        }

        onGround = currentTileBodyType.isSolid();

    }

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        roomObject = null;

        lastReferencedRoom = null;
        currentTileBodyType = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public TileBodyType getCurrentTileBodyType() {
        return currentTileBodyType;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
