package net.lintfordlib.samples;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.assets.ResourceMapLoader;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.debug.Debug;
import net.lintfordlib.core.graphics.batching.TextureBatchPCT;
import net.lintfordlib.core.graphics.textures.Texture;
import net.lintfordlib.core.time.TimeConstants;
import net.lintfordlib.options.DisplayManager;

public class GameResourceLoader extends ResourceMapLoader {

	// ---------------------------------------------
	// Variables
	// ---------------------------------------------

	private TextureBatchPCT mTextureBatch;

	private Texture mLoadingBackgroundTexture;
	private Texture mLoadingTexture;
	private float mRunningTime;

	private long mStartTime;
	private long mMinimumDisplayTimeMs;

	// ---------------------------------------------
	// Constructors
	// ---------------------------------------------

	public GameResourceLoader(ResourceManager resourceManager, DisplayManager displayManager) {
		this(resourceManager, displayManager, 1500);
	}

	public GameResourceLoader(ResourceManager resourceManager, DisplayManager displayManager, long minimumDisplayTimeMs) {
		super(resourceManager, displayManager, "res_map.json", ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mTextureBatch = new TextureBatchPCT();

		mStartTime = System.nanoTime();
		mMinimumDisplayTimeMs = minimumDisplayTimeMs;
	}

	// ---------------------------------------------
	// Core-Methods
	// ---------------------------------------------

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mTextureBatch.loadResources(resourceManager);
		mLoadingTexture = resourceManager.textureManager().loadTexture("TEXTURE_LOADING_ARROW", "res/textures/textureLoadingArrow.png", ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mLoadingBackgroundTexture = mResourceManager.textureManager().loadTexture("TEXTURE_STARTUP", "res/textures/textureLoadingScreen.png", ConstantsGame.GAME_RESOURCE_GROUP_ID);
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		mTextureBatch.unloadResources();
		mLoadingTexture = null;
	}

	@Override
	protected void onDraw(LintfordCore core) {
		super.onDraw(core);

		final var lAnimSpeed = 3.f;
		mRunningTime += (float) frameDelta * .001f * lAnimSpeed;

		final var lHudBounds = core.HUD().boundingRectangle();

		mTextureBatch.begin(core.HUD());
		mTextureBatch.setColorWhite();
		mTextureBatch.draw(mLoadingBackgroundTexture, 0.f, 0.f, 960.f, 576.f, core.HUD().boundingRectangle(), .01f);

		final var lDstX = lHudBounds.right() - 6.f - 32.f;
		final var lDstY = lHudBounds.bottom() - 6.f - 32.f;

		mTextureBatch.drawAroundCenter(mLoadingTexture, 0.f, 0.f, 64.f, 64.f, lDstX, lDstY, 64.f, 64f, -0.01f, mRunningTime, 0.f, 0.f, 1.f);
		mTextureBatch.end();

		// You can optionally display a graphic here which is rendered for as long as the resources are being loaded.

	}

	@Override
	protected void resourcesToLoadInBackground(int entityGroupUid) {
		Debug.debugManager().logger().i(getClass().getSimpleName(), "Loading game assets into group: " + ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mResourceManager.addProtectedEntityGroupUid(ConstantsGame.GAME_RESOURCE_GROUP_ID);

		currentStatusMessage("Loading resources");

		mResourceManager.textureManager().loadTexturesFromMetafile("res/textures/_meta.json", ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mResourceManager.spriteSheetManager().loadSpriteSheetFromMeta("res/spritesheets/_meta.json", ConstantsGame.GAME_RESOURCE_GROUP_ID);

		// If you need to override some of the default textures loaded in the LintfordLib.CoreSpritesheetDefinition, it can be done here with the following lines:
		// mResourceManager.textureManager().loadTexture("TEXTURE_CORE", "res/textures/textureCore.png", GL11.GL_NEAREST, true, LintfordCore.CORE_ENTITY_GROUP_ID);
		// mResourceManager.spriteSheetManager().loadSpriteSheet("res/spritesheets/spritesheetCore.json", LintfordCore.CORE_ENTITY_GROUP_ID);

		mResourceManager.fontManager().loadBitmapFont("FONT_NULSHOCK_12", "res/fonts/fontNulshock12.json");
		mResourceManager.fontManager().loadBitmapFont("FONT_NULSHOCK_16", "res/fonts/fontNulshock16.json");
		mResourceManager.fontManager().loadBitmapFont("FONT_NULSHOCK_22", "res/fonts/fontNulshock22.json");

		try {

			final var lTimeTakenMs = (System.nanoTime() - mStartTime) / TimeConstants.NanoToMilli;
			final var lTimeRemaining = mMinimumDisplayTimeMs - lTimeTakenMs;

			System.out.println("time taken: " + lTimeTakenMs);
			System.out.println("waiting for: " + lTimeRemaining);

			if (lTimeRemaining > 0)
				Thread.sleep((int) (lTimeRemaining / TimeConstants.NanoToMilli));

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}
