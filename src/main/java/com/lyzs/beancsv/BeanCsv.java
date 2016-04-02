package com.lyzs.beancsv;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lyzs.annotation.CsvColumn;

import au.com.bytecode.opencsv.CSVWriter;

public class BeanCsv {
	private static final Logger log = LoggerFactory.getLogger(BeanCsv.class);

	public static String[] pickCsvHeader(Class<?> clazz) {
		List<String> header = new ArrayList<>();
		if (clazz == null) {
			return header.toArray(new String[header.size()]);
		}
		Field[] fields = clazz.getDeclaredFields();
		Map<String, String> map = new TreeMap<>();
		for (Field field : fields) {
			if (field.isAnnotationPresent(CsvColumn.class)) {
				CsvColumn csvColumn = field.getAnnotation(CsvColumn.class);
				String orderKey = csvColumn.orderKey();
				if (orderKey.equals("fieldName")) {
					orderKey = field.getName();
				}
				String name = csvColumn.name();
				if (name.equals("fieldName")) {
					name = field.getName();
				}
				map.put(orderKey, name);
			}
		}
		for (Entry<String, String> entry : map.entrySet()) {
			header.add(entry.getValue());
		}
		return header.toArray(new String[header.size()]);
	}

	public static void write(CSVWriter csvWriter, Object bean) {
		List<Object> list = new ArrayList<>();
		list.add(bean);
		write(csvWriter, list);
	}

	public static void write(CSVWriter csvWriter, List<Object> beans) {
		if (beans == null || beans.isEmpty()) {
			return;
		}
		Class<?> clazz = beans.get(0).getClass();
		Field[] fields = clazz.getDeclaredFields();
		Map<String, Field> map = new TreeMap<>();
		for (Field field : fields) {
			if (field.isAnnotationPresent(CsvColumn.class)) {
				CsvColumn csvColumn = field.getAnnotation(CsvColumn.class);
				String orderKey = csvColumn.orderKey();
				if (orderKey.equals("fieldName")) {
					orderKey = field.getName();
				}
				field.setAccessible(true);
				map.put(orderKey, field);
			}
		}

		List<String[]> list = new ArrayList<>();
		for (Object bean : beans) {
			List<String> tmp = new ArrayList<>();
			for (Entry<String, Field> entry : map.entrySet()) {
				Field field = entry.getValue();
				try {
					String value = field.get(bean).toString();
					tmp.add(value);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					log.warn("", e);
				}
			}
			list.add(tmp.toArray(new String[tmp.size()]));
		}

		csvWriter.writeAll(list);
	}
}
