/* 
 * MapGridResult.java
 * 
 * Copyright (c) 2009-2012 International Integrated System, Inc. 
 * All Rights Reserved.
 * 
 * Licensed Materials - Property of International Integrated System, Inc.
 * 
 * This software is confidential and proprietary information of 
 * International Integrated System, Inc. (&quot;Confidential Information&quot;).
 */
package com.iisigroup.cap.response;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.Calendar;
import com.iisigroup.cap.enums.IGridEnum;
import com.iisigroup.cap.exception.CapException;
import com.iisigroup.cap.formatter.ADDateFormatter;
import com.iisigroup.cap.formatter.ADDateTimeFormatter;
import com.iisigroup.cap.formatter.IBeanFormatter;
import com.iisigroup.cap.formatter.IFormatter;

/**
 * <pre>
 * Grid Result
 * </pre>
 * 
 * @since 2011/10/26
 * @author iristu
 * @version <ul>
 *          <li>2011/10/26,iristu,new
 *          <li>2011/03/28,sunkist,update callback
 *          </ul>
 */
@SuppressWarnings("serial")
public class MapGridResult implements
		IGridResult<MapGridResult, Map<String, Object>> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private JSONObject resultMap;

	private List<? extends Map<String, Object>> rowData;

	private String[] columns;

	private Map<String, IFormatter> dataReformatter;

	public MapGridResult() {
		resultMap = new JSONObject();
	}

	public MapGridResult(List<Map<String, Object>> rowData, int records) {
		this(rowData, records, null);
	}

	public MapGridResult(List<Map<String, Object>> rowData, int records,
			Map<String, IFormatter> dataReformatter) {
		resultMap = new JSONObject();
		setRowData(rowData);
		setRecords(records);
		setDataReformatter(dataReformatter);
	}

	/**
	 * <pre>
	 * 設定頁碼
	 * </pre>
	 * 
	 * @param page
	 *            頁碼
	 * @return this
	 */
	public MapGridResult setPage(int page) {
		resultMap.put(IGridEnum.PAGE.getCode(), page);
		return this;
	}// ;

	/**
	 * 取得頁碼
	 * 
	 * @return 頁碼
	 */
	public int getPage() {
		return (Integer) resultMap.get(IGridEnum.PAGE.getCode());
	}

	/**
	 * <pre>
	 * 設定總筆數、每頁筆數及計算總頁數
	 * </pre>
	 * 
	 * @param rowCount
	 *            總筆數
	 * @param pageRows
	 *            一頁筆數
	 * @return this
	 */
	public MapGridResult setPageCount(int rowCount, int pageRows) {
		resultMap.put(IGridEnum.TOTAL.getCode(), rowCount / pageRows
				+ (rowCount % pageRows > 0 ? 1 : 0));
		resultMap.put(IGridEnum.RECORDS.getCode(), rowCount);
		resultMap.put(IGridEnum.PAGEROWS.getCode(), pageRows);
		return this;
	}// ;

	/**
	 * 取得每頁筆數
	 * 
	 * @return 每頁筆數
	 */
	public int getPageRows() {
		return (Integer) resultMap.get(IGridEnum.PAGEROWS.getCode());
	}

	/**
	 * <pre>
	 * 設定總筆數
	 * </pre>
	 * 
	 * @param rowCount
	 *            總筆數
	 * @return this
	 */
	public MapGridResult setRecords(int rowCount) {
		resultMap.put(IGridEnum.RECORDS.getCode(), rowCount);
		return this;
	}// ;

	/**
	 * <pre>
	 * 取得總筆數
	 * </pre>
	 * 
	 * @return 總筆數
	 */
	public Integer getRecords() {
		Object o = resultMap.get(IGridEnum.RECORDS.getCode());
		return o == null ? 0 : (Integer) o;
	}// ;

	/**
	 * <pre>
	 * 設定資料行
	 * </pre>
	 * 
	 * @param rowData
	 *            資料
	 * @return this
	 */
	public MapGridResult setRowData(List<? extends Map<String, Object>> rowData) {
		this.rowData = rowData;
		return this;
	}

	@Override
	public String getResult() {
		resultMap.put(IGridEnum.PAGEROWS.getCode(), getRowDataToJSON());
		return resultMap.toString();
	}

	@Override
	public String getLogMessage() {
		StringBuffer b = new StringBuffer();
		b.append("page=").append(resultMap.get(IGridEnum.PAGE.getCode()))
				.append(",pagerow=")
				.append(resultMap.get(IGridEnum.PAGEROWS.getCode()))
				.append(",rowData=")
				.append(resultMap.get(IGridEnum.PAGEROWS.getCode()));
		return b.toString();
	}

	@Override
	public void add(IResult result) {
		JSONObject json = JSONObject.fromObject(result);
		resultMap.putAll(json);
	}

	public MapGridResult addReformatData(String key, IFormatter formatter)
			throws CapException {
		if (dataReformatter == null) {
			dataReformatter = new HashMap<String, IFormatter>();
		}
		dataReformatter.put(key, formatter);
		return this;
	}

	public void setColumns(String[] columns) {
		this.columns = columns;
	}

	public List<? extends Map<String, Object>> getRowData() {
		return this.rowData;
	}

	public Map<String, IFormatter> getdataReformatter() {
		return this.dataReformatter;
	}

	private JSONArray getRowDataToJSON() {
		JSONArray rows = new JSONArray();
		Map<String, Object> row = new HashMap<String, Object>();
		if (rowData != null && !rowData.isEmpty()) {
			for (Map<String, Object> data : rowData) {
				try {
					row.put(IGridEnum.CELL.getCode(), dataToJsonString(data));
				} catch (CapException e) {
					e.printStackTrace();
				}
				rows.add(row);
			}
		}
		return rows;
	}// ;

	/** column split regularre char **/
	private static String SPLIT = "\\|";

	protected String dataToJsonString(Map<String, Object> data)
			throws CapException {
		JSONArray row = new JSONArray();
		for (String str : columns) {
			Object val = null;
			try {
				try {
					String[] s = str.split(SPLIT);
					val = s.length == 1 ? data.get(s[0]) : data.get(s[1]);
					str = s[0];
				} catch (Exception e) {
					val = "";
				}
				if (dataReformatter != null && dataReformatter.containsKey(str)) {
					IFormatter callback = dataReformatter.get(str);
					if (callback instanceof IBeanFormatter) {
						val = callback.reformat(data);
					} else {
						val = callback.reformat(val);
					}
				} else if (val instanceof Timestamp) {
					val = new ADDateTimeFormatter().reformat(val);
				} else if (val instanceof Date || val instanceof Calendar) {
					val = new ADDateFormatter().reformat(val);
				}
				row.add(String.valueOf(val));
			} catch (Exception e) {
				throw new CapException(e.getMessage(), e, getClass());
			}
		}
		return row.toString();
	}

	/**
	 * set DataReformatter
	 * 
	 * @param dataReformatter
	 *            Map<String, IFormatter>
	 */
	public void setDataReformatter(Map<String, IFormatter> dataReformatter) {
		this.dataReformatter = dataReformatter;
	}

	// -----------------------------------
	// Order by support
	// -----------------------------------
	private Map<String, Boolean> orderBy;

	public boolean hasOrderBy() {
		return !(orderBy == null || orderBy.isEmpty());
	}

	/**
	 * Specify that results must be ordered by the passed column Null by
	 * default. 預設為升羃排序
	 * 
	 * @param orderBy
	 *            the order by
	 * @return SearchSetting
	 */
	public MapGridResult addOrderBy(String orderBy) {
		if (this.orderBy == null) {
			this.orderBy = new LinkedHashMap<String, Boolean>();
		}
		this.orderBy.put(orderBy, false);
		return this;
	}

	/**
	 * Specify that results must be ordered by the passed column Null by
	 * default.
	 * 
	 * @param orderBy
	 *            orderBy
	 * @param orderDesc
	 *            是否要降羃排序
	 * @return SearchSetting
	 */
	public MapGridResult addOrderBy(String orderBy, boolean orderDesc) {
		if (this.orderBy == null) {
			this.orderBy = new LinkedHashMap<String, Boolean>();
		}
		this.orderBy.put(orderBy, orderDesc);
		return this;
	}

	public MapGridResult setOrderBy(Map<String, Boolean> orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public Map<String, Boolean> getOrderBy() {
		return this.orderBy;
	}

	@Override
	public String getContextType() {
		return "text/plain;charset=UTF-8";
	}

	@Override
	public String getEncoding() {
		return CharEncoding.UTF_8;
	}

	@Override
	public Map<String, IFormatter> getDataReformatter() {
		return this.dataReformatter;
	}

}
