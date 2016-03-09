package io.piotrjastrzebski.bte;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibrary;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser;
import com.badlogic.gdx.ai.btree.utils.DistributionAdapters;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.utils.ObjectMap;
import io.piotrjastrzebski.bte.model.BehaviorTreeModel;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;

/**
 * A {@code BehaviorTreeLibrary} is a repository of behavior tree archetypes. Behavior tree archetypes never run. Indeed, they are
 * only cloned to create behavior tree instances that can run. Has extra functionality useful for AIEditor
 *
 * Created by PiotrJ on 16/02/16.
 */
public class EditorBehaviourTreeLibrary extends BehaviorTreeLibrary {
	public EditorBehaviourTreeLibrary (int parseDebugLevel) {
		this(GdxAI.getFileSystem().newResolver(Files.FileType.Internal), parseDebugLevel);
	}

	protected EditorBehaviourTreeReader<?> reader;
	protected ObjectMap<Task, String> taskToComment = new ObjectMap<>();
	public EditorBehaviourTreeLibrary (FileHandleResolver resolver, int parseDebugLevel) {
		super(resolver, parseDebugLevel);
		reader = new EditorBehaviourTreeReader<>();
		parser = new BehaviorTreeParser<>(new DistributionAdapters(), parseDebugLevel, reader);
	}

	public <T> BehaviorTree<T> createBehaviorTree (String treeReference, T blackboard) {
		BehaviorTree<T> bt = (BehaviorTree<T>)retrieveArchetypeTree(treeReference);
		BehaviorTree<T> cbt = (BehaviorTree<T>)bt.cloneTask();
		cloneCommentMap(bt.getChild(0), cbt.getChild(0));
		cbt.setObject(blackboard);
		return cbt;
	}

	private void cloneCommentMap (Task<?> src, Task<?> dst) {
		String comment = getComment(src);
		if (comment != null) {
			taskToComment.put(dst, comment);
		}
		for (int i = 0; i < src.getChildCount(); i++) {
			cloneCommentMap(src.getChild(i), dst.getChild(i));
		}
	}

	public void updateComments (BehaviorTreeModel model) {
		TaskModel root = model.getRoot();
		updateComments(root);
	}

	protected void updateComments(TaskModel task) {
		Task wrapped = task.getWrapped();
		// wrapped can be null if TaskModel is a Guard
		if (wrapped != null) {
			task.setUserComment(getComment(wrapped));
		}
		for (int i = 0; i < task.getChildCount(); i++) {
			updateComments(task.getChild(i));
		}
	}

	public String getComment(Task task) {
		return taskToComment.get(task, null);
	}

	protected class EditorBehaviourTreeReader<E> extends BehaviorTreeParser.DefaultBehaviorTreeReader<E> {
		public EditorBehaviourTreeReader () {
			super(true);
		}

		protected String lastComment;
		@Override protected void comment (String text) {
			super.comment(text);
			lastComment = text.trim();
		}

		@Override protected void endStatement () {
			super.endStatement();
			if (prevTask != null && lastComment != null) {
				taskToComment.put(prevTask.task, lastComment);
				lastComment = null;
			}
		}
	}
}
