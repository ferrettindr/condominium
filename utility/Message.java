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
        header = "EMPTY";
        content = "";
        parameters = new ArrayList<>();
        timestamp= "";
        ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        om = new ObjectMapper();
    }

    public void fromJSONString(String s) throws JSONException, IOException {
        JSONObject j = new JSONObject(s);
        setHeader(j.getString("header"));
        setContent(j.getString("content"));
        JSONArray a = j.getJSONArray("parameters");
        if (a.length() > 0)
            for (int i = 0; i < a.length(); i++) {
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

    public void setHeader(Object o) throws IOException {
        header = ow.writeValueAsString(o);
    }

    public void setContent(String s) {
        content = s;
    }

    public void setContent(Object o) throws IOException {
        content = ow.writeValueAsString(o);
    }

    public void addParameter(String s) {
        parameters.add(s);
    }

    public void addParameter(Object o) throws IOException {
        parameters.add(ow.writeValueAsString(o));
    }

    public void setParameters(ArrayList<Object> list) throws IOException {
        for (Object o: list) {
            addParameter(o);
        }
    }

    public void setTimestamp(Object o) throws IOException {
        timestamp = ow.writeValueAsString(o);
    }

    public String getHeader() {
        return header;
    }

    public <T> T getHeader(Class<T> cls) throws IOException {
        return om.readValue(header, cls);
    }

    public String getContent() { return content;
    }

    public <T> T getContent(Class<T> cls) throws IOException {
            return om.readValue(content, cls);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public <T> T getTimestamp(Class<T> cls) throws IOException {
            return om.readValue(timestamp, cls);
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    public <T> ArrayList<T> getParameters(Class<T> cls) throws IOException {
        ArrayList<T> tmp = new ArrayList<>();
        for (String s: parameters)
            tmp.add(om.readValue(s, cls));
        return tmp;
    }
}
