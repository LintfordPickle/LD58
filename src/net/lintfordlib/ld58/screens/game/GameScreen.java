package net.lintfordlib.ld58.screens.game;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.sprites.spritesheet.SpriteSheetDefinition;
import net.lintfordlib.core.graphics.textures.FullScreenBuffer;
import net.lintfordlib.core.graphics.textures.FullScreenBuffer.DepthMode;
import net.lintfordlib.core.graphics.textures.FullScreenBuffer.StencilMode;
import net.lintfordlib.core.graphics.textures.Texture;
import net.lintfordlib.core.maths.CollisionExtensions;
import net.lintfordlib.core.maths.InterpolationHelper;
import net.lintfordlib.core.maths.MathHelper;
import net.lintfordlib.core.maths.RandomNumbers;
import net.lintfordlib.core.maths.Vector3f;
import net.lintfordlib.data.DataManager;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.ld58.ConstantsGame;
import net.lintfordlib.ld58.LD58KeyActions;
import net.lintfordlib.ld58.controllers.GameStateController;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.data.GameState;
import net.lintfordlib.ld58.data.GameTextureNames;
import net.lintfordlib.ld58.data.IGameStateListener;
import net.lintfordlib.ld58.renderers.HudRenderer;
import net.lintfordlib.renderers.SimpleRendererManager;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.screens.BaseGameScreen;

public class GameScreen extends BaseGameScreen implements IGameStateListener {

	public static final int TEAM_PLAYER_UID = 0;
	public static final int TEAM_ENEMY_UID = 1;

	public static class PropDefinition {

		public static final PropDefinition COIN = new PropDefinition(GameTextureNames.COIN_00, false, false, true, 1);
		public static final PropDefinition WALL = new PropDefinition(GameTextureNames.WALL_00, true, true, false, 0);

		public final int spriteFrameUid;
		public final boolean immovable;
		public final boolean pickup;
		public final int value;
		public final boolean stopOnCollide;

		private PropDefinition(int spriteFrameUid, boolean immovable, boolean stopOnCollide, boolean pickup, int value) {
			this.spriteFrameUid = spriteFrameUid;
			this.immovable = immovable;
			this.pickup = pickup;
			this.value = value;
			this.stopOnCollide = stopOnCollide;
		}
	}

	public static class EntityDefinition {
		public static final EntityDefinition NORMAL = new EntityDefinition(GameTextureNames.ENEMY_MID, 10f, false, false);
		public static final EntityDefinition SHOOTER = new EntityDefinition(GameTextureNames.ENEMY_MID, 0f, true, true);

		public final int spriteFrameUid;
		public final float moveSpeed;
		public final boolean godMode;
		public final boolean shoots;

		private EntityDefinition(int spriteFrameUid, float moveSpeed, boolean godMode, boolean shoots) {
			this.spriteFrameUid = spriteFrameUid;
			this.moveSpeed = moveSpeed;
			this.godMode = godMode;
			this.shoots = shoots;
		}
	}

	public static class ProjectileDefinition {

		public static final ProjectileDefinition BULLET = new ProjectileDefinition(GameTextureNames.BULLET, 5);

		public final int spriteFrameUid;
		public final float speed;

		private ProjectileDefinition(int spriteFrameUid, float speed) {
			this.spriteFrameUid = spriteFrameUid;
			this.speed = speed;
		}
	}

	// --------------------------------------
	// Inner-Classes
	// --------------------------------------

	private static final int PROJECTILE_POOL_SIZE = 100;

	public class TrackProjectile {
		public boolean isActive;
		private boolean collisionAlive; // can only hit player once

		public ProjectileDefinition def;
		public float xOffset;
		public float zOffset;
		public int forwards;// z+
		public float lifetime;
		public float percent;

		public TrackProjectile() {
			kill();
		}

		public void init(ProjectileDefinition def, float xOffset, float zOffset, int owner, int forwards, float life) {
			this.def = def;
			isActive = true;
			this.xOffset = xOffset;
			this.zOffset = zOffset;
			this.forwards = forwards;
			collisionAlive = true;
			lifetime = life;
		}

		public void kill() {
			isActive = false;
			collisionAlive = false;
			def = null;
			lifetime = 0;
		}
	}

	// moving entities
	public class TrackEntity {

		public EntityDefinition def;

		public boolean isAlive;
		public float dyingTimer;
		public boolean collisionAlive; // can only hit player once

		public float xOffset;
		public float zOffset;
		public float percent;
		public int forwards; // movement direction

		public TrackEntity() {

		}

		public void init(EntityDefinition def, float xOffset, float zOffset) {
			this.def = def;
			this.xOffset = xOffset;
			this.zOffset = zOffset;
			collisionAlive = true;
			isAlive = true;
		}

		public void kill() {
			if (!isAlive)
				return;

			dyingTimer = 300;
			isAlive = false;
			collisionAlive = false;
		}
	}

	// static entities
	public class TrackProp {

		public final PropDefinition definition;

		// state data
		public float xOffset;
		public boolean collisionAlive; // can only hit player once

		public float dyingTimer;
		public boolean isActive;

		public TrackProp(PropDefinition definition, float xOffset) {
			this.definition = definition;
			this.xOffset = xOffset;
			this.collisionAlive = true;
			this.isActive = true;
		}

		public void reset() {
			this.collisionAlive = true;
		}

		public void kill() {
			if (!isActive)
				return;

			dyingTimer = 300;
			isActive = false;
		}

	}

	// tracks are defined in world space, and projected to screen space for rendering
	public class TrackPoint {

		public final Vector3f world = new Vector3f();
		public final Vector3f camera = new Vector3f();
		public final Vector3f screen = new Vector3f();
		public float screenScale = 1;
		public float curvature;

		public void projectWorldToCamera(float camX, float camY, float camZ, float pitch, float yaw) {
			camera.x = world.x - camX;
			camera.y = world.y - camY;
			camera.z = world.z - camZ;

			// Rotate around Y-axis (yaw)
			final var cosYaw = (float) Math.cos(yaw);
			float sinYaw = (float) Math.sin(yaw);

			// Rotate around X-axis (pitch)
			final var cosPitch = (float) Math.cos(pitch);
			final var sinPitch = (float) Math.sin(pitch);

			final var x = camera.x * cosYaw + camera.z * sinYaw;
			final var y = camera.y * cosPitch - camera.z * sinPitch;
			var z = -camera.x * sinYaw + camera.z * cosYaw;

			z = camera.y * sinPitch + camera.z * cosPitch;

			camera.x = x;
			camera.y = y;
			camera.z = z;
		}

		public void projectCameraToScreen(float camDepth, float width, float height, float roadWidth) {
			screenScale = camDepth / camera.z;

			screen.x = Math.round(width / 2 + (screenScale * camera.x * width / 2));
			screen.y = Math.round(height / 2 - (screenScale * camera.y * -height / 2));
			screen.z = Math.round((screenScale * roadWidth * width / 2));
		}
	}

	public class TrackSegment {

		private static int segmentIndexCounter;

		public static void resetIndexCounter() {
			segmentIndexCounter = 0;
		}

		public int index;
		public float curve;
		public boolean isLooped;
		public boolean isClipped;
		public float clipSpaceY;

		public int variation;

		public final TrackPoint p0 = new TrackPoint(); // closest
		public final TrackPoint p1 = new TrackPoint(); // furthest

		public final List<TrackEntity> entities = new ArrayList<>();
		public final List<TrackProp> props = new ArrayList<>();
		public final List<TrackProjectile> projectiles = new ArrayList<>();

		public TrackSegment(float curve, float endHeight) {
			this.index = segmentIndexCounter++;
			this.curve = curve;

			p0.world.x = 0.f;
			p0.world.y = lastSegmentHeight();
			p0.world.z = index * mSegmentLength;

			p1.world.x = 0.f;
			p1.world.y = endHeight;
			p1.world.z = (index + 1) * mSegmentLength;

		}
	}

	// --------------------------------------
	// Variables
	// --------------------------------------

	private FullScreenBuffer mScreenBuffer;

	private SceneHeader mSceneHeader;
	private GameOptions mGameOptions;
	private GameState mGameState;
	private GameStateController mGameStateController;
	private HudRenderer mHudRenderer;
	private Texture mArrowTexture;

	private SpriteSheetDefinition mGameSpriteSheet;

	private List<TrackSegment> mTrackSegments = new ArrayList<>();

	// global update lists (for movement)
	private List<TrackEntity> mEntities = new ArrayList<>();
	private List<TrackProjectile> mProjectiles = new ArrayList<>();

	private final List<TrackProp> mEntityUpdateList = new ArrayList<>();
	private final List<TrackProp> mProjectilesUpdateList = new ArrayList<>();
	private final List<TrackProp> mPropUpdateList = new ArrayList<>();

	// camera vars
	private float mFoV = 100;
	private float mCameraHeight = 150;
	private float mCameraDepth; // computed (cam dist from screen)
	private float mCameraPitch = 0;
	private float mCameraYaw = 0;

	// world vars
	public final int mSegmentLength = 10;
	public final int mRumbleLength = 2;
	private float mTrackLength; // computed
	private int mNumLanes = 4;
	private float mRoadWidth = 300;

	private int mDrawDistance = 50; // number of segments to draw
	private int mPlayerLane;
	private float mPlayerX; // player offset from center on X axis
	private float mPlayerZ; // (computed) player relative z distance from camera
	private float mPlayerWorldZOffset = 20;

	private float mPlayerHitCooldown;
	private float mPlayerHitFlashTimer;
	private boolean mPlayerHitFlash;

	private float mPosition; // camera Z position (add mPlayerZ to get player's absolute Z position).
	private final float step = 1f / 60f;
	private float mSpeed;
	private float mMaxSpeed = mSegmentLength / step;

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public GameScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions options) {
		super(screenManager, new SimpleRendererManager(screenManager.core(), ConstantsGame.GAME_RESOURCE_GROUP_ID));

		mSceneHeader = sceneHeader;
		mGameOptions = options;
		mGameState = new GameState();

		mShowBackgroundScreens = true;

		mScreenBuffer = new FullScreenBuffer(ConstantsGame.GAME_CANVAS_WIDTH, ConstantsGame.GAME_CANVAS_HEIGHT);

		for (int i = 0; i < PROJECTILE_POOL_SIZE; i++) {
			mProjectiles.add(new TrackProjectile()); // pre-allocate a bunch
		}

		reset();
		setupWorld();
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mGameSpriteSheet = resourceManager.spriteSheetManager().getSpriteSheet("SPRITESHEET_GAME", ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mArrowTexture = resourceManager.textureManager().loadTexture("TEXTURE_ARROW", "res/textures/textureArrow.png", ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mScreenBuffer.loadResources(resourceManager);
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		mGameSpriteSheet = null;

		mScreenBuffer.unloadResources();
	}

	@Override
	public void handleInput(LintfordCore core) {
		super.handleInput(core);

		if (core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_ESCAPE, this) || core.input().gamepads().isGamepadButtonDownTimed(GLFW.GLFW_GAMEPAD_BUTTON_START, this)) {

			if (ConstantsGame.START_GAME_IMMEDIATELY) {
				reset();
				setupWorld();
			} else {
				screenManager.addScreen(new PauseScreen(screenManager, mSceneHeader, mGameOptions));
			}

			return;
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_PRIMARY_FIRE)) {
			addProjectile(mPlayerX, mPosition + mPlayerZ + mPlayerWorldZOffset + 10, 0, 1, 2000);
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_PRIMARY_LEFT)) {
			mPlayerLane = MathHelper.clampi(--mPlayerLane, 0, mNumLanes - 1);
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_PRIMARY_RIGHT)) {
			mPlayerLane = MathHelper.clampi(++mPlayerLane, 0, mNumLanes - 1);
		}

		if (core.input().eventActionManager().getCurrentControlActionState(LD58KeyActions.KEY_BINDING_FORWARD)) {
			float speed = 60.f;
			if (core.input().keyboard().isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT))
				speed += 200;

			mSpeed = speed;
		}

		if (core.input().eventActionManager().getCurrentControlActionState(LD58KeyActions.KEY_BINDING_BACKWARD)) {
			float speed = -60.f;
			if (core.input().keyboard().isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT))
				speed = 0;

			mSpeed = speed;
		}

	}

	@Override
	public void update(LintfordCore core, boolean otherScreenHasFocus, boolean coveredByOtherScreen) {
		super.update(core, otherScreenHasFocus, coveredByOtherScreen);

		if (mGameState.hasGameEnded() || !mGameState.hasGameStarted())
			return;

		mPlayerX = getLaneOffsetX(mPlayerLane);
		if (mPlayerHitCooldown > 0) {
			mPlayerHitCooldown -= core.gameTime().elapsedTimeMilli();
			if (mPlayerHitCooldown < 0)
				mPlayerHitCooldown = 0;
		}

		if (mPlayerHitFlashTimer > 0) {
			mPlayerHitFlashTimer -= core.gameTime().elapsedTimeMilli();

			if (mPlayerHitFlashTimer < 0) {
				mPlayerHitFlash = !mPlayerHitFlash;
				mPlayerHitFlashTimer = 50;
			}
		}

		// TODO: remove entities/projectiles/props from lists needs making safe (update list)

		var playerSegment = findSegment(mPosition + mPlayerZ + mPlayerWorldZOffset + 1);
		updateEntities(core);
		updateProps(core);
		updateProjectiles(core, playerSegment);
		updatePlayerCollisions(core, playerSegment);

		mPosition += mSpeed * core.gameTime().elapsedTimeMilli() * 0.001f;
		mGameState.playerDistance(mPosition + mPlayerZ + mPlayerWorldZOffset);
	}

	private void updatePlayerCollisions(LintfordCore core, TrackSegment playerSegment) {
		final var propCount = playerSegment.props.size();
		for (int i = 0; i < propCount; i++) {
			final var prop = playerSegment.props.get(i);

			if (!prop.collisionAlive)
				continue;

			final var propX = (int) projectWorldToScreenX(prop.xOffset * mRoadWidth, (mPlayerX * mRoadWidth), playerSegment.p0.screenScale);
			final var propW = (int) (76 * getWorldScreenRatioX(playerSegment.p0.screenScale));

			final var playerX = (int) projectWorldToScreenX(mPlayerX * mRoadWidth, (mPlayerX * mRoadWidth), playerSegment.p0.screenScale);
			final var playerW = (int) (48 * getWorldScreenRatioX(playerSegment.p0.screenScale));

			if (CollisionExtensions.overlap(playerX, playerW, propX, propW)) {
				final var propDef = prop.definition;

				if (!propDef.immovable)
					prop.collisionAlive = false;

				if (propDef.stopOnCollide)
					mSpeed = 0;

				if (propDef.pickup) {
					mGameState.addCoins(propDef.value);
					screenManager.toastManager().addMessage(getClass().getSimpleName(), "COIN MAN!", 1500);

					prop.kill();
					continue;
				}
			}
		}

		final var entityCount = playerSegment.entities.size();
		for (int i = 0; i < entityCount; i++) {
			final var entity = playerSegment.entities.get(i);

			if (!entity.collisionAlive)
				continue;

			final var propX = (int) projectWorldToScreenX(entity.xOffset * mRoadWidth, (mPlayerX * mRoadWidth), playerSegment.p0.screenScale);
			final var propW = (int) (76 * getWorldScreenRatioX(playerSegment.p0.screenScale));

			final var playerX = (int) projectWorldToScreenX(mPlayerX * mRoadWidth, (mPlayerX * mRoadWidth), playerSegment.p0.screenScale);
			final var playerW = (int) (48 * getWorldScreenRatioX(playerSegment.p0.screenScale));

			if (CollisionExtensions.overlap(playerX, playerW, propX, propW)) {
				entity.collisionAlive = false;
			}
		}
	}

	// only updates entities we can see
	private void updateEntities(LintfordCore core) {
		final var baseSegment = findSegment(mPosition);
		final var numSegments = mTrackSegments.size();

		for (int i = mDrawDistance - 1; i >= 0; i--) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);

			final var numEntities = segment.entities.size();
			for (int j = numEntities - 1; j >= 0; j--) {
				final var entity = segment.entities.get(j);
				final var def = entity.def;

				if (!entity.isAlive) {
					if (entity.dyingTimer > 0) {
						entity.dyingTimer -= core.gameTime().elapsedTimeMilli();
					} else {
						segment.entities.remove(entity);
						continue;
					}
				}

				// update movement
				if (def.moveSpeed > 0) {
					final var origSegment = findSegment(entity.zOffset);
					final var dir = entity.forwards;
					entity.zOffset += def.moveSpeed * dir;
					final var newSegment = findSegment(entity.zOffset);

					if (origSegment.index != newSegment.index) {
						origSegment.entities.remove(entity);
						newSegment.entities.add(entity);
					}
				}

				if (def.shoots) {
					final var s = (float) Math.pow(1f - (1f - 1f), 1f / 60f);
					if (RandomNumbers.getRandomChance(s)) {
						addProjectile(entity.xOffset, segment.p0.world.z - 10, TEAM_ENEMY_UID, -1, 1500);
					}
				}

				// Collision detection in player update
			}
		}
	}

	private void updateProps(LintfordCore core) {
		final var baseSegment = findSegment(mPosition);
		final var numSegments = mTrackSegments.size();

		for (int i = mDrawDistance - 1; i >= 0; i--) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);

			final var numProps = segment.props.size();
			for (int j = numProps - 1; j >= 0; j--) {
				final var prop = segment.props.get(j);

				if (!prop.isActive && prop.dyingTimer <= 0)
					segment.props.remove(prop);

				if (prop.dyingTimer > 0)
					prop.dyingTimer -= core.gameTime().elapsedTimeMilli();

				// Collision detection in player update

			}
		}
	}

	private void updateProjectiles(LintfordCore core, TrackSegment playerSegment) {
		final var dt = (float) core.gameTime().elapsedTimeMilli();
		for (int i = 0; i < PROJECTILE_POOL_SIZE; i++) {
			var projectile = mProjectiles.get(i);
			var origSegment = findSegment(projectile.zOffset);

			if (!projectile.isActive)
				continue;

			// Lifetime
			projectile.lifetime -= dt;
			if (projectile.lifetime <= 0) {
				origSegment.projectiles.remove(projectile);

				projectile.kill();
				continue;
			}

			// update movement
			final var dir = projectile.forwards;
			projectile.zOffset += projectile.def.speed * dir;
			final var newSegment = findSegment(projectile.zOffset);

			if (origSegment.index != newSegment.index) {
				origSegment.projectiles.remove(projectile);
				newSegment.projectiles.add(projectile);

				origSegment = newSegment;
			}

			// update collisions
			if (projectile.collisionAlive) {
				final var projectileX = (int) projectWorldToScreenX(projectile.xOffset * mRoadWidth, (mPlayerX * mRoadWidth), origSegment.p0.screenScale);
				final var projectileW = (int) (16 * getWorldScreenRatioX(origSegment.p0.screenScale));

				if (projectile.forwards > 0) {
					final var entitiesInSegment = origSegment.entities;
					final var numEntities = entitiesInSegment.size();
					for (int j = 0; j < numEntities; j++) {
						final var entity = entitiesInSegment.get(j);
						final var entityX = (int) projectWorldToScreenX(entity.xOffset * mRoadWidth, (mPlayerX * mRoadWidth), origSegment.p0.screenScale);
						final var entityW = (int) (48 * getWorldScreenRatioX(origSegment.p0.screenScale));

						if (CollisionExtensions.overlap(entityX, entityW, projectileX, projectileW)) {

							projectile.kill();
							origSegment.projectiles.remove(projectile);

							mGameState.addKill();
							entity.kill();
						}
					}
				}

				// projectile / player colisions
				if (projectile.forwards < 0 && mPlayerHitCooldown <= 0.f) {
					if (playerSegment.index != origSegment.index)
						continue;

					final var playerX = (int) projectWorldToScreenX(mPlayerX * mRoadWidth, (mPlayerX * mRoadWidth), playerSegment.p0.screenScale);
					final var playerW = (int) (48 * getWorldScreenRatioX(playerSegment.p0.screenScale));

					if (CollisionExtensions.overlap(playerX, playerW, projectileX, projectileW)) {
						mPlayerHitCooldown = 400;
						mPlayerHitFlashTimer = 50;
						mPlayerHitFlash = true;

						mGameState.removeHealth();

						origSegment.projectiles.remove(projectile);
						projectile.kill();
					}

				}
			}
		}
	}

	private int projectWorldToScreenX(float worldX, float camX, float scale) {
		final var wx = worldX - camX;
		return (int) (ConstantsGame.GAME_CANVAS_WIDTH / 2 + (scale * wx * ConstantsGame.GAME_CANVAS_WIDTH / 2));
	}

	private float getWorldScreenRatioX(float scale) {
		return scale * ConstantsGame.GAME_CANVAS_WIDTH / 2;
	}

	// ---

	@Override
	public void draw(LintfordCore core) {

		final var pixels = mScreenBuffer.getPixels();

		clearBuffer(core, pixels);

		mScreenBuffer.mEnableDepth = true;

		mDrawDistance = 200;
		drawTrack(core);

		// maybe call these from within the drawTrack (they are all segment-based)?
		drawProps(core);
		drawEntities(core);
		drawPlayer(core);
		drawProjectiles(core);

		mScreenBuffer.draw(core);

		super.draw(core);
	}

	private void clearBuffer(LintfordCore core, int[] buffer) {
		mScreenBuffer.clear(0xff1f2f3f);
	}

	private void drawTrack(LintfordCore core) {

		final var baseSegment = findSegment(mPosition);
		final var basePercent = (mPosition % mSegmentLength) / mSegmentLength;

		final var playerSegment = findSegment(mPosition + mPlayerZ);
		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;

		final var playerY = InterpolationHelper.lerp(playerSegment.p0.world.y, playerSegment.p1.world.y, playerPercent);

		final var canvasWidth = ConstantsGame.GAME_CANVAS_WIDTH;
		final var canvasHeight = ConstantsGame.GAME_CANVAS_HEIGHT;

		mScreenBuffer.writeOnceLock = true;

		// segments drawn front to back
		float maxY = -ConstantsGame.GAME_CANVAS_HEIGHT / 2; // clip segments based on height
		final var numSegments = mTrackSegments.size();
		float x = 0;
		float dx = -(baseSegment.curve * basePercent);

		for (int i = 0; i < mDrawDistance; i++) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);

			segment.isLooped = segment.index < baseSegment.index;
			segment.clipSpaceY = maxY; // used to clip the sprites/cars in next pass

			segment.p0.curvature = x;
			segment.p1.curvature = x + dx;

			final var camX = mPlayerX;
			segment.p0.projectWorldToCamera((camX * mRoadWidth) - segment.p0.curvature, playerY + mCameraHeight, mPosition, mCameraPitch, mCameraYaw);
			segment.p1.projectWorldToCamera((camX * mRoadWidth) - segment.p1.curvature, playerY + mCameraHeight, mPosition, mCameraPitch, mCameraYaw);

			segment.p0.projectCameraToScreen(mCameraDepth, canvasWidth, canvasHeight, mRoadWidth / 2);
			segment.p1.projectCameraToScreen(mCameraDepth, canvasWidth, canvasHeight, mRoadWidth / 2);

			x = x + dx;
			dx = dx + segment.curve * 3.f;

			// check clipped (height based)
			final var isBehindUs = (segment.p0.camera.z <= mCameraDepth * mPlayerZ);
			final var isOccluded = false; // (segment.p1.screen.y < maxY); // rely on writeOnce lock

			if (isBehindUs || isOccluded)
				continue;
			var drawLanes = (segment.p1.screen.y > maxY);

			// render the track segment
			drawSegment(segment, (int) canvasWidth, mNumLanes, drawLanes);

			// Track the top of this segment
			maxY = segment.p1.screen.y;
		}

		mScreenBuffer.writeOnceLock = false;
	}

	private void drawSegment(TrackSegment segment, int canvasWidth, int numLanes, boolean drawLanes) {
		// @formatter:off
		final var p0 = segment.p0;
		final var p1 = segment.p1;

		final var r0 = p0.screen.z / 30.0f;
		final var r1 = p1.screen.z / 30.0f;

		// mScreenBuffer.drawRect(0, (int) p1.screen.y, canvasWidth, (int) (p1.screen.y - p0.screen.y) + 1, 0xff00ff00, true);

		final var wallHeight = 60;
		
		mScreenBuffer.mStencilEnabled = !drawLanes;
		mScreenBuffer.mStencilMode = StencilMode.LessEqual;
		mScreenBuffer.mStencilValue = 2;
		mScreenBuffer.writeOnceLock = false;
		
		// road
		mScreenBuffer.drawPolygon(
				(int)(p0.screen.x - p0.screen.z), (int)p0.screen.y, 
				(int)(p0.screen.x + p0.screen.z), (int)p0.screen.y, 
				(int)(p1.screen.x + p1.screen.z), (int)p1.screen.y, 
				(int)(p1.screen.x - p1.screen.z), (int)p1.screen.y, 
				p0.world.z,
				segment.variation == 0 ? 0xffa4bfaf : 0xffa4bfef, true);
		
		mScreenBuffer.writeOnceLock = true;
		
//		final var srcBuffer = mArrowTexture.ARGBColorData();
//		mScreenBuffer.drawTexturedPolygon(
//				srcBuffer, mArrowTexture.getTextureWidth(), mArrowTexture.getTextureHeight(), 
//				(int)(p0.screen.x - 1 - p0.screen.z), (int)p0.screen.y, 
//				(int)(p0.screen.x + p0.screen.z), (int)p0.screen.y, 
//				(int)(p1.screen.x + p1.screen.z), (int)p1.screen.y, 
//				(int)(p1.screen.x - 1 - p1.screen.z), (int)p1.screen.y, 
//				0, 0xffffffff);

		mScreenBuffer.mStencilEnabled = true;
		mScreenBuffer.mStencilMode = StencilMode.LessEqual;
		mScreenBuffer.mStencilValue = 2;

		// rumble left
		mScreenBuffer.drawPolygon(
				(int) (p0.screen.x - p0.screen.z), 		(int) p0.screen.y, 
				(int) (p0.screen.x - p0.screen.z - r0), (int) (p0.screen.y + wallHeight * p0.screenScale * 240), 
				(int) (p1.screen.x - p1.screen.z - r1), (int) (p1.screen.y + wallHeight * p1.screenScale * 240), 
				(int) (p1.screen.x - p1.screen.z),		(int) p1.screen.y, 
				p0.world.z, 0xffcc0419, true);

		// rumble right
		mScreenBuffer.drawPolygon(
				(int) (p0.screen.x + p0.screen.z + 1), 		(int) (p0.screen.y), 
				(int) (p0.screen.x + p0.screen.z + r0), (int) (p0.screen.y + wallHeight/2 * p0.screenScale * 240), 
				(int) (p1.screen.x + p1.screen.z + r1), (int) (p1.screen.y + wallHeight/2 * p1.screenScale * 240),
				(int) (p1.screen.x + p1.screen.z + 1), 		(int) (p1.screen.y), 
				p0.world.z, 0x55cc0419, true);

		// Lanes
		if (drawLanes) {
			mScreenBuffer.mDepthMode = DepthMode.Equal;
			mScreenBuffer.mStencilEnabled = true;
			mScreenBuffer.writeOnceLock = false; //

			// only render walls where tracks have not already been rendered
			mScreenBuffer.mStencilMode = StencilMode.LessEqual; 
			mScreenBuffer.mStencilValue = 1;

			final var lineZ0 = p0.screenScale * canvasWidth / 2;
			final var lineZ1 = p1.screenScale * canvasWidth / 2;

			final var laneWidth = 2;

			if (mNumLanes <= 1)
				return;

			var lanes1 = mNumLanes - 1;
			for (int i = 0; i < lanes1; i++) {

				final var lineStepX = (i + 1) * mRoadWidth / (lanes1 + 1);
				final var lx0 = p0.screen.x - p0.screen.z + lineStepX * p0.screenScale * canvasWidth / 2;
				final var lx1 = p1.screen.x - p1.screen.z + lineStepX * p1.screenScale * canvasWidth / 2;

				// if (segment.rumbleColor == 0) {
				mScreenBuffer.drawPolygon(
						(int) (lx0 - laneWidth * lineZ0), (int) p0.screen.y, 
						(int) (lx0 + laneWidth * lineZ0), (int) p0.screen.y, 
						(int) (lx1 + laneWidth * lineZ1), (int) p1.screen.y, 
						(int) (lx1 - laneWidth * lineZ1), (int) p1.screen.y, 
						p0.world.z, 0xff2f4f4f, true);
				// }
			}
			mScreenBuffer.mStencilEnabled = true;
			mScreenBuffer.writeOnceLock = true;
			mScreenBuffer.mDepthMode = DepthMode.Less;
		}

		mScreenBuffer.mStencilEnabled = false;

		// @formatter:on
	}

	private void drawPlayer(LintfordCore core) {

		final var playerSegment = findSegment(mPosition + mPlayerZ + mPlayerWorldZOffset);
		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;
		final var scale = InterpolationHelper.lerp(playerSegment.p0.screenScale, playerSegment.p1.screenScale, playerPercent);

		final var segmentCurvature = InterpolationHelper.lerp(playerSegment.p0.curvature, playerSegment.p1.curvature, playerPercent);
		final var segmentHeight = InterpolationHelper.lerp(playerSegment.p0.screen.y, playerSegment.p1.screen.y, playerPercent);

		final var playerFrame = mGameSpriteSheet.getSpriteFrame(GameTextureNames.PLAYER_MID);

		final var playerW = (int) (playerFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2);
		final var playerH = (int) (playerFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2);
		final var playerX = (int) (mPlayerX + segmentCurvature) + (ConstantsGame.GAME_CANVAS_WIDTH / 2 - playerW / 2);
		final var playerY = (int) segmentHeight + 10;
		final var playerZ = playerSegment.p0.world.z - 10; // cheat a little

		final var texture = mGameSpriteSheet.texture();

		final var srcX = (int) playerFrame.x();
		final var srcY = (int) (texture.getTextureHeight() - playerFrame.height());

		int col = 0xffafafaf;
		if (mPlayerHitCooldown > 0 && mPlayerHitFlash) {
			col = 0xffffffff;
		}

		mScreenBuffer.copyPixelsAtlas(texture.ARGBColorData(), // Src pixels
				srcX, srcY, (int) playerFrame.width(), (int) playerFrame.height(), texture.getTextureWidth(), // src rect
				playerX, playerY, playerW, playerH, // dest rect
				playerZ, col, false);

	}

	private void drawProps(LintfordCore core) {
		final var baseSegment = findSegment(mPosition);
		final var numSegments = mTrackSegments.size();

		final var texture = mGameSpriteSheet.texture();
		final var textureWidth = texture.getTextureWidth();
		final var srcPixels = texture.ARGBColorData();

		// enemies back-to-front along visible segments ..
		for (int i = mDrawDistance - 1; i >= 0; i--) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);
			final var entityZ = segment.p0.world.z - 10; // cheat a little

			final var propCount = segment.props.size();
			for (int j = 0; j < propCount; j++) {
				final var prop = segment.props.get(j);
				final var scale = segment.p0.screenScale;

				final var propDefinition = prop.definition;
				final var spriteFrame = mGameSpriteSheet.getSpriteFrame(propDefinition.spriteFrameUid);

				final var destW = spriteFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2;
				final var destH = spriteFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2;
				final var destX = segment.p0.screen.x + ((prop.xOffset) * scale * mRoadWidth * ConstantsGame.GAME_CANVAS_WIDTH / 2) - destW / 2;
				final var destY = segment.p0.screen.y + 10;

				int srcX = (int) spriteFrame.x();
				int srcY = (int) (spriteFrame.y());
				int srcW = (int) spriteFrame.width();
				int srcH = (int) spriteFrame.height();

				mScreenBuffer.copyPixelsAtlas(srcPixels, // Src pixels
						srcX, srcY, srcW, srcH, textureWidth, // src rect
						(int) destX, (int) destY, (int) destW, (int) destH, // dest rect
						entityZ, 0xffcfcfcf, false);

			}
		}
	}

	private void drawEntities(LintfordCore core) {

		final var baseSegment = findSegment(mPosition);
		final var numSegments = mTrackSegments.size();

		final var texture = mGameSpriteSheet.texture();
		final var textureWidth = texture.getTextureWidth();
		final var srcPixels = texture.ARGBColorData();

		// enemies back-to-front along visible segments ..
		for (int i = mDrawDistance - 1; i >= 0; i--) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);
			final var entityZ = segment.p0.world.z - 10; // cheat a little

			final var entityCount = segment.entities.size();
			for (int j = 0; j < entityCount; j++) {
				final var entity = segment.entities.get(j);
				final var def = entity.def;

				final var scale = segment.p0.screenScale;

				final var spriteFrame = mGameSpriteSheet.getSpriteFrame(def.spriteFrameUid);

				final var destW = spriteFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2;
				final var destH = spriteFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2;
				final var destX = segment.p0.screen.x + ((entity.xOffset) * scale * mRoadWidth * ConstantsGame.GAME_CANVAS_WIDTH / 2) - destW / 2;
				final var destY = segment.p0.screen.y + 10;

				int srcX = (int) spriteFrame.x();
				int srcY = (int) (spriteFrame.y());
				int srcW = (int) spriteFrame.width();
				int srcH = (int) spriteFrame.height();

				mScreenBuffer.copyPixelsAtlas(srcPixels, // Src pixels
						srcX, srcY, srcW, srcH, textureWidth, // src rect
						(int) destX, (int) destY, (int) destW, (int) destH, // dest rect
						entityZ, 0xffcfcfcf, false);

			}
		}
	}

	private void drawProjectiles(LintfordCore core) {

		final var baseSegment = findSegment(mPosition);
		final var numSegments = mTrackSegments.size();

		final var texture = mGameSpriteSheet.texture();
		final var textureWidth = texture.getTextureWidth();
		final var srcPixels = texture.ARGBColorData();

		// enemies back-to-front along visible segments ..
		for (int i = mDrawDistance - 1; i >= 0; i--) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);
			final var entityZ = segment.p0.world.z - 10; // cheat a little

			final var projectileCount = segment.projectiles.size();
			for (int j = 0; j < projectileCount; j++) {
				final var projectile = segment.projectiles.get(j);

				if (!projectile.isActive)
					continue;

				final var projPercent = (projectile.zOffset % mSegmentLength) / mSegmentLength;

				final var scale = InterpolationHelper.lerp(segment.p0.screenScale, segment.p1.screenScale, projPercent);
				final var screenX = InterpolationHelper.lerp(segment.p0.screen.x, segment.p1.screen.x, projPercent);
				final var screenY = InterpolationHelper.lerp(segment.p0.screen.y, segment.p1.screen.y, projPercent);

				final var spriteFrame = mGameSpriteSheet.getSpriteFrame(projectile.def.spriteFrameUid);

				final var destW = spriteFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2;
				final var destH = spriteFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2;
				final var destX = screenX + (projectile.xOffset * scale * mRoadWidth * ConstantsGame.GAME_CANVAS_WIDTH / 2) - destW / 2;
				final var destY = screenY + 1;

				int srcX = (int) spriteFrame.x();
				int srcY = (int) (spriteFrame.y());
				int srcW = (int) spriteFrame.width();
				int srcH = (int) spriteFrame.height();

				mScreenBuffer.copyPixelsAtlas(srcPixels, // Src pixels
						srcX, srcY, srcW, srcH, textureWidth, // src rect
						(int) destX, (int) destY, (int) destW, (int) destH, // dest rect
						entityZ, 0xffcfcfcf, false);

			}
		}
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	// DATA ----------------------------------------

	@Override
	protected void createData(DataManager dataManager) {

	}

	private void reset() {
		mPosition = 0; // back to start
		mPlayerX = 0.f; // center
		mSpeed = 0.f;
	}

	private void setupWorld() {

		// cam
		mFoV = 140;
		mCameraDepth = 1f / (float) Math.tan(mFoV / 2 * Math.PI / 180);
		mPlayerZ = mCameraHeight * mCameraDepth;

		// track
		TrackSegment.resetIndexCounter();
		mTrackSegments.clear();

		final var testHillHeight = 120;

		addRoad(0, 4, 0, 0, 0);
		addRoad(10, 50, 10, 0, testHillHeight * 1.4f);
		addRoad(0, 30, 0, .5f, -testHillHeight / 4);
		addRoad(10, 30, 10, -.6f, testHillHeight);
		addRoad(10, 30, 10, .8f, -testHillHeight);
//		addRoad(10, 30, 10, -.3f, testHillHeight);
//		addRoad(30, 100, 0, -0.1f, -testHillHeight);
//		addRoad(30, 75, 0, .3f, -testHillHeight / 2);
//		addRoad(60, 50, 20, .3f, 0);
//		addRoad(0, 75, 0, .4f, testHillHeight * 2);
//		addRoad(0, 75, 0, 0, testHillHeight);

		// TODO: Need to obstruct your path
		addProp(PropDefinition.WALL, 100, 0);
		addProp(PropDefinition.WALL, 100, 1);

		// TODO: Need to be collectable
		addProp(PropDefinition.COIN, 30, 2);
		addProp(PropDefinition.COIN, 22, 2);
		addProp(PropDefinition.COIN, 34, 2);
		addProp(PropDefinition.COIN, 36, 2);
		addProp(PropDefinition.COIN, 38, 2);
		addProp(PropDefinition.COIN, 40, 2);

		addProp(PropDefinition.COIN, 50, 2);
		addProp(PropDefinition.COIN, 52, 2);
		addProp(PropDefinition.COIN, 54, 2);
		addProp(PropDefinition.COIN, 56, 2);
		addProp(PropDefinition.COIN, 58, 2);
		addProp(PropDefinition.COIN, 50, 2);

		// TODO: These little fuckers need to shoot and move
		addEntity(EntityDefinition.NORMAL, 120, 2);
//		addEntity(EntityDefinition.SHOOTER, 110, 3);
//		addEntity(EntityDefinition.NORMAL, 150, 3);

		final var numSegments = mTrackSegments.size();
		for (int i = 0; i < numSegments; i++) {
			mTrackSegments.get(i).variation = (i % 2);
		}

		mTrackLength = mTrackSegments.size() * mSegmentLength;
		mGameState.startGame(mTrackLength);
	}

	private void addRoad(int enter, int hold, int leave, float curve, float height) {

		var startY = lastSegmentHeight();
		var endY = startY + (int) (height * mSegmentLength);
		var total = enter + hold + leave;

		for (int i = 0; i < enter; i++) {

			final var curveAmt = InterpolationHelper.easeIn(0, curve, (float) i / (float) enter);
			final var hillAmt = InterpolationHelper.easeInOut(startY, endY, (float) i / (float) total);

			mTrackSegments.add(new TrackSegment(curveAmt, hillAmt));

		}

		for (int i = 0; i < hold; i++) {

			final var hillAmt = InterpolationHelper.easeInOut(startY, endY, (float) (enter + i) / (float) total);

			mTrackSegments.add(new TrackSegment(curve, hillAmt));

		}

		for (int i = 0; i < leave; i++) {

			final var curveAmt = InterpolationHelper.easeOut(0, curve, (float) i / (float) leave);
			final var hillAmt = InterpolationHelper.easeInOut(startY, endY, (float) (enter + hold + i) / (float) total);

			mTrackSegments.add(new TrackSegment(curveAmt, hillAmt));

		}
	}

	private void addProp(PropDefinition def, int segmentUid, int laneNum) {
		final var newProp = new TrackProp(def, getLaneOffsetX(laneNum));
		final var segment = getSegment(segmentUid);
		segment.props.add(newProp);
	}

	private void addProjectile(float offsetX, float offsetZ, int owner, int forwards, float life) {
		final var segment = findSegment(offsetZ);
		final var projectile = getFreeProjectile();

		if (projectile == null)
			return;

		projectile.init(ProjectileDefinition.BULLET, offsetX, offsetZ, owner, forwards, life);
		segment.projectiles.add(projectile);

		// global add for update
		mProjectiles.add(projectile);

	}

	private void addEntity(EntityDefinition def, int segmentUid, int laneNum) {
		final var newEntity = new TrackEntity();
		newEntity.init(def, getLaneOffsetX(laneNum), laneNum);

		final var segment = getSegment(segmentUid);
		segment.entities.add(newEntity);
	}

	private TrackSegment getSegment(int index) {
		return mTrackSegments.get(index % mTrackSegments.size());
	}

	private TrackSegment findSegment(float z) {
		if (z < 0)
			return mTrackSegments.get(0);

		final var index = (int) Math.floor((z / mSegmentLength)) % mTrackSegments.size();
		return mTrackSegments.get(index);
	}

	private float lastSegmentHeight() {
		if (mTrackSegments.size() == 0)
			return 0;

		return mTrackSegments.get(mTrackSegments.size() - 1).p1.world.y;
	}

	private float getLaneOffsetX(int lane) {
		lane = MathHelper.clampi(lane, 0, mNumLanes);

		final var laneI = 1.f / (mNumLanes * 2);
		final var laneS = 1.f / (mNumLanes);

		return -0.5f + laneI + laneS * lane;
	}

	public TrackProjectile getFreeProjectile() {
		for (int i = 0; i < PROJECTILE_POOL_SIZE; i++) {
			final var proj = mProjectiles.get(i);
			if (!proj.isActive) {
				return proj;
			}
		}

		return null;
	}

	// CONTROLLERS ---------------------------------

	@Override
	protected void createControllers(ControllerManager controllerManager) {
		mGameStateController = new GameStateController(controllerManager, mGameState, ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mGameStateController.setGameStateListener(this);
	}

	@Override
	protected void initializeControllers(LintfordCore core) {
		mGameStateController.initialize(core);
	}

	// RENDERERS -----------------------------------

	@Override
	protected void createRenderers(LintfordCore core) {
		mHudRenderer = new HudRenderer(mRendererManager, ConstantsGame.GAME_RESOURCE_GROUP_ID);
	}

	@Override
	protected void createRendererStructure(LintfordCore core) {
		mRendererManager.addRenderer(mHudRenderer);

	}

	// --------------------------------------
	// Callback-Methods
	// --------------------------------------

	@Override
	public void onGameWon() {
		screenManager.addScreen(new WonScreen(screenManager, mSceneHeader, mGameOptions));
	}

	@Override
	public void onGameLost() {
		screenManager.addScreen(new LostScreen(screenManager, mSceneHeader, mGameOptions));
	}
}
