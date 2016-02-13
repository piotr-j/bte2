package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;

/**
 * Model for fake GuardTask
 *
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class GuardModel extends TaskModel implements Pool.Poolable {
	private final static Pool<GuardModel> pool = new Pool<GuardModel>() {
		@Override protected GuardModel newObject () {
			return new GuardModel();
		}
	};

	public static GuardModel obtain (TaskModel guard, TaskModel guarded, BehaviorTreeModel model) {
		return pool.obtain().init(guard, guarded, model);
	}

	public static void free (GuardModel leaf) {
		pool.free(leaf);
	}

	private GuardModel () {
		super(Type.GUARD);
	}

	// NOTE this shadows guard from ModelTask, but this fake guard cant have a real guard, so we should be fine maybe
	private TaskModel guard;
	private TaskModel guarded;

	public GuardModel init (TaskModel guard, TaskModel guarded, BehaviorTreeModel model) {
		this.model = model;
		minChildren = ReflectionUtils.getMinChildren(Guard.class);
		maxChildren = ReflectionUtils.getMaxChildren(Guard.class);
		children.clear();
		if ((guard == null && guarded != null) || (guard != null && guarded == null)) {
			throw new AssertionError("Guard and guarded must be either both null or not null");
		}
		if (guard != null) {
			// NOTE we can probably assume that if we have those, we are initializing from working tree
			this.guard = guard;
			children.add(guard);
			guard.setParent(this);
			guard.setIsGuard(guarded);
			this.guarded = guarded;
			children.add(guarded);
			guarded.setParent(this);
		}
		return this;
	}

	protected GuardModel init (GuardModel other) {
		// TODO this doesnt work at all
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
	}

	@Override public void setGuard (TaskModel newGuard) {
		insertChild(0, newGuard);
	}

	public void setGuarded (TaskModel newGuarded) {
		insertChild(1, newGuarded);
	}

	/**
	 * Due to how guards work, we need to do some garbage in here
	 */
	@Override public void insertChild (int at, TaskModel task) {
		task.setParent(this);
		if (parent == null) {
			throw new AssertionError("GuardModel requires parent before children can be added to it");
		}
		int idInParent = parent.children.indexOf(this, true);
		// TODO make sure this works when we are a child of root
		// at == 0 -> insert as guard if possible
		if (at == 0) {
			if (children.size == 0) {
				// set task as child of this models parent at this models position
				children.add(task);
				guarded = task;
				guarded.insertInto(parent, idInParent);
				guarded.setParent(this);
			} else if (children.size == 1) {
				// we already have a guarded task, set new task as guard and guard the task at pos 1
				children.insert(0, task);
				guard = task;
				guard.setParent(this);
				guarded.setGuard(guard);
//				guarded.insertInto(parent, idInParent);
			} else {
				throw new AssertionError("Invalid children count");
			}
		} else if (at == 1) {
			// add guarded task, mark task at 0 as guard and set it as guard of new task
			children.add(task);
			// note we can only be at 1, if we have a child
			guarded.removeFrom(parent);
			guard = guarded;
			guarded = task;
			guarded.setGuard(guard);
			guarded.insertInto(parent, idInParent);
			guarded.setParent(this);
		} else {
			throw new AssertionError("Invalid task at " + at);
		}
	}

	protected void insertInto (TaskModel parent, int at) {
		if (guarded != null){
			ReflectionUtils.insert(guarded.wrapped, at, parent.wrapped);
		}
	}

	@Override public void removeChild (TaskModel task) {
		int at = children.indexOf(task, true);
		int size = children.size;
		children.removeValue(task, true);
		int idInParent = parent.children.indexOf(this, true);
		if (at == 0) {
			if (size == 2) {
				// remove guard from guarded
				// remove guard
				task.setIsNotGuard();
				guarded.removeGuard();
				guard = null;
			} else if (size == 1){
				task.removeFrom(parent);
				task.setParent(null);
				guarded = null;
			} else {
				throw new AssertionError("Invalid children count");
			}
		} else if (at == 1){
			// guarded removed
			guarded.removeGuard();
			guarded.removeFrom(parent);
			guard.setIsNotGuard();
			guarded = guard;
			guarded.insertInto(parent, idInParent);
			guard = null;
		} else {
			throw new AssertionError("Invalid task at " + at);
		}
		task.setParent(null);
	}

	@Override protected void removeFrom (TaskModel parent) {
		if (guarded != null) {
			ReflectionUtils.remove(guarded.wrapped, parent.wrapped);
		}
	}

	@Override public GuardModel copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String getName () {
		return "Guard";
	}

	@Override public String toString () {
		return "GuardModel{"+wrapped+"}";
	}

}
