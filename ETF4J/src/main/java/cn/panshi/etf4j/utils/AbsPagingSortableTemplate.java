package cn.panshi.etf4j.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public abstract class AbsPagingSortableTemplate<T_criteria, T_result_item> {
	private static Logger logger = Logger.getLogger(AbsPagingSortableTemplate.class);
	private int defaultPageSize = 10;

	private String defaultOrderByField;

	private boolean defaultOrderByAscFlag = true;

	public AbsPagingSortableTemplate(int defaultPageSize, String defaultOrderByField, Boolean defaultOrderByAscFlag) {
		super();
		this.defaultPageSize = (defaultPageSize <= 0) ? 10 : defaultPageSize;
		this.defaultOrderByField = defaultOrderByField;
		this.defaultOrderByAscFlag = defaultOrderByAscFlag == null ? true : defaultOrderByAscFlag;
	}

	public final PagingSearchResult<T_result_item> executePagingOrderQuery(int pageNo) {
		PagingSearchResult<T_result_item> result = new PagingSearchResult<>();

		T_criteria criteria = this.constructCriteria();

		int totalRecords = this.queryCountTotal(criteria);
		if (totalRecords == 0) {
			result.setPageNo(0);
			return result;
		}

		int queryIndex = calcQueryIndexAndSetPageNo(pageNo, totalRecords,result);

		Integer requestPageSize = this.currentRequestPageSize();
		int pageSize = requestPageSize == null ? defaultPageSize : requestPageSize.intValue();

		boolean orderByAscFlag = this.currentRequestOrderByAscFlag();
		
		String orderBy = this.currentRequestOrderByField();
		if (StringUtils.isBlank(orderBy)) {
			orderBy = defaultOrderByField;
			orderByAscFlag = defaultOrderByAscFlag;
		}

		List<T_result_item> data = this.doPagingSortQuery(criteria, queryIndex, pageSize, orderBy, orderByAscFlag);
		result.setData(data);
		return result;
	}

	abstract protected T_criteria constructCriteria();

	abstract protected int queryCountTotal(T_criteria criteria);

	protected Integer currentRequestPageSize() {
		return defaultPageSize;
	}

	abstract protected boolean currentRequestOrderByAscFlag();

	abstract protected String currentRequestOrderByField();

	abstract protected List<T_result_item> doPagingSortQuery(T_criteria criteria, int queryIndex, int pageSize,
			String orderBy, boolean orderByAscFlag);

	private int calcQueryIndexAndSetPageNo(int pageNo, int totalRecords,PagingSearchResult<T_result_item> result) {
		int queryPageNo = (pageNo <= 0) ? 1 : pageNo;
		int totalPages = totalRecords / defaultPageSize + 1;
		if (queryPageNo > totalPages) {
			logger.warn("分页请求页[" + queryPageNo + "] 大于总页数[" + totalPages + "]");
			queryPageNo = totalPages;
		}
		result.setPageNo(queryPageNo);

		int queryIndex = (queryPageNo - 1) * defaultPageSize;
		return queryIndex;
	}
}