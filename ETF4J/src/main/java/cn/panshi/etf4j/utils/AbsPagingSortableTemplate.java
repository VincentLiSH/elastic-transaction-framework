package cn.panshi.etf4j.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @param <T_criteria> 查询条件泛型
 * @param <T_result_item> 查询结果中的集合元素的泛型
 */
public abstract class AbsPagingSortableTemplate<T_criteria, T_result_item> {
	private static Logger logger = Logger.getLogger(AbsPagingSortableTemplate.class);
	private int defaultPageSize = 10;

	private String defaultOrderByField;

	private boolean defaultOrderByAscFlag = true;

	/**
	 * @param defaultPageSize 默认pageSize
	 * @param defaultOrderByField 默认排序字段
	 * @param defaultOrderByAscFlag 默认排序标记
	 */
	public AbsPagingSortableTemplate(int defaultPageSize, String defaultOrderByField, Boolean defaultOrderByAscFlag) {
		super();
		this.defaultPageSize = (defaultPageSize <= 0) ? 10 : defaultPageSize;
		this.defaultOrderByField = defaultOrderByField;
		this.defaultOrderByAscFlag = defaultOrderByAscFlag == null ? true : defaultOrderByAscFlag;
	}

	/**
	 * memo:分页查询逻辑入口
	 * @param pageNo 要查询的页号
	 * @return PagingSearchResult：totalCount总记录数，data当前页数据集合，pageNo当前页号
	 */
	public final PagingSearchResult<T_result_item> executePagingOrderQuery(int pageNo) {
		PagingSearchResult<T_result_item> result = new PagingSearchResult<>();

		T_criteria criteria = this.constructCriteria();

		int totalRecords = this.countTotalByCriteria(criteria);
		result.setTotalCount(totalRecords);
		if (totalRecords == 0) {
			result.setPageNo(0);
			return result;
		}

		int queryIndex = calcQueryIndexAndSetPageNo(pageNo, totalRecords, result);

		Integer requestPageSize = this.currentRequestPageSize();
		int pageSize = (requestPageSize == null || requestPageSize <= 0) ? defaultPageSize : requestPageSize.intValue();

		boolean orderByAscFlag = this.currentRequestOrderByAscFlag();

		String orderBy = this.currentRequestOrderByField();
		if (StringUtils.isBlank(orderBy)) {
			orderBy = defaultOrderByField;
			orderByAscFlag = defaultOrderByAscFlag;
		}

		List<T_result_item> data = this.doPagingAndSortQueryByCriteria(criteria, queryIndex, pageSize, orderBy,
				orderByAscFlag);
		result.setData(data);
		return result;
	}

	/**
	 * memo:供子类扩展 构造查询条件对象，例如查询表单值对象QueryFormVo或者Hibernate DetachedCriterial
	 */
	abstract protected T_criteria constructCriteria();

	/**
	 * memo:供子类扩展 查询总记录数
	 * @param criteria 查询条件
	 */
	abstract protected int countTotalByCriteria(T_criteria criteria);

	/**
	 * memo:供子类扩展 获取当前分页请求的pageSize；子类可以不override 默认值为defaultPageSize
	 */
	protected Integer currentRequestPageSize() {
		return defaultPageSize;
	}

	/**
	 * memo:供子类扩展 获取当前分页请求的排序标记
	 * @return true升序 false降序
	 */
	abstract protected boolean currentRequestOrderByAscFlag();

	/**
	 * memo:供子类扩展 获取当前分页查询的排序字段
	 */
	abstract protected String currentRequestOrderByField();

	/**
	 * memo:供子类扩展 做分页查询
	 * @param criteria 查询条件model
	 * @param queryIndex 分页查询起始index
	 * @param pageSize 分页大小
	 * @param orderBy 排序字段
	 * @param orderByAscFlag 升序标记 true升序 false降序
	 */
	abstract protected List<T_result_item> doPagingAndSortQueryByCriteria(T_criteria criteria, int queryIndex,
			int pageSize, String orderBy, boolean orderByAscFlag);

	private int calcQueryIndexAndSetPageNo(int pageNo, int totalRecords, PagingSearchResult<T_result_item> result) {
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