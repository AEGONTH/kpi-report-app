package com.adms.batch.kpireport.app;

import com.adms.batch.kpireport.job.ImportJob;
import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class ImportTracking {
	
	private static Logger logger = Logger.getLogger();

	public static void main(String[] args) {
		try {
			String yyyyMM = args[0];
			String processDate = DateUtil.convDateToString("yyyyMMdd", DateUtil.toEndOfMonth(DateUtil.convStringToDate("yyyyMMdd", yyyyMM + "01")));
			logger.info("processDate: " + processDate);
			
			String rootPath = args[1];
			logger.info("rootPath: " + rootPath);
			
			logger.info("logPath: " + args[2]); 
			logger.setLogFileName(args[2]);
			
//			logger.info("New HH:MM campaign >> " + args[3]);
//			NewTimeFormatHelper.getInstance().setCampaignCodeWithNewFormat(args[3]);
			
			ImportJob.getInstance(processDate).importDataForKPI(rootPath);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
