package ManagmentEngine.Utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Folder {
    protected String file_name,last_updater;
    private Date last_update;
    private MessageDigest digest;
    protected String sha1;
    protected String textual_content;
    protected String type;

    protected void initialize_sha1() throws NoSuchAlgorithmException {
        digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
    }

    abstract public void create_local_file(Folder node, String path);

    public String toString(String path) {
        String res = "Path and filename: " + path +"\\"+ file_name + "\n" +
                "Type: " + type + "\n" +
                "SHA-1: " + sha1 + "\n" +
                "Last updater: " + last_updater + "\n" +
                "Lase update: " + date_to_string(getLast_update()) + "\n\n";


        return res;
    }

    String date_to_string(Date date){
        String str_date;
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");

        //convert date to string
        str_date = dateFormat.format(date);

        return str_date;
    }

    protected String get_sha1(String content) throws UnsupportedEncodingException {
        digest.update(content.getBytes("utf8"));
        String res = String.format("%040x", new BigInteger(1, digest.digest()));

        return res;
    }

    public void setLast_updater(String last_updater) {
        this.last_updater = last_updater;
    }

    public void setLast_update(Date last_update) {
        this.last_update = last_update;
    }

    public String getLast_updater() {
        return last_updater;
    }

    public Date getLast_update() {
        return last_update;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return file_name;
    }

    public void setTextual_content(String textual_content) {
        this.textual_content = textual_content;
    }

    public String getTextual_content() {
        return textual_content;
    }

    public String getSha1() {
        return sha1;
    }

    public void setName(String name) {
        this.file_name = name;
    }

    public void setSha1(String sha1_content) {
        this.sha1 = sha1_content;
    }

    static protected Date get_current_time() throws ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        return sdf.parse(sdf.format(new Date()));
    }
}
