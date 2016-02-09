package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BranchTask;
import com.badlogic.gdx.ai.btree.Decorator;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskConstraint;
import com.badlogic.gdx.ai.btree.decorator.Include;
import com.badlogic.gdx.ai.btree.decorator.Repeat;
import com.badlogic.gdx.ai.utils.random.ConstantIntegerDistribution;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public abstract class TaskModel implements Pool.Poolable {
	private static final String TAG = TaskModel.class.getSimpleName();

	public enum Type {INCLUDE, LEAF, BRANCH, DECORATOR, ROOT, NULL;}

	public static TaskModel wrap (Task task, BehaviorTreeModel model) {
		if (task instanceof Include) {
			return IncludeModel.obtain((Include)task, model);
		} else if (task instanceof LeafTask) {
			return LeafModel.obtain((LeafTask)task, model);
		} else if (task instanceof BranchTask) {
			return BranchModel.obtain((BranchTask)task, model);
		} else if (task instanceof Decorator) {
			return DecoratorModel.obtain((Decorator)task, model);
		} else {
			Gdx.app.error(TAG, "Invalid task class! " + task);
		}
		return NullModel.INSTANCE;
	}

	public static TaskModel wrap (Class<? extends Task> cls, BehaviorTreeModel model) {
		// TODO how do we want to make an instance of this?
		try {
			Task task = ClassReflection.newInstance(cls);
			if (cls == Repeat.class) {
				Repeat<?> repeat = (Repeat<?>)task;
				repeat.times = ConstantIntegerDistribution.ONE;
			}
			return wrap(task, model);
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
		return NullModel.INSTANCE;
	}

	public static void free (TaskModel task) {
		if (task != null) {
			task.free();
		}
	}

	protected final Type type;
	protected TaskModel (Type type) {
		this.type = type;
	}

	protected TaskModel parent;
	protected TaskModel guard;
	protected Task wrapped;
	// NOTE there aren't that many children per task, 4 is a decent start
	protected Array<TaskModel> children = new Array<>(4);
	protected boolean init;
	protected boolean valid;
	protected boolean dirty;
	protected boolean readOnly;
	protected boolean isGuard;
	protected TaskModel guardedTask;
	protected int minChildren;
	protected int maxChildren;
	protected BehaviorTreeModel model;
	protected Array<TaskCommand> pending = new Array<>();

	public void init (Task task, BehaviorTreeModel model) {
		this.model = model;
		init = true;
		wrapped = task;
		minChildren = getMinChildren(task);
		maxChildren = getMaxChildren(task);
		dirty = true;
		for (int i = 0; i < task.getChildCount(); i++) {
			TaskModel child = wrap(task.getChild(i), model);
			child.setParent(this);
			children.add(child);
		}
		Task guard = wrapped.getGuard();
		if (guard != null) {
			this.guard = wrap(guard, model);
			this.guard.setIsGuard(true, this);
		}
		pending.clear();
	}

	public void setIsGuard (boolean isGuard, TaskModel guarded) {
		this.isGuard = isGuard;
		this.guardedTask = guarded;
		for (TaskModel child : children) {
			child.setIsGuard(isGuard, guarded);
		}
	}

	public boolean canAdd (TaskModel task) {
		// TODO special handling for some things maybe
		return !readOnly && children.size < maxChildren;
	}

	public boolean isValid () {
		if (dirty) validate();
		return valid;
	}

	public void validate () {
		if (!dirty) return;
		valid = !(children.size < minChildren || children.size > maxChildren);
		if (guard != null) {
			guard.validate();
			valid &= guard.isValid();
		}
		for (TaskModel child : children) {
			child.validate();
			valid &= child.isValid();
		}
		if (valid) {
			for (TaskCommand command : pending) {
				command.execute();
			}
			// TODO pool etc
			pending.clear();
		}
	}

	public int getChildCount () {
		return children.size;
	}

	public void setReadOnly (boolean readOnly) {
		this.readOnly = readOnly;
		for (int i = 0; i < children.size; i++) {
			children.get(i).setReadOnly(readOnly);
		}
	}

	public TaskModel getChild (int id) {
		return children.get(id);
	}

	/**
	 * Check if given task is in this task
	 */
	public boolean hasChild (TaskModel task) {
		if (this == task) return true;
		for (TaskModel child : children) {
			if (child == task || child.hasChild(task)) {
				return true;
			}
		}
		return false;
	}

	public void addChild (TaskModel task) {
		// can we do this? or do we need some specific code in here
		insertChild(children.size, task);
	}

	public void insertChild (int at, TaskModel task) {
		// if at is larger then size, we will insert as last
		at = Math.min(at, children.size);
		dirty = true;
		if (model.isValid() && isValid()) {
			TaskCommand.insert(this, task, at).execute();
		} else {
			pending.add(TaskCommand.insert(this, task, at));
		}
	}

	public void removeChild (TaskModel task) {
		dirty = true;
		if (model.isValid() && isValid()) {
			TaskCommand.remove(this, task).execute();
			children.removeValue(task, true);
		} else {
			pending.add(TaskCommand.remove(this, task));
		}
	}

	public int getChildId (TaskModel what) {
		return children.indexOf(what, true);
	}

	public Type getType () {
		return type;
	}

	@SuppressWarnings("unchecked")
	public void setGuard(TaskModel newGuard) {
		removeGuard();
		guard = newGuard;
		wrapped.setGuard(newGuard.wrapped);
		newGuard.setIsGuard(true, this);
		dirty = true;
	}

	@SuppressWarnings("unchecked")
	public void removeGuard () {
		guard = null;
		wrapped.setGuard(null);
		dirty = true;
	}

	public TaskModel getGuarded () {
		return guardedTask;
	}

	public boolean isGuard () {
		return isGuard;
	}

	public TaskModel getGuard () {
		return guard;
	}

	public boolean isGuarded () {
		return guard != null;
	}

	public void setParent (TaskModel parent) {
		this.parent = parent;
	}

	public TaskModel getParent () {
		return parent;
	}

	protected String name;
	public String getName () {
		if (name == null) {
			name = wrapped != null? wrapped.getClass().getSimpleName():"<!null!>";
		}
		return name;
	}

	public Task getWrapped () {
		return wrapped;
	}

	@Override public String toString () {
		return "ModelTask{" +
			"type=" + type +
			", parent=" + parent +
			", valid=" + valid +
			", name='" + name + '\'' +
			'}';
	}

	@Override public void reset () {
		for (TaskModel child : children) {
			free(child);
		}
		children.clear();
		if (guard != null) {
			free(guard);
		}
		guard = null;
		guardedTask = null;
		isGuard = false;
		wrapped = null;
		parent = null;
		init = false;
		name = null;
		readOnly = false;
		listeners.clear();
	}

	public boolean isReadOnly () {
		return readOnly;
	}

	public abstract void free();

	public abstract TaskModel copy();

	public TaskModel getModelTask (Task task) {
		if (wrapped == task) return this;
		// TODO use a map for this garbage?
		if (guard != null) {
			TaskModel found = guard.getModelTask(task);
			if (found != null) return found;
		}
		for (TaskModel child : children) {
			TaskModel found = child.getModelTask(task);
			if (found != null) return found;
		}
		return null;
	}

	// TODO move this garbage to dedicated reflection cache class
	private static ObjectIntMap<Class> minChildrenCache = new ObjectIntMap<>();
	private static ObjectIntMap<Class> maxChildrenCache = new ObjectIntMap<>();
	public static int getMinChildren(Task task) {
		Class<?> cls = task.getClass();
		// Constraint can only have >= 0 value
		int min = minChildrenCache.get(cls, -1);
		if (min < 0) {
			findConstraints(task);
			// if its still -1, we failed
			min = minChildrenCache.get(cls, -1);
		}
		return min;
	}

	public static int getMaxChildren(Task task) {
		Class<?> cls = task.getClass();
		// Constraint can only have >= 0 value
		int max = maxChildrenCache.get(cls, -1);
		if (max < 0) {
			findConstraints(task);
			// if its still -1, we failed
			max = maxChildrenCache.get(cls, -1);
		}
		return max;
	}

	private static void findConstraints (Task task) {
		Annotation annotation = null;
		Class<?> cls = task.getClass();
		Class<?> tCls = cls;
		// walk the class hierarchy till we get the annotation
		while (annotation == null && cls != Object.class) {
			annotation = ClassReflection.getDeclaredAnnotation(cls, TaskConstraint.class);
			if (annotation == null) {
				cls = cls.getSuperclass();
			}
		}
		if (annotation == null) {
			Gdx.app.error(TAG, "TaskConstraint annotation not found on class " + tCls);
			return;
		}
		TaskConstraint constraint = annotation.getAnnotation(TaskConstraint.class);
		minChildrenCache.put(tCls, constraint.minChildren());
		maxChildrenCache.put(tCls, constraint.maxChildren());
	}

	public static void clearReflectionCache () {
		minChildrenCache.clear();
		maxChildrenCache.clear();
	}

	private Array<ChangeListener> listeners = new Array<>(2);
	public void addListener (ChangeListener listener) {
		if (!listeners.contains(listener, true)) {
			listeners.add(listener);
		}
	}

	public void removeListener (ChangeListener listener) {
		listeners.removeValue(listener, true);
	}

	public void wrappedUpdated (Task.Status from, Task.Status to) {
		for (ChangeListener listener : listeners) {
			listener.statusChanged(from, to);
		}
	}

	public interface ChangeListener {
		void statusChanged(Task.Status from, Task.Status to);
	}
}
