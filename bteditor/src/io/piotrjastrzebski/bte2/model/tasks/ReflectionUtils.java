package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskConstraint;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;

/**
 * Created by PiotrJ on 10/02/16.
 */
public class ReflectionUtils {
	private final static String TAG = ReflectionUtils.class.getSimpleName();

	private ReflectionUtils () {}

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
}
