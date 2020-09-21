package io.jenkins.plugins.alicloud.edas;

import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.alicloud.edas.enumeration.PackageType;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import java.util.logging.Logger;


public class EDASUtils {
    private static final Logger logger = Logger.getLogger(EDASUtils.class.getName());
    public static final String ALL_NAMESPACE = "";

    public static String getValue(Run<?, ?> build, TaskListener listener, String value) {
        return strip(replaceMacros(build, listener, value));
    }

    public static String strip(String str) {
        return StringUtils.strip(str, "/ ");
    }


    public static String replaceMacros(Run<?, ?> build, TaskListener listener, String inputString) {
        String returnString = inputString;
        if (build != null && inputString != null) {
            try {
                Map<String, String> messageEnvVars = new HashMap<String, String>();

                messageEnvVars.putAll(build.getCharacteristicEnvVars());
                messageEnvVars.putAll(build.getEnvironment(listener));

                returnString = Util.replaceMacro(inputString, messageEnvVars);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Couldn't replace macros in message: ", e);
            }
        }
        return returnString;

    }

    public static String getCurrentTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }

    public static PackageType getPackageType(String target) {
        if (target.startsWith("http") || target.startsWith("https")) {
            return PackageType.REMOTE_FILE;
        }
        return PackageType.LOCAL_FILE;
    }

    public static File getLocalFileObject(FilePath workspace, String targetObject) throws Exception {
        FilePath rootFileObject = new FilePath(workspace, targetObject);
        if (!rootFileObject.exists()) {
            logger.log(Level.SEVERE, "no file found in file path " + rootFileObject.getName());
            return null;
        }
        String suffix = getSuffix(targetObject);
        File resultFile = File.createTempFile(rootFileObject.getBaseName(), suffix);
        rootFileObject.copyTo(new FileOutputStream(resultFile));

        return resultFile;
    }

    public static String getSuffix(String targetObject) {
        String suffix = ".jar";
        if (targetObject.endsWith(".war") || targetObject.endsWith(".War") || targetObject.endsWith(".WAR")) {
            suffix = ".war";
        }
        return suffix;
    }

    public static void edasLog(TaskListener listener, String msg) {
        listener.getLogger().println(msg);
    }
}
