package com.adms.batch.kpireport.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import com.adms.utils.DateUtil;
import com.adms.utils.Logger;

public class UpdateTargetDate {

	private static Logger log = Logger.getLogger();
	
	private static List<File> fileList;
	
	private static final int dateRowIndex = 8;
	private static final int effectiveDateColIndex = CellReference.convertColStringToIndex("E");
	private static final int endDateColIndex = CellReference.convertColStringToIndex("G");
	
	public static void main(String[] args) {
		try {
			String yyyyMM = args[0];
			String rootPath = args[1];
			log.setLogFileName(args[2]);
			
			getFileByRootPath(rootPath);
			if(fileList.isEmpty()) {
				log.error("Not found any file...");
				System.exit(0);
			} else {
				log.info("Changing Effective date in file..." + fileList.get(0).getAbsolutePath());
				changeEffectiveDate(fileList.get(0), yyyyMM);
			}
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		log.info("Finished");
	}
	
	private static void getFileByRootPath(String dir) {
		File dirPath = new File(dir);
		if(dirPath.isDirectory()) {
			List<File> dirs = Arrays.asList(dirPath.listFiles());
			for(File f : dirs) {
				getFileByRootPath(f.getAbsolutePath());
			}
		} else if(dirPath.isFile() 
				&& (dirPath.getName().endsWith(".xls") || dirPath.getName().endsWith(".xlsx"))) {
			if(fileList == null) {
				fileList = new ArrayList<>();
			}
			fileList.add(dirPath);
		} else {
			log.error("nah not excel!! --> " + dirPath.getAbsolutePath());
		}
	}
	
	private static void changeEffectiveDate(File file, String yyyyMM) throws InterruptedException {
		changeEffectiveDate(1, file, yyyyMM);
	}
	
	private static void changeEffectiveDate(int loop, File file, String yyyyMM) throws InterruptedException {
		Workbook wb = null;
		InputStream is = null;
		OutputStream os = null;
		try {
			System.out.println("Open: " + file.getAbsolutePath());
			is = new FileInputStream(file);
			wb = WorkbookFactory.create(is);
			Sheet sheet;
			for(int i = 1; i < wb.getNumberOfSheets(); i++) {
				sheet = wb.getSheetAt(i);
				Row row = sheet.getRow(dateRowIndex);
				Cell effectiveDateCell = row.getCell(effectiveDateColIndex);
				Cell endDateCell = row.getCell(endDateColIndex);
				
				effectiveDateCell.setCellValue(changeMonth(effectiveDateCell.getDateCellValue(), yyyyMM));
				endDateCell.setCellValue(changeMonth(endDateCell.getDateCellValue(), yyyyMM));
			}
			
			os = new FileOutputStream(file.getAbsolutePath());
			wb.write(os);
		} catch(Exception e) {
			log.error("Workbook Error: " + e.getMessage());
		} finally {
			try { is.close();} catch(Exception e) {}
			try { wb.close();} catch(Exception e) {}
			try { os.close();} catch(Exception e) {}
		}
	}
	
	private static Date changeMonth(Date date, String yyyyMM) {
		if(DateUtil.convDateToString("yyyyMM", date).equals(yyyyMM)) {
			log.info("No need to change date");
			System.exit(0);
		}
		
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate newDate = null;
		newDate = LocalDate.of(
				Integer.parseInt(yyyyMM.substring(0, 4))
				, Integer.parseInt(yyyyMM.substring(4, 6))
				, 1);
		if(localDate.lengthOfMonth() == localDate.getDayOfMonth()) {
			newDate = newDate.with(TemporalAdjusters.lastDayOfMonth());
		}
		
		return Date.from(newDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}
}
