package io.piotrjastrzebski.bte;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.branch.*;
import com.badlogic.gdx.ai.btree.decorator.*;
import com.badlogic.gdx.ai.btree.leaf.Failure;
import com.badlogic.gdx.ai.btree.leaf.Success;
import com.badlogic.gdx.ai.btree.leaf.Wait;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisWindow;
import io.piotrjastrzebski.bte.model.BehaviorTreeModel;
import io.piotrjastrzebski.bte.model.tasks.Guard;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;
import io.piotrjastrzebski.bte.view.BehaviorTreeView;

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
 * TODO add Subtree fake task, ala guard so we can split stuff
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
	private EditorWindowClosedListener closedListener;
	private boolean autoStep = true;
	private TaskInjector injector;

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
		view = new BehaviorTreeView(this);
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
		model.reset();
		tree = null;
	}

	/**
	 * Update the editor, call this each frame
	 */
	public void update (float delta) {
		if (model.isValid() && stepStrategy.shouldStep(tree, delta) && autoStep) {
			// TODO figure out a way to break stepping if there is an infinite loop in the tree
			// TODO or more practically, if we run some excessive amount of tasks
			model.step();
		}
	}

	/**
	 * Step the behavior tree immediately if model is valid
	 */
	public void forceStepBehaviorTree () {
		if (model.isValid()) {
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

	public void prepareWindow() {
		prepareWindow(true);
	}

	public void prepareWindow(boolean closeable) {
		if (window == null || isWindowCloseable != closeable) {
			if (window != null) {
				window.clear();
				window.remove();
			}
			window = new VisWindow("AIEditor") {
				@Override protected void setParent (Group parent) {
					super.setParent(parent);
					if (parent != null) {
						view.onShow();
					} else {
						view.onHide();
					}
				}
			};
			window.setResizable(true);
			window.add(getView()).fill().expand();
			isWindowCloseable = closeable;
			if (closeable) {
				window.addCloseButton();
				window.closeOnEscape();
			}
			// TODO need better check
			if (Gdx.graphics.getPpiX() > 100) {
				window.setSize(1600, 1200);
			} else {
				window.setSize(800, 600);
			}
			// TODO is this broken or what? you can't drag out the assets, but the hit boxes are outside
			window.setKeepWithinStage(true);
		}
		window.clearActions();
		window.centerWindow();
	}

	public Window getWindow () {
		if (window == null) {
			Gdx.app.error(TAG, "Using default window, consider calling prepareWindow() first");
			prepareWindow();
		}
		return window;
	}

	private boolean isWindowCloseable;
	private boolean fadingOut;

	public boolean isWindowVisible() {
		return window != null && window.getParent() != null && !fadingOut;
	}

	public void showWindow (Group group) {
		group.addActor(getWindow());
		window.clearActions();
		window.fadeIn();
		fadingOut = false;
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
		addTaskClass(tag, cls, true);
	}

	public void addTaskClass (String tag, Class<? extends Task> cls, boolean visible) {
		view.addSrcTask(tag, cls, visible);
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

	public BehaviorTreeModel getModel () {
		return model;
	}

	public void setAutoStepBehaviorTree (boolean autoStep) {
		this.autoStep = autoStep;
	}

	public void restartBehaviorTree () {
		tree.reset();
	}

	public interface BehaviorTreeStepStrategy {
		boolean shouldStep(BehaviorTree tree, float delta);
	}

	public interface EditorWindowClosedListener {
		boolean onClose ();
	}

	public void setClosedListener (EditorWindowClosedListener closedListener) {
		this.closedListener = closedListener;
	}

	public void setTaskInjector (TaskInjector injector) {
		TaskModel.injector = injector;
	}

	@Override public void dispose () {
		if (ownsSkin) {
			VisUI.dispose();
		}
	}
}
