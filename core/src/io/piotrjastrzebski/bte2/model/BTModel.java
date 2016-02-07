package io.piotrjastrzebski.bte2.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.utils.Array;
import io.piotrjastrzebski.bte2.model.edit.AddCommand;
import io.piotrjastrzebski.bte2.model.edit.Command;
import io.piotrjastrzebski.bte2.model.edit.CommandManager;
import io.piotrjastrzebski.bte2.model.edit.RemoveCommand;
import io.piotrjastrzebski.bte2.model.tasks.ModelFakeRoot;
import io.piotrjastrzebski.bte2.model.tasks.ModelTask;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class BTModel<E> {
	private static final String TAG = BTModel.class.getSimpleName();
	private BehaviorTree<E> tree;
	private TaskLibrary<E> taskLibrary;
	private ModelFakeRoot<E> fakeRoot;
	private ModelTask<E> root;
	private CommandManager commands;
	private boolean dirty;
	private boolean valid;

	public BTModel () {
		taskLibrary = new TaskLibrary<>();
		commands = new CommandManager();
		fakeRoot = new ModelFakeRoot<>();
	}

	public void init (BehaviorTree<E> tree) {
		reset();
		if (tree == null) throw new IllegalArgumentException("BehaviorTree cannot be null!");
		dirty = true;
		this.tree = tree;
		// TODO fix the generic garbage sometime
		root = ModelTask.wrap(tree.getChild(0), this);
		fakeRoot.init(root, this);
		fakeRoot.validate();
		// notify last so we are setup
		for (BTChangeListener listener : listeners) {
			listener.onInit(this);
		}
		taskLibrary.load(tree);
	}

	public void reset () {
		commands.reset();
		// notify first, so listeners have a chance to do stuff
		for (BTChangeListener listener : listeners) {
			listener.onReset(this);
		}
		// TODO Cleanup all the crap
		ModelTask.free(root);
	}

	public ModelTask getRoot () {
		return fakeRoot;
	}


	public boolean canAddBefore (ModelTask what, ModelTask target) {
		// check if can insert what before target
		return false;
	}

	public void addBefore (ModelTask what, ModelTask target) {
		if (!canAddBefore(what, target)) return;

	}

	public boolean canAddAfter (ModelTask what, ModelTask target) {
		// check if can insert what after target
		return false;
	}

	public void addAfter (ModelTask what, ModelTask target) {
		if (!canAddAfter(what, target)) return;

	}

	/**
	 * Check if given task can be added to target task
	 */
	public boolean canAdd (ModelTask what, ModelTask target) {
		return target.canAdd(what);
	}

	/**
	 * Add task that is not in the tree
	 * It is not possible to add tasks to leaf tasks
	 */
	public void add (ModelTask what, ModelTask target) {
		if (!canAdd(what, target)) return;
		dirty = true;
		// TODO pool those
		Command command = AddCommand.obtain(what, target);
		commands.execute(command);
		tree.reset();
		notifyChanged();
	}

	/**
	 * Check if given task can be moved to target
	 * It is not possible to move task into its own children for example
	 */
	public boolean canMove (ModelTask what, ModelTask target) {
		return target.hasChild(what);
	}

	/**
	 * Move task that is in the tree to another position
	 */
	public void move (ModelTask what, ModelTask target) {
		if (!canMove(what, target)) return;
		dirty = true;
		notifyChanged();
	}

	/**
	 * Remove task from the tree
	 */
	public void remove (ModelTask what) {
		dirty = true;
		Command command = RemoveCommand.obtain(what);
		commands.execute(command);
		tree.reset();
		notifyChanged();
	}

	public void undo () {
		dirty = true;
		if (commands.undo()) {
			tree.reset();
			notifyChanged();
		}
	}

	public void redo() {
		dirty = true;
		if (commands.redo()) {
			tree.reset();
			notifyChanged();
		}
	}

	public void notifyChanged () {
		for (int i = 0; i < listeners.size; i++) {
			listeners.get(i).onChange(this);
		}
	}

	private Array<BTChangeListener> listeners = new Array<>();
	public void addChangeListener (BTChangeListener listener) {
		if (!listeners.contains(listener, true)) {
			listeners.add(listener);
			listener.onListenerAdded(this);
		}
	}

	public void removeChangeListener (BTChangeListener listener) {
		listeners.removeValue(listener, true);
		listener.onListenerRemoved(this);
	}

	public boolean isValid () {
		if (isDirty()) {
			boolean newValid = root != null && root.isValid();
			dirty = false;
			if (newValid != valid && newValid) {
				Gdx.app.log(TAG, "Reset tree");
				tree.reset();
			}
			valid = newValid;
		}
		return valid;
	}

	public TaskLibrary getTaskLibrary () {
		return taskLibrary;
	}

	public boolean isDirty () {
		return dirty;
	}

	public void validate () {
		valid = root != null && root.isValid();
	}

	public BehaviorTree<E> getTree () {
		return tree;
	}

	public interface BTChangeListener {
		/**
		 * Called when model was reset
		 */
		void onReset (BTModel model);

		/**
		 * called when model was initialized with new behavior tree
		 */
		void onInit (BTModel model);

		/**
		 * Called when this listener was added to the moved
		 */
		void onListenerAdded (BTModel model);

		/**
		 * Called when this listener was removed from the moved
		 */
		void onListenerRemoved (BTModel model);

		/**
		 * called when model was initialized with new behavior tree
		 */
		void onChange (BTModel model);

	}
}
