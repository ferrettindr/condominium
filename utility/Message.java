package utility;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;

public class Message {


    private String header;
    private String content;
    private ArrayList<String> parameters = new ArrayList<>();
    private String timestamp;

    public Message(String s) {
        try {
            JSONObject j = new JSONObject(s);
            setHeader(j.getString("header"));
            setContent(j.getString("content"));
            JSONArray a = j.getJSONArray("parameters");
            for (int i = 0; i < j.length(); i++) {
                String value = a.getString(i);
                addParameter(value);
            }
            setTimestamp(j.getString("timestamp"));
        } catch (Exception e) {System.err.println(e.getMessage());}
    }

    public String toJSONString() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("header", header);
            obj.put("content", content);
            JSONArray pars = new JSONArray();
            for (String val : parameters)
                pars.put(val);
            obj.put("parameters", pars);
            obj.put("timestamp", timestamp);
        } catch (Exception e) {System.err.println(e.getMessage());}
        return obj.toString();
    }

    public void setHeader(String s) {
        header = s;
    }

    public void setContent(String s) {
        content = s;
    }

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
