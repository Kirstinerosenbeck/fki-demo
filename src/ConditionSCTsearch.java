import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConditionSCTsearch {
    //This class searches in FSIII-conditions using SNOMED CT expression constraints. FSIII conditions does not need to be SNOMED CT coded.
    static FhirContext ctx = FhirContext.forR4();
    static String serverBase = "http://hapi.fhir.org/baseR4";

    public static void main(String[] args) throws IOException {
        //String serverBase = "http://hapi.fhir.org/baseR4";
        //FhirContext ctx = FhirContext.forR4();
        //String serverBase = "http://hapi.fhir.org/baseR4";
         //IGenericClient client = ctx.newRestfulGenericClient(serverBase);

//Upload et sæt af patienter til server
       // ArrayList<String> patientList = uploadAndGetReferenceToPatients("C:/Users/kirst/Projects/TestdataFKI/TestPatienter.csv", client);

        // Giv patienterne tilstande, og gem på disk
        //ArrayList<String> patientList = simplePatientList(1948498,1948531);
        //GivepatientsConditions(patientList);


        //Upload tilstande til serveren
        //uploadConditionsToServer("C:/Users/kirst/Projects/TestdataFKI/Tilstande/");


        //conditionChangeTest();

//søger efter alle Brunos problemer
       System.out.println("Her er alle Brunos tilstande på serveren");
        patientConditionsearch("Patient/2785556", "");

        ArrayList<String> childCodes;
        ArrayList<String> KLcodes;

// søg efter kognitionsproblemer
System.out.println("Her er alle kognitive problemer på serveren");
        childCodes = childrencodes("373930000");
               KLcodes = convertSCTToKLCodes(childCodes);
int number=0;
        for(int i=0; i<KLcodes.size();i++) {
            int g=searchcondition(KLcodes.get(i));
            number=g+number;
        }
System.out.println("Der er dette antal: "+number);

//søger efter Brunos kognitionsproblemer
        System.out.println("Her er alle Brunos kognitive problemer på serveren");

        for(int i=0; i<KLcodes.size();i++) {
            patientConditionsearch("Patient/2785556", KLcodes.get(i));
        }

        // søg efter hukommelsesproblemer

        System.out.println("Her er alle hukommelsesproblemer på serveren");
        childCodes = childrencodes("106136008");

        KLcodes = convertSCTToKLCodes(childCodes);

       number=0;
        for(int i=0; i<KLcodes.size();i++) {
            int g=searchcondition(KLcodes.get(i));
            number=g+number;
        }


        // søg efter ADL
       System.out.println("Her er Brunos ADL tilstande på serveren");//man kan ikke forvente at de større søgninger bliver rigtige
        childCodes = childrencodes("118233009");

        KLcodes = convertSCTToKLCodes(childCodes);


        for(int i=0; i<KLcodes.size();i++) {
            patientConditionsearch("Patient/2785556", KLcodes.get(i));
        }
// søger efter grovmotorisk funktion
        System.out.println("Her er alle gang og bevægelses-ting på serveren");
        childCodes = childrencodes("364832000");
        KLcodes = convertSCTToKLCodes(childCodes);

        number=0;
        for(int i=0; i<KLcodes.size();i++) {
            int g=searchcondition(KLcodes.get(i));
            number=g+number;
        }

    }

    private static void patientConditionsearch(String patient, String code) {
       String s="";

        if(code.equals("")) {
            s="Condition?subject="+patient;
        }
        else{
      s="Condition?subject="+patient+"&code=http://kl.dk/fhir/common/caresocial/CodeSystem/FSIII|"+code;
        }

        // Create a context and a client

        //String serverBase = "http://hapi.fhir.org/baseR4";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

// We'll populate this list
        List<IBaseResource> condi = new ArrayList<>();

// We'll do a search for all Patients and extract the first page
        Bundle bundle = client
                .search()
                .byUrl(s)
                .returnBundle(Bundle.class)
                .execute();

        condi.addAll(BundleUtil.toListOfResources(ctx, bundle));

// Load the subsequent pages
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            condi.addAll(BundleUtil.toListOfResources(ctx, bundle));
        }

        //System.out.println("Loaded " + condi.size() + " conditions!");
        for(int i=0;i<condi.size();i++) {
            Condition c=(Condition) condi.get(i);
            c.getCode().getText();
            System.out.println("Found "+patient+" with the condition: "+c.getCode().getText());
        }

    }

    private static ArrayList<String> convertSCTToKLCodes(ArrayList<String> childCodes) {

        ArrayList<String> KLCodes = new ArrayList<String>();
        try {

            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            BufferedReader br = Files.newBufferedReader(Paths.get("C:/alleSCT.csv"), StandardCharsets.UTF_8);
            CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser).build();

                String[] nextLine;
                int lineNumber = 0;
                while ((nextLine = reader.readNext()) != null) {
                    for (int i = 0; i < childCodes.size(); i++) {
                        if (childCodes.get(i).equals(nextLine[3])) {
                            KLCodes.add(nextLine[1]);
                            //System.out.println(nextLine[5]+" "+nextLine[4]);
                        }
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



        return KLCodes;
    }

    private static ArrayList<String> childrencodes(String code) throws IOException {
        List<IBaseResource> conditions = new ArrayList<>();
        //String code= "284871005";
        URL urlForGetRequest = new URL("http://localhost:8080/concepts?ecQuery=<<" + code + "%20MINUS%20(<<" + code + ":363713009=371150009)");
        ArrayList<String> childCodes = new ArrayList<String>();
        String readLine = null;
        HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
        conection.setRequestMethod("GET");
        conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here
        int responseCode = conection.getResponseCode();


        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conection.getInputStream()));
            StringBuffer response = new StringBuffer();
            while ((readLine = in.readLine()) != null) {
                response.append(readLine);
            }
            in.close();
            // print result

            //GetAndPost.POSTRequest(response.toString());

            JSONParser parser = new JSONParser();
            JSONObject json = new JSONObject();
            try {
                json = (JSONObject) parser.parse(response.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            JSONArray items = (JSONArray) json.get("items");
            JSONObject item;
            for (int i=0;i<items.size(); i++) {

                item = (JSONObject) items.get(i);
                String SCTcode = item.get("id").toString();
                //System.out.println(SCTcode);
                childCodes.add(SCTcode);
            }


    }
        return childCodes;
    }

    private static int searchcondition(String KlCode){


        // Create a context and a client


        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

// We'll populate this list
        List<IBaseResource> condi = new ArrayList<>();

// We'll do a search for all Patients and extract the first page
        Bundle bundle = client
                .search()
                .byUrl("Condition?code=http://kl.dk/fhir/common/caresocial/CodeSystem/FSIII|"+KlCode)
                .returnBundle(Bundle.class)
                .execute();

        condi.addAll(BundleUtil.toListOfResources(ctx, bundle));

// Load the subsequent pages
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            condi.addAll(BundleUtil.toListOfResources(ctx, bundle));
        }

        //System.out.println("Loaded " + condi.size() + " conditions!");
        for(int i=0;i<condi.size();i++) {
            Condition c=(Condition) condi.get(i);
            c.getCode().getText();
            System.out.println("Found a patient with the condition: "+c.getCode().getText());
        }
return condi.size();
            }






    private static void simplesearch() {

        // Create a context and a client
        //FhirContext ctx = FhirContext.forR4();
        //String serverBase = "http://hapi.fhir.org/baseR4";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

// We'll populate this list
        List<IBaseResource> patients = new ArrayList<>();

// We'll do a search for all Patients and extract the first page
        Bundle bundle = client
                .search()
                .byUrl("Condition?code=http://snomed.info/sct|6149008")
                .returnBundle(Bundle.class)
                .execute();

        patients.addAll(BundleUtil.toListOfResources(ctx, bundle));

// Load the subsequent pages
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            patients.addAll(BundleUtil.toListOfResources(ctx, bundle));
        }

        System.out.println("Loaded " + patients.size() + " patients!");


    }
}
