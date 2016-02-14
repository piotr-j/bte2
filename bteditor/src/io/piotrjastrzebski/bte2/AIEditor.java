package io.piotrjastrzebski.bte2;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.branch.*;
import com.badlogic.gdx.ai.btree.decorator.*;
import com.badlogic.gdx.ai.btree.leaf.Failure;
import com.badlogic.gdx.ai.btree.leaf.Success;
import com.badlogic.gdx.ai.btree.leaf.Wait;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisWindow;

import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;
import io.piotrjastrzebski.bte2.model.TaskLibrary;
import io.piotrjastrzebski.bte2.model.tasks.Guard;
import io.piotrjastrzebski.bte2.view.BehaviorTreeView;

/**
 * Main editor class
 *
 * NOTE
 * we must support undo/redo
 * to do that, we will clone all tasks all the time, at least at the beginning
 * when we add something, we store a clone of task with its children in the add node and add another clone to the tree
 * same for remove/move actions
 * we also could store a some sort of history of data changes in each task
 *
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class AIEditor implements Disposable {
	private static final String TAG = AIEditor.class.getSimpleName();

	private boolean ownsSkin;
	/* Current tree we are editing */
	private BehaviorTree tree;
	private BehaviorTreeModel model;
	private BehaviorTreeView view;
	private BehaviorTreeStepStrategy stepStrategy;
	private BehaviorTreeStepStrategy simpleStepStrategy;
	private VisWindow window;
	/**
	 * Create AIEditor with internal VisUI skin
	 * AIEditor must be disposed in this case
	 */
	public AIEditor () {
		this(null);
	}

	/**
	 * Create AIEditor with external VisUI skin
	 * @param skin Skin to use
	 */
	public AIEditor (Skin skin) {
		if (skin == null) {
			ownsSkin = true;
			skin = new Skin(VisUI.SkinScale.X1.getSkinFile());
			try {
				VisUI.load(skin);
			} catch (GdxRuntimeException e) {
				Gdx.app.error(TAG, "VisUI already loaded?", e);
				skin.dispose();
			}
		}
		model = new BehaviorTreeModel();
		view = new BehaviorTreeView(model);
		setUpdateStrategy(null);
	}

	public BehaviorTreeView getView () {
		return view;
	}

	public void initialize (BehaviorTree tree) {
		initialize(tree, false);
	}

	/**
	 * Initialize the editor with new tree
	 * @param tree tree to use
	 * @param copy if we should work on a copy
	 */
	public void initialize (final BehaviorTree tree, boolean copy) {
		reset();
		Gdx.app.log(TAG, "Editing tree " + tree);
		if (copy) {
			this.tree = (BehaviorTree)tree.cloneTask();
		} else {
			this.tree = tree;
		}
		model.init(this.tree);
	}

	public void reset() {
		if (tree == null) return;
		// TODO prompt for save or whatever
		Gdx.app.log(TAG, "Resetting " + tree);
		tree = null;
		model.reset();
	}

	/**
	 * Update the editor, call this each frame
	 */
	public void update (float delta) {
		if (model.isValid() && stepStrategy.shouldStep(tree, delta)) {
			// TODO figure out a way to break stepping if there is an infinite loop in the tree
			// TODO or more practically, if we run some excessive amount of tasks
			tree.step();
		}
	}

	/**
	 * BTUpdateStrategy for the BehaviorTree that will be called if it is in valid state
	 * Pass in null to use default stepStrategy, step() each update
	 * @param strategy stepStrategy to use for BehaviorTree updates
	 */
	public void setUpdateStrategy (BehaviorTreeStepStrategy strategy) {
		if (strategy == null) {
			if (simpleStepStrategy == null) {
				simpleStepStrategy = new BehaviorTreeStepStrategy() {
					@Override public boolean shouldStep (BehaviorTree tree, float delta) {
						return true;
					}
				};
			}
			this.stepStrategy = simpleStepStrategy;
		} else {
			this.stepStrategy = strategy;
		}
	}

	// TODO add api for fullscreen window in new os window, no close btn
	public Window getWindow () {
		if (window == null) {
			window = new VisWindow("AIEditor");
			window.setResizable(true);
			window.add(getView()).fill().expand();
			window.addCloseButton();
			// TODO need better check
			if (Gdx.graphics.getPpiX() > 100) {
				window.setSize(1600, 1200);
			} else {
				window.setSize(800, 600);
			}
			window.fadeIn();
			// TODO is this broken or what? you can't drag out the assets, but the hit boxes are outside
			window.setKeepWithinStage(true);
		}
		fadingOut = false;
		window.clearActions();
		window.centerWindow();
		if (!isWindowVisible())
			window.fadeIn();
		return window;
	}
	private boolean fadingOut;
	public boolean isWindowVisible() {
		return window != null && window.getParent() != null && !fadingOut;
	}

	public void showWindow (Group group) {
		group.addActor(getWindow());
	}

	public void hideWindow() {
		window.clearActions();
		window.fadeOut();
		fadingOut = true;
	}

	public void toggleWindow (Group group) {
		if (isWindowVisible()) {
			hideWindow();
		} else {
			showWindow(group);
		}
	}

	public void addTaskClass (String tag, Class<? extends Task> cls) {
		TaskLibrary library = model.getTaskLibrary();
		library.add(cls);
		view.addSrcTask(tag, cls);
	}

	public void addDefaultTaskClasses () {
		addTaskClass("branch", Sequence.class);
		addTaskClass("branch", Selector.class);
		addTaskClass("branch", Parallel.class);
		addTaskClass("branch", DynamicGuardSelector.class);
		addTaskClass("branch", RandomSelector.class);
		addTaskClass("branch", RandomSequence.class);
		addTaskClass("branch", Guard.class);

		addTaskClass("decorator", AlwaysFail.class);
		addTaskClass("decorator", AlwaysSucceed.class);
		addTaskClass("decorator", Include.class);
		addTaskClass("decorator", Invert.class);
		addTaskClass("decorator", Random.class);
		addTaskClass("decorator", Repeat.class);
		addTaskClass("decorator", SemaphoreGuard.class);
		addTaskClass("decorator", UntilFail.class);
		addTaskClass("decorator", UntilSuccess.class);
		addTaskClass("decorator", Wait.class);

		addTaskClass("leaf", Success.class);
		addTaskClass("leaf", Failure.class);
	}

	public interface BehaviorTreeStepStrategy {
		boolean shouldStep(BehaviorTree tree, float delta);
	}

	@Override public void dispose () {
		if (ownsSkin) {
			VisUI.dispose();
		}
	}
}
