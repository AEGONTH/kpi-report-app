package com.adms.batch.kpireport.app;

import com.adms.batch.kpireport.job.ImportJob;
import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class ImportSupDsm {

	private static Logger logger = Logger.getLogger();
	
	public static void main(String[] args) {
		try {
			String yyyyMM = args[0];
			String processDate = DateUtil.convDateToString("yyyyMMdd", DateUtil.toEndOfMonth(DateUtil.convStringToDate("yyyyMMdd", yyyyMM + "01")));
			String filePath = args[1];
			logger.setLogFileName(args[2]);
			ImportJob.getInstance(processDate).importSupDsm(filePath);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
