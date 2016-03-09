package io.piotrjastrzebski.bte.model.tasks.fields;

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.piotrjastrzebski.bte.TaskComment;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;

/**
 * Wraps a field and allows for easy setting and getting values
 *
 * Created by EvilEntity on 15/02/2016.
 */
public class EditableFields {

	public interface EditableField {
		/**
		 * @return values of this field for assigned instance
		 */
		Object get ();

		/**
		 * @param object value of the field, must be of correct type
		 */
		void set (Object object);

		/**
		 * @return owner instance of this field
		 */
		Object getOwner ();

		/**
		 * @return name of the field
		 */
		String getName ();

		/**
		 * @return comment fpr this field or null
		 */
		String getComment ();

		/**
		 * @return type of this field
		 */
		Class getType ();

		/**
		 * @return if this field is annotated as required
		 */
		boolean isRequired ();

		/**
		 * @return if the field name should be skipped
		 */
		boolean skipName ();

		void free ();
	}

	/**
	 * Get all editable fields for this TaskModel wrapped task + userComment
	 */
	public static Array<EditableField> get (TaskModel modelTask, Array<EditableField> out) {
		out.add(CommentEditableField.obtain(modelTask));
		get(modelTask.getWrapped(), out);
		return out;
	}

	private static Array<EditableField> get (Task task, Array<EditableField> out) {
		Class<?> aClass = task.getClass();
		Field[] fields = ClassReflection.getFields(aClass);
		for (Field f : fields) {
			Annotation a = f.getDeclaredAnnotation(TaskAttribute.class);
			if (a == null)
				continue;
			TaskAttribute annotation = a.getAnnotation(TaskAttribute.class);
			a = f.getDeclaredAnnotation(TaskComment.class);
			TaskComment tc = null;
			if (a != null) {
				tc = a.getAnnotation(TaskComment.class);
			}
			addField(task, annotation, f, tc, out);
		}
		return out;
	}

	public static void release (Array<EditableField> fields) {
		for (EditableField field : fields) {
			field.free();
		}
		fields.clear();
	}

	private static void addField (Task task, TaskAttribute ann, Field field, TaskComment tc, Array<EditableField> out) {
		String name = ann.name();
		if (name == null || name.length() == 0) {
			name = field.getName();
		}
		if (tc != null) {
			String comment = tc.value().trim();
			boolean skipName = tc.skipFieldName();
			out.add(BaseEditableField.obtain(name, task, field, ann.required(), comment, skipName));
		} else {
			out.add(BaseEditableField.obtain(name, task, field, ann.required(), null, false));
		}
	}

	private static class BaseEditableField implements EditableField, Pool.Poolable {
		private static Pool<BaseEditableField> pool = new Pool<BaseEditableField>() {
			@Override protected BaseEditableField newObject () {
				return new BaseEditableField();
			}
		};

		public static EditableField obtain (String name, Task task, Field field, boolean required, String comment, boolean skipName) {
			return pool.obtain().init(name, task, field, required, comment, skipName);
		}

		private String name;
		private Task task;
		private Field field;
		private boolean required;
		private String comment;
		private boolean skipName;

		private EditableField init (String name, Task task, Field field, boolean required, String comment, boolean skipName) {
			this.name = name;
			this.task = task;
			this.field = field;
			this.required = required;
			this.comment = comment;
			this.skipName = skipName;
			return this;
		}

		@Override public Object get () {
			try {
				return field.get(task);
			} catch (ReflectionException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override public void set (Object value) {
			if (required && value == null)
				throw new AssertionError("Field " + name + " in " + task.getClass().getSimpleName() + " is required!");
			// TOOD proper check, this fails for float.class Float.class etc
//			if (value != null && !field.getType().isAssignableFrom(value.getClass()))
//				throw new AssertionError("Invalid value type for field " + name + ", got " + value.getClass() + ", expected " + field.getType());
			try {
				field.set(task, value);
			} catch (ReflectionException e) {
				e.printStackTrace();
			}
		}

		@Override public String getName () {
			return name;
		}

		@Override public String getComment () {
			return comment;
		}

		@Override public Object getOwner () {
			return task;
		}

		@Override public Class getType () {
			return field.getType();
		}

		@Override public boolean isRequired () {
			return required;
		}

		@Override public boolean skipName () {
			return skipName;
		}

		@Override public void free () {
			pool.free(this);
		}

		@Override public void reset () {
			name = null;
			task = null;
			field = null;
		}
	}

	private static class CommentEditableField implements EditableField, Pool.Poolable {
		private static Pool<CommentEditableField> pool = new Pool<CommentEditableField>() {
			@Override protected CommentEditableField newObject () {
				return new CommentEditableField();
			}
		};

		public static EditableField obtain (TaskModel task) {
			return pool.obtain().init(task);
		}

		private TaskModel owner;

		private EditableField init (TaskModel task) {
			owner = task;
			return this;
		}

		@Override public Object get () {
			return owner.getUserComment();
		}

		@Override public void set (Object value) {
			if (value.getClass() != String.class)
				throw new AssertionError("Invalid value type for field "+getName()+", got " + value.getClass() + ", expected String.class");
			owner.setUserComment((String)value);
		}

		@Override public String getName () {
			return "# Comment";
		}

		@Override public String getComment () {
			return null;
		}

		@Override public Object getOwner () {
			return owner;
		}

		@Override public Class getType () {
			return String.class;
		}

		@Override public boolean isRequired () {
			return false;
		}

		@Override public boolean skipName () {
			return false;
		}

		@Override public void free () {
			pool.free(this);
		}

		@Override public void reset () {
			owner = null;
		}
	}
}
