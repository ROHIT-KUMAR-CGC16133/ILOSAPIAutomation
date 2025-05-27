package utils;

import java.io.*;
import java.util.Properties;

public class PropertiesReadWrite {

    public static Properties prop;
    public static String envpath =System.getProperty("user.dir")+ "/src/main/resources/env.properties";
    public static String environment = getEnvvalue("env");
    public static String getEnvvalue(String key) {
        Properties properties= new Properties();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(envpath);
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String getValue(String key) {
        String value=null;
        prop = new Properties();
        FileInputStream ip = null;
        try {
            ip = new FileInputStream(System.getProperty("user.dir")+ "/src/main/resources/config_file/"+environment+ "/config.properties");
            prop.load(ip);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        value=prop.getProperty(key);

        return value;
    }

    public static void setValue(String key,String value){

        try{

            FileInputStream inputStream = new FileInputStream(System.getProperty("user.dir")+ "/src/main/resources/config_file/"+environment+ "/config.properties");
            prop.load(new InputStreamReader(inputStream, "UTF-8"));
            inputStream.close();
            prop.setProperty(key,value);
            FileOutputStream outputStream = new FileOutputStream(System.getProperty("user.dir")+ "/src/main/resources/config_file/"+environment+ "/config.properties");
            prop.store(new OutputStreamWriter(outputStream, "UTF-8"), "Updated token value");
            outputStream.close();

            System.out.println("Properties file written successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
