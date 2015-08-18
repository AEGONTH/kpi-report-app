package com.adms.batch.kpireport.app;

import com.adms.batch.kpireport.service.DataImporter;
import com.adms.batch.kpireport.service.impl.KpiResultsImporter;
import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class ImportKpiResults {
	
	private static Logger logger = Logger.getLogger();

	public static void main(String[] args) {
		try {
			String yyyyMM = args[0];
			String processDate = DateUtil.convDateToString("yyyyMMdd", DateUtil.toEndOfMonth(DateUtil.convStringToDate("yyyyMMdd", yyyyMM + "01")));
			logger.setLogFileName(args[1]);
			
			DataImporter importer = new KpiResultsImporter();
			importer.importData(null, processDate);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
}
