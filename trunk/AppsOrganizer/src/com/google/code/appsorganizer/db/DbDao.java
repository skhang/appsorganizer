package com.google.code.appsorganizer.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class DbDao<T> {

	protected final String name;

	protected SQLiteDatabase db;

	protected final List<DbColumns<T>> columns = new ArrayList<DbColumns<T>>();

	public DbDao(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<DbColumns<T>> getColumns() {
		return columns;
	}

	protected void addColumn(DbColumns<T> c) {
		columns.add(c);
	}

	public String getCreateTableScript() {
		StringBuilder b = new StringBuilder("create table ");
		b.append(name);
		b.append(" (");
		boolean first = true;
		for (DbColumns<T> c : columns) {
			if (first) {
				first = false;
			} else {
				b.append(',');
			}
			b.append(c.getName());
			b.append(' ');
			b.append(c.getDescription());
		}
		b.append(");");
		return b.toString();
	}

	public String getSelectAllQuery() {
		StringBuilder b = new StringBuilder();
		b.append(name);
		b.append(" (");
		for (DbColumns<T> c : columns) {
			if (b.length() > 0) {
				b.append(',');
			}
			b.append(c.getName());
		}
		return "select " + b.toString() + " from " + name;
	}

	public String getDropTableScript() {
		return "DROP TABLE IF EXISTS " + name;
	}

	public abstract T createNewObject();

	public List<T> queryForList(List<DbColumns<T>> cols, Map<DbColumns<T>, String> filter, String orderBy, String groupBy, String having) {
		Cursor c = query(cols, filter, orderBy, groupBy, having);
		return convertCursorToList(c, cols);
	}

	public T queryForObject(List<DbColumns<T>> cols, Map<DbColumns<T>, String> filter, String groupBy, String having) {
		Cursor c = query(cols, filter, null, groupBy, having);
		return convertCursorToObject(c, cols);
	}

	protected List<T> convertCursorToList(Cursor c, List<DbColumns<T>> cols) {
		List<T> l = new ArrayList<T>();
		try {
			// c.moveToFirst();
			while (c.moveToNext()) {
				T t = createNewObject();
				for (DbColumns<T> col : cols) {
					col.populateObject(t, c);
				}
				l.add(t);
			}
		} finally {
			c.close();
		}
		return l;
	}

	protected T convertCursorToObject(Cursor c, List<DbColumns<T>> cols) {
		// c.moveToFirst();
		// ((Activity) context).startManagingCursor(c);
		try {
			while (c.moveToNext()) {
				T t = createNewObject();
				for (DbColumns<T> col : cols) {
					col.populateObject(t, c);
				}
				if (c.moveToNext()) {
					throw new RuntimeException("Query returned more than one object");
				} else {
					return t;
				}
			}
		} finally {
			c.close();
		}
		return null;
	}

	public Cursor query(List<DbColumns<T>> cols, Map<DbColumns<T>, String> filter, String orderBy, String groupBy, String having) {
		return db.query(name, columnsToStringArray(cols), filterToSelection(filter), filterToSelectionArgs(filter), groupBy, having,
				orderBy);
	}

	private String filterToSelection(Map<DbColumns<T>, String> filter) {
		if (filter != null) {
			StringBuilder b = new StringBuilder();
			for (Entry<DbColumns<T>, String> e : filter.entrySet()) {
				if (b.length() > 0) {
					b.append(" and ");
				}
				b.append(e.getKey().getName());
				b.append("=?");
			}
			return b.toString();
		} else {
			return null;
		}
	}

	private String[] filterToSelectionArgs(Map<DbColumns<T>, String> filter) {
		if (filter != null) {
			String[] ret = new String[filter.size()];
			int pos = 0;
			for (Entry<DbColumns<T>, String> e : filter.entrySet()) {
				ret[pos++] = e.getValue();
			}
			return ret;
		} else {
			return null;
		}
	}

	private String[] columnsToStringArray(List<DbColumns<T>> cols) {
		String[] ret = new String[cols.size()];
		int pos = 0;
		for (DbColumns<T> dbColumns : cols) {
			ret[pos++] = dbColumns.getName();
		}
		return ret;
	}

	public long insert(T obj) {
		ContentValues v = new ContentValues();
		for (DbColumns<T> col : columns) {
			col.populateContent(obj, v);
		}
		return db.insert(name, null, v);
	}

	public void setDb(SQLiteDatabase db) {
		this.db = db;
	}

	private final List<DbChangeListener> listeners = new ArrayList<DbChangeListener>();

	public boolean addListener(DbChangeListener object) {
		return listeners.add(object);
	}

	public boolean removeListener(DbChangeListener object) {
		return listeners.remove(object);
	}

	public void notifyDataSetChanged() {
		for (DbChangeListener a : listeners) {
			a.notifyDataSetChanged();
		}
	}
}
