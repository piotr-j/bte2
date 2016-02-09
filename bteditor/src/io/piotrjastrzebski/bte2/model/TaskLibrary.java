package io.piotrjastrzebski.bte2.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.decorator.Include;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

/**
 * Maps Task class to archetype, used by model to get new instances of tasks to add them to the tree
 *
 * @param <E> type of the blackboard object in the task, same as in the model
 */
@SuppressWarnings("rawtypes")
public class TaskLibrary {
	private final static String TAG = TaskLibrary.class.getSimpleName();

	private ObjectMap<Class<? extends Task>, Task> classToInstance;
	private Injector injector;

	protected TaskLibrary () {
		classToInstance = new ObjectMap<>();
	}

	/**
	 * @param aClass type of {@link Task} to add, will be instantiated via reflection
	 */
	public void add (Class<? extends Task> aClass) {
		if (aClass == null)
			throw new IllegalArgumentException("Task class  cannot be null!");
		if (classToInstance.containsKey(aClass)) return;
		try {
			Task task = ClassReflection.newInstance(aClass);
			if (injector != null) injector.inject(task);
			classToInstance.put(aClass, task);
		} catch (ReflectionException e) {
			Gdx.app.error(TAG, "Failed to create task of type " + aClass, e);
		}
	}

	/**
	 * @param task instance of {@link Task} to add
	 */
	public void add (Task task) {
		if (task == null)
			throw new IllegalArgumentException("Task cannot be null!");
		classToInstance.put((Class<? extends Task>)task.getClass(), task);
	}

	/**
	 * @param aClass type of {@link Task}
	 * @return cloned task, via {@link Task#cloneTask()} or null
	 */
	public Task get (Class<? extends Task> aClass) {
		if (aClass == null)
			throw new IllegalArgumentException("Task class cannot be null!");
		Task arch = classToInstance.get(aClass, null);
		if (aClass == Include.class) {
			// lazy by default, so bt doesn't explode
			Include include = new Include<>("", true);
			if (injector != null) injector.inject(include);
			return include;
		}
		if (arch != null)
			return arch.cloneTask();
		return null;
	}

	/**
	 * @param aClass type of {@link Task}
	 * @return actual instance of the {@link Task}
	 */
	public Task getArchetype (Class<? extends Task> aClass) {
		if (aClass == null)
			throw new IllegalArgumentException("Task class cannot be null!");
		return classToInstance.get(aClass, null);
	}

	/**
	 * @param aClass type of {@link Task}
	 * @return if this type has been added
	 */
	public boolean has (Class<? extends Task> aClass) {
		if (aClass == null)
			throw new IllegalArgumentException("Task class cannot be null!");
		return classToInstance.containsKey(aClass);
	}

	/**
	 * @param aClass type of {@link Task} to remove
	 * @return removed {@link Task} or null
	 */
	public Task remove (Class<? extends Task> aClass) {
		if (aClass == null)
			throw new IllegalArgumentException("Task class cannot be null!");
		return classToInstance.remove(aClass);
	}

	/**
	 * Remove all added {@link Task}s
	 */
	public void clear () {
		classToInstance.clear();
	}

	public void load (BehaviorTree bt) {
		addFromTask(bt.getChild(0));
	}

	private void addFromTask (Task task) {
		add((Class<? extends Task>)task.getClass());
		for (int i = 0; i < task.getChildCount(); i++) {
			addFromTask(task.getChild(i));
		}
	}

	public void setInjector(Injector injector) {
		this.injector = injector;
	}

	public interface Injector {
		public void inject(Task task);
	}
}
