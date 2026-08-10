package official.sketchBook.game.gameObject_related.player;

import official.sketchBook.engine.util_related.serialization.SaveData;
import official.sketchBook.engine.util_related.serialization.SaveDataInstance;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;

public class PlayerSaveData extends SaveDataInstance<Player, PlayerContext> {

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

    @Override
    protected Player executeInstantiation(PlayerContext context) {
        Player player = new Player(
            context.getGameObjectContextData().worldDataManager,
            context.getRoomObjectContextData().ownerRoom,
            x,
            y,
            z,
            rotation,
            context.getTransformContextData().width,
            context.getTransformContextData().height,
            context.getTransformContextData().scaleX,
            context.getTransformContextData().scaleY,
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

        return player;
    }

    @Override
    public SaveData save(Player instance) {
        return instance.save();
    }
}
