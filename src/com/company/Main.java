package com.company;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Main {

    private static HttpURLConnection connection;
    private static final String seriesID = "LAUCN040010000000005";

    public static void main(String[] args) {
        BufferedReader reader;
        String line;
        StringBuffer responseContent = new StringBuffer();

        File f = new File(seriesID + ".txt");
        if(f.exists()) {
            BufferedReader brTest = null;

            // Read from existing file
            try {
                brTest = new BufferedReader(new FileReader(seriesID + ".txt"));
                String fileLine = brTest.readLine();
                while (fileLine != null) {
                    responseContent.append(fileLine);
                    fileLine = brTest.readLine();
                }
                brTest.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Writer writer = null;

            // Create and write to file
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("LAUCN040010000000005.txt"), "utf-8"));

                // Fetch API
                URL url = new URL("https://api.bls.gov/publicAPI/v1/timeseries/data/" + seriesID);
                connection = (HttpURLConnection) url.openConnection();

                // Request set up
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int status = connection.getResponseCode();

                // if status is not successful
                if (status > 299) {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                }

                // Write to file
                while((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.write("\n");
                    responseContent.append(line);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
                try {writer.close();} catch (Exception ex) {/*ignore*/}
            }
        }

        // Gather user data
        Scanner scanner = new Scanner(System.in);
        String monthInput = "";
        String yearInput = "";
        System.out.print("Enter month: ");
        monthInput = scanner.nextLine();

        System.out.print("Enter year: ");
        yearInput = scanner.nextLine();

        // Serialize the JSON data
        JSONObject json = new JSONObject(responseContent.toString());
        JSONArray series = json.getJSONObject("Results").getJSONArray("series");
        int cpi = 0;
        String notes = "";
        for(int i = 0; i < series.length(); i++) {
            JSONArray serie = new JSONArray(series);
            JSONArray data = serie.getJSONObject(i).getJSONArray("data");
            for(int j = 0; j < data.length(); j++) {
                JSONObject time = data.getJSONObject(j);
                String period = time.getString("period");
                String year = time.getString("year");
                String periodName = time.getString("periodName");
                String value = time.getString("value");
                JSONArray footnotes = time.getJSONArray("footnotes");

                // Get CPI and Notes
                if(year.equals(yearInput) && periodName.equals(monthInput)) {
                    cpi = Integer.parseInt(value);
                    for(int k = 0; k < footnotes.length() - 1; k++) {
                        JSONObject footnote = footnotes.getJSONObject(k);
                        String code = footnote.getString("code");
                        String text = footnote.getString("text");
                        notes = text;
                    }
                    break;
                }
            }
        }

        // Output CPI and Notes to user
        System.out.println("The CPI is: " + cpi);
        if(!notes.isEmpty()) {
            System.out.println("notes: " + notes);
        }
    }
}
