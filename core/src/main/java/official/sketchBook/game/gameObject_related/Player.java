package official.sketchBook.game.gameObject_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import official.sketchBook.engine.animation_rendering_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_rendering_related.SpriteSheetDataHandler;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.JumpCapableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalGameObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.RoomGroundInteractableObject;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.components_related.movement.JumpComponent;
import official.sketchBook.engine.components_related.physics.RoomTileGroundDetection;
import official.sketchBook.engine.components_related.movement.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.gameObject_related.AnimatedRenderableRoomGameObject;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
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
    JumpCapableObjectII {

    public static Texture playerSheet;

    /// Controlador estatico do player
    private static PlayerControllerComponent controllerC;
    /// Componente de movimento
    private MovementComponent moveC;
    /// Componente de aplicação de movimento ao corpo físico
    private MovableObjectPhysicsComponent physicsC;
    /// Componente de detecção de colisão com tiles de room
    private RoomTileGroundDetection roomGroundDetectC;
    /// Componente de pulo
    private JumpComponent jumpC;

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
        boolean xAxisInverted,
        boolean yAxisInverted
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
            xAxisInverted,
            yAxisInverted
        );

        this.animationRenderC.isRenderDimensionEqualsToObject = false;
    }

    @Override
    protected void initObject() {
        super.initObject();

        //Controlador
        initControllerComponent();
        //Gerenciador de animações
        initAnimationControllerComponent();

        //Detecção de colisão
        initGroundDetectionComponent();

        //Aplicador de movimento
        initMovementComponent();

        //Aplicador final de movimento
        initPhysicsComponent();

        //Aplicador de movimento secundário
        initJumpComponent();

        //Renderizador
        initRenderingComponent();
    }

    private void initAnimationControllerComponent() {
        this.toUpdateComponentList.add(
            new PlayerAnimationControllerComponent(this)
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

        this.toUpdateComponentList.add(jumpC);
        this.toPostUpdateComponentList.add(jumpC);
    }

    private void initGroundDetectionComponent() {
        this.roomGroundDetectC = new RoomTileGroundDetection(
            this,
            0,
            -1
        );

        this.toPostUpdateComponentList.add(roomGroundDetectC);
        this.toUpdateComponentList.add(roomGroundDetectC);
    }

    private void initControllerComponent() {
        if (controllerC == null) {
            controllerC = new PlayerControllerComponent(this);
        } else {
            //Fazemos com que o antigo player perca o acesso ao controller
            removeComponentByType(
                controllerC.player,
                PlayerControllerComponent.class,
                true,
                false,
                false
            );

            //Atualizamos quem é o dono do controller
            controllerC.player = this;
        }

        this.toUpdateComponentList.add(controllerC);
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
        this.toUpdateComponentList.add(moveC);
    }

    private void initPhysicsComponent() {
        this.physicsC = new MovableObjectPhysicsComponent(
            this,
            ALLY_ENTITY.bit(),
            SENSOR.bit() | ENVIRONMENT.bit() | PROJECTILES.bit(),
            0.5f,
            1f,
            0f
        );

        this.createBody();

        this.toUpdateComponentList.add(physicsC);
        this.toPostUpdateComponentList.add(physicsC);
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
            aniPlayer
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
        return roomGroundDetectC.isOnGround();
    }

    @Override
    public RoomTileGroundDetection getRoomGroundDetectC() {
        return roomGroundDetectC;
    }

    @Override
    public boolean canRender() {
        return !this.isPendingRemoval();
    }

    @Override
    protected void disposeData() {
        System.out.println("Player limpando dados de instancia");
        body = null;
        moveC = null;
        jumpC = null;
        roomGroundDetectC = null;
    }

    public static void disposeStaticResources() {
        System.out.println("Player limpando dados estaticos");
        playerSheet.dispose();
        controllerC.dispose();

        playerSheet = null;
        controllerC = null;
    }
}
