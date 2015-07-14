package com.adms.batch.kpireport.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class NewTimeFormatHelper {

	private static NewTimeFormatHelper instance;
	
	private String campaignCodeWithNewFormat = "";
	private Boolean flag;
	
	public static NewTimeFormatHelper getInstance() {
		if(instance == null) {
			instance = new NewTimeFormatHelper();
		}
		return instance;
	}
	
	public void setCampaignCodeWithNewFormat(String campaignCodeWithNewFormat) {
		this.campaignCodeWithNewFormat = campaignCodeWithNewFormat;
	}
	
	public BigDecimal getTimeBase100(String campaignCode, BigDecimal time) throws Exception {
		BigDecimal result = new BigDecimal(0);
		
		if(StringUtils.isBlank(campaignCode) && flag == null) {
			throw new Exception("CampaignCode or Flag is required!!");
		}
		
		if(time == null || time.equals(new BigDecimal(0))) return result;
		
		
		if((!StringUtils.isBlank(campaignCode) && campaignCodeWithNewFormat.contains(campaignCode))
				|| (flag != null && flag == true)) {
			result = new BigDecimal(time.intValue()).add(new BigDecimal(time.doubleValue() % 1d / 60d * 100d));
		} else {
			result = new BigDecimal(time.doubleValue());
		}
		
		return result;
	}
	
	public void setThisFileIsNewTimeFormat(boolean flag) {
		this.flag = flag;
	}
	
	public void validateTimeFormatByTableHeader(String dir) {
		Workbook wb = null;
		InputStream is = null;
		try {
			is = new FileInputStream(dir);
			wb = WorkbookFactory.create(is);
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try { is.close();} catch(Exception e) {}
			try { wb.close();} catch(Exception e) {}
		}
	}
}
