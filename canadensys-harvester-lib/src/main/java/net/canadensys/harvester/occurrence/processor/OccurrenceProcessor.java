package net.canadensys.harvester.occurrence.processor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.time.MonthOfYear;

import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.processor.AbstractDataProcessor;
import net.canadensys.processor.ProcessingResult;
import net.canadensys.processor.datetime.DateIntervalProcessor;
import net.canadensys.processor.datetime.DateProcessor;
import net.canadensys.processor.geography.CountryContinentProcessor;
import net.canadensys.processor.geography.CountryProcessor;
import net.canadensys.processor.geography.DecimalLatLongProcessor;
import net.canadensys.processor.geography.DegreeMinuteToDecimalProcessor;
import net.canadensys.processor.geography.StateProvinceProcessor;
import net.canadensys.processor.numeric.NumericPairDataProcessor;
import net.canadensys.utils.ArrayUtils;
import net.canadensys.vocabulary.Continent;
import net.canadensys.vocabulary.stateprovince.BEProvince;
import net.canadensys.vocabulary.stateprovince.CAProvince;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.vocabulary.Country;
import org.gbif.api.vocabulary.NameType;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import com.google.common.base.CharMatcher;

/**
 * Processing each OccurrenceRawModel into OccurrenceModel.
 * @author canadensys
 *
 */
public class OccurrenceProcessor implements ItemProcessorIF<OccurrenceRawModel, OccurrenceModel>{

	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(OccurrenceProcessor.class);
	
	//CharMatcher matching all invisible characters except space char
	protected static CharMatcher CHAR_MATCHER_WHITESPACE = CharMatcher.INVISIBLE.and(CharMatcher.isNot(' '));
	
	private static final Integer MIN_DATE = 1700;
	private static final Integer MAX_DATE = Calendar.getInstance().get(Calendar.YEAR);
	private static final int DATE_INTERVAL_THRESHOLD = 16; //minimum date length = 8 (2002-1-1)
	
	private NameParser GBIF_NAME_PARSER = new NameParser();
	
	//Processors
	private CountryProcessor countryProcessor = new CountryProcessor();
	private CountryContinentProcessor countryContinentProcessor = new CountryContinentProcessor();
	private AbstractDataProcessor latLongProcessor = new DecimalLatLongProcessor("decimallatitude","decimallongitude");
	private DateProcessor dateProcessor = new DateProcessor("eventdate","syear","smonth","sday");
	private DateIntervalProcessor dateIntervalProcessor = new DateIntervalProcessor();
	
	private AbstractDataProcessor altitudeProcessor = new NumericPairDataProcessor("minimumelevationinmeters","maximumelevationinmeters");
	private DegreeMinuteToDecimalProcessor dmsProcessor = new DegreeMinuteToDecimalProcessor();
	
	private static Map<String,AbstractDataProcessor> stateProvinceProcessorMap = new HashMap<String,AbstractDataProcessor>();
	static{
		stateProvinceProcessorMap.put(Country.CANADA.getTitle(), new StateProvinceProcessor<CAProvince>(Country.CANADA, CAProvince.class));
		stateProvinceProcessorMap.put(Country.BELGIUM.getTitle(), new StateProvinceProcessor<BEProvince>(Country.BELGIUM, BEProvince.class));
	}
	
	@Override
	public void init() {};
		
	@Override
	public OccurrenceModel process(OccurrenceRawModel rawModel, Map<SharedParameterEnum,Object> sharedParameters) {
	
		OccurrenceModel processedModel = new OccurrenceModel();
		
		//keep the same auto_id 
		processedModel.setAuto_id(rawModel.getAuto_id());
		processedModel.setBasisofrecord(rawModel.getBasisofrecord());
		
		//set a cleaned associatedmedia
		processedModel.setAssociatedmedia(normalizeURLSeparator(rawModel.getAssociatedmedia()));
		
		processedModel.setAssociatedsequences(rawModel.getAssociatedsequences());
		
		processedModel.setCatalognumber(rawModel.getCatalognumber());
		processedModel.setOccurrenceid(rawModel.getOccurrenceid());
		processedModel.setCollectioncode(rawModel.getCollectioncode());
		processedModel.set_references(normalizeURLSeparator(rawModel.get_references()));
		processedModel.setBibliographiccitation(rawModel.getBibliographiccitation());
		
		//Country processing
		countryProcessor.processBean(rawModel, processedModel, null, null);

		//Continent processing
		if(!StringUtils.isBlank(rawModel.getContinent())){
			processedModel.setContinent(rawModel.getContinent());
		}
		else{
			Continent continent = null;
			try{
				if(!StringUtils.isBlank(processedModel.getCountry())){
					Country country = Country.valueOf(processedModel.getCountry().toUpperCase().replaceAll(" ", "_"));
					continent = countryContinentProcessor.process(country.getIso2LetterCode(), null);
					if(continent != null){
						processedModel.setContinent(continent.getTitle());
					}
				}
			}
			catch(IllegalArgumentException ignore){}
		}
		
		//state or province processing
		if(!StringUtils.isBlank(processedModel.getCountry()) && stateProvinceProcessorMap.get(processedModel.getCountry()) != null){
			stateProvinceProcessorMap.get(processedModel.getCountry()).processBean(rawModel, processedModel, null, null);
		}
		else{//if we can't process it, copy it 
			processedModel.setStateprovince(rawModel.getStateprovince());
		}
		
		processedModel.setCounty(rawModel.getCounty());
		processedModel.setMunicipality(rawModel.getMunicipality());
		processedModel.setDatasetname(rawModel.getDatasetname());
		
		processedModel.setKingdom(rawModel.getKingdom());
		processedModel.setPhylum(rawModel.getPhylum());
		processedModel.set_class(rawModel.get_class());
		processedModel.set_order(rawModel.get_order());
		processedModel.setFamily(rawModel.getFamily());
		processedModel.setGenus(rawModel.getGenus());
		processedModel.setSpecificepithet(rawModel.getSpecificepithet());
		processedModel.setInfraspecificepithet(rawModel.getInfraspecificepithet());
		
		processedModel.setLocality(rawModel.getLocality());
		
		processedModel.setRecordedby(rawModel.getRecordedby());
		processedModel.setRecordnumber(rawModel.getRecordnumber());
		processedModel.setInstitutioncode(rawModel.getInstitutioncode());
		processedModel.setTaxonrank(rawModel.getTaxonrank());
		processedModel.setEventdate(rawModel.getEventdate());
		
		processedModel.setHasmedia(StringUtils.isNotBlank(rawModel.getAssociatedmedia()));
		processedModel.setTypestatus(rawModel.getTypestatus());
		processedModel.setHastypestatus(StringUtils.isNotBlank(rawModel.getTypestatus()));
		processedModel.setHasassociatedsequences(StringUtils.isNotBlank(rawModel.getAssociatedsequences()));
		
		processScientificName(rawModel, processedModel);
		
		//Process date(s)
		processDate(rawModel, processedModel);
		
		processCoordinates(rawModel, processedModel);
		
		processAltitude(rawModel,processedModel);
		
		processedModel.setVerbatimelevation(rawModel.getVerbatimelevation());
		processedModel.setHabitat(rawModel.getHabitat());
		
		processedModel.setDwcaid(rawModel.getDwcaid());
		processedModel.setSourcefileid(rawModel.getSourcefileid());
		return processedModel;
	}
	
	/**
	 * This method will normalize the separator(in case of multiple URLs) of an URL field.
	 * TODO : test the URL?
	 * @param rawURLField
	 * @return
	 */
	private String normalizeURLSeparator(String rawURLField){
		if(StringUtils.isBlank(rawURLField)){
			return rawURLField;
		}
		String[] urls = rawURLField.split("\\|");
		ArrayList<String> urlList = new ArrayList<String>();
		for(String url : urls){
			if(!StringUtils.isBlank(url)){
				urlList.add(url.trim());
			}
		}
		return StringUtils.join(urlList,"; ");
	}
	
	/**
	 * This method will try to process the scientific name.
	 * If possible, this method will split the raw scientificname into 2 parts : -scientificname and scientificnameauthorship.
	 * The raw scientificname will be set into the rawscientificname.
	 * If it's not possible to parse this scientific name, the raw scientific name will be kept in scientificname
	 * and will also be in rawscientificname.
	 * @param rawModel
	 * @param occModel
	 */
	private void processScientificName(OccurrenceRawModel rawModel, OccurrenceModel occModel){
		String rawScientificName = rawModel.getScientificname();
		occModel.setRawscientificname(rawScientificName);
		
		if(StringUtils.isNotBlank(rawScientificName)){
			//remove all whitespace except space char
			rawScientificName = CHAR_MATCHER_WHITESPACE.removeFrom(rawScientificName);
			//ensure it's not 'quoted'
			rawScientificName = StringUtils.removeStart(rawScientificName, "\"");
			rawScientificName = StringUtils.removeEnd(rawScientificName, "\"");
		}
		//set it to raw scientificname in case the parsing could not be done
		occModel.setScientificname(rawScientificName);
		
		ParsedName parsedName = null;
		try{
			parsedName = GBIF_NAME_PARSER.parse(rawScientificName);
			if (NameType.WELLFORMED.equals(parsedName.getType())
					|| NameType.SCINAME.equals(parsedName.getType())) {
				occModel.setScientificname(parsedName.canonicalNameWithMarker());
				occModel.setScientificnameauthorship(parsedName.authorshipComplete());
				
				//Set the species from the parser
				occModel.setSpecies(StringUtils.trim(parsedName.getGenusOrAbove() + " " + parsedName.getSpecificEpithet()));
			}
		}
		catch(UnparsableException uEx){
			System.out.println("NameParser " + uEx.getMessage());
		}
	}
	
	/**
	 * Process 'eventdate' or 'verbatimeventdate'
	 * @param rawModel
	 * @param occModel
	 */
	private void processDate(OccurrenceRawModel rawModel, OccurrenceModel occModel){
		ProcessingResult pr = new ProcessingResult();
		
		//if something is available in one of those 3 fields, try to use it
		if(StringUtils.isNotBlank(rawModel.getDay()) || StringUtils.isNotBlank(rawModel.getMonth()) ||
				StringUtils.isNotBlank(rawModel.getDay())){
			occModel.setSyear(NumberUtils.toInt(rawModel.getYear(), -1));
			occModel.setSmonth(NumberUtils.toInt(rawModel.getMonth(), -1));
			occModel.setSday(NumberUtils.toInt(rawModel.getDay(), -1));
			validateStartDate(occModel);
		}
		else{
			dateProcessor.processBean(rawModel, occModel, null, pr);
			
			if(occModel.getSday() == null && occModel.getSmonth() == null && occModel.getSyear() == null){
				
				String rawEventDate = rawModel.getEventdate();
				String rawVerbatimEventDate = rawModel.getVerbatimeventdate();
				
				String usedDate = StringUtils.isNotBlank(rawEventDate)?rawEventDate:rawVerbatimEventDate;
				usedDate = StringUtils.defaultString(usedDate, "");
	
				//check if we should try to parse it as date interval
				if(usedDate.length() > DATE_INTERVAL_THRESHOLD){
					//we try the date interval, clear the previous error
					pr.clear();
					processDateInterval(usedDate,occModel,pr);
				}
				else{
					//try verbatim date only if eventDate is blank
					if(StringUtils.isNotBlank(rawVerbatimEventDate) && StringUtils.isBlank(rawEventDate)){
						//we try the verbatim, clear the previous error
						pr.clear();
						Integer[] pDate = dateProcessor.process(rawVerbatimEventDate, pr);
						occModel.setSyear(pDate[DateProcessor.YEAR_IDX]);
						occModel.setSmonth(pDate[DateProcessor.MONTH_IDX]);
						occModel.setSday(pDate[DateProcessor.DAY_IDX]);
					}
				}
				
				if(pr.getErrorList().size() > 0){
					System.out.println("DwcA ID:"+rawModel.getDwcaid() +"->"+pr.getErrorString());
				}
			}
		}
		
		secondPassDateProcess(occModel);
	}
	
	/**
	 * Try to process the providedDateInterval as date interval.
	 * Only complete start and end dates are supported for now.
	 * @param providedDateInterval
	 * @param occModel
	 * @param pr 
	 */
	private void processDateInterval(String providedDateInterval, OccurrenceModel occModel, ProcessingResult pr){
		String[] parsedInterval = dateIntervalProcessor.process(providedDateInterval, pr);
		String startDate = parsedInterval[DateIntervalProcessor.START_DATE_IDX];
		String endDate = parsedInterval[DateIntervalProcessor.END_DATE_IDX];
		
		if(StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)){
			return;
		}
		
		Integer[] pStartDate = dateProcessor.process(startDate, pr);
		Integer[] pEndDate = dateProcessor.process(endDate, pr);
		
		//for now, we only support full date interval
		if(ArrayUtils.containsOnlyNotNull(pStartDate[DateProcessor.YEAR_IDX],pStartDate[DateProcessor.MONTH_IDX],pStartDate[DateProcessor.DAY_IDX],
			pEndDate[DateProcessor.YEAR_IDX],pEndDate[DateProcessor.MONTH_IDX],pEndDate[DateProcessor.DAY_IDX])){
			
			occModel.setSyear(pStartDate[DateProcessor.YEAR_IDX]);
			occModel.setSmonth(pStartDate[DateProcessor.MONTH_IDX]);
			occModel.setSday(pStartDate[DateProcessor.DAY_IDX]);
	
			occModel.setEyear(pEndDate[DateProcessor.YEAR_IDX]);
			occModel.setEmonth(pEndDate[DateProcessor.MONTH_IDX]);
			occModel.setEday(pEndDate[DateProcessor.DAY_IDX]);
		}
	}
	
	/**
	 * //lat long parsing
	 * @param rawModel
	 * @param occModel
	 */
	private void processCoordinates(OccurrenceRawModel rawModel, OccurrenceModel occModel){
		//will be set to true later if we find valid coordinates
		occModel.setHascoordinates(false);
		
		ProcessingResult result = new ProcessingResult();
		latLongProcessor.processBean(rawModel, occModel, null, result);
		//only check latitude since if the coordinate is not valid, both will be null
		if(occModel.getDecimallatitude() != null){
			occModel.setHascoordinates(true);
		}
		else{
			if(result.getErrorList().size() > 0){
				System.out.println(result.getErrorString());
			}
			else{//try verbatim
				Double[] latLong = dmsProcessor.process(rawModel.getVerbatimlatitude(),rawModel.getVerbatimlongitude(), result);
				
				if(latLong[0] != null && latLong[1] != null){
					if(result.getErrorList().size() > 0){
						System.out.println(result.getErrorString());
					}
					else{
						System.out.println("Good one -> " + rawModel.getVerbatimlatitude() +"," + rawModel.getVerbatimlongitude());
						occModel.setDecimallatitude(latLong[0]);
						occModel.setDecimallongitude(latLong[1]);
						occModel.setHascoordinates(true);
					}
				}
			}
		}
	}
	
	private void processAltitude(OccurrenceRawModel rawModel, OccurrenceModel occModel){
		
		altitudeProcessor.processBean(rawModel, occModel, null, null);
		
		Double avgElevationDouble = null;
		Double minElevationDouble = occModel.getMinimumelevationinmeters();
		Double maxElevationDouble = occModel.getMaximumelevationinmeters();
		
		if(minElevationDouble != null){
			if(maxElevationDouble == null){
				occModel.setMaximumelevationinmeters(minElevationDouble);
			}
		}
		if(maxElevationDouble != null){
			if(minElevationDouble == null){
				occModel.setMinimumelevationinmeters(maxElevationDouble);
			}
		}
		//compute the rounded average elevation 
		if(minElevationDouble !=null && maxElevationDouble!=null){
			avgElevationDouble = (minElevationDouble+maxElevationDouble)/2d;
			occModel.setAveragealtituderounded(((int)MathUtils.round(avgElevationDouble/100d,0))*100);
		}
	}
	
	/**
	 * Validate start date value and make sure they are valid. If not, remove them from occModel.
	 * TODO should  be replaced by dwca-validator library 
	 * @param occModel
	 */
	private void validateStartDate(OccurrenceModel occModel){
		//make sure the year is valid
		if(occModel.getSyear() !=null && (occModel.getSyear() < MIN_DATE || occModel.getSyear() > MAX_DATE)){
			occModel.setSyear(null);
		}
		//make sure month is valid
		if(occModel.getSmonth() !=null && (occModel.getSmonth() < MonthOfYear.JANUARY.getValue() || occModel.getSmonth() > MonthOfYear.DECEMBER.getValue())){
			occModel.setSmonth(null);
		}
		
		//to validate a day, we need the month and the ideally, the year
		if(occModel.getSday() != null){
			//validate that we do not provide negative value since calendar will accept them.
			if(occModel.getSyear() == null || occModel.getSmonth() == null || occModel.getSday() <= 0){
				occModel.setSday(null);
			}
			else{
				Calendar cal = Calendar.getInstance();
				cal.setLenient(false);
				//Calendar month id 0-based
				try{
					cal.set(occModel.getSyear(), occModel.getSmonth()-1, occModel.getSday());
				}
				catch (IllegalArgumentException iaEx){
					occModel.setSday(null);
					occModel.setSmonth(null);
					//we keep the year
				}
			}
		}	
	}
	
	/**
	 * Make sure the date is within accepted range and set the decade.
	 * @param occModel
	 */
	private void secondPassDateProcess(OccurrenceModel occModel){		
		//make sure the year is valid
		//start date
		if(occModel.getSyear() !=null && occModel.getSyear() < MIN_DATE){
			occModel.setSyear(null);
		}
		if(occModel.getSyear() !=null && occModel.getSyear() > MAX_DATE){
			occModel.setSyear(null);
		}
		
		//end date
		if(occModel.getEyear() !=null && occModel.getEyear() < MIN_DATE){
			occModel.setSyear(null);
		}
		if(occModel.getEyear() !=null && occModel.getEyear() > MAX_DATE){
			occModel.setSyear(null);
		}
		
		//if we have a date interval
		if(ArrayUtils.containsOnlyNotNull(occModel.getEyear(),occModel.getEmonth(),occModel.getEday())){
			Calendar startCal = Calendar.getInstance();
			startCal.setLenient(false);
			//Calendar month id 0-based
			startCal.set(occModel.getSyear(), occModel.getSmonth()-1, occModel.getSday());
			
			Calendar endCal = Calendar.getInstance();
			endCal.setLenient(false);
			//Calendar month id 0-based
			endCal.set(occModel.getEyear(), occModel.getEmonth()-1, occModel.getEday());
			
			//if start is not before end, clear both dates
			if(!startCal.before(endCal)){
				occModel.setSyear(null);
				occModel.setSmonth(null);
				occModel.setSday(null);
				occModel.setEyear(null);
				occModel.setEmonth(null);
				occModel.setEday(null);
				//should include this in the report
			}
		}
		
		//set the decade (based on start year)
		if(occModel.getSyear() != null){
			occModel.setDecade((occModel.getSyear()/10)*10);
		}
	}
	
	@Override
	public void destroy() {};
	
}
