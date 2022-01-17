import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class UploadPatientsAndConditions {
//This program requires a file-path to MedCom testpatients,
// mapping files expressing the relationship between FSIII and SNOMED CT
// and a file-path to a folder where conditions are temporarily stored.
// It also requires a running SNOMED CT query service https://github.com/IHTSDO/snomed-query-service on localhost:8080
    public static FhirContext ctx = FhirContext.forR4();

    public static void main(String[] args) throws IOException {

        //Set FHIR variables, and make a client
       String serverBase = "http://hapi.fhir.org/baseR4";
       IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        //Upload et sæt af patienter til server
       ArrayList<String> patientList = uploadAndGetReferenceToPatients("C:/TestPatienter.csv", client);

       //giv patienterne tilstande, og gem dem
       GivepatientsConditions(patientList);

       //Upload tilstandene til serveren
        uploadConditionsToServer("C:/conditions/");




    }
    public static ArrayList<String> uploadAndGetReferenceToPatients(String filename, IGenericClient client){
        ArrayList<String> patientList = new ArrayList<String>();

        try {

            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            BufferedReader br = Files.newBufferedReader(Paths.get(filename),  StandardCharsets.UTF_8);
            CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser).build();
            String [] nextLine;
            int lineNumber = 0;
            while ((nextLine = reader.readNext()) != null) {

                Patient patient = new Patient();
                // ..populate the patient object..
                patient.addIdentifier().setSystem("urn:oid:1.2.208.176.1.2").setValue(nextLine[0]).setUse(Identifier.IdentifierUse.OFFICIAL);
                String patientFirstName = nextLine[4];
                patient.addName().setFamily(nextLine[3]).setUse(HumanName.NameUse.OFFICIAL);
                String str[] = patientFirstName.split(" ");
                for (int i = 0; i<str.length;i++){
                    patient.getNameFirstRep().addGiven(str[i]);
                }
                if(nextLine[2].equals("M")){
                    patient.setGender(Enumerations.AdministrativeGender.MALE);

                }
                if(nextLine[2].equals("K")){
                    patient.setGender(Enumerations.AdministrativeGender.FEMALE);

                }


                MethodOutcome outcome = client.create()
                        .resource(patient)
                        .prettyPrint()
                        .encodedJson()
                        .execute();

                IIdType id = outcome.getId();
                System.out.println("Got ID: " + id.getValue());
                // id.getBaseUrl()

                patientList.add(id.toUnqualifiedVersionless().getValue());
            }


        } catch (
                FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (
                CsvValidationException e) {
            e.printStackTrace();
        }

        return patientList;
    }

    private static void GivepatientsConditions(ArrayList<String> patientList) {

       ArrayList<CodeableConcept> conditionList1 = generateRandomConditions(1,"C:/hjemmeplejeSCT.csv");
       saveConditionsToFile(conditionList1, patientList, "homecare");
       ArrayList<CodeableConcept> conditionList2 = generateRandomConditions(1,"C:/sygeplejeSCT.csv");
       saveConditionsToFile(conditionList2, patientList, "nursing");
       ArrayList<CodeableConcept> conditionList3 = generateRandomConditions(1,"C:/119SCT.csv");
       saveConditionsToFile(conditionList3, patientList, "119");
       ArrayList<CodeableConcept> conditionList4 = generateRandomConditions(1,"C:/140SCT.csv");
       saveConditionsToFile(conditionList4, patientList, "140");
        ArrayList<CodeableConcept> conditionList5 = generateRandomConditions(1,"C:/hjemmeplejeSCT.csv");
        saveConditionsToFile(conditionList1, patientList, "homecare");
        ArrayList<CodeableConcept> conditionList6 = generateRandomConditions(1,"C:/sygeplejeSCT.csv");
        saveConditionsToFile(conditionList2, patientList, "nursing");
        ArrayList<CodeableConcept> conditionList7 = generateRandomConditions(1,"C:/119SCT.csv");
        saveConditionsToFile(conditionList3, patientList, "119");
        ArrayList<CodeableConcept> conditionList8 = generateRandomConditions(1,"C:/140SCT.csv");
        saveConditionsToFile(conditionList4, patientList, "140");

    }

    public static ArrayList<CodeableConcept> generateRandomConditions(int antal, String filename){

        ArrayList<CodeableConcept> conditionList = new ArrayList<CodeableConcept>();
        try {

            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            BufferedReader br = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8);
            CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser).build();

            for (int i = 0; i < antal; i++) {
                String[] nextLine;
                int lineNumber = 0;
                while ((nextLine = reader.readNext()) != null) {
                    List<Coding> conditionCoding = new ArrayList<Coding>();
                    conditionCoding.add(new Coding().setSystem("http://kl.dk/fhir/common/caresocial/CodeSystem/FSIII").setCode(nextLine[1]).setDisplay(nextLine[0]));

                    Coding c = GetAMoreDetailedSCTCode(nextLine[3]);

                    conditionCoding.add(c);
                    conditionList.add(new CodeableConcept().setCoding(conditionCoding).setText("Der er tilstanden: " + nextLine[0].toLowerCase() + ", " + c.getDisplay()));
                    System.out.println("Der er tilstanden: " + nextLine[0].toLowerCase() + ", " + c.getDisplay());

                }
            }

        } catch(
                FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        } catch(
                CsvValidationException e){
            e.printStackTrace();
        }

        return conditionList;
    }



    private static void saveConditionsToFile(ArrayList<CodeableConcept> conditions, ArrayList<String> patientlist, String problemType) {
        for(int i=0; i<conditions.size(); i++){

            KLCondition condition = new KLCondition();
            // ..populate the Condition object..
            condition.setCode(conditions.get(i));
            int rand = randBetween(0,patientlist.size()-1);
            condition.setSubject(new Reference().setReference(patientlist.get((rand))));

            //Create a date
            GregorianCalendar gc = new GregorianCalendar();

            int year = randBetween(2019, 2020);
            gc.set(gc.YEAR, year);
            int dayOfYear = randBetween(1, gc.getActualMaximum(gc.DAY_OF_YEAR));
            gc.set(gc.DAY_OF_YEAR, dayOfYear);
            Date newDate=gc.getTime();
            //Apply date to Recorded date
            condition.setRecordedDate(newDate);

            //Set clinical status active
            CodeableConcept c1 = new CodeableConcept();
            List<Coding> cList1 = new ArrayList<Coding>();
            cList1.add(new Coding().setCode("active").setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical"));
            c1.setCoding(cList1);
            condition.setClinicalStatus(c1);

            if(problemType.equals("homecare")){//Severities are added in code for home care conditions. Notice that this might generates a utf-8 encoding problem when loaded to a fhir server
                CodeableConcept c2 = new CodeableConcept();
                List<Coding> cList2 = new ArrayList<Coding>();
                List<String> FSIIIseverities = new ArrayList<String>();

                FSIIIseverities.add("B2");
                FSIIIseverities.add("B3");
                FSIIIseverities.add("B4");
                FSIIIseverities.add("B5");


                List<String> FSIIIseveritiesDisplay = new ArrayList<String>();

                FSIIIseveritiesDisplay.add("Lette begrænsninger");
                FSIIIseveritiesDisplay.add("Moderate begrænsninger");
                FSIIIseveritiesDisplay.add("Svære begrænsninger");
                FSIIIseveritiesDisplay.add("Totale begrænsninger");

                cList2.add(new Coding().setCode(FSIIIseverities.get(i%4)).setSystem("http://kl.dk/fhir/common/caresocial/CodeSystem/FSIII").setDisplay(FSIIIseveritiesDisplay.get(i%4)));
                c2.setCoding(cList2);

                condition.setSeverity(c2);
            }


            //Serialiser tilstanden og print den ud som fil
            ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
            IParser parser = ctx.newJsonParser();

// Indent the output
            parser.setPrettyPrint(true);

// Serialize it
            String serialized = parser.encodeResourceToString(condition);
            //Setting a filename for the condition, so that they are recognizable, which is nice when used for test-purposes
            String s=conditions.get(i).getCoding().get(1).getDisplay().replace(" ","").replace("/","").replace("\\","");
            try (PrintWriter out = new PrintWriter("C:/conditions/"+s+".json")) {
                out.println(serialized);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
    }

    public static Coding GetAMoreDetailedSCTCode(String code) throws IOException {
        URL urlForGetRequest = new URL("http://localhost:8080/concepts?ecQuery=<<"+code+"%20MINUS%20(<<"+code+":363713009=371150009)");
        Coding c= new Coding();
        String readLine = null;
        HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
        conection.setRequestMethod("GET");
        conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here
        int responseCode = conection.getResponseCode();


        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conection.getInputStream()));
            StringBuffer response = new StringBuffer();
            while ((readLine = in. readLine()) != null) {
                response.append(readLine);
            } in .close();
            // print result

            //GetAndPost.POSTRequest(response.toString());

            JSONParser parser = new JSONParser();
            JSONObject json=new JSONObject();
            try {
                json = (JSONObject) parser.parse(response.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            JSONArray items = (JSONArray) json.get("items");
            JSONObject item;
            if (items.size()>1) {
                Random rand = new Random();
                int randInt = rand.nextInt(items.size() - 1);
                item = (JSONObject) items.get(randInt);
            }
            else{
                item = (JSONObject) items.get(0);
            }

            c.setCode(item.get("id").toString()).setDisplay(item.get("fsn").toString()).setSystem("http://snomed.info/sct");


        } else {
            System.out.println("GET NOT WORKED"+code);
        }
        return c;
    }
    public static int randBetween(int start, int end) {
        return start + (int)Math.round(Math.random() * (end - start));
    }

    private static void uploadConditionsToServer(String folder) {
        File dir = new File(folder);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                String serverBase = "http://hapi.fhir.org/baseR4";
                IParser parser = ctx.newJsonParser();
                IGenericClient client = ctx.newRestfulGenericClient(serverBase);
                String conString = getJsonObject(child.getAbsolutePath());
                Condition con = parser.parseResource(Condition.class, conString);
                MethodOutcome outcome = client.create()
                        .resource(con)
                        .prettyPrint()
                        .encodedJson()
                        .execute();
                IIdType id = outcome.getId();
                System.out.println("Got ID: " + id.getValue());

            }
        }
    }

    private static String getJsonObject(String filename) {
        File f = new File(filename);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject=new JSONObject();

        try {
            Object obj = parser.parse(new FileReader(f));

            // A JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
            jsonObject = (JSONObject) obj;


        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
