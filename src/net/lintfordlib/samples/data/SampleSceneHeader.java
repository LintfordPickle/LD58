package net.lintfordlib.samples.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.GsonBuilder;

import net.lintfordlib.core.debug.Debug;
import net.lintfordlib.data.scene.SceneHeader;

public class SampleSceneHeader extends SceneHeader {

	// --------------------------------------
	// Constants
	// --------------------------------------

	private static final long serialVersionUID = 2902243363895479643L;

	// --------------------------------------
	// Variables
	// --------------------------------------

	public int numberOfThings;

	public boolean winningEnabled;
	public boolean losingEnabled;

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public SampleSceneHeader() {
		super();
	}

	public SampleSceneHeader(String sceneName) {
		super(sceneName);
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	public static SampleSceneHeader loadRaceSceneHeaderFileFromFilepath(String filepath) {
		if (filepath == null || filepath.length() == 0) {
			Debug.debugManager().logger().e(SceneHeader.class.getSimpleName(), "Filepath for SampleSceneHeader file cannot be null or empty!");
			return null;
		}

		try {
			final var lGson = new GsonBuilder().create();
			final var lFileContents = new String(Files.readAllBytes(Paths.get(filepath)));
			final var lSceneHeader = lGson.fromJson(lFileContents, SampleSceneHeader.class);

			if (lSceneHeader == null) {
				Debug.debugManager().logger().e(SceneHeader.class.getSimpleName(), "Couldn't deserialize SampleSceneHeader file!");
				return null;
			}

			lSceneHeader._cmpName = lSceneHeader.sceneName().toUpperCase();

			return lSceneHeader;
		} catch (IOException e) {
			Debug.debugManager().logger().e(SceneHeader.class.getSimpleName(), "Error deserializing SampleSceneHeader file.");
			Debug.debugManager().logger().printException(SceneHeader.class.getSimpleName(), e);
		}

		return null;
	}

}
