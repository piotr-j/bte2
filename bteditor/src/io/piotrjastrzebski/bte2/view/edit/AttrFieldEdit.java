package io.piotrjastrzebski.bte2.view.edit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.utils.random.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextField;
import io.piotrjastrzebski.bte2.model.tasks.fields.EditableFields;
import io.piotrjastrzebski.bte2.model.tasks.fields.EditableFields.EditableField;

/**
 * Created by PiotrJ on 06/10/15.
 */
class AttrFieldEdit {
	private final static String TAG = AttrFieldEdit.class.getSimpleName();

	private static VisTextField.TextFieldFilter digitFieldFilter = new VisTextField.TextFieldFilter.DigitsOnlyFilter();
	private static VisTextField.TextFieldFilter digitPeriodFieldFilter = new VisTextField.TextFieldFilter() {
		@Override public boolean acceptChar (VisTextField textField, char c) {
			return Character.isDigit(c) || c == '.';
		}
	};

	protected static Actor stringAreaEditField (final Object object, final Field field) throws ReflectionException {
		String value = (String)field.get(object);
		final VisTextArea ta = new VisTextArea(value);
		ta.setPrefRows(3.25f);
		ta.setMaxLength(80);
		ta.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				String text = ta.getText();
				try {
					field.set(object, text);
				} catch (ReflectionException e) {
					Gdx.app.error("String validator", "Failed to set field " + field + " to " + text, e);
				}
			}
		});
		addCancelOnESC(ta);
		return ta;
	}

	protected static Actor createEditField (EditableField field) {
		Class fType = field.getType();
		if (fType == float.class) {
			return AttrFieldEdit.floatEditField(field);
		} else if (fType == double.class) {
			return AttrFieldEdit.doubleEditField(field);
		} else if (fType == int.class) {
			return AttrFieldEdit.integerEditField(field);
		} else if (fType == long.class) {
			return AttrFieldEdit.longEditField(field);
		} else if (fType == String.class) {
			return AttrFieldEdit.stringEditField(field);
		} else if (fType == boolean.class) {
			return AttrFieldEdit.booleanEditField(field);
		} else if (fType.isEnum()) {
			return AttrFieldEdit.enumEditField(field);
		} if (Distribution.class.isAssignableFrom(fType)) {
			return AttrFieldEdit.distEditField(field);
		} else {
			Gdx.app.error(TAG, "Not supported field type " + fType + " in " + field);
			return null;
		}
	}

	protected static Actor integerEditField (final EditableField field) {
		return valueEditField(new IntField() {
			@Override public int getInt () {
				return (int)field.get();
			}

			@Override public void setInt (int val) {
				field.set(val);
			}
		});
	}

	protected static Actor longEditField (final EditableField field) {
		return valueEditField(new LongField() {
			@Override public long getLong () {
				return (long)field.get();
			}

			@Override public void setLong (long val) {
				field.set(val);
			}
		});
	}

	protected static Actor floatEditField (final EditableField field) {
		return valueEditField(new FloatField() {
			@Override public float getFloat () {
				return (float)field.get();
			}

			@Override public void setFloat (float val) {
				field.set(val);
			}
		});
	}

	protected static Actor doubleEditField (final EditableField field) {
		return valueEditField(new DoubleField() {
			@Override public double getDouble () {
				return (double)field.get();
			}

			@Override public void setDouble (double val) {
				field.set(val);
			}
		});
	}

	protected static Actor stringEditField (final EditableField field) {
		String value = (String)field.get();
		final VisTextField tf = new VisTextField(value);
		if (field.isRequired()) {
			if (value == null || value.length() == 0)
				tf.setColor(Color.RED);
		}
		tf.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				String text = tf.getText();
				if (text.length() == 0 && field.isRequired()) {
					tf.setColor(Color.RED);
				} else {
					tf.setColor(Color.WHITE);
					field.set(text);
				}
			}
		});
		addCancelOnESC(tf);
		return tf;
	}

	protected static Actor enumEditField (final EditableField field) {
		Object[] values = field.getType().getEnumConstants();
		final VisSelectBox<Object> sb = new VisSelectBox<>();
		sb.setItems(values);
		sb.setSelected(field.get());
		sb.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				Object selected = sb.getSelection().getLastSelected();
				field.set(selected);
			}
		});
		return sb;
	}

	protected static Actor booleanEditField (final EditableField field) {
		final VisSelectBox<Object> sb = new VisSelectBox<>();
		sb.setItems(true, false);
		sb.setSelected(field.get());
		sb.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				Object selected = sb.getSelection().getLastSelected();
				field.set(selected);
			}
		});
		return sb;
	}

	private static boolean validateInt(String str) {
		try {
			int val = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private static boolean validateLong(String str) {
		try {
			long val = Long.parseLong(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private static boolean validateFloat(String str) {
		try {
			float val = Float.parseFloat(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private static boolean validateDouble(String str) {
		try {
			double val = Double.parseDouble(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private static void createDistEditField (String text, final EditableField field, Distribution dist, Table cont,
		final  Class<? extends DWrapper>[] classes) {
		final Table fields = new Table();
		final VisSelectBox<DWrapper> sb = new VisSelectBox<>();
		cont.add(new VisLabel(text)).row();
		cont.add(sb).row();
		cont.add(fields);

		DWrapper actual = null;
		final DWrapper[] wrappers = new DWrapper[classes.length];
		for (int i = 0; i < classes.length; i++) {
			Class<? extends DWrapper> aClass = classes[i];
			try {
				Constructor constructor = ClassReflection.getDeclaredConstructor(aClass);
				constructor.setAccessible(true);
				DWrapper wrapper = (DWrapper)constructor.newInstance();
				wrapper.init(field);
				wrappers[i] = wrapper;
				if (wrapper.isWrapperFor(dist)) {
					actual = wrapper;
				}
			} catch (ReflectionException e) {
				e.printStackTrace();
			}
		}

		if (actual == null) {
			Gdx.app.error(text, "Wrapper missing for " + dist);
			return;
		}
		actual.set(dist);
		actual.createEditFields(fields);

		sb.setItems(wrappers);
		sb.setSelected(actual);
		sb.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				DWrapper selected = sb.getSelection().getLastSelected();
				field.set(selected.create());
				fields.clear();
				selected.createEditFields(fields);
			}
		});
	}

	private static void createSimpleDistEditField (String text, final EditableField field, Distribution dist, Table cont,
		 Class<? extends DWrapper> aClass) {
		final Table fields = new Table();
		cont.add(new VisLabel(text)).row();
		cont.add(fields);

		DWrapper wrapper = null;
		try {
			Constructor constructor = ClassReflection.getDeclaredConstructor(aClass);
			constructor.setAccessible(true);
			wrapper = (DWrapper)constructor.newInstance();
			wrapper.init(field);
		} catch (ReflectionException e) {
			e.printStackTrace();
		}

		if (wrapper == null) {
			Gdx.app.error(text, "Wrapper missing for " + dist);
			return;
		}
		wrapper.set(dist);
		wrapper.createEditFields(fields);
	}

	protected static Actor distEditField (final EditableField field) {
		// how do implement this crap? multiple inputs probably for each value in the thing
		Distribution dist = (Distribution)field.get();
		final Table cont = new Table();
		Class type = field.getType();

		// add new edit fields, per type of distribution
		// if field type is one of the abstract classes, we want to be able to pick dist we want
		if (type == IntegerDistribution.class) {
			createDistEditField("Integer distribution", field, dist, cont,
				new Class[] {CIDWrapper.class, TIDWrapper.class, UIDWrapper.class});
			return cont;
		}
		if (type == LongDistribution.class) {
			createDistEditField("Long distribution", field, dist, cont,
				new Class[] {CLDWrapper.class, TLDWrapper.class, ULDWrapper.class});
			return cont;
		}
		if (type == FloatDistribution.class) {
			createDistEditField("Float distribution", field, dist, cont,
				new Class[] {CFDWrapper.class, TFDWrapper.class, UFDWrapper.class, GFDWrapper.class});
			return cont;
		}
		if (type == DoubleDistribution.class) {
			createDistEditField("Double distribution", field, dist, cont,
				new Class[] {CDDWrapper.class, TDDWrapper.class, UDDWrapper.class, GDDWrapper.class});
			return cont;
		}
		// if not we cant pick the type, just edit existing distribution
		Class<? extends DWrapper> wrapper = getWrapperFor(type);
		if (wrapper == null) {
			Gdx.app.error(TAG, "Wrapper for " + type + " not found!");
			return cont;
		}
		createSimpleDistEditField(type.getSimpleName(), field, dist, cont, wrapper);
		return cont;
	}

	private static Class<? extends DWrapper> getWrapperFor (Class type) {
		if (type == ConstantIntegerDistribution.class) {
			return CIDWrapper.class;
		}
		if (type == ConstantLongDistribution.class) {
			return CLDWrapper.class;
		}
		if (type == ConstantFloatDistribution.class) {
			return CFDWrapper.class;
		}
		if (type == ConstantDoubleDistribution.class) {
			return CDDWrapper.class;
		}
		if (type == GaussianFloatDistribution.class) {
			return GFDWrapper.class;
		}
		if (type == GaussianDoubleDistribution.class) {
			return GDDWrapper.class;
		}
		if (type == TriangularIntegerDistribution.class) {
			return TIDWrapper.class;
		}
		if (type == TriangularLongDistribution.class) {
			return TLDWrapper.class;
		}
		if (type == TriangularFloatDistribution.class) {
			return TFDWrapper.class;
		}
		if (type == TriangularDoubleDistribution.class) {
			return TDDWrapper.class;
		}
		if (type == UniformIntegerDistribution.class) {
			return UIDWrapper.class;
		}
		if (type == UniformLongDistribution.class) {
			return ULDWrapper.class;
		}
		if (type == UniformFloatDistribution.class) {
			return UFDWrapper.class;
		}
		if (type == UniformDoubleDistribution.class) {
			return UDDWrapper.class;
		}
		return null;
	}

	private static Actor valueEditField (final ValueField valueField) {
		final VisTextField vtf = new VisTextField("");
		vtf.setText(valueField.get());
		vtf.setTextFieldFilter(valueField.getFilter());
		vtf.setTextFieldListener(new VisTextField.TextFieldListener() {
			@Override public void keyTyped (VisTextField textField, char c) {
				String text = vtf.getText();
				if (valueField.isValid(text)) {
					vtf.setColor(Color.WHITE);
					valueField.set(text);
				} else {
					vtf.setColor(Color.RED);
				}
			}
		});
		addCancelOnESC(vtf);
		return vtf;
	}

	public static Actor createPathEditField (final EditableField field) {
		String value = (String)field.get();
		final VisTextField tf = new VisTextField(value);
		tf.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				String text = tf.getText();
				if (text.length() == 0) {
					tf.setColor(Color.RED);
					return;
				}
				// TODO what else? we could try to parse it...
				FileHandle fh = Gdx.files.internal(text);
				if (fh.isDirectory() || !fh.exists()) {
					tf.setColor(Color.RED);
				} else {
					tf.setColor(Color.WHITE);
					field.set(text);
				}
			}
		});
		addCancelOnESC(tf);
		return tf;
	}

	private static abstract class ValueField {
		protected abstract String get();
		protected abstract boolean isValid(String val);
		protected abstract void set(String val);
		protected abstract VisTextField.TextFieldFilter getFilter ();
	}

	private static abstract class IntField extends ValueField {
		@Override protected boolean isValid (String val) {
			return validateInt(val);
		}

		@Override public VisTextField.TextFieldFilter getFilter () {
			return digitFieldFilter;
		}

		@Override final protected String get () {
			return Integer.toString(getInt());
		}

		@Override final protected void set (String val) {
			setInt(Integer.parseInt(val));
		}

		public abstract int getInt ();
		public abstract void setInt (int val);
	}

	private static abstract class LongField extends ValueField {
		@Override protected boolean isValid (String val) {
			return validateLong(val);
		}

		@Override public VisTextField.TextFieldFilter getFilter () {
			return digitFieldFilter;
		}

		@Override final protected String get () {
			return Long.toString(getLong());
		}

		@Override final protected void set (String val) {
			setLong(Long.parseLong(val));
		}

		public abstract long getLong ();
		public abstract void setLong (long val);
	}

	private static abstract class FloatField extends ValueField {
		@Override protected boolean isValid (String val) {
			return validateFloat(val);
		}

		@Override final protected String get () {
			return Float.toString(getFloat());
		}

		@Override final protected void set (String val) {
			setFloat(Float.parseFloat(val));
		}

		@Override public VisTextField.TextFieldFilter getFilter () {
			return digitPeriodFieldFilter;
		}

		public abstract float getFloat ();
		public abstract void setFloat (float val);
	}

	private static abstract class DoubleField extends ValueField {
		@Override protected boolean isValid (String val) {
			return validateDouble(val);
		}

		@Override public VisTextField.TextFieldFilter getFilter () {
			return digitPeriodFieldFilter;
		}

		@Override final protected String get () {
			return Double.toString(getDouble());
		}

		@Override final protected void set (String val) {
			setDouble(Double.parseDouble(val));
		}

		public abstract double getDouble ();
		public abstract void setDouble (double val);
	}

	protected static abstract class DWrapper {
		protected EditableField field;

		protected DWrapper () {}

		protected final void updateOwner() {
			field.set(create());
		}

		public abstract Distribution create();

		public abstract void set (Distribution dist);

		public abstract void createEditFields (Table fields);

		public abstract boolean isWrapperFor (Distribution distribution);

		public DWrapper init (EditableField field) {
			this.field = field;
			return this;
		}
	}

	protected static class CIDWrapper extends DWrapper {
		protected int value;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("value")).padRight(10).row();
			fields.add(valueEditField(new IntField(){
				@Override public int getInt () {
					return value;
				}

				@Override public void setInt (int val) {
					value = val;
					updateOwner();
				}
			}));
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof ConstantIntegerDistribution;
		}

		public IntegerDistribution create() {
			return new ConstantIntegerDistribution(value);
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof ConstantIntegerDistribution) {
				ConstantIntegerDistribution id = (ConstantIntegerDistribution)dist;
				value = id.getValue();
			}
		}

		@Override public String toString () {
			return "Constant";
		}
	}

	protected static class TIDWrapper extends DWrapper {
		protected int low;
		protected int high;
		protected float mode;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new IntField(){
				@Override public int getInt () {
					return low;
				}

				@Override public void setInt (int val) {
					low = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new IntField(){
				@Override public int getInt () {
					return high;
				}

				@Override public void setInt (int val) {
					high = val;
					updateOwner();
				}
			})).row();;

			fields.add(new VisLabel("mode")).padRight(10).row();
			fields.add(valueEditField(new FloatField(){
				@Override public float getFloat () {
					return mode;
				}

				@Override public void setFloat (float val) {
					mode = val;
					updateOwner();
				}
			}));
		}

		public IntegerDistribution create() {
			return new TriangularIntegerDistribution(low, high, mode);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof TriangularIntegerDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof TriangularIntegerDistribution) {
				TriangularIntegerDistribution id = (TriangularIntegerDistribution)dist;
				low = id.getLow();
				high = id.getHigh();
				mode = id.getMode();
			}
		}

		@Override public String toString () {
			return "Triangular";
		}
	}

	protected static class UIDWrapper extends DWrapper {
		protected int low;
		protected int high;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new IntField() {
				@Override public int getInt () {
					return low;
				}

				@Override public void setInt (int val) {
					low = val;
					updateOwner();
				}
			})).row();;

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new IntField() {
				@Override public int getInt () {
					return high;
				}

				@Override public void setInt (int val) {
					high = val;
					updateOwner();
				}
			}));
		}

		public IntegerDistribution create() {
			return new UniformIntegerDistribution(low, high);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof UniformIntegerDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof UniformIntegerDistribution) {
				UniformIntegerDistribution id = (UniformIntegerDistribution)dist;
				low = id.getLow();
				high = id.getHigh();
			}
		}

		@Override public String toString () {
			return "Uniform";
		}
	}

	protected static class CLDWrapper extends DWrapper {
		protected long value;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("value")).padRight(10).row();
			fields.add(valueEditField(new LongField(){
				@Override public long getLong () {
					return value;
				}

				@Override public void setLong (long val) {
					value = val;
					updateOwner();
				}
			}));
		}

		public Distribution create() {
			return new ConstantLongDistribution(value);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof ConstantLongDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof ConstantLongDistribution) {
				ConstantLongDistribution ld = (ConstantLongDistribution)dist;
				value = ld.getValue();
			}
		}

		@Override public String toString () {
			return "Constant";
		}
	}

	protected static class TLDWrapper extends DWrapper {
		protected long low;
		protected long high;
		protected double mode;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new LongField(){
				@Override public long getLong () {
					return low;
				}

				@Override public void setLong (long val) {
					low = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new LongField(){
				@Override public long getLong () {
					return high;
				}

				@Override public void setLong (long val) {
					high = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("mode")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return mode;
				}

				@Override public void setDouble (double val) {
					mode = val;
					updateOwner();
				}
			}));
		}

		@Override public Distribution create() {
			return new TriangularLongDistribution(low, high, mode);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof TriangularLongDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof TriangularLongDistribution) {
				TriangularLongDistribution ld = (TriangularLongDistribution)dist;
				low = ld.getLow();
				high = ld.getHigh();
				mode = ld.getMode();
			}
		}

		@Override public String toString () {
			return "Triangular";
		}
	}

	protected static class ULDWrapper extends DWrapper {
		protected long low;
		protected long high;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new LongField(){
				@Override public long getLong () {
					return low;
				}

				@Override public void setLong (long val) {
					low = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new LongField(){
				@Override public long getLong () {
					return high;
				}

				@Override public void setLong (long val) {
					high = val;
					updateOwner();
				}
			})).row();
		}

		@Override public Distribution create() {
			return new UniformLongDistribution(low, high);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof UniformLongDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof UniformLongDistribution) {
				UniformLongDistribution ld = (UniformLongDistribution)dist;
				low = ld.getLow();
				high = ld.getHigh();
			}
		}

		@Override public String toString () {
			return "Uniform";
		}
	}

	protected static class CFDWrapper extends DWrapper {
		protected float value;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("value")).padRight(10).row();
			fields.add(valueEditField(new FloatField(){
				@Override public float getFloat () {
					return value;
				}

				@Override public void setFloat (float val) {
					value = val;
					updateOwner();
				}
			}));
		}

		public Distribution create() {
			return new ConstantFloatDistribution(value);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof ConstantFloatDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof ConstantFloatDistribution) {
				ConstantFloatDistribution fd = (ConstantFloatDistribution)dist;
				value = fd.getValue();
			}
		}

		@Override public String toString () {
			return "Constant";
		}
	}

	protected static class TFDWrapper extends DWrapper {
		protected float low;
		protected float high;
		protected float mode;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new FloatField() {
				@Override public float getFloat () {
					return low;
				}

				@Override public void setFloat (float val) {
					low = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new FloatField() {
				@Override public float getFloat () {
					return high;
				}

				@Override public void setFloat (float val) {
					high = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("mode")).padRight(10).row();
			fields.add(valueEditField(new FloatField(){
				@Override public float getFloat () {
					return mode;
				}

				@Override public void setFloat (float val) {
					mode = val;
					updateOwner();
				}
			}));
		}

		@Override public Distribution create() {
			return new TriangularFloatDistribution(low, high, mode);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof TriangularFloatDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof TriangularFloatDistribution) {
				TriangularFloatDistribution fd = (TriangularFloatDistribution)dist;
				low = fd.getLow();
				high = fd.getHigh();
				mode = fd.getMode();
			}
		}

		@Override public String toString () {
			return "Triangular";
		}
	}

	protected static class UFDWrapper extends DWrapper {
		protected float low;
		protected float high;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new FloatField(){
				@Override public float getFloat () {
					return low;
				}

				@Override public void setFloat (float val) {
					low = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new FloatField(){
				@Override public float getFloat () {
					return high;
				}

				@Override public void setFloat (float val) {
					high = val;
					updateOwner();
				}
			})).row();
		}

		@Override public Distribution create() {
			return new UniformFloatDistribution(low, high);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof UniformFloatDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof UniformFloatDistribution) {
				UniformFloatDistribution fd = (UniformFloatDistribution)dist;
				low = fd.getLow();
				high = fd.getHigh();
			}
		}

		@Override public String toString () {
			return "Uniform";
		}
	}

	protected static class GFDWrapper extends DWrapper {
		protected float mean;
		protected float std;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("mean")).padRight(10).row();
			fields.add(valueEditField(new FloatField(){
				@Override public float getFloat () {
					return mean;
				}

				@Override public void setFloat (float val) {
					mean = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("STD")).padRight(10).row();
			fields.add(valueEditField(new FloatField(){
				@Override public float getFloat () {
					return std;
				}

				@Override public void setFloat (float val) {
					std = val;
					updateOwner();
				}
			}));
		}

		@Override public Distribution create() {
			return new GaussianFloatDistribution(mean, std);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof GaussianFloatDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof GaussianFloatDistribution) {
				GaussianFloatDistribution fd = (GaussianFloatDistribution)dist;
				mean = fd.getMean();
				std = fd.getStandardDeviation();
			}
		}

		@Override public String toString () {
			return "Gaussian";
		}
	}

	protected static class CDDWrapper extends DWrapper {
		protected double value;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("value")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return value;
				}

				@Override public void setDouble (double val) {
					value = val;
					updateOwner();
				}
			}));
		}

		public Distribution create() {
			return new ConstantDoubleDistribution(value);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof ConstantDoubleDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof ConstantDoubleDistribution) {
				ConstantDoubleDistribution fd = (ConstantDoubleDistribution)dist;
				value = fd.getValue();
			}
		}

		@Override public String toString () {
			return "Constant";
		}
	}

	protected static class TDDWrapper extends DWrapper {
		protected double low;
		protected double high;
		protected double mode;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return low;
				}

				@Override public void setDouble (double val) {
					low = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return high;
				}

				@Override public void setDouble (double val) {
					high = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("mode")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return mode;
				}

				@Override public void setDouble (double val) {
					mode = val;
					updateOwner();
				}
			}));
		}

		@Override public Distribution create() {
			return new TriangularDoubleDistribution(low, high, mode);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof TriangularDoubleDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof TriangularDoubleDistribution) {
				TriangularDoubleDistribution dd = (TriangularDoubleDistribution)dist;
				low = dd.getLow();
				high = dd.getHigh();
				mode = dd.getMode();
			}
		}

		@Override public String toString () {
			return "Triangular";
		}
	}

	protected static class UDDWrapper extends DWrapper {
		protected double low;
		protected double high;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("low")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return low;
				}

				@Override public void setDouble (double val) {
					low = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("high")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return high;
				}

				@Override public void setDouble (double val) {
					high = val;
					updateOwner();
				}
			}));
		}

		@Override public Distribution create() {
			return new UniformDoubleDistribution(low, high);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof UniformDoubleDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof UniformDoubleDistribution) {
				UniformDoubleDistribution dd = (UniformDoubleDistribution)dist;
				low = dd.getLow();
				high = dd.getHigh();
			}
		}

		@Override public String toString () {
			return "Uniform";
		}
	}

	protected static class GDDWrapper extends DWrapper {
		protected double mean;
		protected double std;

		@Override public void createEditFields (Table fields) {
			fields.add(new VisLabel("mean")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return mean;
				}

				@Override public void setDouble (double val) {
					mean = val;
					updateOwner();
				}
			})).row();

			fields.add(new VisLabel("STD")).padRight(10).row();
			fields.add(valueEditField(new DoubleField(){
				@Override public double getDouble () {
					return std;
				}

				@Override public void setDouble (double val) {
					std = val;
					updateOwner();
				}
			}));
		}

		@Override public Distribution create() {
			return new GaussianDoubleDistribution(mean, std);
		}

		@Override public boolean isWrapperFor (Distribution distribution) {
			return distribution instanceof GaussianDoubleDistribution;
		}

		@Override public void set (Distribution dist) {
			if (dist instanceof GaussianDoubleDistribution) {
				GaussianDoubleDistribution dd = (GaussianDoubleDistribution)dist;
				mean = dd.getMean();
				std = dd.getStandardDeviation();
			}
		}

		@Override public String toString () {
			return "Gaussian";
		}
	}

	private static void addCancelOnESC (final Actor actor) {
		actor.addListener(new InputListener() {
			@Override public boolean keyDown (InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE) {
					actor.getStage().setKeyboardFocus(null);
				}
				return false;
			}
		});
	}
}
