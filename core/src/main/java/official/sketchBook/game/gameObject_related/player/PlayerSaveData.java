package official.sketchBook.game.gameObject_related.player;

import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.util_related.serialization.SaveData;
import official.sketchBook.engine.util_related.serialization.SaveDataException;
import official.sketchBook.engine.util_related.serialization.SaveDataInstance;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.security.InvalidAlgorithmParameterException;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;
import static official.sketchBook.game.util_related.constants.WorldConstants.PlayerConstants.HEIGHT;
import static official.sketchBook.game.util_related.constants.WorldConstants.PlayerConstants.WIDTH;

public class PlayerSaveData extends SaveDataInstance<Player> {

    public static final String TYPE_KEY = "player";

    private boolean
        mirrorX,
        mirrorY,
        canInteract;

    private float
        x,
        y,
        z,
        rotation,
        mass,
        volume;

    @Override
    public void loadFields(SaveData data) {

        x = data.getFloatRequired("x");
        y = data.getFloatRequired("y");
        z = data.getFloatRequired("z");

        mirrorX = data.getBoolean("mirror_x", false);
        mirrorY = data.getBoolean("mirror_y", false);

        rotation = data.getFloatRequired("rotation");

        mass = data.getFloatRequired("mass");
        volume = data.getFloatRequired("volume");

        canInteract = data.getBoolean("can_interact", true);
    }

    private static PhysicalGameObjectDataManager worldDataManager;
    private static PlayableRoom ownerRoom;

    protected Player executeInstantiation() {
        if(ownerRoom == null || worldDataManager == null) {
            throw new SaveDataException(
                "instantiation",
                "PlayerSaveData: dependências de runtime não foram setadas antes de instanciar"
            );
        }
        Player player = new Player(
            worldDataManager,
            ownerRoom,
            x,
            y,
            z,
            rotation,
            WIDTH,
            HEIGHT,
            1,
            1,
            mirrorX,
            mirrorY
        );

        player.getLiquidInteractionC().setVolume(
            volume
        );

        player.getLiquidInteractionC().setMass(
            mass
        );

        player.getLiquidInteractionC().setCanInteract(
            canInteract
        );

        worldDataManager = null;
        ownerRoom = null;

        return player;
    }

    public static void setWorldDataManager(PhysicalGameObjectDataManager worldDataManager) {
        PlayerSaveData.worldDataManager = worldDataManager;
    }

    public static void setOwnerRoom(PlayableRoom ownerRoom) {
        PlayerSaveData.ownerRoom = ownerRoom;
    }

    @Override
    public SaveData save(Player instance) {
        return instance.save();
    }
}
