package io.piotrjastrzebski.bte.window;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.piotrjastrzebski.bte.AIEditor;

/**
 * Simple util class that will open a new Lwjgl3 window with editor in it
 *
 * Usage:
 * <code>
 * AIEditorWindow window = new AIEditorWindow(aiEditor);
 * window.open();
 * </code>
 * Created by EvilEntity on 19/02/2016.
 */
public class AIEditorWindow {
	private static final String TAG = AIEditorWindow.class.getSimpleName();
	private final AIEditor editor;
	private final Window window;
	private WindowListener listener;
	private Lwjgl3Window editorWindow;

	/**
	 *
	 * @param editor editor for window
	 */
	public AIEditorWindow (AIEditor editor) {
		this.editor = editor;
		window = editor.getWindow(false);
	}

	/**
	 * set listener for window events
	 * @param listener listenr for window events
	 */
	public void setListener (WindowListener listener) {
		this.listener = listener;
	}

	/**
	 * Open new window for given editor
	 * @return if window opened successfully
	 */
	public boolean open () {
		return open(-1, -1, 800, 600);
	}

	/**
	 * Open new window for given editor
	 * @param width width of the window
	 * @param height height of the window
	 * @return if window opened successfully
	 */
	public boolean open (int width, int height) {
		return open(-1, -1, width, height);
	}

	/**
	 * Open new window for given editor
	 * @param x x position of the window
	 * @param y y position of the window
	 * @param width width of the window
	 * @param height height of the window
	 * @return if window opened successfully
	 */
	public boolean open (int x, int y, final int width, int height) {
		if (!(Gdx.app instanceof Lwjgl3Application)) {
			Gdx.app.error(TAG, "MultiWindow is supported only in Lwjgl3 backend!");
			return false;
		}
		if (editorWindow != null) {
			Gdx.app.error(TAG, "Window already open!");
			return false;
		}
		Lwjgl3WindowConfiguration editorWindowConfig = new Lwjgl3WindowConfiguration();
		editorWindowConfig.setWindowListener(new Lwjgl3WindowAdapter(){
			@Override public boolean windowIsClosing () {
				window.setMovable(true);
				editorWindow = null;
				if (listener != null) {
					listener.onClose();
				}
				return true;
			}
		});
		editorWindowConfig.setWindowPosition(x, y);
		editorWindowConfig.setWindowedMode(width, height);
		editorWindowConfig.setTitle("AIEditor");
		ApplicationListener editorWindowListener = new ApplicationAdapter() {
			Stage stage;

			@Override public void create () {
				stage = new Stage(new ScreenViewport());
				stage.addActor(window);
				window.setMovable(false);
				Gdx.input.setInputProcessor(stage);
			}

			@Override public void resize (int width, int height) {
				stage.getViewport().update(width, height);
			}

			@Override public void render () {
				Gdx.gl.glClearColor(0f, 0f, 0f, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				stage.act(Gdx.graphics.getDeltaTime());
				stage.draw();
			}

			@Override public void dispose () {
				window.remove();
				stage.dispose();
			}
		};
		Lwjgl3Application app = (Lwjgl3Application)Gdx.app;
		editorWindow = app.newWindow(editorWindowListener, editorWindowConfig);

		return true;
	}

	/**
	 * Close the window if it is visible
	 */
	public void close () {
		if (editorWindow != null) {
			editorWindow.closeWindow();
			editorWindow = null;
		}
	}

	/**
	 * Underlying {@link Lwjgl3Window}
	 * Use with caution!
	 * @return {@link Lwjgl3Window}
	 */
	public Lwjgl3Window getEditorWindow () {
		return editorWindow;
	}

	public interface WindowListener {
		/**
		 * Called when open window is being closed
		 */
		void onClose();
	}

	public static void main (String[] args) {
		System.out.println("Dummy main, move along! Do we even need this? probably not!");
	}
}
