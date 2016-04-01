package xyz.pushpad;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class Notification {
  public Pushpad pushpad;
  public String body;
  public String title;
  public String targetUrl;

  public Notification(Pushpad pushpad, String title, String body, String targetUrl) {
    this.pushpad = pushpad;
    this.title = title;
    this.body = body;
    this.targetUrl = targetUrl;
  }

  public JSONObject broadcast() throws DeliveryException{
    return this.deliver(this.reqBody(null));
  }

  public JSONObject deliverTo(String uid) throws DeliveryException {
    String[] uids = new String[1];
    uids[0] = uid;
    return this.deliverTo(uids);
  }

  public JSONObject deliverTo(String[] uids) throws DeliveryException {
    return this.deliver(this.reqBody(uids));
  }

  private String reqBody(String[] uids) {
    JSONObject body = new JSONObject();
    JSONObject notificationData = new JSONObject();
    notificationData.put("body", this.body);
    notificationData.put("title", this.title);
    notificationData.put("target_url", this.targetUrl);
    body.put("notification", notificationData);
    if (uids != null) {
      JSONArray jsonUids = new JSONArray();
      for (String uid:uids) {
        jsonUids.add(uid);
      }
      body.put("uids", jsonUids);
    }
    return body.toString();
  }

  private JSONObject deliver(String reqBody) throws DeliveryException {
    String endpoint = "https://pushpad.xyz/projects/" + pushpad.projectId + "/notifications";
    HttpsURLConnection connection = null;
    int code;
    String responseBody;
    JSONObject json;

    try {
      // Create connection
      URL url = new URL(endpoint);
      connection = (HttpsURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Token token=\"" + pushpad.authToken + "\"");
      connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Content-Length", String.valueOf(reqBody.length()));
      connection.setUseCaches(false);
      connection.setDoOutput(true);

      // Send request
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.writeBytes(reqBody);
      wr.close();

      // Get Response  
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      StringBuilder response = new StringBuilder(); 
      String line;
      while((line = rd.readLine()) != null) {
        response.append(line);
        response.append('\r');
      }
      rd.close();
      code = connection.getResponseCode();
      responseBody = response.toString();
    } catch(IOException e) {
      throw new DeliveryException(e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }

    if (code != 201) {
      throw new DeliveryException("Response " + (new Integer(code)).toString() + ": " + responseBody);
    }

    try {
      JSONParser parser = new JSONParser();
      json = (JSONObject) parser.parse(responseBody); 
    } catch (ParseException e) {
      throw new DeliveryException(e.getMessage());
    }

    return json;
  }
}