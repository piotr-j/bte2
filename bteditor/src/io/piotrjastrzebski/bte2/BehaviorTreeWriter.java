package io.piotrjastrzebski.bte2;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.ai.btree.decorator.Include;
import com.badlogic.gdx.ai.btree.utils.DistributionAdapters;
import com.badlogic.gdx.ai.utils.random.Distribution;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;

import java.util.Comparator;

/**
 * Utility class for serialization of {@link BehaviorTree}s in a format readable by {@link com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser}
 *
 * TODO implement guards, inline single task guards, stick complex guards into subtrees, add name to GuardModel?
 *
 * Created by PiotrJ on 21/10/15.
 */
public class BehaviorTreeWriter {
	private final static String TAG = BehaviorTreeWriter.class.getSimpleName();

	/**
	 * Save the tree in parsable format
	 * @param tree behavior tree to save
	 * @param path external file path to save to, can't be a folder
	 */
	public static void save (BehaviorTree tree, String path) {
		FileHandle savePath = Gdx.files.external(path);
		if (savePath.isDirectory()) {
			Gdx.app.error("BehaviorTreeSaver", "save path cannot be a directory!");
			return;
		}
		savePath.writeString(serialize(tree), false);
	}

	/**
	 * Serialize the tree to parser readable format
	 * @param tree BehaviorTree to serialize
	 * @return serialized tree
	 */
	public static String serialize(BehaviorTree tree) {
		return serialize(tree.getChild(0));
	}

	/**
	 * Serialize the tree to parser readable format
	 * @param task task to serialize
	 * @return serialized tree
	 */
	public static String serialize(Task task) {
		Array<Class<? extends Task>> classes = new Array<>();
		findClasses(task, classes);
		ObjectMap<Task, GuardHolder> taskToGuard = new ObjectMap<>();
 		findGuards(task, taskToGuard, 0);
		Gdx.app.log(TAG, "Found guards: " + taskToGuard.toString());

		classes.sort(new Comparator<Class<? extends Task>>() {
			@Override public int compare (Class<? extends Task> o1, Class<? extends Task> o2) {
				return o1.getSimpleName().compareTo(o2.getSimpleName());
			}
		});

		StringBuilder sb = new StringBuilder("# Alias definitions\n");

		for (Class<? extends Task> aClass : classes) {
			sb.append("import ").append(getAlias(aClass)).append(":\"").append(aClass.getCanonicalName()).append("\"\n");
		}

		writeGuards(sb, taskToGuard);

		sb.append("\nroot\n");
		writeTask(sb, task, 1, taskToGuard);
		return sb.toString();
	}


	private static void writeGuards (StringBuilder sb, ObjectMap<Task, GuardHolder> taskToGuard) {
		Array<GuardHolder> sorted = new Array<>();
		ObjectMap.Values<GuardHolder> values = taskToGuard.values();
		for (GuardHolder value : values) {
			sorted.add(value);
		}
		// TODO does the order matter id guard has a guard?
		sorted.sort();

		for (GuardHolder guard : sorted) {
			sb.append("\nsubtree name:\"");
			sb.append(guard.name);
			sb.append("\"\n");
			writeTask(sb, guard.guard, 1, taskToGuard);
		}
	}

	private static int findGuards (Task task, ObjectMap<Task, GuardHolder> guards, int count) {
		Task guard = task.getGuard();
		int initialCount = count;
		if (guard != null) {
			guards.put(task, new GuardHolder("guard"+count, guard, task));
			count += 1;
			count += findGuards(guard, guards, count);
		}
		for (int i = 0; i < task.getChildCount(); i++) {
			count += findGuards(task.getChild(i), guards, count);
		}
		return count - initialCount;
	}

	private static void writeTask (StringBuilder sb, Task task, int depth, ObjectMap<Task, GuardHolder> taskToGuard) {
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
		GuardHolder guard = taskToGuard.get(task);
		if (guard != null){
			sb.append("($");
			sb.append(guard.name);
			sb.append(") ");
		}
		sb.append(getAlias(task.getClass()));
		getTaskAttributes(sb, task);
		sb.append("\n");
		// include may have a whole tree as child, ignore it
		if (task instanceof Include) return;
		for (int i = 0; i < task.getChildCount(); i++) {
			writeTask(sb, task.getChild(i), depth + 1, taskToGuard);
		}
	}

	/**
	 * Serialize the tree to parser readable format
	 * @param task task to serialize
	 * @return serialized tree
	 */
	public static String serialize(TaskModel task) {
		Array<Class<? extends Task>> classes = new Array<>();
		findClasses(task.getWrapped(), classes);
		classes.sort(new Comparator<Class<? extends Task>>() {
			@Override public int compare (Class<? extends Task> o1, Class<? extends Task> o2) {
				return o1.getSimpleName().compareTo(o2.getSimpleName());
			}
		});

		StringBuilder sb = new StringBuilder("# Alias definitions\n");

		for (Class<? extends Task> aClass : classes) {
			sb.append("import ").append(getAlias(aClass)).append(":\"").append(aClass.getCanonicalName()).append("\"\n");
		}

		sb.append("\nroot\n");
		writeTask(sb, task, 1);
		return sb.toString();
	}

	private static void writeTask (StringBuilder sb, TaskModel modelTask, int depth) {
		if (modelTask.hasComment()) {
			for (int i = 0; i < depth; i++) {
				sb.append("  ");
			}
			sb.append("# ");
			sb.append(modelTask.getComment());
			sb.append('\n');
		}
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
		Task task = modelTask.getWrapped();
		sb.append(getAlias(task.getClass()));
		getTaskAttributes(sb, task);
		sb.append('\n');
		// include may have a whole tree as child, ignore it
		if (task instanceof Include) return;
		for (int i = 0; i < modelTask.getChildCount(); i++) {
			writeTask(sb, modelTask.getChild(i), depth + 1);
		}
	}

	private static void getTaskAttributes (StringBuilder sb, Task task) {
		Class<?> aClass = task.getClass();
		Field[] fields = ClassReflection.getFields(aClass);
		for (Field f : fields) {
			Annotation a = f.getDeclaredAnnotation(TaskAttribute.class);
			if (a == null)
				continue;
			TaskAttribute annotation = a.getAnnotation(TaskAttribute.class);
			sb.append(" ");
			getFieldString(sb, task, annotation, f);
		}
	}

	private static void getFieldString (StringBuilder sb, Task task, TaskAttribute ann, Field field) {
		// prefer name from annotation if there is one
		String name = ann.name();
		if (name == null || name.length() == 0) {
			name = field.getName();
		}
		sb.append(name);
		Object o;
		try {
			field.setAccessible(true);
			o = field.get(task);
		} catch (ReflectionException e) {
			Gdx.app.error("", "Failed to get field", e);
			return;
		}
		if (field.getType().isEnum() || field.getType() == String.class) {
			sb.append(":\"").append(o).append("\"");
		} else if (Distribution.class.isAssignableFrom(field.getType())) {
			sb.append(":\"").append(toParsableString((Distribution)o)).append("\"");
		} else {
			sb.append(":").append(o);
		}
	}

	private static DistributionAdapters adapters;
	/**
	 * Attempts to create a parsable string for given distribution
	 * @param distribution distribution to create parsable string for
	 * @return string that can be parsed by distribution classes
	 */
	public static String toParsableString (Distribution distribution) {
		if (distribution == null)
			throw new IllegalArgumentException("Distribution cannot be null");
		if (adapters == null)
			adapters = new DistributionAdapters();
		return adapters.toString(distribution);
	}

	private static void findClasses (Task task, Array<Class<? extends Task>> classes) {
		Class<? extends Task> aClass = task.getClass();
		String cName = aClass.getCanonicalName();
		// ignore task classes from gdx-ai, as they are already accessible by the parser
		if (!cName.startsWith("com.badlogic.gdx.ai.btree.") && !classes.contains(aClass, true)) {
			classes.add(aClass);
		}
		for (int i = 0; i < task.getChildCount(); i++) {
			findClasses(task.getChild(i), classes);
		}
	}

	private static ObjectMap<Class<? extends Task>, String> taskToAlias = new ObjectMap<>();

	/**
	 * Get alias for given {@link Task} generated from its class name
	 * @param aClass class of task
	 * @return valid alias for the class
	 */
	public static String getAlias (Class<? extends Task> aClass) {
		if (aClass == null) throw new IllegalArgumentException("Class cannot be null");
		String alias = taskToAlias.get(aClass, null);
		if (alias == null) {
			String name = aClass.getSimpleName();
			alias = Character.toLowerCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
			taskToAlias.put(aClass, alias);
		}
		return alias;
	}

	/**
	 * Override default alias of a {@link Task} generated by {@link BehaviorTreeWriter#getAlias(Class)}
	 * Passing in null alias will revert it to the default
	 * @param aClass class of task
	 * @param alias alias that will be used in saved tree
	 */
	public static void setAlias (Class<? extends Task> aClass, String alias) {
		if (aClass == null) throw new IllegalArgumentException("Class cannot be null");
		taskToAlias.put(aClass, alias);
	}

	private static class GuardHolder implements Comparable<GuardHolder> {
		public final String name;
		public final Task guard;
		public final Task guarded;

		public GuardHolder (String name, Task guard, Task guarded) {
			this.name = name;
			this.guard = guard;
			this.guarded = guarded;
		}

		@Override public String toString () {
			return "GuardHolder{" +
				"name='" + name + '\'' +
				", guard=" + guard.getClass().getSimpleName() +
				", guarded=" + guarded.getClass().getSimpleName() +
				'}';
		}

		@Override public int compareTo (GuardHolder o) {
			return name.compareTo(o.name);
		}
	}
}
