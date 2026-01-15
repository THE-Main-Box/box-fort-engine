package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.GroundInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.RoomGroundInteractableObject;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.model.TileModel;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

import static official.sketchBook.game.util_related.constants.WorldC.TILE_SIZE_PX;

public class GroundDetectionComponent implements Component {

    /// Referencia ao dono do componente
    private GroundInteractableObjectII object;
    /// Referencia opcional a uma variante do dono do componente
    private RoomGroundInteractableObject roomObject;

    public final Vector2 checkOffset;

    private int
        lastCellX,
        lastCellY;

    /// Variavel auxiliar para determinar se podemos iterar sobre a variante do dono
    private final boolean isRoomObject;

    /// Referencia dinamica a tile que podemos interagir
    private TileBodyType currentInteractableTile;

    private boolean disposed = false;
    private boolean onGround = false;
    private boolean rayCasting = false;

    //TODO:Adicionar um sistema de validação omnidirecional de colisão com tiles solidas
    // usando rayCasting para não termos que ser dependentes do sistema de coordenadas da sala,
    // vale lembrar que as tiles que,
    // em sua maioria usam as tags correspondentes que determinam se são físicas ou não

    public GroundDetectionComponent(
        GroundInteractableObjectII object,
        Vector2 checkOffset
    ) {
        this.object = object;
        this.isRoomObject = object instanceof RoomGroundInteractableObject;

        if (isRoomObject) {
            this.roomObject = (RoomGroundInteractableObject) object;
        }

        this.checkOffset = checkOffset;
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void postUpdate() {
        if (isRoomObject && !rayCasting) {
            detectRoomTileAt();
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
        int cellCheckX = currentTileX + (int) checkOffset.x,
            cellCheckY = (currentRoom.gridHeight - 1) - (currentTileY + (int) checkOffset.y);


        //Se a celula que estivermos tentando identificar estiver fora da area,
        // ignoramos, pois não temos uma coordenada decente
        if (cellCheckX < 0 || cellCheckX > currentRoom.gridWidth -1) return;
        if (cellCheckY < 0 || cellCheckY > currentRoom.gridHeight -1) return;

        //Se as celulas forem as mesmas não prosseguimos
        if (cellCheckX == lastCellX && cellCheckY == lastCellY) return;

        lastCellX = cellCheckX;
        lastCellY = cellCheckY;

        TileModel checkedCell = currentRoom.tileModelIdMap.get(//Tentamos acessar o dado pela grid
            currentRoom.grid[cellCheckY][cellCheckX]
        );

        if (checkedCell == null) {
            onGround = false;
            currentInteractableTile = null;
            return;
        }

        //Atualiza a tile que estamos colidindo atualmente
        TileBodyType newTile = TileBodyType.fromId(checkedCell.getBodyId());

        if (newTile != currentInteractableTile) {
            currentInteractableTile = newTile;
        }

        onGround = currentInteractableTile.isSolid();
    }

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        object = null;
        roomObject = null;

        currentInteractableTile = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public TileBodyType getCurrentInteractableTile() {
        return currentInteractableTile;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
