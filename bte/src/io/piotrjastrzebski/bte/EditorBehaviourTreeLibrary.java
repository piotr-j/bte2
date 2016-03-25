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
 * If useEditorBehaviourTree is {@code true}, a subclass of BehaviourTree will be returned, with some extra functionality, default is {@code false}
 *
 * Created by PiotrJ on 16/02/16.
 */
public class EditorBehaviourTreeLibrary extends BehaviorTreeLibrary {
	public EditorBehaviourTreeLibrary () {
		this(BehaviorTreeParser.DEBUG_NONE);
	}

	public EditorBehaviourTreeLibrary (int parseDebugLevel) {
		this(GdxAI.getFileSystem().newResolver(Files.FileType.Internal), parseDebugLevel);
	}

	protected EditorBehaviourTreeReader<?> reader;
	protected EditorParser<?> editorParser;
	protected ObjectMap<Task, String> taskToComment = new ObjectMap<>();

	public EditorBehaviourTreeLibrary (FileHandleResolver resolver, int parseDebugLevel) {
		this(resolver, parseDebugLevel, false);
	}

	public EditorBehaviourTreeLibrary (FileHandleResolver resolver, int parseDebugLevel, boolean useEditorBehaviourTree) {
		super(resolver, parseDebugLevel);
		reader = new EditorBehaviourTreeReader<>();
		parser = editorParser =  new EditorParser<>(new DistributionAdapters(), parseDebugLevel, reader);
		editorParser.useEditorBehaviourTree = useEditorBehaviourTree;
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

	/**
	 * If set to {@code true} newly parsed trees will be {@link EditorBehaviourTree}s with extra capabilities for the editor
	 * The tree will ignore {@link BehaviorTree#step()} if it is being edited
	 * @param useEditorBehaviourTree if true, newly parsed trees will be {@link EditorBehaviourTree}s
	 */
	public void setUseEditorBehaviourTree (boolean useEditorBehaviourTree) {
		editorParser.useEditorBehaviourTree = useEditorBehaviourTree;
	}

	public boolean isUseEditorBehaviourTree () {
		return editorParser.useEditorBehaviourTree;
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

	protected class EditorParser<E> extends BehaviorTreeParser<E> {
		protected boolean useEditorBehaviourTree;

		public EditorParser (DistributionAdapters distributionAdapters, int debugLevel, DefaultBehaviorTreeReader<E> reader) {
			super(distributionAdapters, debugLevel, reader);
		}

		@Override public BehaviorTree<E> createBehaviorTree (Task<E> root, E object) {
			if (debugLevel > BehaviorTreeParser.DEBUG_LOW) printTree(root, 0);
			if (useEditorBehaviourTree) {
				return new EditorBehaviourTree<>(root, object);
			}
			return new BehaviorTree<E>(root, object);
		}
	}

	public static class EditorBehaviourTree<E> extends BehaviorTree<E> {
		/** Creates a {@code BehaviorTree} with no root task and no blackboard object. Both the root task and the blackboard object must
		 * be set before running this behavior tree, see {@link #addChild(Task) addChild()} and {@link #setObject(Object) setObject()}
		 * respectively. */
		public EditorBehaviourTree () {
			this(null, null);
		}

		/** Creates a behavior tree with a root task and no blackboard object. Both the root task and the blackboard object must be set
		 * before running this behavior tree, see {@link #addChild(Task) addChild()} and {@link #setObject(Object) setObject()}
		 * respectively.
		 *
		 * @param rootTask the root task of this tree. It can be {@code null}. */
		public EditorBehaviourTree (Task<E> rootTask) {
			this(rootTask, null);
		}

		/** Creates a behavior tree with a root task and a blackboard object. Both the root task and the blackboard object must be set
		 * before running this behavior tree, see {@link #addChild(Task) addChild()} and {@link #setObject(Object) setObject()}
		 * respectively.
		 *
		 * @param rootTask the root task of this tree. It can be {@code null}.
		 * @param object the blackboard. It can be {@code null}. */
		public EditorBehaviourTree (Task<E> rootTask, E object) {
			super(rootTask, object);
		}

		protected boolean isEdited;

		@Override public void step () {
			if (!isEdited) {
				super.step();
			}
		}

		public void forceStep () {
			super.step();
		}

		public void setEdited (boolean edited) {
			isEdited = edited;
		}

		public boolean isEdited () {
			return isEdited;
		}
	}
 }
