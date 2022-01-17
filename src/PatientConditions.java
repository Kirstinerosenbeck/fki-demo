import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
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

public class PatientConditions {


    public static FhirContext ctx = FhirContext.forR4();

        public static void main(String[] args) throws IOException {
            conditionChangeTest();

        }



    private static void conditionChangeTest() {
       String serverBase = "http://hapi.fhir.org/baseR4";
        FhirContext ctx = FhirContext.forR4();
        //String serverBase = "http://hapi.fhir.org/baseR4";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        ArrayList<String> patientList = uploadAndGetReferenceToPatients("C:/ConditionChange/TestPatienterEinar.csv", client);
        ArrayList<String> practitionerList = uploadAndGetReferenceToPractitioners(client);
        String resourceDir="C:/ConditionChange/";
        conditionChange(resourceDir, patientList,practitionerList,client);



    }

    private static void conditionChange(String resourceDir, ArrayList<String> patientList, ArrayList<String> practitionerList, IGenericClient client) {

       Reference patientRef = new Reference().setReference(patientList.get(0));
       //Reference pracRef = new Reference().setReference(practitionerList.get(0));

        //Læg området på serveren
        String Obs1234 = getJsonObject(resourceDir+"/resources/Observation-Observation1234.json");
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        Observation obs = parser.parseResource(Observation.class, Obs1234);
        obs.setSubject(patientRef);
        obs.getPerformerFirstRep().setReference(practitionerList.get(0));
        //System.out.println(parser.encodeResourceToString(obs));
        MethodOutcome outcome = client.create()
                .resource(obs)
                .prettyPrint()
                .encodedJson()
                .execute();

        String Obs1234Reference = outcome.getId().toUnqualifiedVersionless().getValue();

        //Læg årsagen på serveren
        String conString444 = getJsonObject(resourceDir+"/resources/Condition-Condition444.json");
        Condition con444 = parser.parseResource(Condition.class, conString444);
        con444.setSubject(patientRef);
        con444.getRecorder().setReference(practitionerList.get(0));
        con444.getAsserter().setReference(practitionerList.get(0));
        //System.out.println(parser.encodeResourceToString(obs));
        outcome = client.create()
                .resource(con444)
                .prettyPrint()
                .encodedJson()
                .execute();

        String con444Reference = outcome.getId().toUnqualifiedVersionless().getValue();

        //læg opfølgningsdatoen på serveren
        String encounterString333 = getJsonObject(resourceDir+"/resources/Encounter-Encounter333.json");
        Encounter enc333 = parser.parseResource(Encounter.class, encounterString333);
        enc333.setSubject(patientRef);
        outcome = client.create()
                .resource(enc333)
                .prettyPrint()
                .encodedJson()
                .execute();

        String enc333Reference = outcome.getId().toUnqualifiedVersionless().getValue();

        //læg tilstandenv1 på serveren
        String conString111 = getJsonObject(resourceDir+"/resources/Condition-Condition111.json");
        String correctedConString111= conString111.replace("Encounter\\/Encounter333", enc333Reference);
        correctedConString111=correctedConString111.replace("Observation\\/Observation1234", Obs1234Reference);
        correctedConString111=correctedConString111.replace("Condition\\/Condition444", con444Reference);
        //System.out.println(correctedConString111);

        Condition con111 = parser.parseResource(Condition.class, correctedConString111);
        con111.setSubject(patientRef);
        con111.getAsserter().setReference(practitionerList.get(0));
        //System.out.println(parser.encodeResourceToString(con111));
        outcome = client.create()
                .resource(con111)
                .prettyPrint()
                .encodedJson()
                .execute();

        String con111Reference = outcome.getId().toUnqualified().getValue();
        System.out.println(con111Reference);

        //læg provenanceV1 på server
        String provString111 = getJsonObject(resourceDir+"/Provenance-ProvenanceCondition111.json");
        String correctedProvString111= provString111.replace("Practitioner\\/7777777", practitionerList.get(0));
        correctedProvString111=correctedProvString111.replace("Condition\\/Condition111", con111Reference);
        System.out.println(correctedProvString111);
        parser.setStripVersionsFromReferences(false);
        Provenance prov111 = parser.parseResource(Provenance.class, correctedProvString111);
        System.out.println(parser.encodeResourceToString(prov111));
        outcome = client.create()
                .resource(prov111)
                .prettyPrint()
                .encodedJson()
                .execute();

        String prov111Reference = outcome.getId().toUnqualifiedVersionless().getValue();

        //læg tilstand v2 på server
        String conString111v2 = getJsonObject(resourceDir+"/resources/Condition-Condition111v2.json");
        String correctedConString111v2= conString111v2.replace("Encounter\\/Encounter333", enc333Reference);
        correctedConString111v2=correctedConString111v2.replace("Observation\\/Observation1234", Obs1234Reference);
        correctedConString111v2=correctedConString111v2.replace("Condition\\/Condition444", con444Reference);
        correctedConString111v2=correctedConString111v2.replace("Provenance\\/ProvenanceCondition111", prov111Reference);
        correctedConString111v2=correctedConString111v2.replace("Practitioner\\/7777777", practitionerList.get(0));
        //System.out.println(correctedConString111);

        Condition con111v2 = parser.parseResource(Condition.class, correctedConString111v2);
        con111v2.setSubject(patientRef);
        con111v2.setId(con111Reference);
        //con111.getAsserter().setReference(practitionerList.get(0));
        //System.out.println(parser.encodeResourceToString(con111));

        outcome = client.update()
                .resource(con111v2)
                .prettyPrint()
                .encodedJson()
                .execute();

        String con111v2Reference = outcome.getId().toUnqualified().getValue();
        System.out.println(con111v2Reference);


        //læg provenanceV2 på server
        String provString111v2 = getJsonObject(resourceDir+"/Provenance-ProvenanceCondition111v2.json");
        String correctedProvString111v2= provString111v2.replace("Practitioner\\/7777777", practitionerList.get(0));
        correctedProvString111v2=correctedProvString111v2.replace("Condition\\/Condition111v2", con111v2Reference);
        parser.setStripVersionsFromReferences(false);
        Provenance prov111v2 = parser.parseResource(Provenance.class, correctedProvString111v2);
        outcome = client.create()
                .resource(prov111v2)
                .prettyPrint()
                .encodedJson()
                .execute();

        String prov111v2Reference = outcome.getId().toUnqualifiedVersionless().getValue();

//læg den eksekverede opfølgning på serveren
        String encounterString333v2 = getJsonObject(resourceDir+"/resources/Encounter-Encounter333v2.json");
        Encounter enc333v2 = parser.parseResource(Encounter.class, encounterString333v2);
        enc333v2.setSubject(patientRef);
        enc333v2.setId(enc333Reference);
        outcome = client.create()
                .resource(enc333v2)
                .prettyPrint()
                .encodedJson()
                .execute();

        String enc333v2Reference = outcome.getId().toUnqualifiedVersionless().getValue();

// læg opfølgningsresultatet på serveren

        String Obs22 = getJsonObject(resourceDir+"/resources/Observation-Observation22.json");
        Observation obs22 = parser.parseResource(Observation.class, Obs22);
        obs22.setSubject(patientRef);
        obs22.getPerformerFirstRep().setReference(practitionerList.get(0));
        //System.out.println(parser.encodeResourceToString(obs));
        outcome = client.create()
                .resource(obs22)
                .prettyPrint()
                .encodedJson()
                .execute();

        String obs22Reference = outcome.getId().toUnqualifiedVersionless().getValue();

//læg tilstand version 3 på serveren
        String conString111v3 = getJsonObject(resourceDir+"/resources/Condition-Condition111v3.json");
        String correctedConString111v3= conString111v3.replace("Encounter\\/Encounter333", enc333Reference);
        correctedConString111v3=correctedConString111v3.replace("Observation\\/Observation1234", Obs1234Reference);
        correctedConString111v3=correctedConString111v3.replace("Condition\\/Condition444", con444Reference);
        correctedConString111v3=correctedConString111v3.replace("Provenance\\/ProvenanceCondition111v2", prov111v2Reference);
        correctedConString111v3=correctedConString111v3.replace("Provenance\\/ProvenanceCondition111", prov111Reference);
        //correctedConString111v3=correctedConString111v3.replace("Provenance\\/ProvenanceCondition111v2", prov111v2Reference);
        correctedConString111v3=correctedConString111v3.replace("Practitioner\\/7777777", practitionerList.get(0));
        correctedConString111v3=correctedConString111v3.replace("Observation\\/Observation22", obs22Reference);
        //System.out.println(correctedConString111);

        Condition con111v3 = parser.parseResource(Condition.class, correctedConString111v3);
        con111v3.setSubject(patientRef);
        con111v3.setId(con111v2Reference);
        //con111.getAsserter().setReference(practitionerList.get(0));
        //System.out.println(parser.encodeResourceToString(con111));

        outcome = client.update()
                .resource(con111v3)
                .prettyPrint()
                .encodedJson()
                .execute();

        String con111v3Reference = outcome.getId().toUnqualified().getValue();

        // Læg provenance version 3 på serveren
        String provString111v3 = getJsonObject(resourceDir+"/Provenance-ProvenanceCondition111v3.json");
        String correctedProvString111v3= provString111v3.replace("Practitioner\\/7777777", practitionerList.get(0));
        correctedProvString111v3=correctedProvString111v3.replace("Condition\\/Condition111v3", con111v3Reference);
        parser.setStripVersionsFromReferences(false);
        Provenance prov111v3 = parser.parseResource(Provenance.class, correctedProvString111v3);
        outcome = client.create()
                .resource(prov111v3)
                .prettyPrint()
                .encodedJson()
                .execute();




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


    private static ArrayList<String> uploadAndGetReferenceToPractitioners(IGenericClient client) {
        ArrayList<String> practitionerList = new ArrayList<String>();




                Practitioner practitioner = new Practitioner();
                practitioner.addName().setFamily("Hansen").setUse(HumanName.NameUse.OFFICIAL).addGiven("Kristina");

                MethodOutcome outcome = client.create()
                        .resource(practitioner)
                        .prettyPrint()
                        .encodedJson()
                        .execute();

                IIdType id = outcome.getId();
                System.out.println("Got ID: " + id.getValue());
                // id.getBaseUrl()

                practitionerList.add(id.toUnqualifiedVersionless().getValue());


        return practitionerList;
    }

    private static ArrayList<String> simplePatientList(int i, int i1) {
        ArrayList<String> patientList = new ArrayList<String>();

        for(int l=i; l<(i1+1);l++){
            patientList.add("Patient/"+l);
        }

            return patientList;
    }


    public static ArrayList<String> uploadAndGetReferenceToPatients(String filename, IGenericClient client){
            ArrayList<String> patientList = new ArrayList<String>();

            try {

                //csv file containing data
//                        String strFile = filename;
//                        CSVReader reader = new CSVReader(new FileReader(strFile));
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


    }





