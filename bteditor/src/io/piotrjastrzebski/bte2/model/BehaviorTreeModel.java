package io.piotrjastrzebski.bte2.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import io.piotrjastrzebski.bte2.model.edit.*;
import io.piotrjastrzebski.bte2.model.tasks.FakeRootModel;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class BehaviorTreeModel implements BehaviorTree.Listener {
	private static final String TAG = BehaviorTreeModel.class.getSimpleName();
	private BehaviorTree tree;
	private TaskLibrary taskLibrary;
	private FakeRootModel fakeRoot;
	private TaskModel root;
	private CommandManager commands;
	private boolean dirty;
	private boolean valid;

	public BehaviorTreeModel () {
		taskLibrary = new TaskLibrary();
		commands = new CommandManager();
		fakeRoot = new FakeRootModel();
	}

	@SuppressWarnings("unchecked")
	public void init (BehaviorTree tree) {
		reset();
		if (tree == null) throw new IllegalArgumentException("BehaviorTree cannot be null!");
		dirty = true;
		this.tree = tree;
		tree.addListener(this);
		root = TaskModel.wrap(tree.getChild(0), this);
		fakeRoot.init(root, this);
		fakeRoot.validate();
		// notify last so we are setup
		for (BTChangeListener listener : listeners) {
			listener.onInit(this);
		}
		taskLibrary.load(tree);
	}

	@SuppressWarnings("unchecked")
	public void reset () {
		commands.reset();
		if (tree != null) {
			tree.listeners.removeValue(this, true);
		}
		// notify first, so listeners have a chance to do stuff
		for (BTChangeListener listener : listeners) {
			listener.onReset(this);
		}
		// TODO Cleanup all the crap
		TaskModel.free(root);
	}

	public TaskModel getRoot () {
		return fakeRoot;
	}


	public boolean canAddBefore (TaskModel what, TaskModel target) {
		// check if can insert what before target
		TaskModel parent = target.getParent();
		if (parent == null) return false;
		if (!parent.canAdd(what)) return false;
		return true;
	}

	public void addBefore (TaskModel what, TaskModel target) {
		if (!canAddBefore(what, target)) return;
		TaskModel parent = target.getParent();
		int id = parent.getChildId(target);

		commands.execute(AddCommand.obtain(id, what, parent));
		tree.reset();
		dirty = true;
		notifyChanged();
	}

	public boolean canAddAfter (TaskModel what, TaskModel target) {
		// check if can insert what after target
		TaskModel parent = target.getParent();
		if (parent == null) return false;
		if (!parent.canAdd(what)) return false;
		return true;
	}

	public void addAfter (TaskModel what, TaskModel target) {
		if (!canAddAfter(what, target)) return;
		TaskModel parent = target.getParent();
		int id = parent.getChildId(target);
		commands.execute(AddCommand.obtain(id + 1, what, parent));
		tree.reset();
		dirty = true;
		notifyChanged();
	}

	/**
	 * Check if given task can be added to target task
	 */
	public boolean canAdd (TaskModel what, TaskModel target) {
		return target.canAdd(what);
	}

	/**
	 * Add task that is not in the tree
	 * It is not possible to add tasks to leaf tasks
	 */
	public void add (TaskModel what, TaskModel target) {
		if (!canAdd(what, target)) return;
		dirty = true;
		commands.execute(AddCommand.obtain(what, target));
		tree.reset();
		notifyChanged();
	}

	public boolean canMoveBefore (TaskModel what, TaskModel target) {
		// we can move stuff within same parent
		if (what.getParent() == target.getParent())
			return true;
		// if we cant add to target, we cant move
		if (!canAddBefore(what, target))
			return false;
		// cant add into itself or into own children
		return what == target || !what.hasChild(target);
	}

	/**
	 * Check if given task can be moved to target
	 * It is not possible to move task into its own children for example
	 */
	public boolean canMove (TaskModel what, TaskModel target) {
		if (!canAdd(what, target)) return false;
		return !what.hasChild(target);
	}

	public boolean canMoveAfter (TaskModel what, TaskModel target) {
		// we can move stuff within same parent
		if (what.getParent() == target.getParent())
			return true;
		// if we cant add to target, we cant move
		if (!canAddAfter(what, target))
			return false;
		// cant add into itself or into own children
		return what == target || !what.hasChild(target);
	}

	public void moveBefore (TaskModel what, TaskModel target) {
		if (!canMoveBefore(what, target)) return;
		TaskModel parent = target.getParent();
		int id = parent.getChildId(target);
		commands.execute(MoveCommand.obtain(id, what, parent));
		dirty = true;
		notifyChanged();
	}

	/**
	 * Move task that is in the tree to another position
	 */
	public void move (TaskModel what, TaskModel target) {
		if (!canMove(what, target)) return;
		commands.execute(MoveCommand.obtain(what, target));
		tree.reset();
		dirty = true;
		notifyChanged();
	}

	public void moveAfter (TaskModel what, TaskModel target) {
		if (!canMoveAfter(what, target)) return;
		TaskModel parent = target.getParent();
		int id = parent.getChildId(target);
		commands.execute(MoveCommand.obtain(id + 1, what, parent));
		dirty = true;
		notifyChanged();
	}

	/**
	 * Remove task from the tree
	 */
	public void remove (TaskModel what) {
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

	private ObjectIntMap<TaskModel> modelTasks = new ObjectIntMap<>();
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
				for (ObjectIntMap.Entry<TaskModel> entry : modelTasks.entries()) {
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

	private void doubleCheck (ObjectIntMap<TaskModel> modelTasks, ObjectIntMap<Task> tasks, TaskModel task) {
		modelTasks.put(task, modelTasks.get(task, 0) + 1);
		Task wrapped = task.getWrapped();
		if (wrapped != null) {
			tasks.put(wrapped, tasks.get(wrapped, 0) + 1);
		} else {
			Gdx.app.error(TAG, "Wrapped task of " + task + " is null!");
		}
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

	public BehaviorTree getTree () {
		return tree;
	}

	@Override public void statusUpdated (Task task, Task.Status previousStatus) {
		if (task instanceof BehaviorTree) {
			root.wrappedUpdated(previousStatus, task.getStatus());
			return;
		}
		TaskModel taskModel = root.getModelTask(task);
		if (taskModel == null) {
			Gdx.app.error(TAG, "Mddel task for " + task + " not found, wtf?");
			return;
		}
		taskModel.wrappedUpdated(previousStatus, task.getStatus());
	}

	@Override public void childAdded (Task task, int index) {

	}

	public void setValid (boolean invalid) {
		this.valid = invalid;
	}

	public interface BTChangeListener {
		/**
		 * Called when model was reset
		 */
		void onReset (BehaviorTreeModel model);

		/**
		 * called when model was initialized with new behavior tree
		 */
		void onInit (BehaviorTreeModel model);

		/**
		 * Called when this listener was added to the moved
		 */
		void onListenerAdded (BehaviorTreeModel model);

		/**
		 * Called when this listener was removed from the moved
		 */
		void onListenerRemoved (BehaviorTreeModel model);

		/**
		 * called when model was initialized with new behavior tree
		 */
		void onChange (BehaviorTreeModel model);

	}
}
