package official.sketchBook.game.gameObject_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import official.sketchBook.engine.animation_rendering_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_rendering_related.SpriteSheetDataHandler;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.*;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.components_related.movement.JumpComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.RayCastGroundDetectionComponent;
import official.sketchBook.engine.components_related.physics.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.animated_renderable_game_object.AnimatedRenderableRoomGameObject;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.pools.RayCastPool;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.components_related.PlayerAnimationControllerComponent;
import official.sketchBook.game.components_related.PlayerControllerComponent;
import official.sketchBook.game.util_related.constants.WorldConstants;
import official.sketchBook.game.util_related.path.GameAssetsPaths;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;
import static official.sketchBook.game.components_related.PlayerAnimationInitializerComponent.initAnimations;

public class Player extends AnimatedRenderableRoomGameObject
    implements
    StaticResourceDisposable,
    MovableObjectII,
    PhysicalGameObjectII,
    RoomGroundInteractableObject,
    JumpCapableObjectII,
    LiquidInteractableObjectII {

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

    private RayCastPool rayCastPoolInstance;

    private PhysicalMobLiquidInteractionComponent liquidInteractionC;

    /// Corpo físico
    private Body body;

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

        this.rayCastPoolInstance = RayCastPool.getInstance(worldDataManager.getPhysicsWorld());
        this.animationRenderC.isRenderDimensionEqualsToObject = false;

        this.initObject();
    }

    @Override
    protected void initObject() {

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

        this.liquidInteractionC.setVolume(
            transformC.width * transformC.height
        );

        this.liquidInteractionC.setMass(240f);

        this.liquidInteractionC.setNeutralBuoyancy(true);
//        this.liquidInteractionC.setCanInteractWithLiquid(false);


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
            rayCastPoolInstance,
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
            WorldConstants.PlayerConstants.MAX_SPEED_X,
            WorldConstants.PlayerConstants.MAX_SPEED_Y,
            0,
            WorldConstants.PlayerConstants.X_DECELERATION,
            WorldConstants.PlayerConstants.Y_DECELERATION,
            0,
            true,
            true,
            false,
            true,
            true,
            false,
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

        liquidInteractionC.setCanInteractWithLiquid(false);

        this.physicsC = new MovableObjectPhysicsComponent(
            this,
            ALLY_ENTITY.bit() | LIQUID_SUBMERGEABLE.bit() | VEHICLE_PASSENGER.bit(),
            SENSOR.bit() | ENVIRONMENT.bit() | PROJECTILES.bit() | LIQUID.bit() | VEHICLE.bit(),
            0.5f,
            1f,
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
            this.transformC.rotation,
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
        System.out.println("Player limpando dados de instancia");
        body = null;
        moveC = null;
        jumpC = null;
        groundDetection = null;
    }

    public static void disposeStaticResources() {
        System.out.println("Player limpando dados estaticos");
        controllerC.dispose();

        controllerC = null;
        disposeSheet();
    }

    @Override
    protected void disposeGeneralData() {

    }

    @Override
    protected void executeDisposeGraphics() {
    }

    private static void disposeSheet(){
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

    }

    @Override
    public PhysicalMobLiquidInteractionComponent getLiquidInteractionC() {
        return liquidInteractionC;
    }
}
