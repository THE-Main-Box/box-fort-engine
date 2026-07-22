package official.sketchBook.game.gameObject_related;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import official.sketchBook.engine.animation_rendering_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_rendering_related.SpriteSheetDataHandler;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractionTriggerer;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.LiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.JumpCapableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.PhysicalGameObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.RoomGroundInteractableObject;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle.SubmarinePassenger;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.components_related.interact.InteractTriggerComponent;
import official.sketchBook.engine.components_related.movement.JumpComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.physics.*;
import official.sketchBook.engine.components_related.system_utils.SubmersibleVolume;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.animated_renderable_game_object.AnimatedRenderableRoomGameObject;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.pools.RayCastPool;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.components_related.player.PlayerAnimationControllerComponent;
import official.sketchBook.game.components_related.player.PlayerControllerComponent;
import official.sketchBook.game.util_related.constants.WorldConstants;
import official.sketchBook.game.util_related.path.GameAssetsPaths;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.components_related.player.PlayerAnimationInitializerComponent.initAnimations;

public class Player extends AnimatedRenderableRoomGameObject
    implements
    StaticResourceDisposable,
    MovableObjectII,
    PhysicalGameObjectII,
    RoomGroundInteractableObject,
    JumpCapableObjectII,
        LiquidInteractableObjectII,
    SubmarinePassenger,
    InteractionTriggerer {

    private boolean inScreen = true;

    public static boolean sheetDisposed = false;
    public static Texture playerSheet;

    /// Controlador estatico do player
    private static PlayerControllerComponent controllerC;
    /// Componente de movimento
    private MovementComponent moveC;
    /// Componente de aplicação de movimento ao corpo físico
    private MovableObjectPhysicsComponent physicsC;
    /// Componente de detecção de colisão com rayCast
    private RayCastGroundDetectionComponent groundDetection;
    /// Componente de pulo
    private JumpComponent jumpC;

    private PhysicalMobLiquidInteractionComponent liquidInteractionC;

    private InteractTriggerComponent triggerC;

    /// Corpo físico
    private Body body;

    private final List<SubmersibleVolume> submersibleVolumeList;

    public Player(
        PhysicalGameObjectDataManager worldDataManager,
        PlayableRoom ownerRoom,
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
        super(
            worldDataManager,
            ownerRoom,
            RoomObjectScope.GLOBAL,
            x,
            y,
            z,
            rotation,
            width,
            height,
            scaleX,
            scaleY,
            mirrorX,
            mirrorY
        );

        this.animationRenderC.isRenderDimensionEqualsToObject = false;

        this.initObject();

        this.submersibleVolumeList = new ArrayList<>();
        this.submersibleVolumeList.add(
            new SubmersibleVolume(
                0,
                0,
                transformC.width,
                transformC.height
            )
        );

    }

    @Override
    public void initObject() {

        //Controlador
        initControllerComponent();
        //Gerenciador de animações
        initAnimationControllerComponent();

        //Aplicador de movimento
        initMovementComponent();

        //Aplicador final de movimento
        initPhysicsComponent();

        //Aplicador de movimento secundário
        initJumpComponent();

        //Renderizador
        initRenderingComponent();

        //Detecção de colisão
        initGroundDetectionComponent();

        initTriggerComponent();

        this.liquidInteractionC.setVolume(
            transformC.width * transformC.height
        );

        this.liquidInteractionC.setMass(200f);

    }

    private void initTriggerComponent() {
        this.triggerC = new InteractTriggerComponent(this);

        this.managerC.add(
            triggerC,
            true,
            false
        );
    }

    private void initAnimationControllerComponent() {
        this.managerC.add(
            new PlayerAnimationControllerComponent(this),
            true,
            false
        );
    }

    private void initJumpComponent() {
        jumpC = new JumpComponent(
            this,
            WorldConstants.PlayerConstants.JUMP_FORCE,
            WorldConstants.PlayerConstants.FALL_SPEED_AFTER_JUMP_CANCEL,
            WorldConstants.PlayerConstants.COYOTE_T,
            WorldConstants.PlayerConstants.JUMP_BUFF_T,
            0.2f,
            1f,
            1,
            false
        );

        this.managerC.add(
            jumpC,
            true,
            true
        );
    }

    private void initGroundDetectionComponent() {
        this.groundDetection = new RayCastGroundDetectionComponent(
            this,
            RayCastPool.getInstance(),
            ObjectType.ENVIRONMENT,
            ObjectType.VEHICLE
        );

        this.managerC.add(
            groundDetection,
            false,
            true
        );
    }

    private void initControllerComponent() {
        if (controllerC == null || controllerC.player == null) {
            controllerC = new PlayerControllerComponent(this);
        } else {

            //Fazemos com que o antigo player perca o acesso ao controller
            controllerC.player.managerC.remove(
                PlayerControllerComponent.class,
                true,
                false,
                false
            );

            //Atualizamos quem é o dono do controller
            controllerC.player = this;
        }

        this.managerC.add(
            controllerC,
            true,
            false
        );
    }

    private void initMovementComponent() {
        this.moveC = new MovementComponent(
            this,
            WorldConstants.PlayerConstants.MAX_MOVE_SPEED_X,
            WorldConstants.PlayerConstants.MAX_MOVE_SPEED_Y,
            WorldConstants.PlayerConstants.MAX_MOVE_SPEED_R,
            WorldConstants.PlayerConstants.MAX_SPEED_X,
            WorldConstants.PlayerConstants.MAX_SPEED_Y,
            WorldConstants.PlayerConstants.MAX_SPEED_R,
            WorldConstants.PlayerConstants.X_DECELERATION,
            WorldConstants.PlayerConstants.Y_DECELERATION,
            WorldConstants.PlayerConstants.R_DECELERATION,
            true,
            true,
            false,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            true
        );

        this.managerC.add(
            moveC,
            true,
            false
        );
    }

    private void initPhysicsComponent() {
        this.liquidInteractionC = new PhysicalMobLiquidInteractionComponent(this);

        this.physicsC = new VehiclePassengerPhysicsComponent(
            this,
            WorldConstants.PlayerConstants.categoryBit,
            WorldConstants.PlayerConstants.maskBit,
            0.5f,
            2f,
            0f
        );

        this.createBody();

        this.managerC.add(
            liquidInteractionC,
            true,
            false
        );

        this.managerC.add(
            physicsC,
            true,
            true
        );
    }

    private void initRenderingComponent() {
        if (playerSheet == null) {
            playerSheet = new Texture(GameAssetsPaths.EntitiesAssetsPaths.PLAYER_SHEET_PATH);
        }

        ObjectAnimationPlayer aniPlayer = new ObjectAnimationPlayer();
        SpriteSheetDataHandler sheetHandler = new SpriteSheetDataHandler(
            transformC.x,
            transformC.y,
            8,
            0,
            5,
            4,
            transformC.getScaleX(),
            transformC.getScaleY(),
            transformC.mirrorX,
            transformC.mirrorY,
            true,
            true,
            playerSheet
        );

        initAnimations(aniPlayer);

        this.animationRenderC.addNewLayer(
            sheetHandler,
            aniPlayer,
            transformC
        );
    }

    private void createBody() {
        body = BodyCreatorHelper.createBox(
            this.getPhysicalManager().getPhysicsWorld(),
            new Vector2(
                this.transformC.x,
                this.transformC.y
            ),
            this.transformC.getRotation(),
            this.transformC.width,
            this.transformC.height,
            BodyDef.BodyType.DynamicBody,
            physicsC.getDensity(),
            physicsC.getFrict(),
            physicsC.getRest(),
            physicsC.getCategoryBit(),
            physicsC.getMaskBit()
        );

        this.body.setLinearDamping(0);

        this.body.setUserData(
            new GameObjectTag(
                ObjectType.ENTITY,
                this
            )
        );

    }

    @Override
    public void onObjectAndBodyPosSync() {
    }

    @Override
    public void onRoomSwitch(PlayableRoom oldRoom, PlayableRoom newRoom) {
        super.onRoomSwitch(oldRoom, newRoom);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

//        System.out.println(moveC.dataComponent.xAxis.velocity);
    }

    @Override
    public void postUpdate() {
        super.postUpdate();
    }

    @Override
    public void updateVisuals(float delta) {
        super.updateVisuals(delta);
    }

    @Override
    protected void onObjectDestruction() {
        super.onObjectDestruction();
    }

    public PlayerControllerComponent getControllerC() {
        return controllerC;
    }

    public MovementComponent getMoveC() {
        return moveC;
    }

    @Override
    public JumpComponent getJumpC() {
        return jumpC;
    }

    @Override
    public boolean canJump() {
        return jumpC.isCoyoteJumpAvailable() || this.isOnGround();
    }

    @Override
    public Body getBody() {
        return body;
    }

    @Override
    public PhysicsComponent getPhysicsC() {
        return physicsC;
    }

    @Override
    public PhysicalGameObjectDataManager getPhysicalManager() {
        return (PhysicalGameObjectDataManager) this.worldDataManager;
    }

    @Override
    public boolean isOnGround() {
        return groundDetection.isOnGround();
    }

    public RayCastGroundDetectionComponent getGroundDetectionC() {
        return groundDetection;
    }

    @Override
    public boolean canRender() {
        return !this.isPendingRemoval();
    }

    @Override
    public boolean isInScreen() {
        return inScreen;
    }

    public void setInScreen(boolean inScreen) {
        this.inScreen = inScreen;
    }

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();
        body = null;
        moveC = null;
        jumpC = null;
        groundDetection = null;
        triggerC = null;

    }

    public static void disposeStaticResources() {
        controllerC.dispose();

        controllerC = null;
        disposeSheet();
    }

    @Override
    protected void disposeGeneralData() {
        submersibleVolumeList.clear();
    }

    @Override
    protected void executeDisposeGraphics() {
    }

    private static void disposeSheet() {
        if (sheetDisposed) return;
        playerSheet.dispose();

        playerSheet = null;

        sheetDisposed = true;

    }

    @Override
    public void onLiquidExit() {

    }

    @Override
    public void onLiquidEnter() {
    }

    @Override
    public void inLiquidUpdate() {

        if(Gdx.input.isKeyPressed(
            Input.Keys.R
        )){

            this.liquidInteractionC.setMass(
                liquidInteractionC.getMass() - 100
            );
        } else if(Gdx.input.isKeyPressed(
            Input.Keys.C
        )){
            this.liquidInteractionC.setMass(
                liquidInteractionC.getMass() + 100
            );
        }

    }

    @Override
    public List<SubmersibleVolume> getSubmersibleVolumeList() {
        return submersibleVolumeList;
    }

    @Override
    public PhysicalMobLiquidInteractionComponent getLiquidInteractionC() {
        return liquidInteractionC;
    }

    @Override
    public VehiclePassengerPhysicsComponent getVehiclePassengerPhysicsC() {
        return (VehiclePassengerPhysicsComponent) physicsC;
    }

    @Override
    public InteractTriggerComponent getTriggerC() {
        return triggerC;
    }

    @Override
    public Vector2 getCoordinatesInMeters() {
        return this.body.getPosition();
    }

}
