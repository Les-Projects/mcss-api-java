package com.mcssapi;

import com.mcssapi.exceptions.APIInvalidTaskDetailsException;
import com.mcssapi.exceptions.APINotFoundException;
import com.mcssapi.exceptions.APIUnauthorizedException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Task {

    private final MCSSApi api;
    private final String GUID;
    private final String TaskID;
    private String TaskName;
    private boolean Enabled;
    private final TaskType TaskType;

    private final TaskJobType TaskJobType;

    private boolean Deleted = false;


    //Not passing timing information because not all tasks have it.
    protected Task(MCSSApi api, String GUID, String TaskID, String TaskName, boolean Enabled) throws APIUnauthorizedException, IOException, APINotFoundException, APIInvalidTaskDetailsException {
        this.api = api;
        this.GUID = GUID;
        this.TaskID = TaskID;
        this.TaskName = TaskName;
        this.Enabled = Enabled;
        this.TaskType = figureOutTaskType();
        this.TaskJobType = figureOutTaskJobType();
    }

    private TaskJobType figureOutTaskJobType() throws APIInvalidTaskDetailsException, APIUnauthorizedException, IOException, APINotFoundException {

        if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.TASK_DELETED.getMessage());
        }

        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID).replace("{TASK_ID}", TaskID));

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

        //if the response code is 401, throw an APIUnauthorizedException
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //save the response in a JSONObject
        JSONObject json = new JSONObject(conn.getOutputStream());

        //close connection
        conn.disconnect();

        JSONObject jobJson = json.getJSONObject("job");

        if (jobJson.has("action")) {
            return com.mcssapi.TaskJobType.SERVER_ACTION;
        } else if (jobJson.has("commands")) {
            return com.mcssapi.TaskJobType.RUN_COMMANDS;
        } else if (jobJson.has("backupIdentifier")) {
            return com.mcssapi.TaskJobType.START_BACKUP;
        } else {
            throw new APIInvalidTaskDetailsException(Errors.INVALID_JOB_TYPE.getMessage());
        }

    }

    private TaskType figureOutTaskType() throws IOException, APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException {


        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

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

        //if the response code is 401 or 404, throw the relevant exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }
        //save the response in a JSONObject
        JSONObject json = new JSONObject(conn.getOutputStream());

        //close connection
        conn.disconnect();

        //parse the task type from the JSONObject "timing"
        JSONObject timing = json.getJSONObject("timing");
        if (timing.has("time")) {
            return com.mcssapi.TaskType.FIXED_TIME;
        } else if (timing.has("interval")) {
            return com.mcssapi.TaskType.INTERVAL;
        } else if (timing.has("timeless")) {
            return com.mcssapi.TaskType.TIMELESS;
        } else {
            throw new APIInvalidTaskDetailsException(Errors.NO_TIMING_INFORMATION.getMessage());
        }
    }

    /**
     * @return the Task ID
     */
    public String getTaskID() {
        return TaskID;
    }

    /**
     * @return the Task Name
     */
    public String getTaskName() {
        return TaskName;
    }

    /**
     * @return the Task Type
     */
    public TaskType getTaskType() {
        return TaskType;
    }

    /**
     * @return the Enabled status of the Task
     */
    public boolean isEnabled() {
        return Enabled;
    }

    /**
     * Check if the task repeats at the set interval/fixed time
     * @return true if the task repeats at the set interval/fixed time
     * @throws APIUnauthorizedException if the API key is invalid
     * @throws APINotFoundException if the server or task is not found
     * @throws APIInvalidTaskDetailsException if the task has no timing information
     * @throws IOException if there is an error connecting to the server
     */
    public boolean isRepeating() throws APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException, IOException {
        if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.REPEAT_TIMELESS.getMessage());
        } else if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.REPEAT_DELETED.getMessage());
        }

        //Create URL
        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

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

        //if the response code is 401 or 404, throw the relevant exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //save the response in a JSONObject
        JSONObject json = new JSONObject(conn.getOutputStream());

        //close connection
        conn.disconnect();

        //get the "timing" object from the main JSONObject
        JSONObject timing = json.getJSONObject("timing");

        //get the "repeat" boolean value from the timing object
        return timing.getBoolean("repeat");
    }

    /**
     * Get the timing information for the Task.
     * @return the timing information for the Task
     * @throws IOException if there is an error connecting to the server
     * @throws APIUnauthorizedException if the API key is invalid
     * @throws APINotFoundException if the server or task is not found
     * @throws APIInvalidTaskDetailsException if the task has no timing information, or if the task has an invalid timing information
     */
    public LocalTime getTime() throws IOException, APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException {
        if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.TIME_TIMELESS.getMessage());
        } else if (TaskType == com.mcssapi.TaskType.INTERVAL) {
            throw new APIInvalidTaskDetailsException(Errors.TIME_INTERVAL.getMessage());
        } else if (Deleted) {
            throw new APINotFoundException(Errors.TIME_DELETED.getMessage());
        }

        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

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

        //if the response code is 401 or 404, throw the relevant exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //save the response in a JSONObject
        JSONObject json = new JSONObject(conn.getOutputStream());

        //close connection
        conn.disconnect();

        //Extract the timing JSONObject
        JSONObject timing = json.getJSONObject("timing");

        //Extract the time variable and parse it to a LocalTime
        String time = timing.getString("time");
        Pattern p = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
        Matcher m = p.matcher(time);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            int minute = Integer.parseInt(m.group(2));
            int second = Integer.parseInt(m.group(3));
            return LocalTime.of(hour, minute, second);
        } else {
            throw new APIInvalidTaskDetailsException(Errors.COULD_NOT_PARSE_TIME.getMessage());
        }

    }

    /**
     * Get the interval information for the Task.
     * @return Long int of the interval in seconds
     * @throws APIUnauthorizedException if the API key is invalid
     * @throws APINotFoundException if the server or task is not found
     * @throws APIInvalidTaskDetailsException if the task has no interval information, or if the task has an invalid interval information
     * @throws IOException if there is an error connecting to the server
     */
    public long getInterval() throws APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException, IOException {

        //Check if the task has an interval
        if (TaskType == com.mcssapi.TaskType.FIXED_TIME) {
            throw new APIInvalidTaskDetailsException(Errors.INTERVAL_FIXED_TIME.getMessage());
        } else if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.INTERVAL_TIMELESS.getMessage());
        } else if (Deleted) {
            throw new APINotFoundException(Errors.INTERVAL_DELETED.getMessage());
        }

        //Create the URL
        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

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

        //if the response code is 401 or 404, throw the relevant exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //save the response in a JSONObject
        JSONObject json = new JSONObject(conn.getOutputStream());

        //close connection
        conn.disconnect();

        //Extract the timing JSONObject
        JSONObject timing = json.getJSONObject("timing");
        return timing.getLong("interval");
    }

    public Job getJob() throws APINotFoundException {
        if (Deleted) {
            throw new APINotFoundException(Errors.JOB_DELETED.getMessage());
        }
        if (TaskJobType == com.mcssapi.TaskJobType.SERVER_ACTION) {
            return new ServerActionJob(api, GUID, TaskID);
        } else if (TaskJobType == com.mcssapi.TaskJobType.RUN_COMMANDS) {
            return new RunCommandsJob(api, GUID, TaskID);
        } else if (TaskJobType == com.mcssapi.TaskJobType.START_BACKUP) {
            return new BackupJob(api, GUID, TaskID);
        } else {
            throw new APINotFoundException(Errors.INVALID_JOB_TYPE.getMessage());
        }
    }

    /**
     * @return true if the task has been deleted from the API
     */
    public boolean isDeleted() {
        return Deleted;
    }

    /**
     * Enables the task
     * @throws IOException if there is an error connecting to the server
     * @throws APINotFoundException if the server returns a 404 response code
     * @throws APIUnauthorizedException if the server returns a 401 response code
     * @throws APIInvalidTaskDetailsException if the server returns a 409 response code
     */
    public void setEnabled() throws IOException, APINotFoundException, APIUnauthorizedException, APIInvalidTaskDetailsException {

        if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.ENABLE_TIMELESS.getMessage());
        } else if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.ENABLE_DELETED.getMessage());
        }

        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //create a connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the request method and request properties
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);


        String json = "{\"enabled\":true}";

        //connect to the server
        conn.connect();

        //write the JSON to the output stream
        conn.getOutputStream().write(json.getBytes());

        //get the response code
        int responseCode = conn.getResponseCode();

        //if the response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 409:
                throw new APIInvalidTaskDetailsException(Errors.INVALID_TASK_DETAILS.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        Enabled = true;

        //close connection
        conn.disconnect();

    }

    /**
     * Disables the task
     * @throws IOException if there is an error connecting to the server
     * @throws APINotFoundException if the server returns a 404 response code
     * @throws APIUnauthorizedException if the server returns a 401 response code
     * @throws APIInvalidTaskDetailsException if the server returns a 409 response code
     */
    public void setDisabled() throws IOException, APINotFoundException, APIUnauthorizedException, APIInvalidTaskDetailsException {

        if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.DISABLE_TIMELESS.getMessage());
        } else if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.DISABLE_DELETED.getMessage());
        }
        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //create a connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the request method and request properties
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = "{\"enabled\":false}";

        //connect to the server
        conn.connect();

        //write the JSON to the output stream
        conn.getOutputStream().write(json.getBytes());

        //get the response code
        int responseCode = conn.getResponseCode();

        //if the response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 409:
                throw new APIInvalidTaskDetailsException(Errors.INVALID_TASK_DETAILS.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        Enabled = false;

        //close connection
        conn.disconnect();

    }

    /**
     * Change the interval of an Interval task
     * @throws APIUnauthorizedException if the API key is invalid
     * @throws APINotFoundException if the server or task is not found
     * @throws APIInvalidTaskDetailsException if the task has no interval information, or if the task has an invalid interval information
     * @throws IOException if there is an error connecting to the server
     */
    public void setInterval(long newInterval) throws APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException, IOException {

        //Check if the task has the interval value and that it's not deleted
        if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.INTERVAL_TIMELESS.getMessage());
        } else if (TaskType == com.mcssapi.TaskType.FIXED_TIME) {
            throw new APIInvalidTaskDetailsException(Errors.INTERVAL_FIXED_TIME.getMessage());
        } else if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.INTERVAL_DELETED.getMessage());
        }

        //Check if the interval is valid
        if (newInterval < 1) {
            throw new APIInvalidTaskDetailsException(Errors.INTERVAL_GREATER_0.getMessage());
        }

        //Create the URL
        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //Create the connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //Set the request method and request properties
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        //Create the JSON
        String json = """
                {  "timing": {
                    "interval":"200"
                    }
                }""";

        //Open the connection
        conn.connect();

        //Write the JSON to the output stream
        conn.getOutputStream().write(json.getBytes());

        //Get the response code
        int responseCode = conn.getResponseCode();

        //If the response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 409:
                throw new APIInvalidTaskDetailsException(Errors.INVALID_TASK_DETAILS.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //Close the connection
        conn.disconnect();
    }

    public void setTime(LocalTime newTime) throws APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException, IOException {

        if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.TIME_DELETED.getMessage());
        } else if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.TIME_TIMELESS.getMessage());
        } else if (TaskType == com.mcssapi.TaskType.INTERVAL) {
            throw new APIInvalidTaskDetailsException(Errors.TIME_INTERVAL.getMessage());
        }

        //Create URL
        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //Create connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //Set request method and request properties
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        //Create JSON with the newTime
        String json = "{\"timing\":{\"time\":\"" + newTime.toString() + "\"}}";

        //Open the connection
        conn.connect();

        //Write the JSON to the output stream
        conn.getOutputStream().write(json.getBytes());

        //Get the response code
        int responseCode = conn.getResponseCode();

        //If the response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 409:
                throw new APIInvalidTaskDetailsException(Errors.INVALID_TASK_DETAILS.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //Close the connection
        conn.disconnect();
    }

    /**
     * Manually run the task
     * @throws IOException if there is an error connecting to the server
     * @throws APINotFoundException if the server returns a 404 response code
     * @throws APIUnauthorizedException if the server returns a 401 response code
     */
    public void runTask() throws IOException, APINotFoundException, APIUnauthorizedException, APIInvalidTaskDetailsException {

        if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.RUN_DELETED.getMessage());
        }

        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //create a connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the request method and request properties
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);

        //connect to the server
        conn.connect();
        //get the response code
        int responseCode = conn.getResponseCode();

        //if the response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }
        //close connection
        conn.disconnect();
    }

    /**
     * Set the task to repeat
     * @param repeat boolean of the new repeat value, true or false
     * @throws APIUnauthorizedException if the API key is invalid
     * @throws APINotFoundException if the server or task is not found
     * @throws APIInvalidTaskDetailsException if the task has no repeat information, or if the task has an invalid repeat information
     * @throws IOException if there is an error connecting to the server
     */
    public void setRepeating(boolean repeat) throws APIUnauthorizedException, APINotFoundException, APIInvalidTaskDetailsException, IOException {

        if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.REPEAT_DELETED.getMessage());
        } else if (TaskType == com.mcssapi.TaskType.TIMELESS) {
            throw new APIInvalidTaskDetailsException(Errors.REPEAT_TIMELESS.getMessage());
        }

        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //create a connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the request method and request properties
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        //connect to the server
        conn.connect();

        //create the JSON
        String json = " { \"timing\" { \"repeat\": " + repeat + " } }";

        //write the JSON to the output stream
        conn.getOutputStream().write(json.getBytes());

        //get the response code
        int responseCode = conn.getResponseCode();

        //if the response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 409:
                throw new APIInvalidTaskDetailsException(Errors.INVALID_TASK_DETAILS.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //close connection
        conn.disconnect();
    }

    /**
     * Change the task name
     * @param newName the new name for the task
     * @throws APIInvalidTaskDetailsException if the server returns a 409 response code
     * @throws APIUnauthorizedException if the server returns a 401 response code
     * @throws APINotFoundException if the server returns a 404 response code
     * @throws IOException if there is an error connecting to the server
     */
    public void changeName(String newName) throws APIInvalidTaskDetailsException, APIUnauthorizedException, APINotFoundException, IOException {

        if (Deleted) {
            throw new APIInvalidTaskDetailsException(Errors.CHANGE_NAME_DELETED.getMessage());
        }

        //Check if new name contains special characters
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(newName);
        if (m.find()) {
            throw new APIInvalidTaskDetailsException(Errors.NAME_SPECIAL_CHAR.getMessage());
        }

        //Create URL
        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //Create connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //Set request method and request properties
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        //Create JSON
        String json = "{\"name\":\"" + newName + "\"}";

        //Connect to server
        conn.connect();

        //Write JSON to output stream
        conn.getOutputStream().write(json.getBytes());

        //Get response code
        int responseCode = conn.getResponseCode();

        //If response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 409:
                throw new APIInvalidTaskDetailsException(Errors.INVALID_TASK_DETAILS.getMessage());
            default:
                throw new APIInvalidTaskDetailsException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        //Close connection
        conn.disconnect();

        TaskName = newName;

    }



    /**
     * Delete the task from the API
     * @throws IOException if there is an error connecting to the server
     * @throws APINotFoundException if the server returns a 404 response code
     * @throws APIUnauthorizedException if the server returns a 401 response code
     */
    public void deleteTask() throws IOException, APINotFoundException, APIUnauthorizedException {

        if (Deleted) {
            throw new APINotFoundException(Errors.TASK_ALREADY_DELETED.getMessage());
        }

        URL url = new URL(Endpoints.GET_TASK.getEndpoint().replace("{IP}", api.IP).replace("{GUID}", GUID)
                .replace("{TASK_ID}", TaskID));

        //create a connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //set the request method and request properties
        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", api.token);
        conn.setDoOutput(true);
        conn.connect();
        //get the response code
        int responseCode = conn.getResponseCode();

        //if the response code indicates an error, throw the appropriate exception
        switch (responseCode) {
            case 200:
                break;
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            default:
                throw new APINotFoundException(Errors.NOT_RECOGNIZED.getMessage() + responseCode);
        }

        Deleted = true;
        //close connection
        conn.disconnect();
    }

}
