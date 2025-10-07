package net.lintfordlib.ld58.screens.game;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.debug.Debug;
import net.lintfordlib.core.graphics.ColorHelper;
import net.lintfordlib.core.graphics.fonts.CharAtlasRenderer;
import net.lintfordlib.core.graphics.sprites.spritesheet.SpriteSheetDefinition;
import net.lintfordlib.core.graphics.textures.FullScreenBuffer;
import net.lintfordlib.core.graphics.textures.FullScreenBuffer.DepthMode;
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
import net.lintfordlib.ld58.controllers.SoundFxController;
import net.lintfordlib.ld58.data.GameEndState;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.data.GameState;
import net.lintfordlib.ld58.data.GameTextureNames;
import net.lintfordlib.ld58.data.IGameStateListener;
import net.lintfordlib.ld58.data.IResetLevel;
import net.lintfordlib.ld58.renderers.HudRenderer;
import net.lintfordlib.renderers.SimpleRendererManager;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.screens.BaseGameScreen;

public class GameScreen extends BaseGameScreen implements IGameStateListener, IResetLevel {

	public static final float JUMP_ALT_POWER = 100;

	public static final float HIT_FLASH_TIME = 50;
	public static final float HIT_COOLDOWN_TIME = 300;

	public static final float JUMP_COOLDOWN_TIME = 300;

	public static final int NUM_LANES = 4;

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
		public static final EntityDefinition BLOCKER = new EntityDefinition(GameTextureNames.ENEMY_MID, 0f, 1, false);
		public static final EntityDefinition NORMAL = new EntityDefinition(GameTextureNames.ENEMY_MID, 5f, 2, false);
		public static final EntityDefinition BLOCKER_SHOOTER = new EntityDefinition(GameTextureNames.ENEMY_HARD, 0f, 3, true);
		public static final EntityDefinition WALKER_SHOOTER = new EntityDefinition(GameTextureNames.ENEMY_MID, 1f, 1, true);

		public final int spriteFrameUid;
		public final float moveSpeed;
		public final int totalLives;
		public final boolean shoots;

		private EntityDefinition(int spriteFrameUid, float moveSpeed, int totalLives, boolean shoots) {
			this.spriteFrameUid = spriteFrameUid;
			this.moveSpeed = moveSpeed;
			this.totalLives = totalLives;
			this.shoots = shoots;
		}
	}

	public static class ProjectileDefinition {

		public static final ProjectileDefinition P_BULLET = new ProjectileDefinition(GameTextureNames.BULLET, 6, 1000);
		public static final ProjectileDefinition E_BULLET = new ProjectileDefinition(GameTextureNames.BULLET, 2, 2500);

		public final int spriteFrameUid;
		public final float speed;
		public final float life;

		private ProjectileDefinition(int spriteFrameUid, float speed, float life) {
			this.spriteFrameUid = spriteFrameUid;
			this.speed = speed;
			this.life = life;
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
		public float yOffset;
		public float zOffset;
		public int forwards;// z+
		public float lifetime;
		public float percent;

		public TrackProjectile() {
			kill();
		}

		public void init(ProjectileDefinition def, float xOffset, float yOffset, float zOffset, int forwards) {
			this.def = def;
			isActive = true;
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.zOffset = zOffset;
			this.forwards = forwards;
			collisionAlive = true;
			lifetime = def.life;
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
		public int lives;
		public float hitCooldown;
		public float flashTimer;
		public boolean isFlashing;

		public boolean isOnCooldown() {
			return hitCooldown > 0;
		}

		public TrackEntity() {

		}

		public void init(EntityDefinition def, float xOffset, float zOffset) {
			this.def = def;
			this.xOffset = xOffset;
			this.zOffset = zOffset;
			collisionAlive = true;
			isAlive = true;
			lives = def.totalLives;
		}

		// ret true if kill
		public boolean hit() {
			if (hitCooldown > 0)
				return false;

			hitCooldown = HIT_COOLDOWN_TIME;
			flashTimer = HIT_FLASH_TIME;

			lives--;

			if (lives <= 0) {
				kill();
				return true;
			}

			return false;
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

			final var cosYaw = (float) Math.cos(yaw);
			final var sinYaw = (float) Math.sin(yaw);

			final var xYaw = camera.x * cosYaw + camera.z * sinYaw;
			final var zYaw = -camera.x * sinYaw + camera.z * cosYaw;

			final var cosPitch = (float) Math.cos(pitch);
			final var sinPitch = (float) Math.sin(pitch);

			final var z = camera.y * sinPitch + zYaw * cosPitch;

			camera.x = xYaw;
			camera.y = camera.y;
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

		public final boolean[] laneFill = new boolean[NUM_LANES];

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

			for (int i = 0; i < NUM_LANES; i++) {
				laneFill[i] = true; // default all filled
			}
		}
	}

	// --------------------------------------
	// Variables
	// --------------------------------------

	private FullScreenBuffer mScreenBuffer;

	private GameStateController mGameStateController;
	private SoundFxController mSoundFxController;

	private CharAtlasRenderer mCharAtlasRenderer;
	private SceneHeader mSceneHeader;
	private GameOptions mGameOptions;
	private GameState mGameState;
	private HudRenderer mHudRenderer;
	private Texture mArrowTexture;
	private Texture mBackgroundTexture;
	private Texture mCloudsTexture;

	private SpriteSheetDefinition mGameSpriteSheet;

	private List<TrackSegment> mTrackSegments = new ArrayList<>();

	// global update lists (for movement)
	private List<TrackEntity> mEntities = new ArrayList<>();
	private List<TrackProjectile> mProjectiles = new ArrayList<>();

	float backgroundXOffset;
	float backgroundYOffset;
	float backgroundXOffsetNat;

	float backgroundCloudsXOffset;
	float backgroundCloudsYOffset;
	float backgroundCloudsXOffsetNat;

	// camera vars
	private float mFoV = 100;
	private float mCameraHeight = 150;
	private float mCameraDepth; // computed (cam dist from screen)
	private float mCameraPitch = 0;
	private float mCameraYaw = 0;
	private float mCameraTargetZ; // target for yaw
	private float mCameraOffsetZ; // camera Z position (add mPlayerZ to get player's absolute Z position).

	// world vars
	public final int mSegmentLength = 15;
	public final int mRumbleLength = 2;
	private float mTrackLength; // computed
	private float mRoadWidth = 300;

	private int mDrawDistance = 50; // number of segments to draw
	private int mPlayerLane;

	private float mPlayerX; // player offset from center on X axis
	private float mPlayerAltitude; // altitude
	private float mPlayerYAcc;
	private float mPlayerJumpCooldown;
	private float mPlayerYVel;
	private float mPlayerZ; // (computed) player relative z distance from camera

	private float mPlayerHitCooldown;
	private float mPlayerHitFlashTimer;
	private boolean mPlayerHitFlash;

	private float mPosition; // camera Z position (add mPlayerZ to get player's absolute Z position).
	private final float step = 1f / 60f;
	private float mSpeed;

	private int mLevelMinCoins;
	private float mLevelEndDist;

	private float mMinLevelSpeed;
	private float mBaseLevelSpeed;
	private float mMaxSpeed = mSegmentLength / step;

	private float mSkyTime;
	private int mSkyTint;

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public GameScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions options) {
		super(screenManager, new SimpleRendererManager(screenManager.core(), ConstantsGame.GAME_RESOURCE_GROUP_ID));

		mSceneHeader = sceneHeader;
		mGameOptions = options;
		mGameState = new GameState();

		mShowBackgroundScreens = true;

		mCharAtlasRenderer = new CharAtlasRenderer();
		mCharAtlasRenderer.setCharacterSequence("0123456789:.,/");
		mScreenBuffer = new FullScreenBuffer(ConstantsGame.GAME_CANVAS_WIDTH, ConstantsGame.GAME_CANVAS_HEIGHT);

		for (int i = 0; i < PROJECTILE_POOL_SIZE; i++) {
			mProjectiles.add(new TrackProjectile()); // pre-allocate a bunch
		}

		reset();
		buildLevel(mGameOptions.levelNumber);
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void initialize() {
		super.initialize();

		final var controllerManager = screenManager.core().controllerManager();
		mSoundFxController = (SoundFxController) controllerManager.getControllerByNameRequired(SoundFxController.CONTROLLER_NAME, LintfordCore.CORE_ENTITY_GROUP_ID);

	}

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mBackgroundTexture = resourceManager.textureManager().loadTexture("TEXTURE_GAMEBACKGROUND", "res/textures/textureGameBackground.png", ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mCloudsTexture = resourceManager.textureManager().loadTexture("TEXTURE_CLOUDS", "res/textures/textureGameClouds.png", ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mGameSpriteSheet = resourceManager.spriteSheetManager().getSpriteSheet("SPRITESHEET_GAME", ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mArrowTexture = resourceManager.textureManager().loadTexture("TEXTURE_ARROW", "res/textures/textureArrow.png", ConstantsGame.GAME_RESOURCE_GROUP_ID);

		final var digitsTexture = resourceManager.textureManager().getTexture("TEXTURE_DIGITS", ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mCharAtlasRenderer.textureAtlas(digitsTexture);

		mScreenBuffer.loadResources(resourceManager);
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		mCharAtlasRenderer.unloadResources();

		mGameSpriteSheet = null;
		mBackgroundTexture = null;
		mCloudsTexture = null;
		mArrowTexture = null;

		mScreenBuffer.unloadResources();
	}

	@Override
	public void handleInput(LintfordCore core) {
		super.handleInput(core);

		if (core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_ESCAPE, this) || core.input().gamepads().isGamepadButtonDownTimed(GLFW.GLFW_GAMEPAD_BUTTON_START, this)) {

			if (ConstantsGame.START_GAME_IMMEDIATELY) {
				reset();
				buildLevel(mGameOptions.levelNumber);
			} else {
				screenManager.addScreen(new PauseScreen(screenManager, mSceneHeader, mGameOptions, this));
			}

			return;
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_LEFT)) {
			mPlayerLane = MathHelper.clampi(--mPlayerLane, 0, NUM_LANES - 1);
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_RIGHT)) {
			mPlayerLane = MathHelper.clampi(++mPlayerLane, 0, NUM_LANES - 1);
		}

		if (!mGameState.hasGameStarted()) {
			var fireButton = core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_FIRE);
			var jumpButton = core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_JUMP);

			var space = core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_SPACE, this);

			if (space || fireButton || jumpButton) {
				mGameState.startGame();
			}

			return;
		}

		// After this is only once the game has started

		if (core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_LEFT_CONTROL, this)) {
			mSpeed += 200;
			if (mSpeed > 600) {
				mSpeed = 600;
			}
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_FIRE)) {
			addProjectile(ProjectileDefinition.P_BULLET, mPlayerX, mPlayerAltitude, mPosition + mPlayerZ, 1);
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_JUMP)) {
			updatePlayerJump(core);
		}

		final var forwardPressed = core.input().eventActionManager().getCurrentControlActionState(LD58KeyActions.KEY_BINDING_FORWARD);
		final var backwardPressed = core.input().eventActionManager().getCurrentControlActionState(LD58KeyActions.KEY_BINDING_BACKWARD);

		if (forwardPressed) {
			if (mSpeed < mMinLevelSpeed) {
				mSpeed = mMinLevelSpeed;
			}

			mSpeed += 10.f;

			if (mSpeed > mMaxSpeed)
				mSpeed = mMaxSpeed;

		}

		if (backwardPressed) {

			if (ConstantsGame.STOP_ON_BACKWARDS)
				mSpeed = 0;
			else

			if (mSpeed > mMinLevelSpeed) {
				mSpeed -= 2.f;

				if (mSpeed < mMinLevelSpeed)
					mSpeed = mMinLevelSpeed;
			}
		}

		if (ConstantsGame.ENABLED_AUTOWALK && !backwardPressed && !forwardPressed) {
			if (mSpeed < mBaseLevelSpeed) {
				mSpeed += 2.f;
			} else if (mSpeed > mBaseLevelSpeed) {
				mSpeed -= 1.f;
			}
		}
	}

	// UPDATE

	@Override
	public void update(LintfordCore core, boolean otherScreenHasFocus, boolean coveredByOtherScreen) {
		super.update(core, otherScreenHasFocus, coveredByOtherScreen);

		if (otherScreenHasFocus)
			return; // pause/lost/won screens

		mPlayerX = getLaneOffsetX(mPlayerLane);

		if (mGameState.hasGameEnded() || !mGameState.hasGameStarted())
			return;

		final float dt = (float) core.gameTime().elapsedTimeMilli() * 0.001f;
		mSkyTime += dt * 1000 * 10;
		mSkyTint = getSkyTint();

		if (mPlayerJumpCooldown > 0) {
			mPlayerJumpCooldown -= (float) core.gameTime().elapsedTimeMilli();
		}

		if (mPlayerHitCooldown > 0) {
			mPlayerHitCooldown -= (float) core.gameTime().elapsedTimeMilli();
			if (mPlayerHitCooldown < 0)
				mPlayerHitCooldown = 0;
		}

		if (mPlayerHitFlashTimer > 0) {
			mPlayerHitFlashTimer -= (float) core.gameTime().elapsedTimeMilli();

			if (mPlayerHitFlashTimer < 0) {
				mPlayerHitFlash = !mPlayerHitFlash;
				mPlayerHitFlashTimer = 50;
			}
		}

		var playerSegment = findSegment(mPosition + mPlayerZ);
		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;

		// Update parallax layer offsets
		var speedPercent = MathHelper.clamp(mSpeed / 100.f, 0, 1);
		final var cameraSegment11 = findSegment(mPosition);
		backgroundXOffset += 0.001f * speedPercent * cameraSegment11.curve * 2.f;
		backgroundXOffsetNat += 0.001f * dt;

		backgroundCloudsXOffset += 0.001f * speedPercent * cameraSegment11.curve * 2.f;
		backgroundCloudsXOffsetNat += 0.01f * dt;

		final var maxHeight = 2000;
		final var floorHeight = InterpolationHelper.lerp(playerSegment.p0.world.y, playerSegment.p1.world.y, playerPercent);
		backgroundYOffset = MathHelper.clamp(floorHeight / maxHeight, -1f, 1f) * 50.f;
		backgroundCloudsYOffset = MathHelper.clamp(floorHeight / maxHeight, -1f, 1f) * 100.f;

		updateEntities(core);
		updateProps(core);
		updateProjectiles(core, playerSegment);
		updatePlayerAltitude(core, playerSegment);
		updatePlayerCollisions(core, playerSegment);

		if (mSpeed > mMinLevelSpeed)
			mSpeed *= 0.99f;

		if (!ConstantsGame.IS_DEBUG_MODE && mGameState.hasGameStarted() && mSpeed < mMinLevelSpeed)
			mSpeed = mMinLevelSpeed;

		if (mSpeed > mMaxSpeed)
			mSpeed *= 0.99f;

		mPosition += mSpeed * dt;
		mGameState.playerDistance(mPosition + mPlayerZ);
		mGameState.speed(mSpeed);

		// update the camera stuff
		mCameraTargetZ = 10;
		final var cameraSegment = findSegment(mPosition + mPlayerZ + mCameraTargetZ);
		final var cameraPercent = ((mPosition + mPlayerZ + mCameraTargetZ) % mSegmentLength) / mSegmentLength;
		final var cameraTargetH = InterpolationHelper.lerp(cameraSegment.p0.world.y, cameraSegment.p1.world.y, cameraPercent);

		final var maxYawHeight = 50.f;
		final var relYawHeight = (cameraTargetH - floorHeight);
		final var maxYawAmt = -MathHelper.clamp(-relYawHeight / maxYawHeight, -1.f, 1.f);

		final var cameraBaseHeight = 160;
		final var cameraPitchExtent = 150;
		final var maxPitchExtent = .25f;

		mCameraPitch = MathHelper.clamp(maxYawAmt * maxPitchExtent, -2f, 0f);
		mCameraHeight = MathHelper.clamp(cameraBaseHeight + -(maxYawAmt * cameraPitchExtent), cameraBaseHeight, 700f);
		mCameraOffsetZ = MathHelper.clamp(-maxYawAmt * 20, 0f, 20f);

//		mCameraPitch = 0.f;
//		mCameraHeight = 200;
//		mCameraOffsetZ = 0;
	}

	private float getDayPhase() {
		final int cycle = 240000;
		return (mSkyTime % cycle) / (float) cycle; // normalized
	}

	private int getSkyTint() {
		final var phase = getDayPhase();

		final int dayTint = 0xFFBFBFBF; // bright (no tint)
		final int sunsetTint = 0xFFFF7E5F; // warm orange
		final int nightTint = 0xFF220022; // dark bluish

		int tint;
		if (phase < 0.33f) {
			// Day → Sunset
			float t = phase / 0.33f;
			tint = ColorHelper.lerpColorARGB(dayTint, sunsetTint, t);
		} else if (phase < 0.66f) {
			// Sunset → Night
			float t = (phase - 0.33f) / 0.33f;
			tint = ColorHelper.lerpColorARGB(sunsetTint, nightTint, t);
		} else {
			// Night → Day
			float t = (phase - 0.66f) / 0.34f;
			tint = ColorHelper.lerpColorARGB(nightTint, dayTint, t);
		}

		return tint;
	}

	public static int applyFog(int distance, int maxDistance, int objectColor, int fogColor) {
		// Calculate fog factor (0-256, where 256 = full fog)
		int fogFactor = (distance * 256) / maxDistance;

		// Clamp to [0, 256] range
		if (fogFactor < 0)
			fogFactor = 0;
		if (fogFactor > 256)
			fogFactor = 256;

		int invFogFactor = 256 - fogFactor;

		// Extract ARGB components from object color
		int objA = (objectColor >> 24) & 0xFF;
		int objR = (objectColor >> 16) & 0xFF;
		int objG = (objectColor >> 8) & 0xFF;
		int objB = objectColor & 0xFF;

		// Extract ARGB components from fog color
		int fogA = (fogColor >> 24) & 0xFF;
		int fogR = (fogColor >> 16) & 0xFF;
		int fogG = (fogColor >> 8) & 0xFF;
		int fogB = fogColor & 0xFF;

		// Blend using fixed-point math: result = object * (1 - fogFactor) + fog * fogFactor
		int blendA = (objA * invFogFactor + fogA * fogFactor) >> 8;
		int blendR = (objR * invFogFactor + fogR * fogFactor) >> 8;
		int blendG = (objG * invFogFactor + fogG * fogFactor) >> 8;
		int blendB = (objB * invFogFactor + fogB * fogFactor) >> 8;

		// Combine back into ARGB8888 format
		return (blendA << 24) | (blendR << 16) | (blendG << 8) | blendB;
	}

	private void updatePlayerAltitude(LintfordCore core, TrackSegment playerSegment) {

		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;
		final var segmentHeight = InterpolationHelper.lerp(playerSegment.p0.screen.y, playerSegment.p1.screen.y, playerPercent);

		// target segHeight + 10 ?
		// mPlayerY = segmentHeight + 0;

		final var dt = core.gameTime().elapsedTimeMilli() * 0.001f;
		final var k = 0.5f;

		final var isFloored = playerSegment.laneFill[mPlayerLane];
		var G = 9.87f;
		if (isFloored) {
			final var desiredAltitude = segmentHeight + 5;
			final var relativeAltitude = desiredAltitude - mPlayerAltitude;
			mPlayerYAcc += relativeAltitude * k;

			if (mPlayerAltitude < segmentHeight) {
				mPlayerAltitude = segmentHeight + 3.f;
				mPlayerYAcc += relativeAltitude * 1.f;
				mPlayerYVel = -mPlayerYVel * .6f;
			}

		} else {
			// no floor
			G = 500.f;
		}

		mPlayerYVel -= G * dt; // gravity
		mPlayerYVel += mPlayerYAcc;

		mPlayerAltitude += mPlayerYVel * dt;

		mPlayerYAcc = 0;
		mPlayerYVel *= 0.995f;

		if (!isFloored && mPlayerAltitude < segmentHeight - 5.f) {
			mGameState.removeHealth();
			mSoundFxController.playSound(SoundFxController.SOUND_FALL);
		}
	}

	private void updatePlayerJump(LintfordCore core) {

		if (mPlayerJumpCooldown > 0)
			return;

		mPlayerJumpCooldown = JUMP_COOLDOWN_TIME;

		final var playerSegment = findSegment(mPosition + mPlayerZ);
		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;
		final var segmentHeight = InterpolationHelper.lerp(playerSegment.p0.screen.y, playerSegment.p1.screen.y, playerPercent);

		final var isFloored = playerSegment.laneFill[mPlayerLane];
		final var isOnFloor = mPlayerAltitude - segmentHeight - 15 < 10.0f;

		if (isFloored && isOnFloor) {
			mPlayerYVel = JUMP_ALT_POWER; // Apply directly to velocity, not acceleration

			mSoundFxController.playSound(SoundFxController.SOUND_JUMP);
		}

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

					coinSoundCounter++;
					if (coinSoundCounter % 2 == 0)
						mSoundFxController.playSound(SoundFxController.SOUND_COIN_0);
					else
						mSoundFxController.playSound(SoundFxController.SOUND_COIN_1);

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

				mGameState.removeHealth();

			}
		}
	}

	private int coinSoundCounter;

	// only updates entities we can see
	private void updateEntities(LintfordCore core) {

		final var dt = (float) core.gameTime().elapsedTimeMilli() * 0.001f;

		final var baseSegment = findSegment(mPosition);
		final var numSegments = mTrackSegments.size();

		for (int i = mDrawDistance - 1; i >= 0; i--) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);

			final var numEntities = segment.entities.size();
			for (int j = numEntities - 1; j >= 0; j--) {
				final var entity = segment.entities.get(j);
				final var def = entity.def;

				final var distFromPlayer = entity.zOffset - mPosition + mPlayerZ;
				if (distFromPlayer > 1000)
					continue;

				if (!entity.isAlive) {
					if (entity.dyingTimer > 0) {
						entity.dyingTimer -= core.gameTime().elapsedTimeMilli();
					} else {
						segment.entities.remove(entity);
						continue;
					}
				}

				if (entity.hitCooldown > 0) {
					entity.hitCooldown -= core.gameTime().elapsedTimeMilli();

					if (entity.flashTimer > 0) {
						entity.flashTimer -= core.gameTime().elapsedTimeMilli();
					}

					if (entity.flashTimer <= 0) {
						entity.flashTimer = HIT_FLASH_TIME;
						entity.isFlashing = !entity.isFlashing;
					}
				}

				// update movement
				if (def.moveSpeed > 0) {
					final var origSegment = findSegment(entity.zOffset);
					entity.zOffset += -def.moveSpeed * dt;
					final var newSegment = findSegment(entity.zOffset);

					if (origSegment.index != newSegment.index) {
						origSegment.entities.remove(entity);
						newSegment.entities.add(entity);
					}

					if (entity.zOffset < mPosition) {
						entity.kill();
					}

				}

				if (def.shoots) {
					final var s = (float) Math.pow(1f - (1f - 1f), 1f / 60f);
					if (RandomNumbers.getRandomChance(s)) {
						addProjectile(ProjectileDefinition.E_BULLET, entity.xOffset, segment.p0.world.y, segment.p0.world.z - 10, -1);
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

							if (entity.hit()) {
								mGameState.addKill();
								mSoundFxController.playSound(SoundFxController.SOUND_EXPLOSION);
							} else {
								mSoundFxController.playSound(SoundFxController.SOUND_HURT);
							}

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

						mSoundFxController.playSound(SoundFxController.SOUND_HURT);

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

	// --- DRAW

	@Override
	public void draw(LintfordCore core) {

		final var pixels = mScreenBuffer.getPixels();

		drawBackground(core);

		clearBuffer(core, pixels);

		drawSky(pixels);
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

		if (!mGameState.hasGameStarted()) {
			final var spriteBatch = mRendererManager.sharedResources().uiSpriteBatch();
			spriteBatch.begin(core.gameCamera());
			final var frame = mGameSpriteSheet.getSpriteFrame(GameTextureNames.PRESS_SPACE);
			final var frameCoinsNeeded = mGameSpriteSheet.getSpriteFrame(GameTextureNames.COINS_NEEDED);

			spriteBatch.draw(mGameSpriteSheet, frame, -frame.width() / 2, -frame.height() / 2 - 20, frame.width(), frame.height(), 1.f);
			spriteBatch.draw(mGameSpriteSheet, frameCoinsNeeded, -frameCoinsNeeded.width() / 2 - 30, -frameCoinsNeeded.height() / 2 + 40, frameCoinsNeeded.width(), frameCoinsNeeded.height(), 1.f);

			mCharAtlasRenderer.drawNumberAN(spriteBatch, String.valueOf(mLevelMinCoins), frameCoinsNeeded.width() / 2 - 30, -frameCoinsNeeded.height() / 2 + 40 + 8, 0.3f, 1f);
			spriteBatch.end();

		}

		if (ConstantsGame.IS_DEBUG_MODE) {
			final var debugSegment = findSegment(mPosition + mPlayerZ);
			Debug.debugManager().drawers().drawTextImmediate(mGameCamera, "pos: " + mPosition, -150, -10, .5f);
			Debug.debugManager().drawers().drawTextImmediate(mGameCamera, "id: " + debugSegment.index, -150, 0, .5f);
		}

	}

	private void clearBuffer(LintfordCore core, int[] buffer) {
		mScreenBuffer.clear(0x001f2f3f);
	}

	private void drawBackground(LintfordCore core) {

		final var hudBounds = mGameCamera.boundingRectangle();

		final var textureBatch = mRendererManager.sharedResources().uiSpriteBatch();
		textureBatch.setColorWhite();
		{
			textureBatch.begin(mGameCamera);
			final var srcX = (backgroundXOffset + backgroundXOffsetNat) * 320;
			final var srcY = backgroundYOffset;
			final var srcW = 320;
			final var srcH = 240;

			// @formatter:off
			textureBatch.draw(mBackgroundTexture, 
					srcX, srcY, srcW, srcH, 
					hudBounds.left(), hudBounds.top(), hudBounds.width(), hudBounds.height(), 
					10.f);
			// @formatter:on
			textureBatch.end();
		}

		textureBatch.begin(mGameCamera);
		final var srcX = (backgroundCloudsXOffset + backgroundCloudsXOffsetNat) * 320;
		final var srcY = 0;
		final var srcW = 320;
		final var srcH = 240;

		// @formatter:off
		textureBatch.draw(mCloudsTexture, 
				srcX, srcY, srcW, srcH, 
				hudBounds.left(), hudBounds.top(), hudBounds.width(), hudBounds.height(), 
				10.f);
		// @formatter:on
		textureBatch.end();

	}

	private void drawSky(int[] buffer) {
		final var phase = getDayPhase();

		// Define keyframe colors
		final int dayTop = 0x0087CEEB; // sky blue
		final int dayMid = 0xFFBFBFBF; // light horizon
		final int sunsetTop = 0x002A2A72; // deep purple-blue
		final int sunsetMid = 0xFFFF7E5F; // orange/pink
		final int nightTop = 0x00000022; // almost black-blue
		final int nightMid = 0xFF220022; // dark purple

		// Interpolate keyframes based on phase
		int topColor, midColor;

		if (phase < 0.33f) {
			// Day → Sunset
			float t = phase / 0.33f;
			topColor = ColorHelper.lerpColorARGB(dayTop, sunsetTop, t);
			midColor = ColorHelper.lerpColorARGB(dayMid, sunsetMid, t);
		} else if (phase < 0.66f) {
			// Sunset → Night
			float t = (phase - 0.33f) / 0.33f;
			topColor = ColorHelper.lerpColorARGB(sunsetTop, nightTop, t);
			midColor = ColorHelper.lerpColorARGB(sunsetMid, nightMid, t);
		} else {
			// Night → Day
			float t = (phase - 0.66f) / 0.34f;
			topColor = ColorHelper.lerpColorARGB(nightTop, dayTop, t);
			midColor = ColorHelper.lerpColorARGB(nightMid, dayMid, t);
		}

		// need to force top color to have 0x00 alpha
		topColor = topColor & 0xccFFFFFF;

		int skyHeight = ConstantsGame.GAME_CANVAS_HEIGHT;

		for (int y = 0; y < skyHeight; y++) {
			float t = (float) y / (skyHeight - 1);
			int color = ColorHelper.lerpColorARGB(topColor, midColor, t);

			for (int x = 0; x < ConstantsGame.GAME_CANVAS_WIDTH; x++) {
				buffer[(ConstantsGame.GAME_CANVAS_HEIGHT - 1 - y) * ConstantsGame.GAME_CANVAS_WIDTH + x] = color;
			}
		}
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

		mDrawDistance = MathHelper.clampi(mDrawDistance, 0, numSegments);
		for (int i = 0; i < mDrawDistance; i++) {
			int ii = (baseSegment.index + i) % numSegments;
			final var segment = mTrackSegments.get(ii);

			segment.isLooped = segment.index < baseSegment.index;
			segment.clipSpaceY = maxY; // used to clip the sprites/cars in next pass

			segment.p0.curvature = x;
			segment.p1.curvature = x + dx;

			final var camX = mPlayerX;

			segment.p0.projectWorldToCamera((camX * mRoadWidth) - segment.p0.curvature, playerY + mCameraHeight, mPosition + mCameraOffsetZ, mCameraPitch, mCameraYaw);
			segment.p1.projectWorldToCamera((camX * mRoadWidth) - segment.p1.curvature, playerY + mCameraHeight, mPosition + mCameraOffsetZ, mCameraPitch, mCameraYaw);

			segment.p0.projectCameraToScreen(mCameraDepth, canvasWidth, canvasHeight, mRoadWidth / 2);
			segment.p1.projectCameraToScreen(mCameraDepth, canvasWidth, canvasHeight, mRoadWidth / 2);

			x = x + dx;
			dx = dx + segment.curve;

			// check clipped (height based)
			final var isBehindUs = (segment.p0.camera.z <= mCameraDepth * mPlayerZ);
			final var isOccluded = false; // (segment.p1.screen.y < maxY); // rely on writeOnce lock

			if (isBehindUs || isOccluded)
				continue;

			var drawLanes = (segment.p1.screen.y > maxY);

			drawSegment(segment, (int) canvasWidth, NUM_LANES, drawLanes);

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
		
		final var fog_md = (int)(mDrawDistance * mSegmentLength * .7f);
		final var fog_d = mPosition + mPlayerZ + fog_md * .5f;

		// Animated projection
		final var srcBuffer = mArrowTexture.ARGBColorData();
//		mScreenBuffer.drawTexturedPolygon(
//				srcBuffer, mArrowTexture.getTextureWidth(), mArrowTexture.getTextureHeight(), 
//				(int)(p0.screen.x - 1 - p0.screen.z), 	(int)p0.screen.y, 
//				(int)(p0.screen.x + p0.screen.z), 		(int)p0.screen.y, 
//				(int)(p1.screen.x + p1.screen.z), 		(int)p1.screen.y, 
//				(int)(p1.screen.x - 1 - p1.screen.z), 	(int)p1.screen.y, 
//				0, 0xffffffff);
		
		mScreenBuffer.writeOnceLock = true;
		
		final var lineZ0 = p0.screenScale * canvasWidth / 2;
		final var lineZ1 = p1.screenScale * canvasWidth / 2;

		final var laneWidth = 2;
		
		// purple: 0xaa282141
		// orange: 0xc16a3a
		
		// wall left
		final var wallHeight = 60;
		mScreenBuffer.drawPolygon(
				(int) (p0.screen.x - p0.screen.z), 		(int) p0.screen.y, 
				(int) (p0.screen.x - p0.screen.z - r0), (int) (p0.screen.y + wallHeight * p0.screenScale * 240), 
				(int) (p1.screen.x - p1.screen.z - r1), (int) (p1.screen.y + wallHeight * p1.screenScale * 240), 
				(int) (p1.screen.x - p1.screen.z),		(int) p1.screen.y, 
				p0.world.z, applyFog((int)(p0.world.z - fog_d), fog_md, 0xaa282141, 0x00ffffff), true);

		// wall right
		mScreenBuffer.drawPolygon(
				(int) (p0.screen.x + p0.screen.z + 1), 		(int) (p0.screen.y), 
				(int) (p0.screen.x + p0.screen.z + r0), (int) (p0.screen.y + wallHeight/2 * p0.screenScale * 240), 
				(int) (p1.screen.x + p1.screen.z + r1), (int) (p1.screen.y + wallHeight/2 * p1.screenScale * 240),
				(int) (p1.screen.x + p1.screen.z + 1), 		(int) (p1.screen.y), 
				p0.world.z, applyFog((int)(p0.world.z - fog_d), fog_md, 0xaac16a3a, 0x00ffffff), true);
		
		// road
		var lanes = NUM_LANES - 1;
		for (int i = 0; i < NUM_LANES; i++)  {
			
			if(!segment.laneFill[i])
				continue;
			
			final var lineStepX = i * mRoadWidth / (lanes + 1);
			final var lx0 = p0.screen.x - p0.screen.z + lineStepX * p0.screenScale * canvasWidth / 2;
			final var lx1 = p1.screen.x - p1.screen.z + lineStepX * p1.screenScale * canvasWidth / 2;
			
			final var blockWidth0 = (mRoadWidth / 4) * p0.screenScale * canvasWidth / 2;
			final var blockWidth1 = (mRoadWidth / 4) * p1.screenScale * canvasWidth / 2;
			
			var segColor = segment.variation == 0 ? 0xffa4bfaf : 0xffa4bfef;
			
			mScreenBuffer.drawPolygon(
					(int)(lx0), (int)p0.screen.y, 
					(int)(lx0 + blockWidth0), (int)p0.screen.y, 
					(int)(lx1 + blockWidth1), (int)p1.screen.y, 
					(int)(lx1), (int)p1.screen.y, 
					p0.world.z,
					applyFog((int)(p0.world.z - fog_d), fog_md, segColor, 0x00ffffff), true);
		}
					
		// Lanes
		if (drawLanes) {
			mScreenBuffer.mDepthMode = DepthMode.Equal;
			mScreenBuffer.writeOnceLock = false;
			
			for (int i = 0; i < lanes; i++) {
				
				final var lineStepX = (i + 1) * mRoadWidth / (lanes + 1);
				final var lx0 = p0.screen.x - p0.screen.z + lineStepX * p0.screenScale * canvasWidth / 2;
				final var lx1 = p1.screen.x - p1.screen.z + lineStepX * p1.screenScale * canvasWidth / 2;
				
				mScreenBuffer.drawPolygon(
						(int) (lx0 - laneWidth * lineZ0), (int) p0.screen.y, 
						(int) (lx0 + laneWidth * lineZ0), (int) p0.screen.y, 
						(int) (lx1 + laneWidth * lineZ1), (int) p1.screen.y, 
						(int) (lx1 - laneWidth * lineZ1), (int) p1.screen.y, 
						p0.world.z, 0x552f4f4f, true);
			}
			
			mScreenBuffer.mDepthMode = DepthMode.Less;
			
		}


		// @formatter:on
	}

	private void drawPlayer(LintfordCore core) {

		final var playerSegment = findSegment(mPosition + mPlayerZ);
		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;

		final var scale = InterpolationHelper.lerp(playerSegment.p0.screenScale, playerSegment.p1.screenScale, playerPercent);
		final var segmentCurvature = InterpolationHelper.lerp(playerSegment.p0.curvature, playerSegment.p1.curvature, playerPercent);

		final var playerFrame = mGameSpriteSheet.getSpriteFrame(GameTextureNames.PLAYER_MID);

//		final var baseSegment = findSegment(mPosition + mPlayerZ + mPlayerWorldZOffset);
//		final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);
//		final var screenX = InterpolationHelper.lerp(segment.p0.screen.x, segment.p1.screen.x, projPercent);

		final var playerW = (int) (playerFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2);
		final var playerH = (int) (playerFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2);
		final var playerX = (int) (0 + segmentCurvature) + (ConstantsGame.GAME_CANVAS_WIDTH / 2 - playerW / 2);
		final var playerY = (int) mPlayerAltitude;
		final var playerZ = playerSegment.p0.world.z - 20; // cheat a little

		final var texture = mGameSpriteSheet.texture();

		{
			final var srcX = (int) playerFrame.x();
			final var srcY = (int) playerFrame.y();
			final var srcW = (int) playerFrame.width();
			final var srcH = (int) playerFrame.height();

			int col = 0xffafafaf;
			if (mPlayerHitCooldown > 0 && mPlayerHitFlash) {
				col = 0xffffffff;
			}

			mScreenBuffer.copyPixelsAtlas(texture.ARGBColorData(), // Src pixels
					srcX, srcY, srcW, srcH, texture.getTextureWidth(), // src rect
					playerX, playerY, playerW, playerH, // dest rect
					playerZ, col, false);
		}

		if (playerSegment.laneFill[mPlayerLane]) {
			final var shadowFrame = mGameSpriteSheet.getSpriteFrame(GameTextureNames.OBJECT_SHADOW);

			final var srcX = (int) shadowFrame.x();
			final var srcY = (int) shadowFrame.y();
			final var srcW = (int) shadowFrame.width();
			final var srcH = (int) shadowFrame.height();

			final var floorHeight = (int) InterpolationHelper.lerp(playerSegment.p0.screen.y, playerSegment.p1.screen.y, playerPercent);

			final var shadowScale = InterpolationHelper.lerp(1.5f, 0.15f, mPlayerAltitude / (floorHeight + 100)) * .5f;

			mScreenBuffer.copyPixelsAtlas(texture.ARGBColorData(), // Src pixels
					srcX, srcY, srcW, srcH, texture.getTextureWidth(), // src rect
					(int) (playerX + 32 * (1 - shadowScale) / 2), floorHeight, (int) (playerW * shadowScale), (int) (srcH * shadowScale), // dest rect
					playerZ, 0xffffffff, false);
		}

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
				final var scale = InterpolationHelper.lerp(segment.p0.screenScale, segment.p1.screenScale, .5f);

				final var propDefinition = prop.definition;
				final var spriteFrame = mGameSpriteSheet.getSpriteFrame(propDefinition.spriteFrameUid);

				final var destW = spriteFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2;
				final var destH = spriteFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2;
				final var destX = segment.p0.screen.x + (prop.xOffset * scale * mRoadWidth * ConstantsGame.GAME_CANVAS_WIDTH / 2) - destW / 2;
				final var destY = segment.p0.screen.y + 15 * scale * ConstantsGame.GAME_CANVAS_HEIGHT;

				{
					int srcX = (int) spriteFrame.x();
					int srcY = (int) (spriteFrame.y());
					int srcW = (int) spriteFrame.width();
					int srcH = (int) spriteFrame.height();

					mScreenBuffer.copyPixelsAtlas(srcPixels, // Src pixels
							srcX, srcY, srcW, srcH, textureWidth, // src rect
							(int) destX, (int) destY, (int) destW, (int) destH, // dest rect
							entityZ, 0xffcfcfcf, false);
				}

				{
					final var shadowFrame = mGameSpriteSheet.getSpriteFrame(GameTextureNames.OBJECT_SHADOW);

					final var srcX = (int) shadowFrame.x();
					final var srcY = (int) shadowFrame.y();
					final var srcW = (int) shadowFrame.width();
					final var srcH = (int) shadowFrame.height();

					final var floorHeight = (int) InterpolationHelper.lerp(segment.p0.screen.y, segment.p1.screen.y, .5f);
					final var shadowScale = 1.f;

					mScreenBuffer.copyPixelsAtlas(texture.ARGBColorData(), // Src pixels
							srcX, srcY, srcW, srcH, texture.getTextureWidth(), // src rect
							(int) (destX), floorHeight, (int) (destW * shadowScale), (int) (srcH * .5f), // dest rect
							entityZ, 0xffffffff, false);
				}

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

				final var entityPercent = (entity.zOffset % mSegmentLength) / mSegmentLength;
				final var scale = InterpolationHelper.lerp(segment.p0.screenScale, segment.p1.screenScale, entityPercent);
				final var screenX = InterpolationHelper.lerp(segment.p0.screen.x, segment.p1.screen.x, entityPercent);
				final var screenY = InterpolationHelper.lerp(segment.p0.screen.y, segment.p1.screen.y, entityPercent);

				final var spriteFrame = mGameSpriteSheet.getSpriteFrame(def.spriteFrameUid);

				final var destW = spriteFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2;
				final var destH = spriteFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2;
				final var destX = screenX + (entity.xOffset * scale * mRoadWidth * ConstantsGame.GAME_CANVAS_WIDTH / 2) - destW / 2;
				final var destY = screenY + 15 * scale * ConstantsGame.GAME_CANVAS_HEIGHT;

				{
					int srcX = (int) spriteFrame.x();
					int srcY = (int) (spriteFrame.y());
					int srcW = (int) spriteFrame.width();
					int srcH = (int) spriteFrame.height();

					int col = 0xffcfcfcf;
					if (entity.hitCooldown > 0 && entity.isFlashing) {
						col = 0xffffffff;
					}

					mScreenBuffer.copyPixelsAtlas(srcPixels, // Src pixels
							srcX, srcY, srcW, srcH, textureWidth, // src rect
							(int) destX, (int) destY, (int) destW, (int) destH, // dest rect
							entityZ, col, false);
				}

				{
					final var shadowFrame = mGameSpriteSheet.getSpriteFrame(GameTextureNames.OBJECT_SHADOW);

					final var srcX = (int) shadowFrame.x();
					final var srcY = (int) shadowFrame.y();
					final var srcW = (int) shadowFrame.width();
					final var srcH = (int) shadowFrame.height();

					final var shadowScale = 1.f;

					mScreenBuffer.copyPixelsAtlas(texture.ARGBColorData(), // Src pixels
							srcX, srcY, srcW, srcH, texture.getTextureWidth(), // src rect
							(int) (destX), (int) screenY, (int) (destW * shadowScale), (int) (srcH * .5f), // dest rect
							entityZ, 0xffffffff, false);
				}

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
				final var destY = screenY + 30 * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2;

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

		backgroundXOffset = 0.f;
		backgroundXOffsetNat = 0.f;

		mGameState.reset();

		// cam
		mCameraHeight = 200;
		mFoV = 140;
		mCameraDepth = 1f / (float) Math.tan(mFoV / 2 * Math.PI / 180);
		mPlayerZ = mCameraHeight * mCameraDepth;
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

	private void digOutSegments(int startSegId, int length, int lane) {
		if (lane < 0 || lane >= NUM_LANES)
			return;

		for (int i = startSegId; i < startSegId + length; i++) {
			if (startSegId + length > mTrackSegments.size())
				return;

			final var segment = getSegment(i);
			segment.laneFill[lane] = false;
		}

	}

	private void addProp(PropDefinition def, int segmentUid, int laneNum) {
		final var newProp = new TrackProp(def, getLaneOffsetX(laneNum));
		final var segment = getSegment(segmentUid);
		segment.props.add(newProp);
	}

	private void addProjectile(ProjectileDefinition def, float offsetX, float yOffset, float zOffset, int direction) {
		if (direction == 0) {
			Debug.debugManager().logger().w(GameScreen.class.getSimpleName(), "Someone is shooting projectiles with no direction!");
			return;
		}

		final var segment = findSegment(zOffset);
		final var projectile = getFreeProjectile();

		if (projectile == null)
			return;

		projectile.init(def, offsetX, yOffset, zOffset, direction);
		segment.projectiles.add(projectile);

		mSoundFxController.playSound(SoundFxController.SOUND_SHOOT);

		// global add for update
		mProjectiles.add(projectile);

	}

	private void addEntity(EntityDefinition def, int segmentUid, int laneNum) {
		final var newEntity = new TrackEntity();
		newEntity.init(def, getLaneOffsetX(laneNum), segmentUid * mSegmentLength);

		final var segment = getSegment(segmentUid);
		segment.entities.add(newEntity);

		// global update list
		mEntities.add(newEntity);
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
		lane = MathHelper.clampi(lane, 0, NUM_LANES);

		final var laneI = 1.f / (NUM_LANES * 2);
		final var laneS = 1.f / (NUM_LANES);

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
		screenManager.addScreen(new WonScreen(screenManager, mSceneHeader, mGameOptions, this));
	}

	@Override
	public void onGameLost() {

		var finishedDistance = mGameState.playerDistance() >= mGameState.endTrackDistance();
		var loosByNotEnoughCoinage = mGameState.coins() < mGameState.endLevelCoinAmt();

		var endState = GameEndState.LOST_DEATH;
		if (finishedDistance && loosByNotEnoughCoinage)
			endState = GameEndState.LOST_NOT_ENOUGH_COINS;

		screenManager.addScreen(new LostScreen(screenManager, mSceneHeader, mGameOptions, this, endState));
	}

	// LEVELS --------------------------------------

	private void buildLevel(int levelNum) {
		TrackSegment.resetIndexCounter();
		mTrackSegments.clear();

		mEntities.clear();

		switch (levelNum) {
		default:
		case 0:
			setupWorld_Tutorial();
			break;

		case 1:
			setupWorld_0(); // easy
			break;
		case 2:
			setupWorld_1(); // hard
			break;
		}

		finalizeBuild();
	}

	private void finalizeBuild() {
		if (ConstantsGame.IS_DEBUG_MODE) {
			mPosition = mDebugStartOnSegmentId * mSegmentLength;
		} else {
			mPosition = 0;
		}

		final var numSegments = mTrackSegments.size();
		for (int i = 0; i < numSegments; i++) {
			mTrackSegments.get(i).variation = (i % 2);
		}

		if (mGameOptions.allowStopping) {
			mMinLevelSpeed = 0;
		}

		mTrackLength = mTrackSegments.size() * mSegmentLength;
		mLevelEndDist = mTrackLength - 10 * mSegmentLength;
		mGameState.readyGame(mTrackLength, mLevelEndDist, mLevelMinCoins);
	}

	private void setupWorld_Tutorial() { // tutorial
		mMinLevelSpeed = 50;
		mBaseLevelSpeed = 100;
		mMaxSpeed = 200;

		// @formatter:off
		final var testHillHeight = 40;
		final var turnMod = 5.f;
		
		addRoad(0, 	20, 	0, 		0 * turnMod, 		0);
		addRoad(0, 	20, 	0, 		0 * turnMod, 		testHillHeight);
		addRoad(0, 	20, 	0, 		0 * turnMod, 		0);
		addRoad(0, 	20, 	0, 		0 * turnMod, 		-testHillHeight);
		addRoad(0, 	20, 	0, 		0 * turnMod, 		0);
		
		addRoad(0, 	20, 	0, 		-.3f * turnMod, 	testHillHeight / 2);
		addRoad(0, 	20, 	10,		-.6f * turnMod, 	testHillHeight / 2);
		addRoad(0, 	20,   	0, 		1.f * turnMod, 		testHillHeight / 4);
		addRoad(0, 	20,   	0, 		0f * turnMod, 		-testHillHeight / 4);
		addRoad(0, 10,  	0, 		-.6f * turnMod, 	testHillHeight);
		
		addRoad(0, 30,  	0, 		0f * turnMod, 		0);
		addRoad(0, 30,  	0, 		0f * turnMod, 		testHillHeight / 2);
		addRoad(0, 40,  	0, 		0f * turnMod, 		0);
		// @formatter:on

		addProp(PropDefinition.COIN, 40, 0);
		addProp(PropDefinition.COIN, 42, 0);
		addProp(PropDefinition.COIN, 44, 0);

		addProp(PropDefinition.COIN, 40, 3);
		addProp(PropDefinition.COIN, 42, 3);
		addProp(PropDefinition.COIN, 44, 3);

		addProp(PropDefinition.COIN, 75, 1);
		addProp(PropDefinition.COIN, 76, 1);
		addProp(PropDefinition.COIN, 77, 1);
		addProp(PropDefinition.COIN, 75, 2);
		addProp(PropDefinition.COIN, 76, 2);
		addProp(PropDefinition.COIN, 77, 2);

		addProp(PropDefinition.COIN, 86, 0);
		addProp(PropDefinition.COIN, 88, 1);
		addProp(PropDefinition.COIN, 90, 2);
		addProp(PropDefinition.COIN, 92, 3);
		addProp(PropDefinition.COIN, 94, 3);
		addProp(PropDefinition.COIN, 96, 3);

		addProp(PropDefinition.WALL, 98, 2);

		addProp(PropDefinition.WALL, 104, 0);
		addProp(PropDefinition.WALL, 104, 2);

		addProp(PropDefinition.WALL, 179, 0);

		for (int i = 145; i < 150; i++) {
			addProp(PropDefinition.COIN, i, 0);
			addProp(PropDefinition.COIN, i + 20, 2);
			addProp(PropDefinition.COIN, i + 40, 3);

			digOutSegments(i + 40, 10, 0);
		}

		digOutSegments(220, 3, 0);
		digOutSegments(225, 3, 3);
		digOutSegments(240, 3, 2);
		digOutSegments(245, 3, 1);
		digOutSegments(255, 3, 3);
		addProp(PropDefinition.WALL, 261, 1);
		addProp(PropDefinition.WALL, 260, 0);

		digOutSegments(257, 3, 0);

		addProp(PropDefinition.COIN, 240, 3);
		addProp(PropDefinition.COIN, 242, 3);

		addProp(PropDefinition.COIN, 260, 2);
		addProp(PropDefinition.COIN, 261, 2);

		addEntity(EntityDefinition.BLOCKER, 146, 2);
		addEntity(EntityDefinition.BLOCKER, 146, 3);

		mLevelMinCoins = 22;

	}

	private void setupWorld_0() { // medium
		mMinLevelSpeed = 40;
		mBaseLevelSpeed = 50;
		mMaxSpeed = 200;

		// @formatter:off
		final var testHillHeight = 60;
		final var turnMod = 5.f;
		
		addRoad(0, 	20, 	0, 		0 * turnMod, 		0);
		addRoad(0, 	30, 	0, 		-.4f * turnMod, 		testHillHeight);
		addRoad(0, 	25, 	0, 		.4f * turnMod, 		-testHillHeight / 2f);
		addRoad(0, 	20, 	0, 		.4f * turnMod, 		testHillHeight);
		addRoad(0, 	20, 	0, 		.6f * turnMod, 		0);
		
		addRoad(0, 	20, 	0, 		-.3f * turnMod, 	testHillHeight / 2);
		addRoad(0, 	20, 	10,		-.6f * turnMod, 	testHillHeight / 2);
		addRoad(0, 	20,   	0, 		1.f * turnMod, 		testHillHeight / 4);
		addRoad(0, 	20,   	0, 		0f * turnMod, 		-testHillHeight / 4);
		addRoad(0, 10,  	0, 		-.6f * turnMod, 	testHillHeight);
		
		addRoad(0, 30,  	0, 		0f * turnMod, 		0);
		addRoad(0, 30,  	0, 		0f * turnMod, 		testHillHeight / 2);
		addRoad(0, 40,  	0, 		0f * turnMod, 		0);
		// @formatter:on

		digOutSegments(20, 5, 3);
		digOutSegments(40, 10, 0);
		digOutSegments(70, 10, 1);
		digOutSegments(80, 10, 2);

		digOutSegments(111, 4, 0);
		digOutSegments(111, 4, 1);
		digOutSegments(111, 4, 2);
		digOutSegments(111, 4, 3);

		digOutSegments(113, 4, 1);
		digOutSegments(113, 4, 2);

		digOutSegments(139, 4, 3);

		addProp(PropDefinition.WALL, 100, 0);
		addProp(PropDefinition.WALL, 100, 1);

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

		addEntity(EntityDefinition.NORMAL, 120, 2);
		addEntity(EntityDefinition.NORMAL, 130, 3);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 145, 0);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 145, 1);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 157, 2);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 157, 3);

		digOutSegments(159, 10, 1);
		digOutSegments(164, 3, 0);

		addEntity(EntityDefinition.NORMAL, 185, 1);
		addEntity(EntityDefinition.NORMAL, 185, 2);

		addProp(PropDefinition.COIN, 200, 2);
		addProp(PropDefinition.COIN, 198, 3);

		addProp(PropDefinition.WALL, 195, 0);
		addProp(PropDefinition.WALL, 195, 1);

		digOutSegments(222, 3, 0);
		digOutSegments(222, 3, 1);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 222, 2);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 222, 3);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 235, 2);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 235, 3);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 282, 0);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 282, 3);

		addEntity(EntityDefinition.NORMAL, 289, 1);
		addEntity(EntityDefinition.NORMAL, 289, 2);

		digOutSegments(232, 2, 0);
		digOutSegments(232, 2, 1);
		digOutSegments(232, 2, 2);
		digOutSegments(232, 2, 3);

		digOutSegments(238, 2, 0);
		digOutSegments(238, 2, 1);
		digOutSegments(238, 2, 2);
		digOutSegments(238, 2, 3);

		digOutSegments(244, 2, 0);
		digOutSegments(244, 2, 1);
		digOutSegments(244, 2, 2);
		digOutSegments(244, 2, 3);

		digOutSegments(252, 2, 0);
		digOutSegments(250, 2, 1);
		digOutSegments(250, 2, 2);
		digOutSegments(252, 2, 3);

		digOutSegments(260, 4, 0);
		digOutSegments(260, 2, 1);
		digOutSegments(260, 2, 2);
		digOutSegments(260, 4, 3);

		digOutSegments(266, 2, 1);
		digOutSegments(266, 2, 2);

		addProp(PropDefinition.COIN, 155, 0);
		addProp(PropDefinition.COIN, 156, 0);
		addProp(PropDefinition.COIN, 157, 0);
		addProp(PropDefinition.COIN, 158, 0);
		addProp(PropDefinition.COIN, 159, 0);
		addProp(PropDefinition.COIN, 160, 0);

		addEntity(EntityDefinition.NORMAL, 150, 3);

		mLevelMinCoins = 15;

	}

	private static final int mDebugStartOnSegmentId = 0;

	private void setupWorld_1() { // hard
		mMinLevelSpeed = 50;
		mBaseLevelSpeed = 75;
		mMaxSpeed = 200;

		// @formatter:off
		final var testHillHeight = 50;
		final var turnMod = 6.f;
		
		addRoad(0, 	25, 	0, 		0 * turnMod, 		0);
		addRoad(0, 	25, 	0, 		.5f * turnMod, 		testHillHeight);
		addRoad(0, 	25, 	0, 		-.4f * turnMod, 	-testHillHeight);
		addRoad(0, 	25, 	0, 		-.4f * turnMod, 	testHillHeight*2);
		addRoad(0, 	30, 	0, 		-.6f * turnMod, 	-testHillHeight);
		
		addRoad(0, 	20, 	0, 		.3f * turnMod, 		-testHillHeight);
		addRoad(0, 	20, 	0,		-.6f * turnMod, 	testHillHeight);
		addRoad(0, 	20,   	0, 		1.f * turnMod, 		testHillHeight / 4);
		addRoad(0, 	20,   	0, 		0.5f * turnMod, 	-testHillHeight / 4);
		addRoad(0,  30,  	0, 		-.6f * turnMod, 	testHillHeight);
		
		addRoad(0, 	30,  	0, 		0f * turnMod, 		0);
		addRoad(0, 	30,  	0, 		0f * turnMod, 		testHillHeight / 2);
		addRoad(0, 	40,  	0, 		-.40f * turnMod, 	testHillHeight / 2);
		addRoad(0, 	40,  	0, 		.10f * turnMod, 	-testHillHeight / 2);
		// @formatter:on

		digOutSegments(20, 5, 3);
		digOutSegments(25, 4, 0);
		digOutSegments(45, 10, 0);
		digOutSegments(70, 10, 1);
		digOutSegments(80, 10, 2);
		digOutSegments(94, 3, 3);

		// TEST
		addEntity(EntityDefinition.NORMAL, 30, 2);
		addEntity(EntityDefinition.NORMAL, 10, 3);

		addProp(PropDefinition.WALL, 100, 0);
		addProp(PropDefinition.WALL, 100, 1);

		addProp(PropDefinition.COIN, 30, 2);
		addProp(PropDefinition.COIN, 22, 1);
		addProp(PropDefinition.COIN, 34, 2);
		addProp(PropDefinition.COIN, 36, 1);
		addProp(PropDefinition.COIN, 38, 2);
		addProp(PropDefinition.COIN, 40, 1);

		addProp(PropDefinition.COIN, 50, 2);
		addProp(PropDefinition.COIN, 52, 1);
		addProp(PropDefinition.COIN, 54, 2);
		addProp(PropDefinition.COIN, 56, 1);
		addProp(PropDefinition.COIN, 58, 2);
		addProp(PropDefinition.COIN, 50, 1);

		addProp(PropDefinition.COIN, 85, 3);
		addProp(PropDefinition.COIN, 87, 3);
		addProp(PropDefinition.COIN, 89, 3);
		addProp(PropDefinition.COIN, 91, 3);

		addEntity(EntityDefinition.NORMAL, 88, 0);
		addEntity(EntityDefinition.NORMAL, 130, 3);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 145, 0);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 145, 1);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 157, 2);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 157, 3);

		digOutSegments(113, 4, 1);
		digOutSegments(113, 4, 2);

		addProp(PropDefinition.WALL, 118, 1);
		addProp(PropDefinition.WALL, 118, 2);

		digOutSegments(139, 4, 3);

		digOutSegments(159, 10, 1);
		digOutSegments(164, 3, 0);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 145, 0);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 145, 1);

		addEntity(EntityDefinition.NORMAL, 185, 1);
		addEntity(EntityDefinition.NORMAL, 185, 2);

		addProp(PropDefinition.COIN, 200, 2);
		addProp(PropDefinition.COIN, 198, 3);

		addProp(PropDefinition.WALL, 195, 0);
		addProp(PropDefinition.WALL, 195, 1);

		digOutSegments(222, 3, 0);
		digOutSegments(222, 3, 1);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 222, 2);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 222, 3);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 235, 2);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 235, 3);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 282, 0);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 282, 3);

		addEntity(EntityDefinition.NORMAL, 289, 1);
		addEntity(EntityDefinition.NORMAL, 289, 2);

		digOutSegments(232, 2, 0);
		digOutSegments(232, 2, 1);
		digOutSegments(232, 2, 2);
		digOutSegments(232, 2, 3);

		digOutSegments(238, 2, 0);
		digOutSegments(238, 2, 1);
		digOutSegments(238, 2, 2);
		digOutSegments(238, 2, 3);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 282, 0);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 282, 3);

		addEntity(EntityDefinition.NORMAL, 200, 0);
		addEntity(EntityDefinition.NORMAL, 208, 0);

		digOutSegments(244, 2, 0);
		digOutSegments(244, 2, 1);
		digOutSegments(244, 2, 2);
		digOutSegments(244, 2, 3);

		digOutSegments(252, 2, 0);
		digOutSegments(250, 2, 1);
		digOutSegments(250, 2, 2);
		digOutSegments(252, 2, 3);

		digOutSegments(260, 4, 0);
		digOutSegments(260, 2, 1);
		digOutSegments(260, 2, 2);
		digOutSegments(260, 4, 3);

//		digOutSegments(260, 4, 0);
		digOutSegments(266, 2, 1);
		digOutSegments(266, 2, 2);
//		digOutSegments(260, 4, 3);

		addProp(PropDefinition.COIN, 155, 0);
		addProp(PropDefinition.COIN, 156, 0);
		addProp(PropDefinition.COIN, 157, 0);
		addProp(PropDefinition.COIN, 158, 0);
		addProp(PropDefinition.COIN, 159, 0);
		addProp(PropDefinition.COIN, 160, 0);

		addProp(PropDefinition.COIN, 305, 1);
		addProp(PropDefinition.COIN, 306, 1);
		addProp(PropDefinition.COIN, 307, 3);
		addProp(PropDefinition.COIN, 308, 2);
		addProp(PropDefinition.COIN, 309, 2);

		digOutSegments(318, 3, 1);
		digOutSegments(318, 3, 2);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 330, 2);

		digOutSegments(335, 3, 0);
		digOutSegments(337, 3, 1);
		addProp(PropDefinition.WALL, 340, 1);

		addEntity(EntityDefinition.NORMAL, 345, 2);
		addEntity(EntityDefinition.NORMAL, 345, 3);

		digOutSegments(346, 3, 2);
		digOutSegments(346, 3, 3);

		addEntity(EntityDefinition.BLOCKER_SHOOTER, 356, 0);
		addEntity(EntityDefinition.BLOCKER_SHOOTER, 356, 1);

		addEntity(EntityDefinition.NORMAL, 150, 3);

		addProp(PropDefinition.COIN, 317, 3);
		addProp(PropDefinition.COIN, 319, 3);
		addProp(PropDefinition.COIN, 321, 3);

		mLevelMinCoins = 30;
	}

	@Override
	public void resetLevel() {
		// this lets us reset the level faster than reloading this class instance
		reset();
		buildLevel(mGameOptions.levelNumber);
	}
}
