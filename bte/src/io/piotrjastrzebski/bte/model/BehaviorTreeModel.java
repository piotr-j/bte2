package io.piotrjastrzebski.bte.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibrary;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import io.piotrjastrzebski.bte.BehaviorTreeWriter;
import io.piotrjastrzebski.bte.EditorBehaviourTreeLibrary;
import io.piotrjastrzebski.bte.model.edit.AddCommand;
import io.piotrjastrzebski.bte.model.edit.CommandManager;
import io.piotrjastrzebski.bte.model.edit.MoveCommand;
import io.piotrjastrzebski.bte.model.edit.RemoveCommand;
import io.piotrjastrzebski.bte.model.tasks.FakeRootModel;
import io.piotrjastrzebski.bte.model.tasks.ReflectionUtils;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class BehaviorTreeModel implements BehaviorTree.Listener {
	private static final String TAG = BehaviorTreeModel.class.getSimpleName();
	private BehaviorTree tree;
	private FakeRootModel fakeRoot;
	private TaskModel root;
	private CommandManager commands;
	private boolean dirty;
	private boolean valid;
	private boolean initialized;

	public BehaviorTreeModel () {
		commands = new CommandManager();
		fakeRoot = new FakeRootModel();
	}

	@SuppressWarnings("unchecked")
	public void init (BehaviorTree tree) {
		reset();
		if (tree == null) throw new IllegalArgumentException("BehaviorTree cannot be null!");
		initialized = true;
		dirty = false;
		this.tree = tree;
		tree.addListener(this);
		root = TaskModel.wrap(tree.getChild(0), this);
		fakeRoot.init(root, this);
		valid = root.isValid();
		// notify last so we are setup
		for (ModelChangeListener listener : listeners) {
			listener.onInit(this);
		}
	}

	@SuppressWarnings("unchecked")
	public void reset () {
		initialized = false;
		commands.reset();
		if (tree != null) {
			tree.listeners.removeValue(this, true);
		}
		// notify first, so listeners have a chance to do stuff
		for (ModelChangeListener listener : listeners) {
			listener.onReset(this);
		}
		TaskModel.free(fakeRoot);
		tree = null;
		root = null;
		dirty = false;
		valid = false;
	}

	public TaskModel getRoot () {
		if (!initialized) return null;
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

	private Array<ModelChangeListener> listeners = new Array<>();
	public void addChangeListener (ModelChangeListener listener) {
		if (!listeners.contains(listener, true)) {
			listeners.add(listener);
		}
	}

	public void removeChangeListener (ModelChangeListener listener) {
		listeners.removeValue(listener, true);
	}

	private ObjectIntMap<TaskModel> modelTasks = new ObjectIntMap<>();
	private ObjectIntMap<Task> tasks = new ObjectIntMap<>();
	public boolean isValid () {
		if (initialized && isDirty()) {
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

	public void saveTree (FileHandle fh) {
		String serialize = BehaviorTreeWriter.serialize(tree);
		fh.writeString(serialize, false);
	}

	public void loadTree (FileHandle fh) {
		try {
			// TODO we probably want an option to load/save a tree without current one set
			BehaviorTreeLibrary library = BehaviorTreeLibraryManager.getInstance().getLibrary();
			BehaviorTree loadedTree = library.createBehaviorTree(fh.path());
	//				model.btLoaded(loadedTree);
			BehaviorTree old = tree;
			reset();
			// we do this so whatever is holding original tree is updated
			// TODO maybe a callback instead of this garbage
	//				if (old != null) {
			ReflectionUtils.replaceRoot(old, loadedTree);
			init(old);
	//				} else {
	//					model.init(loadedTree);
	//				}
			if (library instanceof EditorBehaviourTreeLibrary) {
				// TODO this is super garbage
				((EditorBehaviourTreeLibrary)library).updateComments(this);
			}
			TaskModel.inject(old);
			for (ModelChangeListener listener : listeners) {
				listener.onLoad(loadedTree, fh, this);
			}
		} catch (Exception ex) {
			for (ModelChangeListener listener : listeners) {
				listener.onLoadError(ex, fh, this);
			}
		}
	}

	public void step () {
		if (initialized && isValid()) {
			try {
				tree.step();
			} catch (IllegalStateException ex) {
				valid = false;
				for (ModelChangeListener listener : listeners) {
					listener.onStepError(ex, this);
				}
				notifyChanged();
			}
		}
	}

	public boolean isInitialized () {
		return initialized;
	}

	public interface ModelChangeListener {
		/**
		 * Called when model was reset
		 */
		void onReset (BehaviorTreeModel model);

		/**
		 * called when model was initialized with new behavior tree
		 */
		void onInit (BehaviorTreeModel model);
		/**
		 * called when model was initialized with new behavior tree
		 */
		void onChange (BehaviorTreeModel model);

		/**
		 * called when model loaded a tree from file
		 */
		void onLoad (BehaviorTree tree, FileHandle file, BehaviorTreeModel model);

		/**
		 * called when model loaded a tree from file
		 */
		void onLoadError (Exception ex, FileHandle file, BehaviorTreeModel model);

		/**
		 * called when model saved a tree from file
		 */
		void onSave (BehaviorTree tree, FileHandle file, BehaviorTreeModel model);

		void onStepError (Exception ex, BehaviorTreeModel model);

	}
}
