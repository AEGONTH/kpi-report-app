package com.adms.batch.kpireport.app;

import com.adms.batch.kpireport.service.DataImporter;
import com.adms.batch.kpireport.service.impl.KpiTargetSetupImporter;
import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class ImportTarget {

	private static Logger logger = Logger.getLogger();
	
	public static void main(String[] args) {
		try {
			String yyyyMM = args[0];
			String processDate = DateUtil.convDateToString("yyyyMMdd", DateUtil.toEndOfMonth(DateUtil.convStringToDate("yyyyMMdd", yyyyMM + "01")));
			
			String filePath = args[1];
			logger.setLogFileName(args[2]);
			
			DataImporter importer = new KpiTargetSetupImporter();
			importer.importData(filePath, processDate);
			logger.info("## Finished ##");
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
