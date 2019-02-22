package org.athento.nuxeo.report.plugin.datasource;

import org.athento.nuxeo.report.api.model.BasicReportData;
import org.athento.nuxeo.report.api.model.DefaultDataSource;

import java.util.Collection;
import java.util.List;

/**
 * Bean list datasource.
 * 
 * @author victorsanchez
 * 
 */
public class BeanListDatasource extends DefaultDataSource<BasicReportData> {

	/**
	 * Constructor.
	 * 
	 * @param list
	 */
	public BeanListDatasource(List<BasicReportData> list) {
		super(list);
	}

	/**
	 * Get values.
	 */
	@Override
	public Collection<BasicReportData> getValues() {
		return this.values;
	}

}
