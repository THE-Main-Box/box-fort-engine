package official.sketchBook.game.gameObject_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import official.sketchBook.engine.animation_rendering_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_rendering_related.Sprite;
import official.sketchBook.engine.animation_rendering_related.SpriteSheetDataHandler;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.JumpCapableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
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
import official.sketchBook.game.components_related.PlayerControllerComponent;
import official.sketchBook.game.util_related.path.GameAssetsPaths;
import official.sketchBook.game.util_related.values.AnimationKeys;

import java.util.Arrays;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;

public class Player extends AnimatedRenderableRoomGameObject
    implements
    StaticResourceDisposable,
    MovableObjectII,
    PhysicalObjectII,
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
    private short maskBit, categoryBit;
    private float density, frict, rest;

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

        initBodyData();
        createBody();
        initPhysicsComponent();

        initRenderingComponent();
        initControllerComponent();
        initMovementComponent();
        initJumpComponent();
        initGroundDetectionComponent();
    }

    private void initJumpComponent() {
        jumpC = new JumpComponent(
            this,
            40,
            200,
            0.1f,
            0.2f,
//            aniPlayer.getTotalAnimationTime(aniPlayer.getAnimationByKey(afterFall)),
            0.2f,
            1f,
            1,
            false
        );

        this.toUpdateComponentList.add(jumpC);
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
            250,
            500,
            999f,
            0,
            true,
            true,
            true,
            true,
            true,
            true,
            false
        );
        this.toUpdateComponentList.add(moveC);
    }

    private void initPhysicsComponent() {
        this.physicsC = new MovableObjectPhysicsComponent(this);

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

    private void initAnimations(ObjectAnimationPlayer aniPlayer) {
        aniPlayer.addAnimation(AnimationKeys.Entities.idle, Arrays.asList(
            new Sprite(0, 0, 0.15f),
            new Sprite(1, 0, 0.15f),
            new Sprite(2, 0, 0.15f),
            new Sprite(3, 0, 0.15f)
        ));

        aniPlayer.playAnimation(AnimationKeys.Entities.idle);
    }

    protected void initBodyData() {
        this.categoryBit = ALLY_ENTITY.bit();
        this.maskBit = (short) (SENSOR.bit() | ENVIRONMENT.bit());

        this.density = 0.5f;
        this.frict = 1f;
        this.rest = 0;
    }

    public void createBody() {
        this.body = BodyCreatorHelper.createBox(
            this.getPhysicalManager().getPhysicsWorld(),
            new Vector2(
                this.transformC.x,
                this.transformC.y
            ),
            this.transformC.rotation,
            this.transformC.width,
            this.transformC.height,
            BodyDef.BodyType.DynamicBody,
            density,
            frict,
            rest,
            categoryBit,
            maskBit
        );

        this.body.setFixedRotation(true);
        this.body.resetMassData();
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
    public short getMaskBit() {
        return maskBit;
    }

    @Override
    public short getCategoryBit() {
        return categoryBit;
    }

    @Override
    public float getDensity() {
        return density;
    }

    @Override
    public float getFrict() {
        return frict;
    }

    @Override
    public float getRest() {
        return rest;
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
