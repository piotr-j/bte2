package io.piotrjastrzebski.bte2.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import io.piotrjastrzebski.bte2.model.edit.*;
import io.piotrjastrzebski.bte2.model.tasks.ModelFakeRoot;
import io.piotrjastrzebski.bte2.model.tasks.ModelTask;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class BTModel<E> implements BehaviorTree.Listener<E> {
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
		tree.addListener(this);
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
		if (tree != null) {
			tree.removeListener(this);
		}
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
		ModelTask parent = target.getParent();
		if (parent == null) return false;
		if (!parent.canAdd(what)) return false;
		return true;
	}

	public void addBefore (ModelTask what, ModelTask target) {
		if (!canAddBefore(what, target)) return;
		ModelTask parent = target.getParent();
		int id = parent.getChildId(target);

		commands.execute(AddCommand.obtain(id, what, parent));
		tree.reset();
		notifyChanged();
	}

	public boolean canAddAfter (ModelTask what, ModelTask target) {
		// check if can insert what after target
		ModelTask parent = target.getParent();
		if (parent == null) return false;
		if (!parent.canAdd(what)) return false;
		return true;
	}

	public void addAfter (ModelTask what, ModelTask target) {
		if (!canAddAfter(what, target)) return;
		ModelTask parent = target.getParent();
		int id = parent.getChildId(target);
		commands.execute(AddCommand.obtain(id + 1, what, parent));
		tree.reset();
		notifyChanged();
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
		commands.execute(AddCommand.obtain(what, target));
		tree.reset();
		notifyChanged();
	}

	public boolean canMoveBefore (ModelTask what, ModelTask target) {
		if (!canAddBefore(what, target)) return false;
		ModelTask parent = what.getParent();
		if (parent == null) return false;
		// since we dont actually add to add, this is fine
		return what == target || !what.hasChild(target);
	}

	/**
	 * Check if given task can be moved to target
	 * It is not possible to move task into its own children for example
	 */
	public boolean canMove (ModelTask what, ModelTask target) {
		if (!canAdd(what, target)) return false;
		return !what.hasChild(target);
	}

	public boolean canMoveAfter (ModelTask what, ModelTask target) {
		if (!canAddAfter(what, target))
			return false;
		ModelTask parent = what.getParent();
		if (parent == null)
			return false;
		// since we dont actually add to add, this is fine
		return what == target || !what.hasChild(target);
	}

	public void moveBefore (ModelTask what, ModelTask target) {
		if (!canMoveBefore(what, target)) return;
		ModelTask parent = target.getParent();
		int id = parent.getChildId(target);
		commands.execute(MoveCommand.obtain(id, what, parent));
		dirty = true;
		notifyChanged();
	}

	/**
	 * Move task that is in the tree to another position
	 */
	public void move (ModelTask what, ModelTask target) {
		if (!canMove(what, target)) return;
		commands.execute(MoveCommand.obtain(what, target));
		tree.reset();
		dirty = true;
		notifyChanged();
	}

	public void moveAfter (ModelTask what, ModelTask target) {
		if (!canMoveAfter(what, target)) return;
		ModelTask parent = target.getParent();
		int id = parent.getChildId(target);
		commands.execute(MoveCommand.obtain(id + 1, what, parent));
		dirty = true;
		notifyChanged();
	}

	/**
	 * Remove task from the tree
	 */
	public void remove (ModelTask what) {
		dirty = true;
		commands.execute(RemoveCommand.obtain(what));
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

	private ObjectIntMap<ModelTask> modelTasks = new ObjectIntMap<>();
	private ObjectIntMap<Task> tasks = new ObjectIntMap<>();
	public boolean isValid () {
		if (isDirty()) {
			boolean newValid = root != null && root.isValid();
			dirty = false;
			if (newValid != valid && newValid) {
				Gdx.app.log(TAG, "Reset tree");
				tree.reset();
			}
			valid = newValid;
			if (valid) {
				// TODO remove this probably
				modelTasks.clear();
				tasks.clear();
				Gdx.app.log(TAG, "doubleCheck");
				doubleCheck(modelTasks, tasks, root);
				for (ObjectIntMap.Entry<ModelTask> entry : modelTasks.entries()) {
					if (entry.value > 1) {
						Gdx.app.error(TAG, "Duped model task " + entry.key);
					}
				}

				for (ObjectIntMap.Entry<Task> entry : tasks) {
					if (entry.value > 1) {
						Gdx.app.error(TAG, "Duped task " + entry.key);
					}
				}
			}
		}
		return valid;
	}

	private void doubleCheck (ObjectIntMap<ModelTask> modelTasks, ObjectIntMap<Task> tasks, ModelTask<E> task) {
		modelTasks.put(task, modelTasks.get(task, 0) + 1);
		tasks.put(task.getWrapped(), tasks.get(task.getWrapped(), 0) + 1);
		for (int i = 0; i < task.getChildCount(); i++) {
			doubleCheck(modelTasks, tasks, task.getChild(i));
		}
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

	@Override public void statusUpdated (Task<E> task, Task.Status previousStatus) {
		if (task instanceof BehaviorTree) {
			root.wrappedUpdated(previousStatus, task.getStatus());
			return;
		}
		ModelTask modelTask = root.getModelTask(task);
		if (modelTask == null) {
			Gdx.app.error(TAG, "Mddel task for " + task + " not found, wtf?");
			return;
		}
		modelTask.wrappedUpdated(previousStatus, task.getStatus());
	}

	@Override public void childAdded (Task<E> task, int index) {

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
