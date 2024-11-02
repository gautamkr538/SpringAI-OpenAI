package com.openai.springopenai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ZillizApiTest {

    public static void main(String[] args) {
        String apiKey = "84902ffa40a1d00e46d8a4a3ed4949608429379ae58b980a761ed10b636e09055d0386cf259a6e3781220e4f07454481c2e0b6c5";
        String urlString = "https://in03-b599fcdc06c6cf4.serverless.gcp-us-west1.cloud.zilliz.com/v2/vectordb/collections/list";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write("{}".getBytes());
                os.flush();
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Response: " + response.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
