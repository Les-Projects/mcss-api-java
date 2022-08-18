package com.mcssapi;

import com.mcssapi.exceptions.APINotFoundException;
import com.mcssapi.exceptions.APIUnauthorizedException;
import com.mcssapi.exceptions.APIVersionMismatchException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public class mcssapi {

    public boolean online = false;
    protected String IP = null;
    protected String token = null;
    protected String version = null;

    public mcssapi(String IP, String token) throws APIUnauthorizedException, APIVersionMismatchException, IOException {
        this.IP = IP;
        this.token = token;

        Info in = getInfo();
        this.version = in.getMCSSApiVersion();
        checkVersionMismatch();
    }

    public Info getInfo() throws IOException, APIUnauthorizedException, APIVersionMismatchException {
            URL url;

            url = new URL("https://" + IP + "/api/v1/info");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
            conn.setReadTimeout(5000);
            conn.setRequestProperty("APIKey", token);

            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode == 401) {
                throw new APIUnauthorizedException("Got 401 response code when getting info.");
            }

            //save the response in a JSONObject
            JSONObject json = new JSONObject(conn.getOutputStream());

            //close connection
            conn.disconnect();

            return new Info(json.getBoolean("isDev"), json.getString("MCSSVersion"), json.getString("MCSSApiVersion"), json.getString("UniqueID"), json.getBoolean("youAreAwesome"));
    }

    public ArrayList<Server> getServers() throws APIUnauthorizedException, APINotFoundException, IOException {

        //create the ArrayList
        ArrayList<Server> servers = new ArrayList<>();

        //create the URL
        URL url = new URL("https://" + IP + "/api/v1/servers");
        //Create and open the connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the connection variables, request proprieties and request method
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", token);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        //Connect to the API
        conn.connect();
        //Get the response code of the connection
        int responseCode = conn.getResponseCode();
        //if the responsecode is an error, throw an exception
        if (responseCode == 401) {
            throw new APIUnauthorizedException("Got 401 response code when getting servers.");
        } else if (responseCode == 404) {
            //Might never fire, better safe than sorry
            throw new APINotFoundException("Got 404 response code when getting servers.");
        }
        //save the response in a JSONObject
        JSONObject json = new JSONObject(conn.getOutputStream());
        //close connection
        conn.disconnect();
        //Create the JsonArray from the JSONObject
        JSONArray serversArray = new JSONArray(json);
        //Create a DateTimeFormatter to parse the creationDate
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        //loop through the JsonArray and create a Server object for each server
        for (int i = 0; i < serversArray.length(); i++) {
            JSONObject server = serversArray.getJSONObject(i);
            //Create the Server object with parsed values from JSON, and add it to the ArrayList
            servers.add(new Server(server.getString("guid"), server.getInt("status"),
                    server.getString("name"), server.getString("description"), server.getString("pathToFolder"),
                    server.getString("folderName"),  LocalDateTime.parse(server.getString("creationDate"),formatter),
                    server.getBoolean("isSetToAutostart"), server.getInt("keepOnline"), server.getInt("javaAllocatedMemory"),
                    server.getString("javaStartupLine"), this));
        }

        //return the ArrayList
        return servers;

    }

    private void checkVersionMismatch() throws APIVersionMismatchException {
        if (!Objects.equals(version, "1.0.0")) {
            throw new APIVersionMismatchException("MCSSApi version mismatch. Expected 1.0.0, got " + version + "." +
                    "API Wrapper might have issues. Proceed with caution.");
        }
    }


    /**
     * Get the version of the API
     * @return version as a string, or ERR_ and the error code. As an
     * example, ERR_401 means that the API returned an error code 401
     */
    /*
    public String getApiVersion() {
        try {
            this.version = getApiVersion.get(IP, token);
        } catch (Exception e) {
            this.version = "ERR_" + e.getMessage();
            System.out.println("Error while getting API version! Error: " + e.getMessage());
        }
        return this.version;
    }
    */


    /**
     * Get the number of servers. Parameters can be NULL
     * @param filter not required, 0 for all, 1 for online, 2 for offline, 3 if using servertype filter
     * @param serverTypeID only required if using filter 3, servertypeID is the GUID of a servertype
     * @return number of servers, -1 if error during request. (INT)
     */
    /*
    public int getServerCount(@Nullable String filter, @Nullable String serverTypeID) {
        try {
            return getServerCount.get(IP, token, filter, serverTypeID);
        } catch (Exception e) {
            System.out.println("Error while getting server count! Error: " + e.getMessage());
            return -1;
        }
    }
    */




}
