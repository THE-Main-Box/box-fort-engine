package official.sketchBook.engine.world_gen.model;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhysicalPlayableRoom extends PlayableRoom {

    protected final World physicsWorld;
    public List<Body> nativeBodies;
    protected final boolean physicsWorldAccessible;

    /**
     * Estilo de gera??o f?sica por camada, ?ndice = ?ndice da camada na grid.
     * -1 = n?o gerar/ignorar essa camada, 0 = gera??o padr?o (grid comum),
     * 1+ = estilos especiais.
     */
    protected int[] layerGenerationStyles;

    /**
     * Body est?tica ?nica compartilhada da sala, onde vivem todas as fixtures
     * de tiles configuradas com ownBodyPerCluster=false (a maioria: blocos
     * comuns, slopes, etc). Criada sob demanda na primeira vez que alguma
     * tile pedir ela ? nunca alocada preventivamente.
     */
    protected Body sharedStaticBody;

    public PhysicalPlayableRoom(int id, float roomX, float roomY, World physicsWorld) {
        super(id, roomX, roomY);
        this.physicsWorld = physicsWorld;
        this.physicsWorldAccessible = physicsWorld != null;
    }

    @Override
    public void initRoomGrid(int[][][] roomTileGrid, int pixelsPerTile) {
        super.initRoomGrid(roomTileGrid, pixelsPerTile);

        this.layerGenerationStyles = new int[getLayerCount()];
        Arrays.fill(this.layerGenerationStyles, -1);
    }

    /**
     * Determina o estilo de geração de uma camada
     * @param layerIndex a camada que iremos atualizar o dado de geração
     * @param style indice de geração
     * */
    public void setLayerGenerationStyle(int layerIndex, int style) {
        if (layerGenerationStyles == null) {
            throw new IllegalStateException("grid ainda n?o foi inicializada (initRoomGrid)");
        }
        layerGenerationStyles[layerIndex] = style;
    }

    public int getLayerGenerationStyle(int layerIndex) {
        if (layerGenerationStyles == null) {
            throw new IllegalStateException("grid ainda n?o foi inicializada (initRoomGrid)");
        }
        return layerGenerationStyles[layerIndex];
    }

    /**
     * Retorna a body est?tica compartilhada da sala, criando-a na primeira
     * chamada (sob demanda). Todas as fixtures de tiles n?o-independentes
     * (ownBodyPerCluster=false) s?o anexadas nela, em vez de cada uma ganhar
     * sua pr?pria Body ? reduz drasticamente o n?mero de bodies est?ticas
     * no world.
     *
     * @throws IllegalStateException se o world f?sico n?o estiver acess?vel
     */
    public Body getOrCreateSharedStaticBody() {
        if (!physicsWorldAccessible) {
            throw new IllegalStateException("physicsWorld n?o est? acess?vel nesta sala");
        }

        if (sharedStaticBody == null) {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.StaticBody;
            bodyDef.position.set(roomXPos, roomYPos);

            sharedStaticBody = physicsWorld.createBody(bodyDef);

            if (nativeBodies == null) nativeBodies = new ArrayList<>();
            nativeBodies.add(sharedStaticBody);
        }

        return sharedStaticBody;
    }

    @Override
    protected void executeDispose() {
        disposeNativeBodies();
        layerGenerationStyles = null;
        sharedStaticBody = null; // j? destru?da dentro de disposeNativeBodies, s? limpamos a refer?ncia
        super.executeDispose();
    }

    private void disposeNativeBodies() {
        if (!physicsWorldAccessible || nativeBodies == null) return;
        for (int i = nativeBodies.size() - 1; i >= 0; i--) {
            physicsWorld.destroyBody(nativeBodies.get(i));
        }
        nativeBodies.clear();
        nativeBodies = null;
    }

    public boolean isPhysicsWorldAccessible() {
        return physicsWorldAccessible;
    }

    public World getPhysicsWorld() {
        return physicsWorld;
    }
}
