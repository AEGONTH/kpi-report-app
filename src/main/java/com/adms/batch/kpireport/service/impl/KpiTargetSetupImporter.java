package com.adms.batch.kpireport.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.adms.batch.kpireport.enums.EFileFormat;
import com.adms.batch.kpireport.service.DataImporter;
import com.adms.batch.kpireport.util.AppConfig;
import com.adms.entity.Campaign;
import com.adms.entity.KpiCategorySetup;
import com.adms.entity.Tsr;
import com.adms.imex.excelformat.DataHolder;
import com.adms.imex.excelformat.ExcelFormat;
import com.adms.kpireport.service.CampaignService;
import com.adms.kpireport.service.KpiCategorySetupService;
import com.adms.kpireport.service.TsrService;
import com.adms.utils.Logger;

public class KpiTargetSetupImporter implements DataImporter {
	
	private static Logger logger = Logger.getLogger();
	
	private final String DSM = "DSM";
	private final String SUP = "SUP";
	private final String TSR = "TSR";
	private final String NEW_TSR = "NEW TSR";
	
	private final String USER_LOGIN = "KPI Target Importer";

	private KpiCategorySetupService kpiCategorySetupService = (KpiCategorySetupService) AppConfig.getInstance().getBean("kpiCategorySetupService");
	
	private TsrService tsrService = (TsrService) AppConfig.getInstance().getBean("tsrService");
	
	private CampaignService campaignService = (CampaignService) AppConfig.getInstance().getBean("campaignService");
	
	@Override
	public void importData(String path) throws Exception {
		
	}

	@Override
	public void importData(String path, String processDate) throws Exception {
		InputStream is = null;
		InputStream fileFormatStream = null;
		
		try {
//			delete old kpi category
			clearDataByDate(processDate);
			
			fileFormatStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(EFileFormat.KPI_TARGET_FORMAT.getValue());
			ExcelFormat ef = new ExcelFormat(fileFormatStream);
			
			is = new FileInputStream(path);
			DataHolder wb = ef.readExcel(is);
			
			for(String sheetName : wb.getKeyList()) {
				try {
					logger.info("Sheet name: " + sheetName);
					DataHolder sheet = wb.get(sheetName);
					
					if(sheetName.equalsIgnoreCase(NEW_TSR)) {
						logicImportNewTSRFloorDate(sheet);
					} else {
						logicKpiSetup(sheetName.equalsIgnoreCase(DSM) ? DSM : SUP, sheet);
					}
					
				} catch(Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {is.close();} catch (IOException e) {}
			try {fileFormatStream.close();} catch (IOException e) {}
			logger.info("########## Finish ##########");
		}
	}
	
	private void clearDataByDate(String processDate) throws Exception {
//		String hql = "from KpiCategorySetup d "
//				+ " where 1 = 1"
//				+ " and CONVERT(nvarchar(6), d.effectiveDate, 112) = ?";
//		List<KpiCategorySetup> deletes = kpiCategorySetupService.findByHql(hql, processDate.substring(0, 6));
//		for(KpiCategorySetup del : deletes) {
//			kpiCategorySetupService.delete(del);
//		}
		
		String hql = "delete from KpiCategorySetup where CONVERT(nvarchar(6), effectiveDate, 112) = ? ";
		kpiCategorySetupService.deleteByHql(hql, processDate.substring(0, 6));
	}
	
	private void logicKpiSetup(String level, DataHolder sheet) throws Exception {
		logger.info("### " + level + " ###");
		int loop = 999;
		boolean isTsrLevel = false;
		
		Campaign campaign = null;
		String listlotCode = null;
		String levelCode = "";
		String targetList = "";
		
//		<!-- prepare -->
		if(level.equalsIgnoreCase(DSM)) {
			loop = 4;
			levelCode = "dsmCode";
			targetList = "dsmTargetList";
		} else if(level.equalsIgnoreCase(SUP)) {
			loop = 5;
			levelCode = "supCode";
			targetList = "supTargetList";
			campaign = campaignService.find(new Campaign(sheet.get("campaignCode").getStringValue())).get(0);
			listlotCode = sheet.get("listLotCode") == null ? null : sheet.get("listLotCode").getStringValue();
		} else if(level.equalsIgnoreCase(TSR)) {
			isTsrLevel = true;
			loop = 4;
			levelCode = "";
			targetList = "tsrTargetList";
			campaign = campaignService.find(new Campaign(sheet.get("campaignCode").getStringValue())).get(0);
			listlotCode = sheet.get("listLotCode") == null ? null : sheet.get("listLotCode").getStringValue();
		}

//		<!-- setup -->
		Date effectiveDate = (Date) sheet.get("effectiveDate").getValue();
		Date endDate = (Date) sheet.get("endDate").getValue();
		
		for(DataHolder data : sheet.getDataList(targetList)) {
			String tsrCode = "";
			Tsr tsr = null;
			if(!isTsrLevel) {
				try {
					tsrCode = data.get(levelCode).getStringValue();
					if(StringUtils.isBlank(tsrCode)) continue;
					tsr = tsrService.find(new Tsr(tsrCode)).get(0);
				} catch(Exception e) {
					throw new Exception("not found tsr data for: " + tsrCode);
				}
			}
			
			for(int i = 1; i <= loop; i++) {
				KpiCategorySetup kpiSetup = new KpiCategorySetup();
				kpiSetup.setTsrLevel(level);
				kpiSetup.setTsr(tsr);
				
				kpiSetup.setCampaign(campaign);
				kpiSetup.setListLotCode(listlotCode);
				
				kpiSetup.setEffectiveDate(new java.sql.Date(effectiveDate.getTime()));
				kpiSetup.setEndDate(new java.sql.Date(endDate.getTime()));
				
				String category = "";
				BigDecimal targetCat = null;
				BigDecimal weightCat = null;
				
				if(isTsrLevel) {
					DataHolder tsrCat = sheet.getDataList("tsrCat").get(0);
					category = tsrCat.get("tsrCat" + i).getStringValue();
				} else {
					category = sheet.get("cat" + i).getStringValue();
				}
				targetCat = data.get("targetCat" + i).getDecimalValue() == null ? new BigDecimal(0) : data.get("targetCat" + i).getDecimalValue().setScale(2, BigDecimal.ROUND_HALF_UP);
				weightCat = data.get("weightCat" + i).getDecimalValue() == null ? new BigDecimal(0) : data.get("weightCat" + i).getDecimalValue().setScale(2, BigDecimal.ROUND_HALF_UP);
				
				kpiSetup.setCategory(category);
				kpiSetup.setTarget(targetCat);
				kpiSetup.setWeight(weightCat);
				
				kpiCategorySetupService.add(kpiSetup, USER_LOGIN);
			}
		}
		
		if(level.equalsIgnoreCase(SUP)) logicKpiSetup(TSR, sheet);
	}

	private void logicImportNewTSRFloorDate(DataHolder sheetHolder) throws Exception {
		logger.info("### Update New Tsr on Floor Date ###");
		List<DataHolder> dataList = sheetHolder.getDataList("newTsrList");
		if(dataList == null) return;
		int count = 0;
		for(DataHolder data : dataList) {
			String tsrCode = data.get("tsrCode") != null ? data.get("tsrCode").getStringValue() : null;
			Date floorDate = (Date) data.get("floorDate").getValue();
			
			if(!StringUtils.isBlank(tsrCode)) {
				DetachedCriteria criteria = DetachedCriteria.forClass(Tsr.class);
				criteria.add(Restrictions.eq("tsrCode", tsrCode));
				criteria.addOrder(Order.desc("id"));
				
				List<Tsr> list = tsrService.findByCriteria(criteria);
				if(!list.isEmpty()) {
					Tsr tsr = list.get(0);
					tsrService.update(tsr.setFloorDate(floorDate), USER_LOGIN);
					count++;
				}
			}
		}
		logger.info("## Total Updated: " + count + " records");
	}
}
