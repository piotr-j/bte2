package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.branch.Selector;
import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class FakeRootModel extends TaskModel {
	public FakeRootModel () {
		super(Type.ROOT);
		minChildren = 1;
		maxChildren = 1;
	}

	@SuppressWarnings("unchecked")
	public void init (TaskModel root, BehaviorTreeModel model) {
		this.model = model;
		children.clear();
		children.add(root);
		// need some wrapped task so remove command works
		@SuppressWarnings("rawtypes")
		Selector selector = new Selector();
		selector.addChild(root.wrapped);
		wrapped = selector;
		root.setParent(this);
	}

	@Override public void free () {
		super.reset();
	}

	@Override public TaskModel copy () {
		return new FakeRootModel();
	}

	@Override public String toString () {
		return "FakeRootModel{}";
	}

	@Override public String getName () {
		// TODO could use tree file name or something
		return "ROOT";
	}

	@Override public boolean isValid () {
		return model.isValid();
	}
}
