package net.lintfordlib.samples;

import java.io.File;

import net.lintfordlib.options.ResourcePathsConfig;

public class SampleGameResourcePaths {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String SCENES_DIRECTORY = "res/tracks/";
	public static final String SUBDIRECTORY_CUSTOM = "custom";

	public static final String SCENES_DIR_KEY_NAME = "ScenesDirKeyName";

	// --------------------------------------
	// Variables
	// --------------------------------------

	private String mSceneHeaderFileExtension = ".hdr";
	private String mSceneDataFileExtension = ".data";

	private String mScenesBaseDirectory;

	protected ResourcePathsConfig mResourcePathsConfig;

	// --------------------------------------
	// Properties
	// --------------------------------------

	public ResourcePathsConfig pathsConfig() {
		return mResourcePathsConfig;
	}

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public SampleGameResourcePaths(ResourcePathsConfig pathsConfig) {
		mResourcePathsConfig = pathsConfig;

		// get (or set) the paths directory.
		final var lPath = new File("res/def/scenes/");
		mScenesBaseDirectory = mResourcePathsConfig.getKeyValue(SCENES_DIR_KEY_NAME, lPath.getAbsolutePath());

	}
}
