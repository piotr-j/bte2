package io.piotrjastrzebski.bte2.view.edit;

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.ai.btree.decorator.Include;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import io.piotrjastrzebski.bte2.TaskComment;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;

/**
 * View for attribute editing on tasks
 *
 * Created by PiotrJ on 21/10/15.
 */
public class ViewTaskAttributeEdit extends VisTable {
	private VisLabel top;
	private VisLabel name;
	private VisLabel taskComment;

	public ViewTaskAttributeEdit () {
		super();
		add(top = new VisLabel("Edit task")).row();
		add(name = new VisLabel("<?>"));
		taskComment = new VisLabel();
		taskComment.setWrap(true);
		row();
	}

	public void startEdit (TaskModel task) {
		stopEdit();
		name.setText(task.getName());
		if (task.isReadOnly()) {
			add(new VisLabel("Task is read only")).row();
		} else {
			addTaskAttributes(task.getWrapped());
			addComment(task);
		}
	}

	private void addTaskAttributes (Task task) {
		if (task instanceof TaskComment) {
			String comment = ((TaskComment)task).getComment();
			if (comment != null && comment.length() > 0) {
				taskComment.setText(comment);
				add(taskComment).expandX().fillX().pad(0, 5, 0, 5).row();
			}
		}
		Class<?> aClass = task.getClass();
		Field[] fields = ClassReflection.getFields(aClass);
		int added = 0;
		for (Field f : fields) {
			Annotation a = f.getDeclaredAnnotation(TaskAttribute.class);
			if (a == null)
				continue;
			TaskAttribute annotation = a.getAnnotation(TaskAttribute.class);
			addField(task, annotation, f);
			added++;
		}
		if (added == 0) {
			add(new VisLabel("No TaskAttributes")).row();
		}
	}

	private void addField (Task task, TaskAttribute ann, Field field) {
		// prefer name from annotation if there is one
		String name = ann.name();
		if (name == null || name.length() == 0) {
			name = field.getName();
		}
		VisTable cont = new VisTable();
		cont.add(new VisLabel(name)).row();
		// include is magic, need magic handling
		if (task instanceof Include && name.equals("subtree")) {
			try {
				cont.add(AttrFieldEdit.createPathEditField(task, field, ann.required()));
			} catch (ReflectionException e) {
				e.printStackTrace();
				cont.add(new VisLabel("<Failed>"));
			}
		} else {
			try {
				cont.add(AttrFieldEdit.createEditField(task, field, ann.required()));
			} catch (ReflectionException e) {
				e.printStackTrace();
				cont.add(new VisLabel("<Failed>"));
			}
		}
		add(cont).row();
	}

	private void addComment (TaskModel task) {
		Table cont = new Table();
		cont.add(new VisLabel("# Comment")).row();
		// include is magic, need magic handling
		try {
			Field comment = ClassReflection.getDeclaredField(TaskModel.class, "comment");
			comment.setAccessible(true);
			cont.add(AttrFieldEdit.stringAreaEditField(task, comment));
		} catch (ReflectionException e) {
			e.printStackTrace();
			cont.add(new VisLabel("<Failed>"));
		}
		add(cont).expandY().fillY().row();
	}

	public void stopEdit () {
		clear();
		add(top).row();
		add(name).row();
		name.setText("<?>");
	}
}
