package com.lyzs.beancsv;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    /**
     * Write a header line to the csvWriter. The header line is parsed from
     * clazz according to the CsvColumn's name. The column order is decided by
     * CsvColumn's orderKey, in literal order.
     * 
     * @param csvWriter
     * @param clazz
     */
    public static <T> void writeHeader(CSVWriter csvWriter, Class<T> clazz) {
        String[] header = pickCsvHeader(clazz);
        csvWriter.writeNext(header);
    }

    /**
     * Pick the header line from clazz according to the CsvColumn's name. The
     * column order is decided by CsvColumn's orderKey, in literal order.
     * 
     * @param clazz
     * @return
     */
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

    /**
     * Write a bean object to csvWriter.
     * 
     * @param csvWriter
     * @param bean
     */
    public static <T> void write(CSVWriter csvWriter, T bean) {
        List<T> list = new ArrayList<>();
        list.add(bean);
        write(csvWriter, list);
    }

    /**
     * Write a list of bean objects to csvWriter.
     * 
     * @param csvWriter
     * @param beans
     */
    public static <T> void write(CSVWriter csvWriter, List<T> beans) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        Class<?> clazz = beans.get(0).getClass();
        List<Field> fields = pickFields(clazz);
        Map<String, CsvColumnInfo> orderKey2CsvColumnInfo = pickOrderKey2CsvColumnInfo(fields);

        List<String[]> list = new ArrayList<>();
        for (Object bean : beans) {
            List<String> tmp = new ArrayList<>();
            for (Entry<String, CsvColumnInfo> entry : orderKey2CsvColumnInfo.entrySet()) {
                CsvColumnInfo csvColumnInfo = entry.getValue();
                Field field = csvColumnInfo.getField();
                try {
                    Object object = field.get(bean);
                    String value = "";
                    if (object != null) {
                        if (field.getType() == Date.class) {
                            value = csvColumnInfo.getDateFormat().format((Date) object);
                        } else {
                            value = String.valueOf(object);
                        }
                    }
                    tmp.add(value);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    log.warn("", e);
                }
            }
            list.add(tmp.toArray(new String[tmp.size()]));
        }

        csvWriter.writeAll(list);
    }

    /**
     * Parse bean objects from csvReader, according to clazz.
     * 
     * @param csvReader
     * @param clazz
     * @param excludeHeader
     *            Whether to skip the header line. If true, the first line will
     *            be skipped; otherwise, not skipped.
     * @return List<T>
     */
    public static <T> List<T> parseBeans(CSVReader csvReader, Class<T> clazz, boolean excludeHeader) {
        List<T> list = new ArrayList<>();
        try {
            List<Field> fields = pickFields(clazz);
            Map<String, CsvColumnInfo> orderKey2CsvColumnInfo = pickOrderKey2CsvColumnInfo(fields);
            List<String[]> lines = csvReader.readAll();
            if (excludeHeader && !lines.isEmpty()) {
                lines.remove(0);
            }
            for (String[] line : lines) {
                T t = clazz.newInstance();
                int index = 0;
                for (Entry<String, CsvColumnInfo> entry : orderKey2CsvColumnInfo.entrySet()) {
                    CsvColumnInfo csvColumnInfo = entry.getValue();
                    Field field = csvColumnInfo.getField();
                    String value = line[index];
                    index++;
                    try {
                        if (field.getType() == Date.class) {
                            if (value.length() > 0) {
                                Date date = csvColumnInfo.getDateFormat().parse(value);
                                BeanUtils.setProperty(t, field.getName(), date);
                            }
                        } else {
                            BeanUtils.setProperty(t, field.getName(), value);
                        }
                    } catch (InvocationTargetException | ParseException e) {
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

    private static Map<String, CsvColumnInfo> pickOrderKey2CsvColumnInfo(List<Field> fields) {
        Map<String, CsvColumnInfo> map = new TreeMap<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(CsvColumn.class)) {
                CsvColumn csvColumn = field.getAnnotation(CsvColumn.class);
                CsvColumnInfo csvColumnInfo = new CsvColumnInfo();
                String name = csvColumn.name();
                if (name.equals("fieldName")) {
                    name = field.getName();
                }
                csvColumnInfo.setName(name);

                String orderKey = csvColumn.orderKey();
                if (orderKey.equals("fieldName")) {
                    orderKey = field.getName();
                }
                csvColumnInfo.setOrderKey(orderKey);

                String format = csvColumn.format();
                csvColumnInfo.setFormat(format);

                field.setAccessible(true);
                csvColumnInfo.setField(field);

                if (field.getType() == Date.class) {
                    DateFormat dateFormat = new SimpleDateFormat(format);
                    csvColumnInfo.setDateFormat(dateFormat);
                }

                if (map.containsKey(orderKey)) {
                    throw new RuntimeException("not support duplicated orderKey");
                }
                map.put(orderKey, csvColumnInfo);
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
