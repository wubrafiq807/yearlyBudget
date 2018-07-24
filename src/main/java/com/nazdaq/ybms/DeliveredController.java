package com.nazdaq.ybms;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.nazdaq.ybms.model.Budget;
import com.nazdaq.ybms.model.Delivered;
import com.nazdaq.ybms.model.FiscalYear;
import com.nazdaq.ybms.model.Point;
import com.nazdaq.ybms.service.CommonService;
import com.nazdaq.ybms.util.Constants;
import com.nazdaq.ybms.util.UTF8Converter;

@Controller
@PropertySource("classpath:common.properties")
public class DeliveredController extends UTF8Converter implements Constants{
	
	@Autowired
	private CommonService commonService;
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Value("${cc.email.addresss}")
	String ccEmailAddresss;
	
	@Value("${common.email.address}")
	String commonEmailAddress;
	
	//addDeliveredForm
	@RequestMapping(value = "/addDeliveredForm", method = RequestMethod.GET)
	public ModelAndView addDeliveredForm (@ModelAttribute("command") Delivered deliveredBean, BindingResult result, Principal principal){		
		
		if(principal == null) {
			return new ModelAndView("redirect:/login");
		}
		
		return new ModelAndView("addDeliveredForm");
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = { "/getDeliveredList" }, method = RequestMethod.POST)
	private @ResponseBody String getDeliveredList(@RequestBody String jesonString, Principal principal)
			throws JsonGenerationException, JsonMappingException, Exception {
		String toJson = "";
		Gson gson = new Gson();
		Delivered deliveredBean = gson.fromJson(jesonString, Delivered.class);
		List<Delivered> deliveredListLeatest = new ArrayList<Delivered>();
		
		List<Delivered> finalDeliveredList = new ArrayList<Delivered>();
		List<Point> pointListNew = new ArrayList<Point>();
		List<Point> pointList = (List<Point>)(Object)commonService.getAllObjectList("Point");
		FiscalYear fiscalYear = (FiscalYear)commonService.getAnObjectByAnyUniqueColumn("FiscalYear", " name", getCurrentFiscalYear());
		
		List<Delivered> deliveredList = (List<Delivered>)(Object)
				commonService.getObjectListByAnyColumn("Delivered", "fiscalYear.name", getCurrentFiscalYear());
		
		if(deliveredList != null && deliveredList.size() > 0) {			
			if(deliveredList.size() < pointList.size()) {
				boolean flag = false;
				for (Point point : pointList) {
					for (Delivered delivered : deliveredList) {
						if(point.getId().toString().equals(delivered.getPoint().getId().toString())) {
							flag = true;
							break;
						}
					}
					
					if(!flag) {
						pointListNew.add(point);
					}
				}
				
				Delivered delivered = null;
				for (Point point : pointListNew) {
					delivered = new Delivered();
					delivered.setPoint(point);
					delivered.setFiscalYear(fiscalYear);
					delivered.setCreatedBy(principal.getName());
					delivered.setCreatedDate(new Date());
					delivered.setDlvAmount(0.0);
					delivered.setId(null);
					commonService.saveOrUpdateModelObjectToDB(delivered);
				}
				
				deliveredListLeatest = (List<Delivered>)(Object)
						commonService.getObjectListByAnyColumn("Delivered", "fiscalYear.name", getCurrentFiscalYear());
			} else {
				deliveredListLeatest.addAll(deliveredList);
			}
		} else {
			Delivered delivered = null;
			for (Point point : pointList) {
				delivered = new Delivered();
				delivered.setPoint(point);
				delivered.setFiscalYear(fiscalYear);
				delivered.setCreatedBy(principal.getName());
				delivered.setCreatedDate(new Date());
				delivered.setDlvAmount(0.0);
				delivered.setId(null);
				commonService.saveOrUpdateModelObjectToDB(delivered);
			}
			
			deliveredListLeatest = (List<Delivered>)(Object)
					commonService.getObjectListByAnyColumn("Delivered", "fiscalYear.name", getCurrentFiscalYear());
		}
		
		String queryString = "FROM Budget b WHERE fiscalYear.id != '" + fiscalYear.getId().toString() + "'";
		List<Budget> budgetList = (List<Budget>)(Object) commonService.getListBySqlQuery(queryString);
		
		String queryStringDelivered = "FROM Delivered b WHERE fiscalYear.name = '" + getPreviousFiscalYear() + "'";
		List<Delivered> deliveredListOld = (List<Delivered>)(Object) commonService.getListBySqlQuery(queryStringDelivered);
		
		for (Delivered delivered : deliveredListOld) {
			
		}
		
		
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		toJson = ow.writeValueAsString(null);
		return toJson;
	}
	
	private static String getCurrentFiscalYear() {
		Date d = new Date();
		String res;
		Integer month = (d.getMonth()+1);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		
		Integer year = cal.get(Calendar.YEAR);
		if(month > 6) {
			res = year+"-"+(year+1);
		} else {
			res = (year-1)+"-"+(year);
		}
		return res;
	}
	
	private static String getPreviousFiscalYear() {
		Date d = new Date();
		String res;
		Integer month = (d.getMonth()+1);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		
		Integer year = cal.get(Calendar.YEAR);
		if(month > 6) {
			res = (year-1)+"-"+(year);
		} else {
			res = (year-2)+"-"+(year-1);
		}
		return res;
	}

	
}
