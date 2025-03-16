package utils;

import java.io.*;
import java.util.Properties;

public class PropertiesReadWrite {

    public static Properties prop;
    public static String path =System.getProperty("user.dir")+ "/src/main/resources/config.properties";
    public static String getValue(String key) {
        String value=null;
        prop = new Properties();
        FileInputStream ip = null;
        try {
            ip = new FileInputStream(path);
            prop.load(ip);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        value=prop.getProperty(key);

        return value;
    }

    public static void setValue(String key,String value){

        try{

            FileInputStream inputStream = new FileInputStream(path);
            prop.load(new InputStreamReader(inputStream, "UTF-8"));
            inputStream.close();
            prop.setProperty(key,value);
            FileOutputStream outputStream = new FileOutputStream(path);
            prop.store(new OutputStreamWriter(outputStream, "UTF-8"), "Updated token value");
            outputStream.close();

            System.out.println("Properties file written successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
