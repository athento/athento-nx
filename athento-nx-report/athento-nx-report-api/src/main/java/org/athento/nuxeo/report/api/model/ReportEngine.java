package org.athento.nuxeo.report.api.model;

import org.athento.nuxeo.report.api.ReportException;

import java.util.Map;

/**
 * Interface of report engine.
 * 
 * @author victorsanchez
 * 
 */
public interface ReportEngine {

	/**
	 * Print a report.
	 * 
	 * @param report
	 * @param output
	 * @param printParams
	 * @return
	 * @throws ReportException
	 */
	byte[] print(Report report, OutputReport output,
			Map<String, Object> printParams) throws ReportException;

	/**
	 * Print a report given a id.
	 * 
	 * @param id
	 * @param output
	 * @param printParams
	 * @return
	 * @throws ReportException
	 */
	byte[] print(String id, OutputReport output, Map<String, Object> printParams)
			throws ReportException;

}
