package net.lintfordlib.samples.renderers;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.rendering.RenderPass;
import net.lintfordlib.renderers.BaseRenderer;
import net.lintfordlib.renderers.RendererManagerBase;

public class HudRenderer extends BaseRenderer {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String RENDERER_NAME = "Hud Renderer";

	// --------------------------------------
	// Properties
	// --------------------------------------

	@Override
	public boolean isInitialized() {
		return true;
	}

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public HudRenderer(RendererManagerBase rendererManager, int entityGroupUid) {
		super(rendererManager, RENDERER_NAME, entityGroupUid);
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void initialize(LintfordCore core) {
		super.initialize(core);

		// Get any controllers or renderers created with the game screen.
	}

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		// load hud related game resources
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		// unload hud related game resources
	}

	@Override
	public boolean handleInput(LintfordCore core) {
		return super.handleInput(core);

		// Handle any hud input actions
	}

	@Override
	public void update(LintfordCore core) {
		super.update(core);

		// Update hud elements
	}

	@Override
	public void draw(LintfordCore core, RenderPass renderPass) {
		final var lHudBounds = core.HUD().boundingRectangle();

		final var lFontBatch = mRendererManager.sharedResources().uiTitleFont();

		lFontBatch.begin(core.HUD());
		lFontBatch.drawShadowedText("Hud Renderer", lHudBounds.left() + 10.f, lHudBounds.top() + 40.f, .1f, 1.f, 1.f, 1.f);
		lFontBatch.end();
	}
}
