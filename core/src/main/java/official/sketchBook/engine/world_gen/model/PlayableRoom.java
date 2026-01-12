package official.sketchBook.engine.world_gen.model;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.gameObject_related.BaseRoomGameObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayableRoom {

    /// Id da sala
    private final int roomId;

    /// Referência a posição da grid de world
    private final float
        roomXPos,
        roomYPos;

    /// Lista de referência a todas as tiles que estamos usando
    public final Map<Integer, TileModel> tileModelIdMap;

    /// Grade contendo os id das tile
    public int[][] grid;

    /// Dimensões da grid
    public int
        gridWidth,
        gridHeight;

    /// Dimensões da sala vindas da grid
    public int
        roomWidthPx,
        roomHeightPx;

    /// Referência ao mundo físico do box2d, apenas uma referencia para determinar
    /// se a sala tem acesso a esse mundo ou não, ela é quem determinará o fluxo da geração das tiles,
    /// se elas irão ter um corpo físico ou não,
    /// porém isso não dita a existencia de um mundo físico para o manager de objetos nem aos seus objetos
    private final World physicsWorld;

    /// Corpos nativos da sala, são gerenciados pelo manager de sala,
    /// são na verdade os corpos das tiles e outros elementos nativos exclusivos da room
    public List<Body> nativeBodies;

    /// Lista de referência a objetos presentes na sala, são apenas referencias,
    /// e não são gerenciados aqui dentro
    public List<BaseRoomGameObject> roomGameObjectList;

    /// Flag que dita se podemos ou acessar o world
    private final boolean physicsWorldAccessible;

    public boolean disposed = false;

    public PlayableRoom(
        int id,
        float roomX,
        float roomY,
        World physicsWorld
    ) {
        this.roomId = id;                                   //Id da sala
        this.roomXPos = roomX;                              //Posição que iremos começar a gerar a sala no eixo x
        this.roomYPos = roomY;                              //Posição que iremos começar a gerar a sala no eixo y

        this.physicsWorld = physicsWorld;                   //Inicializamos com o world
        this.physicsWorldAccessible = physicsWorld != null; //Validamos se podemos usar o world

        this.tileModelIdMap = new HashMap<>();               //Inicializamos o hashMap
        roomGameObjectList = new ArrayList<>();
    }

    public void addNewRoomGameObject(BaseRoomGameObject roomObject) {
        this.roomGameObjectList.add(roomObject);
    }

    public void removeRoomObject(BaseRoomGameObject roomObjectToRemove) {
        this.roomGameObjectList.remove(roomObjectToRemove);
    }

    public void dispose() {
        if (disposed) return;

        disposeNativeBodies();
        disposeLists();

        disposed = true;
    }

    private void disposeNativeBodies() {
        if (!physicsWorldAccessible || nativeBodies == null) return;
        for (int i = nativeBodies.size() - 1; i >= 0; i--) {
            physicsWorld.destroyBody(nativeBodies.get(i));
        }
    }

    /// Limpa todas as listas
    private void disposeLists() {
        tileModelIdMap.clear();
        roomGameObjectList.clear();

        if (nativeBodies != null) {
            nativeBodies.clear();
        }

    }

    public boolean isPhysicsWorldAccessible() {
        return physicsWorldAccessible;
    }

    public World getPhysicsWorld() {
        return physicsWorld;
    }

    public float getRoomXPos() {
        return roomXPos;
    }

    public float getRoomYPos() {
        return roomYPos;
    }

    public int getRoomId() {
        return roomId;
    }
}
