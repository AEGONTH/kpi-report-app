package com.adms.batch.kpireport.app;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.adms.batch.kpireport.util.AppConfig;
import com.adms.entity.Tsr;
import com.adms.entity.TsrTracking;
import com.adms.kpireport.service.TsrService;
import com.adms.kpireport.service.TsrTrackingService;
import com.adms.utils.Logger;

public class TsrCodeSeeker {

	private static Logger log = Logger.getLogger();
	
	private TsrTrackingService tsrTrackingService = (TsrTrackingService) AppConfig.getInstance().getBean("tsrTrackingService");
	private TsrService tsrService = (TsrService) AppConfig.getInstance().getBean("tsrService");
	
	public static void main(String[] args) {
		try {
			log.setLogFileName(args[0]);
			
			log.info("###########################################################################");
			log.info("################################ Start ####################################");
			Map<String, String> tsrCodeMap = new HashMap<>();
			
			TsrCodeSeeker app = new TsrCodeSeeker();
			List<TsrTracking> tsrTrackingList = app.findTsrCodeIsNull();
			log.info("Found TSR with no TSR_CODE total: " + tsrTrackingList.size() + " records");
			List<String> tsrNameList = app.distinctTsrName(tsrTrackingList);
			int updatedTotal = 0;
			
			if(!tsrNameList.isEmpty()) {
				log.info("### do mapping TSR Code and Name ###");
				for(String tsrName : tsrNameList) {
					String tsrCode = app.seekTsrCode(tsrName);
					if(!StringUtils.isBlank(tsrCode)) tsrCodeMap.put(tsrName, tsrCode);
				}
				updatedTotal = app.updateProcess(tsrTrackingList, tsrCodeMap);
				log.info("total " + updatedTotal + " updated");
				log.info("re-checking the name that still no code...");
				app.distinctTsrName(app.findTsrCodeIsNull());
			}
			log.info("############################### Finished ##################################");
			log.info("###########################################################################");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private List<TsrTracking> findTsrCodeIsNull() throws Exception {
		DetachedCriteria criteria = DetachedCriteria.forClass(TsrTracking.class);
		criteria.add(Restrictions.isNull("tsr.tsrCode"));
		return tsrTrackingService.findByCriteria(criteria);
	}

	private List<String> distinctTsrName(List<TsrTracking> list) throws Exception {
		List<String> tsrNames = new ArrayList<>();
		if(!list.isEmpty()) {
			log.info("######## Collecting TSR Name with no TSR_CODE ########");
			for(TsrTracking tt : list) {
				if(!tsrNames.contains(tt.getTsrName())) {
					tsrNames.add(tt.getTsrName());
				}
			}
			log.info("######## Total TSR Name: " + tsrNames.size());
		}
		return tsrNames;
	}
	
	private String seekTsrCode(String name) throws Exception {
		String tsrCode = null;
		
		DetachedCriteria criteria = DetachedCriteria.forClass(TsrTracking.class);
		criteria.add(Restrictions.eq("tsrName", name));
		criteria.add(Restrictions.isNotNull("tsr.tsrCode"));
		criteria.addOrder(Order.desc("id"));
		List<TsrTracking> list = tsrTrackingService.findByCriteria(criteria);
		if(!list.isEmpty()) {
			tsrCode = list.get(0).getTsr().getTsrCode();
			
			if(StringUtils.isBlank(tsrCode)) {
				log.warn("tsrCode not found for " + name);
			}
		}
		
		return tsrCode;
	}
	
	private int updateProcess(List<TsrTracking> tsrTrackingList, Map<String, String> tsrCodeMap) throws Exception {
		int total = 0;
		log.info("#### Updating...");
		for(TsrTracking t : tsrTrackingList) {
			String tsrCode = tsrCodeMap.get(t.getTsrName());
			if(!StringUtils.isBlank(tsrCode)) {
				t.setTsr(getTsrbyTsrCode(tsrCode));
				tsrTrackingService.update(t, "System Admin");
				total++;
			}
		}
		return total;
	}
	
	private Tsr getTsrbyTsrCode(String tsrCode) throws Exception {
		DetachedCriteria criteria = DetachedCriteria.forClass(Tsr.class);
		criteria.add(Restrictions.eq("tsrCode", tsrCode));
		List<Tsr> list = tsrService.findByCriteria(criteria);
		if(!list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}
}
