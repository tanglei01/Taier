package com.dtstack.engine.flink.util;

import com.dtstack.engine.common.enums.ComputeType;
import com.dtstack.engine.common.enums.EJobType;
import com.dtstack.engine.common.util.SFTPHandler;
import com.dtstack.engine.flink.constrant.ConfigConstrant;
import com.dtstack.engine.flink.enums.FlinkYarnMode;
import com.dtstack.engine.worker.enums.ClassLoaderType;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Reason:
 * Date: 2017/2/21
 * Company: www.dtstack.com
 * @author xuchao
 */

public class FlinkUtil {

    private static final Logger logger = LoggerFactory.getLogger(FlinkUtil.class);

    private static final String URL_SPLITE = "/";

    private static String fileSP = File.separator;


    public static PackagedProgram buildProgram(String fromPath, String toPath, List<URL> classpaths, EJobType jobType,
                                               String entryPointClass, String[] programArgs,
                                               SavepointRestoreSettings spSetting, Configuration hadoopConf, org.apache.flink.configuration.Configuration flinkConfiguration)
            throws FileNotFoundException, ProgramInvocationException {
        if (fromPath == null) {
            throw new IllegalArgumentException("The program JAR file was not specified.");
        }

        File jarFile = downloadJar(fromPath, toPath, hadoopConf);

        ClassLoaderType classLoaderType = ClassLoaderType.getClassLoaderType(jobType);
        if (ClassLoaderType.CHILD_FIRST == classLoaderType) {
            flinkConfiguration.setString(CoreOptions.CLASSLOADER_RESOLVE_ORDER, "child-first");
            flinkConfiguration.setString(ClassLoaderType.CLASSLOADER_DTSTACK_CACHE, ClassLoaderType.CLASSLOADER_DTSTACK_CACHE_FALSE);
        } else if (ClassLoaderType.PARENT_FIRST == classLoaderType) {
            flinkConfiguration.setString(CoreOptions.CLASSLOADER_RESOLVE_ORDER, "parent-first");
            flinkConfiguration.setString(ClassLoaderType.CLASSLOADER_DTSTACK_CACHE, ClassLoaderType.CLASSLOADER_DTSTACK_CACHE_FALSE);
        } else if (ClassLoaderType.CHILD_FIRST_CACHE == classLoaderType) {
            flinkConfiguration.setString(CoreOptions.CLASSLOADER_RESOLVE_ORDER, "child-first");
            flinkConfiguration.setString(ClassLoaderType.CLASSLOADER_DTSTACK_CACHE, ClassLoaderType.CLASSLOADER_DTSTACK_CACHE_TRUE);
        } else if (ClassLoaderType.PARENT_FIRST_CACHE == classLoaderType) {
            flinkConfiguration.setString(CoreOptions.CLASSLOADER_RESOLVE_ORDER, "parent-first");
            flinkConfiguration.setString(ClassLoaderType.CLASSLOADER_DTSTACK_CACHE, ClassLoaderType.CLASSLOADER_DTSTACK_CACHE_TRUE);
        }

        PackagedProgram program = PackagedProgram.newBuilder()
                .setJarFile(jarFile)
                .setUserClassPaths(classpaths)
                .setEntryPointClassName(entryPointClass)
                .setConfiguration(flinkConfiguration)
                .setArguments(programArgs)
                .setSavepointRestoreSettings(spSetting)
                .build();

        return program;
    }

    public static String getTmpFileName(String fileUrl, String toPath){
        String name = fileUrl.substring(fileUrl.lastIndexOf(URL_SPLITE) + 1);
        String tmpFileName = toPath  + fileSP + name;
        return tmpFileName;
    }

    public static File downloadJar(String fromPath, String toPath, Configuration hadoopConf) throws FileNotFoundException {
        String localJarPath = FlinkUtil.getTmpFileName(fromPath, toPath);
        if(!FileUtil.downLoadFile(fromPath, localJarPath, hadoopConf)){
            //如果不是http 或者 hdfs协议的从本地读取
            File localFile = new File(fromPath);
            if(localFile.exists()){
                return localFile;
            }
            return null;
        }

        File jarFile = new File(localJarPath);

        // Check if JAR file exists
        if (!jarFile.exists()) {
            throw new FileNotFoundException("JAR file does not exist: " + jarFile);
        } else if (!jarFile.isFile()) {
            throw new FileNotFoundException("JAR file is not a file: " + jarFile);
        }

        return jarFile;
    }

    public static File downloadJar(String fromPath, String toPath, Configuration hadoopConf, Map<String, String> sftpConf) throws FileNotFoundException {
        boolean downloadJarFlag = false;
        if (sftpConf != null && !sftpConf.isEmpty()){
            downloadJarFlag = downloadFileFromSftp(fromPath, toPath, sftpConf);
        }
        if (!downloadJarFlag) {
            return downloadJar(fromPath, toPath, hadoopConf);
        } else {
            String localJarPath = FlinkUtil.getTmpFileName(fromPath, toPath);
            return new File(localJarPath);
        }
    }

    private static boolean downloadFileFromSftp(String fromPath, String toPath, Map<String, String> sftpConf) {
        //从Sftp下载文件到目录下
        SFTPHandler handler = null;
        try {
            handler = SFTPHandler.getInstance(sftpConf);
            int files = handler.downloadDir(fromPath, toPath);
            logger.info("download file from SFTP, fromPath:{} toPath:{} fileSize:{}", fromPath, toPath, files);
            if (files > 0) {
                return true;
            }
        } catch (Throwable e) {
            logger.error("download file from SFTP error, fromPath:{} toPath:{} ", e);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
        return false;
    }


    /**
     *
     * FIXME 仅针对sql执行方式,暂时未找到区分设置source,transform,sink 并行度的方式
     * 设置job运行的并行度
     * @param properties
     */
    public static int getEnvParallelism(Properties properties){
        String parallelismStr = properties.getProperty(ConfigConstrant.SQL_ENV_PARALLELISM);
        return StringUtils.isNotBlank(parallelismStr)?Integer.parseInt(parallelismStr):1;
    }


    /**
     * 针对MR类型整个job的并发度设置
     * @param properties
     * @return
     */
    public static int getJobParallelism(Properties properties){
        String parallelismStr = properties.getProperty(ConfigConstrant.MR_JOB_PARALLELISM);
        return StringUtils.isNotBlank(parallelismStr)?Integer.parseInt(parallelismStr):1;
    }


    /**
     * get task run mode
     * @param properties
     * @return
     */
    public static FlinkYarnMode getTaskRunMode(Properties properties, ComputeType computeType){
        String modeStr = properties.getProperty(ConfigConstrant.FLINK_TASK_RUN_MODE_KEY);

        if (StringUtils.isEmpty(modeStr)){
            if (ComputeType.STREAM == computeType) {
                return FlinkYarnMode.PER_JOB;
            } else {
                return FlinkYarnMode.SESSION;
            }
        }
        return FlinkYarnMode.mode(modeStr);
    }

}