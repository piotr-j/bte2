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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.piotrjastrzebski.bte2.model.BTModel;
import io.piotrjastrzebski.bte2.view.BTView;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public abstract class ModelTask<E> implements Pool.Poolable {
	private static final String TAG = ModelTask.class.getSimpleName();

	public enum Type {INCLUDE, LEAF, BRANCH, DECORATOR, ROOT, NULL;}

	public static ModelTask wrap (Task task, BTModel model) {
		if (task instanceof Include) {
			return ModelInclude.obtain((Include)task, model);
		} else if (task instanceof LeafTask) {
			return ModelLeaf.obtain((LeafTask)task, model);
		} else if (task instanceof BranchTask) {
			return ModelBranch.obtain((BranchTask)task, model);
		} else if (task instanceof Decorator) {
			return ModelDecorator.obtain((Decorator)task, model);
		} else {
			Gdx.app.error(TAG, "Invalid task class! " + task);
		}
		return ModelNull.INSTANCE;
	}

	public static ModelTask wrap (Class<? extends Task> cls, BTModel model) {
		// TODO how do we want to make an instance of this?
		try {
			Task task = ClassReflection.newInstance(cls);
			if (cls == Repeat.class) {
				Repeat repeat = (Repeat)task;
				repeat.times = ConstantIntegerDistribution.ONE;
			}
			return wrap(task, model);
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
		return ModelNull.INSTANCE;
	}

	public static void free (ModelTask task) {
		if (task != null) {
			task.free();
		}
	}

	protected final Type type;
	protected ModelTask (Type type) {
		this.type = type;
	}

	protected ModelTask parent;
	protected Task wrapped;
	// NOTE there aren't that many children per task, 4 is a decent start
	protected Array<ModelTask> children = new Array<>(4);
	protected boolean init;
	protected boolean valid;
	protected boolean dirty;
	protected boolean readOnly;
	protected int minChildren;
	protected int maxChildren;
	protected BTModel model;
	protected Array<TaskCommand> pending = new Array<>();

	public void init (Task task, BTModel model) {
		this.model = model;
		init = true;
		wrapped = task;
		minChildren = getMinChildren(task);
		maxChildren = getMaxChildren(task);
		dirty = true;
		for (int i = 0; i < task.getChildCount(); i++) {
			ModelTask child = wrap(task.getChild(i), model);
			child.setParent(this);
			children.add(child);
		}
		pending.clear();
	}

	public boolean canAdd (ModelTask task) {
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
		for (ModelTask child : children) {
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

	public ModelTask getChild (int id) {
		return children.get(id);
	}

	/**
	 * Check if given task is in this task
	 */
	public boolean hasChild (ModelTask task) {
		if (this == task) return true;
		for (ModelTask child : children) {
			if (child == task || child.hasChild(task)) {
				return true;
			}
		}
		return false;
	}

	public void addChild (ModelTask task) {
		// can we do this? or do we need some specific code in here
		insertChild(children.size, task);
	}

	public void insertChild (int at, ModelTask task) {
		// if at is larger then size, we will insert as last
		at = Math.min(at, children.size);
		children.insert(at, task);
		task.setParent(this);
		dirty = true;
		if (model.isValid() && isValid()) {
			TaskCommand.insert(wrapped, task.wrapped, at).execute();
		} else {
			pending.add(TaskCommand.insert(wrapped, task.wrapped, at));
		}
	}

	public void removeChild (ModelTask task) {
		children.removeValue(task, true);
		dirty = true;
		if (model.isValid() && isValid()) {
			TaskCommand.remove(wrapped, task.wrapped).execute();
		} else {
			pending.add(TaskCommand.remove(wrapped, task.wrapped));
		}
	}

	public int getChildId (ModelTask what) {
		return children.indexOf(what, true);
	}

	public Type getType () {
		return type;
	}

	public void setParent (ModelTask parent) {
		this.parent = parent;
	}

	public ModelTask getParent () {
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
		for (ModelTask child : children) {
			free(child);
		}
		children.clear();
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

	public abstract ModelTask copy();

	public ModelTask getModelTask (Task<E> task) {
		if (wrapped == task) return this;
		for (ModelTask child : children) {
			ModelTask found = child.getModelTask(task);
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
