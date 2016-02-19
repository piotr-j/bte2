package io.piotrjastrzebski.bte.model.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.*;
import com.badlogic.gdx.ai.btree.annotation.TaskConstraint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

/**
 * Created by PiotrJ on 10/02/16.
 */
public class ReflectionUtils {
	private final static String TAG = ReflectionUtils.class.getSimpleName();

	private ReflectionUtils () {}

	private static ObjectIntMap<Class> minChildrenCache = new ObjectIntMap<>();
	private static ObjectIntMap<Class> maxChildrenCache = new ObjectIntMap<>();
	public static int getMinChildren(Task task) {
		return getMinChildren(task.getClass());
	}

	public static int getMinChildren(Class<? extends Task> cls) {
		// Constraint can only have >= 0 value
		int min = minChildrenCache.get(cls, -1);
		if (min < 0) {
			findConstraints(cls);
			// if its still -1, we failed
			min = minChildrenCache.get(cls, -1);
		}
		return min;
	}

	public static int getMaxChildren(Task task) {
		return getMaxChildren(task.getClass());
	}

	public static int getMaxChildren(Class<? extends Task> cls) {
		// Constraint can only have >= 0 value
		int max = maxChildrenCache.get(cls, -1);
		if (max < 0) {
			findConstraints(cls);
			// if its still -1, we failed
			max = maxChildrenCache.get(cls, -1);
		}
		return max;
	}

	private static void findConstraints (Class<?> cls) {
		Annotation annotation = null;
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

	public static boolean insert(Task what, int at, Task into) {
		try {
			// we need to check it task is in target before we add, as that will happen on init
			if (into instanceof BranchTask) {
				Field field = ClassReflection.getDeclaredField(BranchTask.class, "children");
				field.setAccessible(true);
				@SuppressWarnings("unchecked") Array<Task> children = (Array<Task>)field.get(into);
				// disallow if out of bounds,  allow to insert if empty
				if (at > children.size && at > 0) {
					Gdx.app.error("INSERT", "cannot insert " + what + " to " + into + " at " + at + " as its out of range");
					return false;
				}
				if (!children.contains(what, true)) {
					children.insert(at, what);
					// note in this class there are some more children that we need to deal with
					if (into instanceof SingleRunningChildBranch) {
						// set the field to null so it is recreated with correct size
						Field randomChildren = ClassReflection.getDeclaredField(SingleRunningChildBranch.class, "randomChildren");
						randomChildren.setAccessible(true);
						randomChildren.set(into, null);
					}
				} else {
					Gdx.app.error("INSERT",
						"cannot insert " + what + " to " + into + " at " + at + ", target already contains task");
					return false;
				}
				return true;
			} else if (into instanceof Decorator) {
				// can insert if decorator is empty
				Field field = ClassReflection.getDeclaredField(Decorator.class, "child");
				field.setAccessible(true);
				Object old = field.get(into);
				// ignore at, just replace
				if (old == null || old != what) {
					field.set(into, what);
					return true;
				} else {
					Gdx.app.error("INSERT", "cannot insert " + what + " to " + into + " as its a decorator");
				}
			} else {
				Gdx.app.error("INSERT", "cannot insert " + what + " to " + into + " as its a leaf");
			}
		} catch (ReflectionException e) {
			Gdx.app.error("REMOVE", "ReflectionException error", e);
		}
		return false;
	}

	public static boolean remove(Task what, Task from) {
		try {
			// we need to check it task is in target before we add, as that will happen on init
			if (from instanceof BranchTask) {
				Field field = ClassReflection.getDeclaredField(BranchTask.class, "children");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				Array<Task> children = (Array<Task>)field.get(from);
				if (children.removeValue(what, true)) {
					// note in this class there are some more children that we need to deal with
					if (from instanceof SingleRunningChildBranch) {
						// set the field to null so it is recreated with correct size
						Field randomChildren = ClassReflection.getDeclaredField(SingleRunningChildBranch.class, "randomChildren");
						randomChildren.setAccessible(true);
						randomChildren.set(from, null);
					}
				}
				return false;
			} else if (from instanceof Decorator) {
				Field field = ClassReflection.getDeclaredField(Decorator.class, "child");
				field.setAccessible(true);
				Object old = field.get(from);
				if (old == what || old == null) {
					field.set(from, null);
				} else {
					return false;
				}
				return old != null;
			} else {
				Gdx.app.error("REMOVE", "cannot remove " + what + " from " + from + " as its a leaf");
			}
		} catch (ReflectionException e) {
			Gdx.app.error("REMOVE", "ReflectionException error", e);
		}
		return false;
	}

	/**
	 * Replace root of tree with root of with
	 * @param tree to replace root of
	 * @param with donor tree
	 * @return if replacement was successful
	 */
	public static boolean replaceRoot (BehaviorTree tree, BehaviorTree with) {
		tree.reset();
		with.reset();
		try {
			Field field = ClassReflection.getDeclaredField(BehaviorTree.class, "rootTask");
			field.setAccessible(true);
			field.set(tree, field.get(with));
			return true;
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
		return false;
	}
}
