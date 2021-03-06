package com.adms.batch.kpireport.service.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import com.adms.batch.kpireport.enums.EFileFormat;
import com.adms.batch.kpireport.service.DataImporter;
import com.adms.batch.kpireport.util.AppConfig;
import com.adms.batch.kpireport.util.NewTimeFormatHelper;
import com.adms.entity.ListLot;
import com.adms.entity.Tsr;
import com.adms.entity.TsrTracking;
import com.adms.imex.excelformat.DataHolder;
import com.adms.imex.excelformat.ExcelFormat;
import com.adms.kpireport.service.ListLotService;
import com.adms.kpireport.service.TsrService;
import com.adms.kpireport.service.TsrTrackingService;
import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class TsrTrackingImporter implements DataImporter {
	
	private static Logger logger = Logger.getLogger();
	
	private final String LOGIN_USER = "TSR_TRACKING_IMPORTER";
	
	private TsrService tsrService = (TsrService) AppConfig.getInstance().getBean("tsrService");
	private TsrTrackingService tsrTrackingService = (TsrTrackingService) AppConfig.getInstance().getBean("tsrTrackingService");
	private ListLotService listLotService = (ListLotService) AppConfig.getInstance().getBean("listLotService");

	private final List<String> titles = Arrays.asList(new String[]{"���", "�.�.", "�ҧ", "��ҷ��", "��.", "�ҧ���"});
	
	@Override
	public void importData(final String path) throws Exception {
		InputStream fileformatStream = null;
		InputStream wbStream = null;
		ExcelFormat ef = null;
		
		try {
			fileformatStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.getFileFormatPath(path));
			wbStream = new FileInputStream(path);
			
			ef = new ExcelFormat(fileformatStream);
			
			DataHolder wbHolder = null;
			wbHolder = ef.readExcel(wbStream);
			
			List<String> sheetNames = wbHolder.getKeyList();
			if(sheetNames.size() == 0) {
				return;
			}
			
			boolean flagTimeFormat = isNewTimeFormat(path);
			
			for(String sheetName : sheetNames) {
				logic(wbHolder.get(sheetName), sheetName, flagTimeFormat);
			}
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try { fileformatStream.close(); } catch(Exception e) {}
			try { wbStream.close(); } catch(Exception e) {}
		}
	}
	
	private boolean isNewTimeFormat(String dir) {
		boolean flag = false;
		String criteria = "hh.mm";
		
		InputStream is = null;
		Workbook wb = null;
		try {
			is = new FileInputStream(dir);
			wb = WorkbookFactory.create(is);
			Sheet sheet = wb.getSheetAt(0);
			Cell cell = sheet.getRow(7).getCell(CellReference.convertColStringToIndex("AC"), Row.CREATE_NULL_AS_BLANK);
			String val = cell.getStringCellValue();
			
			if(val.contains(criteria)) {
				flag = true;
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {wb.close();}catch(Exception e) {}
		}
		
		return flag;
	}
	
	private BigDecimal getTimeBase100(DataHolder obj, boolean flagFormat) {
		BigDecimal result = null;
		if(obj != null) {
			BigDecimal time = obj.getDecimalValue();
			if(flagFormat) {
				Double mm = time.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() % 1 * 100;
				BigDecimal base100 = new BigDecimal(mm / 60 * 100).setScale(0, BigDecimal.ROUND_FLOOR).divide(new BigDecimal(100));
				result = time.setScale(0, BigDecimal.ROUND_FLOOR).add(base100);
			} else {
				result = time.setScale(14, BigDecimal.ROUND_HALF_UP);
			}
		} else {
			result = BigDecimal.ZERO;
		}
		return result;
	}
	
	private void logic(DataHolder sheetHolder, String sheetName, boolean isNewTimeFormat) {
		try {
			int countImported = 0;
			if(sheetHolder.get("period") == null || (sheetHolder.get("period") != null && sheetHolder.get("period").getStringValue().isEmpty())) {
				logger.error("SKIP Sheet: " + sheetName + ", due to cannot get period value");
				return;
			}
			String period = sheetHolder.get("period").getStringValue().trim().substring(0, 10);
			String listLotName = sheetHolder.get("listLotName") == null ? null : sheetHolder.get("listLotName").getStringValue();
			List<DataHolder> datas = sheetHolder.getDataList("tsrTrackingList");
			String hoursFormat = sheetHolder.get("hoursFormat").getStringValue();
			
			//TODO Temporary setup
			NewTimeFormatHelper.getInstance().setThisFileIsNewTimeFormat(hoursFormat.contains("hh.mm"));
			
//			<!-- getting Listlot -->
			if(listLotName == null) {
				throw new Exception("Cannot get ListLotname on sheet: " + sheetName);
			}
			if(listLotName.contains(",")) { logger.info("SKIP>> sheetName:" + sheetName + " | listLotName: " + listLotName + " | period: " + period); return;}
			logger.info("## Do sheet: " + sheetName + " | listLotName: " + listLotName + " | period: " + period); 
			
			String listLotCode = getListLotCode(listLotName);
			if(StringUtils.isBlank(listLotCode)) throw new Exception("Cannot get listlot >> " + listLotName + " | period: " + period);

//			logger.info("# Retrived: " + datas.size() + " records");
			
			if(datas.isEmpty()) {
				return;
			}
			
			for(DataHolder data : datas) {
				try {
//					<!-- Checking work day -->
					Integer workday = Integer.valueOf(data.get("workday").getDecimalValue() != null 
							? Integer.valueOf(data.get("workday").getDecimalValue().intValue()) : new Integer(0));
					
					if(workday == 0 || workday > 1) {
//						logger.info("## Skip >> workday: " + workday + " | sheetName: " + sheetName);
						continue;
					}
					
//					<!-- getting data -->
					Tsr tsr = null;
					String fullName = "";
					String tsrCode = data.get("tsrCode").getStringValue();
					String tsrName = data.get("tsrName").getStringValue();
					
					if(tsrCode.isEmpty() || !tsrCode.isEmpty() && tsrCode.length() != 6) {
						if(StringUtils.isEmpty(tsrName)) continue;
//						<!-- get Tsr by name -->
//						logger.info("tsrCode '" + tsrCode + "' can't be used. Find by Name instead.");
						fullName = removeTitle(tsrName.replaceAll("�", "").replaceAll("  ", " "));
						tsr = getTsrByName(fullName);
					} else {
						tsr = getTsrByTsrCode(tsrCode);
						if(tsr == null) {
							fullName = removeTitle(tsrName.replaceAll("�", "").replaceAll("  ", " "));
							tsr = getTsrByName(fullName);
						} else {
							fullName = tsr.getFullName();
						}
					}

					if(tsr == null) logger.info("Not found TSR: '" + fullName + "' | listLot: " + listLotCode);
					
					Integer listUsed = data.get("listUsed") != null ? data.get("listUsed").getIntValue() : new Integer(0);
					Integer complete = data.get("complete") != null ? data.get("complete").getIntValue() : new Integer(0);
//					listUsed, complete
					
//					hours = data.get("hours") != null ? data.get("hours").getDecimalValue().setScale(14, BigDecimal.ROUND_HALF_UP) : new BigDecimal(0);
					BigDecimal hours = getTimeBase100(data.get("hours"), isNewTimeFormat);
//					BigDecimal talkTime = new BigDecimal(data.get("totalTalkTime").getStringValue()).setScale(14, BigDecimal.ROUND_HALF_UP);
					BigDecimal talkTime = getTimeBase100(data.get("totalTalkTime"), isNewTimeFormat);
					
					Integer newUsed = data.get("newUsed") != null ? data.get("newUsed").getIntValue() : 0;
					Integer totalPolicy = data.get("totalPolicy") != null ? data.get("totalPolicy").getIntValue() : 0;
					
					Date trackingDate = null;
					try {
						trackingDate = DateUtil.convStringToDate(period);
					} catch(Exception e) {
						logger.info("Cannot convert String to date > " + period + " | with pattern > " + DateUtil.getDefaultDatePattern());
						logger.info("Try another pattern > dd-MM-yyyy");
						trackingDate = DateUtil.convStringToDate("dd-MM-yyyy", period);
					}
					
					countImported += saveTsrTracking(tsr, fullName, trackingDate, listLotCode, workday, listUsed, complete, hours, talkTime, newUsed, totalPolicy);
				} catch(Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			logger.info("Imported total: " + countImported);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private int saveTsrTracking(Tsr tsr, String tsrName, Date trackingDate, String listLotCode, Integer workday, Integer listUsed, Integer complete, BigDecimal hours, BigDecimal talkTime, Integer newUsed, Integer totalPolicy) throws Exception {
		DetachedCriteria isExisted = DetachedCriteria.forClass(TsrTracking.class);
		isExisted.add(Restrictions.eq("trackingDate", trackingDate));
		isExisted.add(Restrictions.eq("listLot.listLotCode", listLotCode));
		
		isExisted.add(Restrictions.disjunction()
				.add(Restrictions.eq("tsrName", tsrName))
				.add(Restrictions.eq("tsr.tsrCode", tsr == null ? "" : tsr.getTsrCode())));

		List<TsrTracking> list = tsrTrackingService.findByCriteria(isExisted);
		ListLot listLot = listLotService.find(new ListLot(listLotCode)).get(0);
		
		TsrTracking tsrTracking = null;
		
		BigDecimal hoursBase100 = NewTimeFormatHelper.getInstance().getTimeBase100(listLot.getCampaign().getCampaignCode(), hours).setScale(13, BigDecimal.ROUND_HALF_UP);
		BigDecimal talkTimeBase100 = NewTimeFormatHelper.getInstance().getTimeBase100(listLot.getCampaign().getCampaignCode(), talkTime).setScale(13, BigDecimal.ROUND_HALF_UP);
		
		if(list.isEmpty()) {
			tsrTracking = new TsrTracking();
			tsrTracking.setTrackingDate(trackingDate);
			tsrTracking.setTsr(tsr);
			tsrTracking.setTsrName(tsrName);
			tsrTracking.setWorkDays(workday);
			tsrTracking.setListUsed(listUsed);
			tsrTracking.setComplete(complete);
			tsrTracking.setListLot(listLot);
			tsrTracking.setWorkHours(hoursBase100);
			tsrTracking.setTotalTalkTime(talkTimeBase100);
			tsrTracking.setNewUsed(newUsed);
			tsrTracking.setTotalPolicy(totalPolicy);
			
			tsrTrackingService.add(tsrTracking, LOGIN_USER);
			return 1;
		} else if(list.size() == 1) {
			boolean isUpdate = false;
			
			tsrTracking = list.get(0);
			if(tsrTracking.getTsr() == null && tsr != null) {
				tsrTracking.setTsr(tsr);
				isUpdate = true;
			}
			if(tsrTracking.getListUsed() == null ? !(new Integer(0)).equals(listUsed) : !tsrTracking.getListUsed().equals(listUsed)) {
				tsrTracking.setListUsed(listUsed);
				isUpdate = true;
			}
			if(!tsrTracking.getComplete().equals(complete)) {
				tsrTracking.setComplete(complete);
				isUpdate = true;
			}
			if(!tsrTracking.getWorkHours().equals(hoursBase100)) {
				tsrTracking.setWorkHours(hoursBase100);
				isUpdate = true;
			}
			if(!tsrTracking.getTotalTalkTime().equals(talkTimeBase100)) {
				tsrTracking.setTotalTalkTime(talkTimeBase100);
				isUpdate = true;
			}
			if(!tsrTracking.getNewUsed().equals(newUsed)) {
				tsrTracking.setNewUsed(newUsed);
				isUpdate = true;
			}
			if(!tsrTracking.getTotalPolicy().equals(totalPolicy)) {
				tsrTracking.setTotalPolicy(totalPolicy);
				isUpdate = true;
			}
			
			if(isUpdate) {
				tsrTrackingService.update(tsrTracking, LOGIN_USER);
			}
			return 1;
		} else {
			throw new Exception("Found tsr tracking more than 1 records b >> " + (tsr == null ? "tsrName: " + tsrName : "tsrCode: " + tsr.getTsrCode()) + " | trackingDate: " + trackingDate + " | Listlot: " + listLotCode);
		}
	}
	
	private Tsr getTsrByTsrCode(String tsrCode) {
		DetachedCriteria criteria = DetachedCriteria.forClass(Tsr.class);
		criteria.add(Restrictions.eq("tsrCode", tsrCode));
		try {
			List<Tsr> list = tsrService.findByCriteria(criteria);
			if(list != null && list.size() == 1) {
				return list.get(0);
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	private Tsr getTsrByName(String tsrName) {
		if(StringUtils.isBlank(tsrName)) {logger.info("## TSR Name is Blank"); return null;}
		
		DetachedCriteria criteria = DetachedCriteria.forClass(Tsr.class);
//		criteria.add(Restrictions.eq("fullName", tsrName));
		criteria.add(Restrictions.like("fullName", tsrName, MatchMode.ANYWHERE));
		criteria.add(Restrictions.isNull("resignDate"));
		List<Tsr> list = null;
		
		try {
			list = tsrService.findByCriteria(criteria);
//			<!-- if not found, find without resign date is null -->
			if(list.isEmpty()) {
				criteria = DetachedCriteria.forClass(Tsr.class);
//				criteria.add(Restrictions.eq("fullName", tsrName));
				criteria.add(Restrictions.like("fullName", tsrName, MatchMode.ANYWHERE));
				list = tsrService.findByCriteria(criteria);
			}
			
			if(!list.isEmpty() && list.size() == 1) {
				return list.get(0);
			} else if(!list.isEmpty() && list.size() > 1) {
				Tsr temp = null;
				for(Tsr tsr : list) {
					if(temp == null) temp = tsr;
					
					if(temp.getEffectiveDate().compareTo(tsr.getEffectiveDate()) < 0) {
						temp = tsr;
					}
				}
				return temp;
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return null;
	}

	private String removeTitle(String val) {
		if(val.startsWith(" ")) val = val.substring(1, val.length());
		for(String s : titles) {
			if(val.contains(s)) {
				return val.replace(s, "").trim();
			}
		}
		return val;
	}
	
	private String getListLotCode(String val) {
		String result = "";
		if(!StringUtils.isEmpty(val)) {
			int count = 0;
			
//			<!-- Check -->
			for(int i = 0; i < val.length(); i++) {
				if(val.charAt(i) == '(') {
					count++;
				}
			}
			
//			<!-- process -->
			if(count == 1) {
				return val.substring(val.indexOf("(") + 1, val.indexOf(")")).trim();
			} else if(count == 2) {
				return val.substring(val.indexOf("(", val.indexOf("(") + 1) + 1, val.length() - 1).trim();
			} else {
				logger.info("Cannot find Keycode");
			}
		}
		return result;
	}
	
	private String getFileFormatPath(String fileName) throws Exception {
		if(fileName.contains("OTO")) {
			return EFileFormat.TSR_TRACKING_OTO.getValue();
		} else if(fileName.contains("TELE")) {
			return EFileFormat.TSR_TRACKING_TELE.getValue();
		} else if(fileName.contains("3RD")) {
			return EFileFormat.TSR_TRACKING_3RD.getValue();
		} else {
			throw new Exception("Cannot find file format for this file. " + "\'" + fileName + "\'");
		}
	}

	@Override
	public void importData(String path, String processDate) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
