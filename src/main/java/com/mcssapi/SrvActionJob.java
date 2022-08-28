package com.mcssapi;

import com.mcssapi.exceptions.APIInvalidTaskDetailsException;
import com.mcssapi.exceptions.APINotFoundException;
import com.mcssapi.exceptions.APIUnauthorizedException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class SrvActionJob extends Job {

    private MCSSApi api;

    private String GUID;

    private String TaskID;

    public SrvActionJob(MCSSApi api, String GUID, String TaskID) {
        this.api = api;
        this.GUID = GUID;
        this.TaskID = TaskID;
    }

    /**
     * Get the action of the job.
     * @return The action of the job.
     * @throws APIUnauthorizedException If the API key is not valid.
     * @throws APINotFoundException If the server is not found.
     * @throws IOException If there is an IO error (e.g. server is offline).
     * @throws APIInvalidTaskDetailsException If the task is not found.
     */
    @Override
    public ServerAction getAction() throws APIUnauthorizedException, APINotFoundException, IOException, APIInvalidTaskDetailsException {

        URL url = new URL("https://" + api.IP + "/api/v1/servers/" + GUID + "/scheduler/" + TaskID);

        //create a connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the request method and request properties
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);

        //connect to the server
        conn.connect();

        //get the response code
        int responseCode = conn.getResponseCode();

        //if the response code indicates an error, throw the appropriate exception
        if (responseCode == 401) {
            throw new APIUnauthorizedException("API Token is invalid or expired.");
        } else if (responseCode == 404) {
            throw new APINotFoundException("TaskID or ServerID invalid.");
        }

        //Save the response in a jsonobject
        JSONObject json = new JSONObject(conn.getOutputStream());

        //Get the Job object
        JSONObject job = json.getJSONObject("job");

        //Get the action
        int action = job.getInt("action");

        //close the connection
        conn.disconnect();

        //Search the action
        for (ServerAction a : ServerAction.values()) {
            //if the action is found, return it
            if (a.getValue() == action) {
                return a;
            }
        }

        //if the action is not found, throw an exception
        throw new APIInvalidTaskDetailsException("Action not found or invalid.");

    }

    /**
     * The action to be performed on the server.
     * @param action the action to be performed on the server.
     * @throws APIUnauthorizedException If the API key is not valid.
     * @throws APINotFoundException If the server is not found.
     * @throws IOException If there is an IO error (e.g. server is offline).
     * @throws APIInvalidTaskDetailsException If the task is not found.
     */
    public void setAction(ServerAction action) throws APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException, IOException {

        URL url = new URL("https://" + api.IP + "/api/v1/servers/" + GUID + "/scheduler/" + TaskID);

        //create a connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the request method and request properties
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setRequestProperty("Content-Type", "application/json");

        //create the json object to send
        String json = "{\"job\" : {\"action\" : " + action.getValue() + "}}";

        //set the output stream to the json object
        conn.setDoOutput(true);

        //connect to the server
        conn.connect();

        //write the json object to the output stream
        conn.getOutputStream().write(json.getBytes());

        //get the response code
        int responseCode = conn.getResponseCode();

        //if the response code indicates an error, throw the appropriate exception
        if (responseCode == 401) {
            throw new APIUnauthorizedException("API Token is invalid or expired.");
        } else if (responseCode == 404) {
            throw new APINotFoundException("TaskID or ServerID invalid.");
        } else if (responseCode == 409) {
            throw new APIInvalidTaskDetailsException("Action not found or invalid.");
        }

        //close the connection
        conn.disconnect();

    }

}
