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
public class TaskLibrary<E> {
	private final static String TAG = TaskLibrary.class.getSimpleName();

	private ObjectMap<Class<? extends Task>, Task<E>> classToInstance;
	private Injector<E> injector;

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
			@SuppressWarnings("unchecked")
			Task<E> task = ClassReflection.newInstance(aClass);
			if (injector != null) injector.inject(task);
			classToInstance.put(aClass, task);
		} catch (ReflectionException e) {
			Gdx.app.error(TAG, "Failed to create task of type " + aClass, e);
		}
	}

	/**
	 * @param task instance of {@link Task} to add
	 */
	public void add (Task<E> task) {
		if (task == null)
			throw new IllegalArgumentException("Task cannot be null!");
		classToInstance.put(task.getClass(), task);
	}

	/**
	 * @param aClass type of {@link Task}
	 * @return cloned task, via {@link Task#cloneTask()} or null
	 */
	public Task<E> get (Class<? extends Task> aClass) {
		if (aClass == null)
			throw new IllegalArgumentException("Task class cannot be null!");
		Task<E> arch = classToInstance.get(aClass, null);
		if (aClass == Include.class) {
			// lazy by default, so bt doesnt explode
			Include<E> include = new Include<>("", true);
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
	public Task<E> getArchetype (Class<? extends Task> aClass) {
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
	public Task<E> remove (Class<? extends Task> aClass) {
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

	public void load (BehaviorTree<E> bt) {
		addFromTask(bt.getChild(0));
	}

	private void addFromTask (Task<E> task) {
		add(task.getClass());
		for (int i = 0; i < task.getChildCount(); i++) {
			addFromTask(task.getChild(i));
		}
	}

	public void setInjector(Injector<E> injector) {
		this.injector = injector;
	}

	public interface Injector<E> {
		public void inject(Task<E> task);
	}
}
