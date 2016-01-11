package com.adms.batch.kpireport.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.adms.batch.kpireport.service.DataImporter;
import com.adms.batch.kpireport.service.DataImporterFactory;
import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class ImportTracking {
	
	private static Logger logger = Logger.getLogger();
	private static List<String> dirs;

	public static void main(String[] args) {
		try {
			String yyyyMM = args[0];
			String processDate = DateUtil.convDateToString("yyyyMMdd", DateUtil.toEndOfMonth(DateUtil.convStringToDate("yyyyMMdd", yyyyMM + "01")));
			logger.info("processDate: " + processDate);
			String rootPath = args[1];
			logger.info("rootPath: " + rootPath);
			logger.info("logPath: " + args[2]); 
			logger.setLogFileName(args[2]);
			
			logger.info("#### Start Import Data for KPI");
			if(StringUtils.isBlank(rootPath)) return;
			logicImportKpi(rootPath
					, new String[]{"TsrTracking", "TSRTracking", "TSRTRA"}
					, new String[]{"CTD", "MTD", "DAI_ALL", "QA_Report", "QC_Reconfirm", "SalesReportByRecords", "archive"});
			logger.info("#### Finish Import Data for KPI");
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private static void logicImportKpi(String root, String[] require, String[] notIn) {
		logger.info("## Filter > " + Arrays.toString(require) + " | and not > " + Arrays.toString(notIn));
		dirs = new ArrayList<>();
		getExcelByName(root, require, notIn);
		
		for(String dir : dirs) {
			logger.info("# do: " + dir);
			DataImporter importer = null;
			try {
				importer = DataImporterFactory.getDataImporter(dir);
				importer.importData(dir);
			} catch(Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				
			}
		}
	}
	
	private static void getExcelByName(String rootPath, String[] containNames, String[] notInNames) {
		File file = new File(rootPath);
		if(file.isDirectory() && (!file.getName().contains("archive") || !file.getName().contains("zipfiles"))) {
			for(File sub : file.listFiles()) {
				getExcelByName(sub.getAbsolutePath(), containNames, notInNames);
			}
		} else {
			for(String name : containNames) {
				if(file.getName().contains(name)) {
					if(file.getName().toLowerCase().endsWith(".xls") || file.getName().toLowerCase().endsWith(".xlsx")) {
						boolean flag = true;
						if(notInNames != null && notInNames.length > 0) {
							for(String not : notInNames) {
								if(file.getName().contains(not)) {
									flag = false;
									break;
								}
							}
						}
						if(flag) addToDirs(file.getAbsolutePath());
						
					}
				}
			}
		}
	}
	
	private static void addToDirs(String dir) {
		if(dirs == null) {
			dirs = new ArrayList<String>();
		}
		dirs.add(dir);
	}
}
