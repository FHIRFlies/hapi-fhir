package org.hl7.fhir.convertors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.util.JSON;
import org.bson.BSONObject;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.Resource;

import java.util.*;

public class ExecuteConverter {

	public static void main(String[] args) throws Exception {

		String srcFilePath = "";
		String tgtFilePath = "";
		if (args[0].equalsIgnoreCase("-src") && args[2].equalsIgnoreCase("-tgt"))
		{
			srcFilePath = args[1];
			tgtFilePath = args[3];
		}

		if (!srcFilePath.isEmpty() && !tgtFilePath.isEmpty())
		{
			ExecuteConverter converter = new ExecuteConverter();
			try
			{
				converter.execute(srcFilePath,tgtFilePath);
			}catch(Exception e){
				e.printStackTrace();
			}
		}else
		{
			System.out.println("Arguments: -src {srcFilepath} -tgt {tgtFilePath}");
		}
	}


	public ExecuteConverter()
	{
		 this.srcParser = new FhirContext(FhirVersionEnum.DSTU2_HL7ORG).newJsonParser();
		 this.tgtParser = new FhirContext(FhirVersionEnum.DSTU3).newJsonParser();
		 this.converter = new VersionConvertor_10_30(new NullVersionConverterAdvisor30());
		 this.bsonHelper = new BsonHelper();
		 this.jsonParser = new JsonParser();
		 this.notConvertedResources = Arrays.asList("AllergyIntolerance", "BodySite", "Claim", "ClaimResponse", "DiagnosticOrder",
			 "Goal", "MedicationOrder", "NutritionOrder", "PaymentReconciliation", "PaymentResponse","Observation");
		 this.mongoFiled = Arrays.asList("@method","@REFERENCE","@typename","@VersionId","@when","@state","_id");
	}
	private BsonHelper bsonHelper;
	private List<String> notConvertedResources;
	private List<String> mongoFiled;
	private JsonParser jsonParser;
	private IParser srcParser;
	private IParser tgtParser;
	private VersionConvertor_10_30 converter;

	public static final String RESOURCETYPE = "resourceType";

	public void execute(String srcFilePath, String tgtFilePath) throws Exception {

		List<String> resultJson = bsonHelper.Read(srcFilePath);

		List<BSONObject> results = new ArrayList<BSONObject>();

		for (String s: resultJson
			) {
			System.out.println(s);

			JsonObject jsonObject =jsonParser.parse(s).getAsJsonObject();

			if (!jsonObject.has(RESOURCETYPE) ||notConvertedResources.contains(jsonObject.get(RESOURCETYPE).getAsString()) )
			{
				continue;
			}
//			JsonElement method = jsonObject.get("@method");
			//			JsonElement reference = jsonObject.get("@REFERENCE");
			//			JsonElement typename = jsonObject.get("@typename");
			//			JsonElement VersionId = jsonObject.get("@VersionId");
			//			JsonElement when = jsonObject.get("@when");
			//			JsonElement state = jsonObject.get("@state");
			//			JsonElement _id = jsonObject.get("_id");

//			jsonObject.remove("@method");
//			jsonObject.remove("@REFERENCE");
//			jsonObject.remove("@typename");
//			jsonObject.remove("@VersionId");
//			jsonObject.remove("@when");
//			jsonObject.remove("@state");
//			jsonObject.remove("_id");
			Map<String,JsonElement> mongoProperties = GetMongoFields(jsonObject,this.mongoFiled);
			RemoveMongoFields(jsonObject,this.mongoFiled);
			String resultResourceJson = this.convert(this.converter,this.srcParser,this.tgtParser,jsonObject.toString());
			JsonObject resultJsonObject = new JsonParser().parse(resultResourceJson).getAsJsonObject();
			resultJsonObject = AddMongoFields(resultJsonObject,mongoProperties);
//			resultJsonObject.add("@method",method);
//			resultJsonObject.add("@REFERENCE",reference);
//			resultJsonObject.add("@typename",typename);
//			resultJsonObject.add("@VersionId",VersionId);
//			resultJsonObject.add("@when",when);
//			resultJsonObject.add("@state",state);
//			resultJsonObject.add("_id",_id);
			BSONObject bsonDocument = (BSONObject) JSON.parse(resultJsonObject.toString());
			results.add(bsonDocument);
		}

		bsonHelper.Write(tgtFilePath,results);
	}

	private static Map<String,JsonElement> GetMongoFields(JsonObject jsonObject,List<String> fields)
	{
		Map<String,JsonElement> results = new HashMap<String,JsonElement>();
		for (String s: fields
			  ) {
			JsonElement element = jsonObject.get(s);
			results.put(s,element);
		}
		return results;
	}


	private static void RemoveMongoFields(JsonObject jsonObject,List<String> fields)
	{
		for (String s: fields
			) {
			jsonObject.remove(s);
		}
	}

	private static JsonObject AddMongoFields(JsonObject jsonObject,Map<String,JsonElement> fields)
	{
		for (Map.Entry<String,JsonElement> entry: fields.entrySet()) {
			jsonObject.add(entry.getKey(),entry.getValue());
		}
		return jsonObject;
	}

	public static String convert(VersionConvertor_10_30 converter,IParser srcParser, IParser tgtParser,String src)
	{
		try {
			org.hl7.fhir.dstu3.model.Resource output = converter.convertResource((Resource) srcParser.parseResource(src));
			return tgtParser.encodeResourceToString(output);
		} catch (FHIRException e) {
			e.printStackTrace();
			return null;
		}
	}

}
