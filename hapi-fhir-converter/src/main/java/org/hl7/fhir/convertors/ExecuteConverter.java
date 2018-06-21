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
import org.hl7.fhir.instance.model.DateTimeType;
import org.hl7.fhir.instance.model.DateType;
import org.hl7.fhir.instance.model.Resource;

import java.util.*;

public class ExecuteConverter {

	public ExecuteConverter()
	{
		 this.srcParser = new FhirContext(FhirVersionEnum.DSTU2_HL7ORG).newJsonParser();
		 this.tgtParser = new FhirContext(FhirVersionEnum.DSTU3).newJsonParser();
		 this.converter = new VersionConvertor_10_30(new NullVersionConverterAdvisor30());
		 this.bsonHelper = new BsonHelper();
		 this.jsonParser = new JsonParser();
		 this.notConvertedResources = Arrays.asList("BodySite","ClaimResponse","DiagnosticOrder",
			 "Goal", "NutritionOrder","PaymentResponse");
		 this.mongoFiled = Arrays.asList("@method","@REFERENCE","@typename","@VersionId","@when","@state","_id","@MultiKey");
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
			//System.out.println(s);

			JsonObject jsonObject =jsonParser.parse(s).getAsJsonObject();

			if (!jsonObject.has(RESOURCETYPE) ||notConvertedResources.contains(jsonObject.get(RESOURCETYPE).getAsString()) )
			{
				continue;
			}
			//System.out.println(jsonObject.get(RESOURCETYPE));
			Map<String,JsonElement> mongoProperties = GetMongoFields(jsonObject,this.mongoFiled);
			RemoveMongoFields(jsonObject,this.mongoFiled);
			if (jsonObject.get(RESOURCETYPE).getAsString().equalsIgnoreCase("PaymentReconciliation"))
			{
				if (jsonObject.has("created"))
				{
					String paymentReconCreatedTime = jsonObject.get("created").getAsString();
					DateTimeType createdTime = new DateTimeType(new Date(paymentReconCreatedTime));
					jsonObject.remove("created");
					jsonObject.addProperty("created",createdTime.asStringValue());
				}
			}
            if (jsonObject.get(RESOURCETYPE).getAsString().equalsIgnoreCase("Patient"))
            {
                if (jsonObject.has("birthDate"))
                {
                    String birthdatetime = jsonObject.get("birthDate").getAsString();
                    DateType birthDate = new DateType(birthdatetime);
                    jsonObject.remove("birthDate");
                    jsonObject.addProperty("birtheDate",birthDate.asStringValue());
                }
            }

            if (jsonObject.get(RESOURCETYPE).getAsString().equalsIgnoreCase("Observation")) {
                if (jsonObject.has("category")) {

                    if(jsonObject.get("category").getAsJsonObject().has("text"))
                    {
                        if(jsonObject.get("category").getAsJsonObject().get("text").getAsString().equalsIgnoreCase("Social History"))
                        {
                            if(jsonObject.has("component"))
                            {
                                for (JsonElement component :
                                        jsonObject.get("component").getAsJsonArray()) {

                                    if (component.getAsJsonObject().has("valueDateTime")) {
                                        String valueDateTimeString = component.getAsJsonObject().get("valueDateTime").getAsString();
                                        component.getAsJsonObject().remove("valueDateTime");
                                        component.getAsJsonObject().addProperty("valueString", valueDateTimeString);
                                    }
                                }
                            }
                        }
                    }
                }

			}
            String resultResourceJson = this.convert(this.converter,this.srcParser,this.tgtParser,jsonObject.toString());
            JsonObject resultJsonObject = new JsonParser().parse(resultResourceJson).getAsJsonObject();
            resultJsonObject = AddMongoFields(resultJsonObject,mongoProperties);

            //System.out.println(resultJsonObject.toString());

            BSONObject bsonDocument = (BSONObject) JSON.parse(resultJsonObject.toString());
            results.add(bsonDocument);
            if(results.size() > 1000)
            {
                bsonHelper.Write(tgtFilePath,results);
                results = new ArrayList<BSONObject>();
            }
		}
		if (results.size() >0)
		{
			bsonHelper.Write(tgtFilePath,results);
		}

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
			if (entry.getValue()!=null)
			{
				jsonObject.add(entry.getKey(),entry.getValue());
			}
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
