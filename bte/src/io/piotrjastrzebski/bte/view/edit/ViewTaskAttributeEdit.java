package io.piotrjastrzebski.bte.view.edit;

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import io.piotrjastrzebski.bte.TaskComment;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;
import io.piotrjastrzebski.bte.model.tasks.fields.EditableFields.EditableField;

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
		taskComment = new VisLabel("", "small");
		taskComment.setColor(Color.LIGHT_GRAY);
		row();
	}

	public void startEdit (TaskModel task) {
		stopEdit();
		name.setText(task.getName());
		String comment = task.getComment();
		if (comment != null) {
			taskComment.setText(comment);
			add(taskComment).row();
		}

		if (task.isReadOnly()) {
			add(new VisLabel("Task is read only")).row();
		} else {
			addTaskFields(task);
		}
	}

	private void addTaskFields (TaskModel task) {
		for (EditableField field : task.getEditableFields()) {
			VisTable cont = new VisTable();
			cont.add(new VisLabel(field.getName())).row();
			String comment = field.getComment();
			if (comment != null) {
				VisLabel tc = new VisLabel(comment, "small");
				tc.setColor(Color.LIGHT_GRAY);
				cont.add(tc).row();
			}
			if (task.getType() == TaskModel.Type.INCLUDE && field.getName().equals("subtree")) {
				cont.add(AttrFieldEdit.createPathEditField(field));
			} else {
				// TODO need to handle area edit for comments
				cont.add(AttrFieldEdit.createEditField(field));
			}
			add(cont).row();
		}
	}

	public void stopEdit () {
		clear();
		add(top).row();
		add(name).row();
		name.setText("<?>");
	}
}
