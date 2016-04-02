package com.lyzs.beancsv;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lyzs.annotation.CsvColumn;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class BeanCsv {
	private static final Logger log = LoggerFactory.getLogger(BeanCsv.class);

	public static <T> void writeHeader(CSVWriter csvWriter, Class<T> clazz) {
		String[] header = pickCsvHeader(clazz);
		csvWriter.writeNext(header);
	}

	public static <T> String[] pickCsvHeader(Class<T> clazz) {
		List<String> header = new ArrayList<>();
		if (clazz == null) {
			return header.toArray(new String[header.size()]);
		}
		List<Field> fields = pickFields(clazz);
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
				if (map.containsKey(orderKey)) {
					throw new RuntimeException("not support duplicated orderKey");
				}
				map.put(orderKey, name);
			}
		}
		for (Entry<String, String> entry : map.entrySet()) {
			header.add(entry.getValue());
		}
		return header.toArray(new String[header.size()]);
	}

	public static <T> void write(CSVWriter csvWriter, T bean) {
		List<T> list = new ArrayList<>();
		list.add(bean);
		write(csvWriter, list);
	}

	public static <T> void write(CSVWriter csvWriter, List<T> beans) {
		if (beans == null || beans.isEmpty()) {
			return;
		}
		Class<?> clazz = beans.get(0).getClass();
		List<Field> fields = pickFields(clazz);
		Map<String, Field> map = pickOrderKey2Field(fields);

		List<String[]> list = new ArrayList<>();
		for (Object bean : beans) {
			List<String> tmp = new ArrayList<>();
			for (Entry<String, Field> entry : map.entrySet()) {
				Field field = entry.getValue();
				try {
					String value = String.valueOf(field.get(bean));
					tmp.add(value);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					log.warn("", e);
				}
			}
			list.add(tmp.toArray(new String[tmp.size()]));
		}

		csvWriter.writeAll(list);
	}

	public static <T> List<T> parseBeans(CSVReader csvReader, Class<T> clazz, boolean excludeHeader) {
		List<T> list = new ArrayList<>();
		try {
			List<Field> fields = pickFields(clazz);
			Map<String, Field> map = pickOrderKey2Field(fields);
			List<String[]> lines = csvReader.readAll();
			if (excludeHeader && !lines.isEmpty()) {
				lines.remove(0);
			}
			for (String[] line : lines) {
				T t = clazz.newInstance();
				int index = 0;
				for (Entry<String, Field> entry : map.entrySet()) {
					Field field = entry.getValue();
					String value = line[index];
					index++;
					try {
						BeanUtils.setProperty(t, field.getName(), value);
					} catch (InvocationTargetException e) {
						log.warn("", e);
					}
				}
				list.add(t);
			}
		} catch (IOException | InstantiationException | IllegalAccessException e) {
			log.warn("", e);
		}
		return list;
	}

	private static Map<String, Field> pickOrderKey2Field(List<Field> fields) {
		Map<String, Field> map = new TreeMap<>();
		for (Field field : fields) {
			if (field.isAnnotationPresent(CsvColumn.class)) {
				CsvColumn csvColumn = field.getAnnotation(CsvColumn.class);
				String orderKey = csvColumn.orderKey();
				if (orderKey.equals("fieldName")) {
					orderKey = field.getName();
				}
				field.setAccessible(true);
				if (map.containsKey(orderKey)) {
					throw new RuntimeException("not support duplicated orderKey");
				}
				map.put(orderKey, field);
			}
		}
		return map;
	}

	private static List<Field> pickFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
		Class<?> parent = clazz.getSuperclass();
		while (parent != Object.class) {
			fields.addAll(Arrays.asList(parent.getDeclaredFields()));
			parent = parent.getSuperclass();
		}
		return fields;
	}
}
