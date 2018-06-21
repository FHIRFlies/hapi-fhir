package org.hl7.fhir.convertors;

public class Main {
    public static void main(String[] args) throws Exception {
      // String srcFilePath = "C:\\Work\\Repos\\hapi-fhir\\hapi-fhir-converter\\target\\resources.bson";
       // String tgtFilePath = "C:\\Work\\Repos\\hapi-fhir\\hapi-fhir-converter\\target\\resources2.bson";
        String srcFilePath="";
        String tgtFilePath="";

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

}
