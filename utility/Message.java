package utility;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class Message {


    private String header;
    private String content;
    private ArrayList<String> parameters;
    private String timestamp;
    private ObjectWriter ow;
    private ObjectMapper om;

    public Message(){
        setHeader("");
        setContent("");
        parameters = new ArrayList<>();
        setTimestamp("");
        ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    }

    public void fromJSONString(String s) throws JSONException {
        JSONObject j = new JSONObject(s);
        setHeader(j.getString("header"));
        setContent(j.getString("content"));
        JSONArray a = j.getJSONArray("parameters");
        if (a.length() > 0)
            for (int i = 0; i < j.length(); i++) {
                String value = a.getString(i);
                addParameter(value);
            }
        setTimestamp(j.getString("timestamp"));
    }

    public String toJSONString() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("header", header);
        obj.put("content", content);
        JSONArray pars = new JSONArray();
        for (String val : parameters)
            pars.put(val);
        obj.put("parameters", pars);
        obj.put("timestamp", timestamp);
        return obj.toString();
    }

    public void setHeader(String s) {
        header = s;
    }

    /*
    public void setHeader(Object o) throws IOException {
        header = ow.writeValueAsString(o);
    }
    */

    public void setContent(String s) {
        content = s;
    }

    /*
    public void setContent(Object o) throws IOException {
        content = ow.writeValueAsString(o);
    }
    */

    public void addParameter(String s) {
        parameters.add(s);
    }

    public void addParameters(ArrayList<String> s) {
        for (String el: s) {
            addParameter(el);
        }
    }

    public void setTimestamp(String s) {
        timestamp = s;
    }

    public String getHeader() {
        return header;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }
}
