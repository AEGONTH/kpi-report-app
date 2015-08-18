package com.adms.batch.kpireport.app;

import com.adms.batch.kpireport.service.ReportExporter;
import com.adms.batch.kpireport.service.impl.KpiReportExporter;
import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class ExportKpiReports {
	private static Logger logger = Logger.getLogger();

	public static void main(String[] args) {
		try {
			String yyyyMM = args[0];
			String processDate = DateUtil.convDateToString("yyyyMMdd", DateUtil.toEndOfMonth(DateUtil.convStringToDate("yyyyMMdd", yyyyMM + "01")));
			String outPath = args[1];
			logger.setLogFileName(args[2]);
			
			logger.info("### Start Export KPI Reports ###");
			ReportExporter export = new KpiReportExporter();
			export.exportExcel(outPath, processDate);
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("### Finish Export KPI Reports ###");
	}
}
