package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.branch.Selector;
import io.piotrjastrzebski.bte2.model.BTModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class ModelFakeRoot extends ModelTask {
	public ModelFakeRoot () {
		super(Type.ROOT);
	}

	@SuppressWarnings("unchecked")
	public void init (ModelTask root, BTModel model) {
		this.model = model;
		dirty = true;
		minChildren = 1;
		maxChildren = 1;
		// TODO make sure this is correct
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
		wrapped = null;
	}

	@Override public ModelTask copy () {
		return new ModelFakeRoot();
	}

	@Override public String toString () {
		return "ModelFakeRoot{}";
	}

	@Override public String getName () {
		// TODO could use tree file name or something
		return "ROOT";
	}
}
