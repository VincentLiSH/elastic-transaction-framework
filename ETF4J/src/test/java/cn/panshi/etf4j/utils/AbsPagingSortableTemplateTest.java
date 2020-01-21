package cn.panshi.etf4j.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AbsPagingSortableTemplateTest {
	Logger logger = Logger.getLogger(AbsPagingSortableTemplateTest.class);
	MockDao dao;

	@Before
	public void beforeTest() {
		dao = Mockito.mock(MockDao.class);
	}

	@Test
	public void testExecutePagingOrderQuery() throws Exception {

		Boolean requestOrderByAscFlag = false;
		String requestOrderByField = "age";
		String requestType = "vip";

		AbsPagingSortableTemplate<String, Integer> query = new AbsPagingSortableTemplate<String, Integer>(10,
				"createTime", true) {

			@Override
			protected String constructCriteria() {
				return " from t_user where type = '" + requestType + "' ";
			}

			@Override
			protected int countTotalByCriteria(String criteria) {
				String sql = "select count(1) " + criteria;
				return dao.countBySql(sql);
			}

			@Override
			protected boolean currentRequestOrderByAscFlag() {
				return requestOrderByAscFlag;
			}

			@Override
			protected String currentRequestOrderByField() {
				return requestOrderByField;
			}

			@Override
			protected List<Integer> doPagingAndSortQueryByCriteria(String criteria, int queryIndex, int pageSize,
					String orderBy, boolean orderByAscFlag) {
				String sql = "select age " + criteria;
				if (StringUtils.isNotBlank(orderBy)) {
					sql += " order by " + orderBy + " " + (orderByAscFlag ? "asc" : "desc");
				}

				return dao.queryBySql(sql, queryIndex, pageSize);
			}

		};

		//Mock and practice
		Mockito.when(dao.countBySql(Mockito.anyString())).thenReturn(13);
		Mockito.when(dao.queryBySql(Mockito.anyString(), Mockito.eq(0), Mockito.eq(10)))
				.thenReturn(Arrays.asList(33, 32, 31));
		Mockito.when(dao.queryBySql(Mockito.anyString(), Mockito.eq(10), Mockito.eq(10)))
				.thenReturn(Arrays.asList(30, 29, 28));
		Mockito.when(dao.queryBySql(Mockito.anyString(), Mockito.eq(20), Mockito.eq(10)))
				.thenReturn(Arrays.asList(27, 26, 25));

		//Test and assert
		PagingSearchResult<Integer> result = query.executePagingOrderQuery(1);
		logger.debug(result.getData().get(0));
		Assert.assertEquals("33", result.getData().get(0).toString());
		Assert.assertEquals(1, result.getPageNo());

		result = query.executePagingOrderQuery(2);
		logger.debug(result.getData().get(0));
		Assert.assertEquals("30", result.getData().get(0).toString());
		Assert.assertEquals(2, result.getPageNo());

		result = query.executePagingOrderQuery(3);
		logger.debug(result.getData().get(0));
		Assert.assertEquals("30", result.getData().get(0).toString());
		Assert.assertEquals(2, result.getPageNo());
	}

	class MockDao {
		public int countBySql(String sql) {
			return 0;
		}

		public List<Integer> queryBySql(String sql, int queryIndex, int pageSize) {
			return null;
		}
	}
}