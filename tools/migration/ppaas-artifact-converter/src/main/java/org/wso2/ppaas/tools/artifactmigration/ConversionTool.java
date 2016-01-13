/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.ppaas.tools.artifactmigration;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.stratos.common.beans.application.SubscribableInfo;

import java.io.*;

/*
Class for conversion
 */
class ConversionTool {

    private static final Logger log = Logger.getLogger(ConversionTool.class);

    /**
     * Method to handle console inputs
     */
    public static void handleConsoleInputs() {

        log.info("CLI started...");
        Console console = System.console();
        if (System.getProperty(Constants.BASE_URL400) == null || System.getProperty(Constants.BASE_URL400).isEmpty()) {
            System.out.println("Enter the Base URL of PPaaS 4.0.0:");
            System.setProperty(Constants.BASE_URL400, console.readLine());
        }
        if (System.getProperty(Constants.USERNAME400) == null || System.getProperty(Constants.USERNAME400).isEmpty()) {
            System.out.println("Enter the User name of PPaaS 4.0.0:");
            System.setProperty(Constants.USERNAME400, console.readLine());
        }
        if (System.getProperty(Constants.PASSWORD400) == null || System.getProperty(Constants.PASSWORD400).isEmpty()) {
            System.out.println("Enter the Password of PPaaS 4.0.0:");
            char[] passwordChars = console.readPassword();
            System.setProperty(Constants.PASSWORD400, new String(passwordChars));
        }
        if (System.getProperty(Constants.BASE_URL410) == null || System.getProperty(Constants.BASE_URL410).isEmpty()) {
            System.out.println("Enter the Base URL of PPaaS 4.1.0:");
            System.setProperty(Constants.BASE_URL410, console.readLine());
        }
        if (System.getProperty(Constants.USERNAME410) == null || System.getProperty(Constants.USERNAME410).isEmpty()) {
            System.out.println("Enter the User name of PPaaS 4.1.0:");
            System.setProperty(Constants.USERNAME410, console.readLine());
        }
        if (System.getProperty(Constants.PASSWORD410) == null || System.getProperty(Constants.PASSWORD410).isEmpty()) {
            System.out.println("Enter the Password of PPaaS 4.1.0:");
            char[] passwordChars = console.readPassword();
            System.setProperty(Constants.PASSWORD410, new String(passwordChars));
        }
        if (System.getProperty(Constants.IAAS) == null || System.getProperty(Constants.IAAS).isEmpty()) {
            System.out.println("Enter the IaaS provider name:");
            System.setProperty(Constants.IAAS, console.readLine());
        }
        if (System.getProperty(Constants.IAAS_IMAGE_ID) == null || System.getProperty(Constants.IAAS_IMAGE_ID)
                .isEmpty()) {
            System.out.println("Enter the IaaS image id:");
            System.setProperty(Constants.IAAS_IMAGE_ID, console.readLine());
        }
    }

    /**
     * Method to start transformation
     */
    public static void startTransformation() {

        log.info("Artifact Migration started...");
        try {
            Transformer.transformNetworkPartitionList();
            Transformer.transformAutoscalePolicyList();
            Transformer.transformDeploymentPolicyList();
            Transformer.transformCartridgeList();
            Transformer.addDefaultApplicationPolicies(System.getProperty("defaultNetworkPartitionId"));
            System.out.println("Conversion completed successfully");
            System.out.println(
                    "Default values have been used for the port mappings of the cartridges and can be updated in conf/config.properties file.");
        } catch (Exception e) {
            log.error("Error while converting the artifacts ", e);
            System.out.println("Error while transforming NetworkPartition list. See log for more details.");
        }
    }

    /**
     * Method to add script directories specific to each IaaS
     *
     * @param outputLocation output location of the script directories
     */
    private static void addIaasScriptDirectories(String outputLocation) {

        File sourceLocationEc2 = new File(Constants.DIRECTORY_SOURCE_SCRIPT + Constants.DIRECTORY_SOURCE_SCRIPT_EC2);
        File sourceLocationGce = new File(Constants.DIRECTORY_SOURCE_SCRIPT + Constants.DIRECTORY_SOURCE_SCRIPT_GCE);
        File sourceLocationKub = new File(
                Constants.DIRECTORY_SOURCE_SCRIPT + Constants.DIRECTORY_SOURCE_SCRIPT_KUBERNETES);
        File sourceLocationMock = new File(Constants.DIRECTORY_SOURCE_SCRIPT + Constants.DIRECTORY_SOURCE_SCRIPT_MOCK);
        File sourceLocationOS = new File(
                Constants.DIRECTORY_SOURCE_SCRIPT + Constants.DIRECTORY_SOURCE_SCRIPT_OPENSTACK);
        File targetLocation = new File(outputLocation);
        try {
            FileUtils.copyDirectoryToDirectory(sourceLocationEc2, targetLocation);
            FileUtils.copyDirectoryToDirectory(sourceLocationGce, targetLocation);
            FileUtils.copyDirectoryToDirectory(sourceLocationKub, targetLocation);
            FileUtils.copyDirectoryToDirectory(sourceLocationMock, targetLocation);
            FileUtils.copyDirectoryToDirectory(sourceLocationOS, targetLocation);
        } catch (IOException e) {
            log.error("Error in copying scripts directory ", e);
        }
    }

    /**
     * Method to add common deploying script
     *
     * @param outputLocation   output location of the script
     * @param subscribableInfo subscribable information
     * @param cartridgeName    cartridge name
     */
    public static void addCommonDeployingScript(String outputLocation, SubscribableInfo subscribableInfo,
            String cartridgeName) {
        BufferedReader reader = null;
        FileWriter writer = null;
        try {
            File file = new File(Constants.DIRECTORY_SOURCE_SCRIPT + Constants.DIRECTORY_SOURCE_SCRIPT_DEPLOY);
            reader = new BufferedReader(new FileReader(file));
            String line, scriptText = "";
            while ((line = reader.readLine()) != null) {
                scriptText += line + System.getProperty("line.separator");
            }

            if (subscribableInfo.getDeploymentPolicy() != null) {
                scriptText = scriptText.replaceAll("deployment-policy_name", subscribableInfo.getDeploymentPolicy());
            }
            if (subscribableInfo.getAutoscalingPolicy() != null) {
                scriptText = scriptText.replaceAll("autoscaling-policy_name", subscribableInfo.getAutoscalingPolicy());
            }
            if (cartridgeName != null) {
                scriptText = scriptText.replaceAll("cartridge_name", cartridgeName);
                scriptText = scriptText.replaceAll("application_name", cartridgeName);
            }
            scriptText = scriptText.replaceAll("uname", System.getProperty(Constants.USERNAME410));
            scriptText = scriptText.replaceAll("pword", System.getProperty(Constants.PASSWORD410));
            scriptText = scriptText.replaceAll("base-url", System.getProperty(Constants.BASE_URL410));

            //Updating network partitions deploying commands
            String beginScriptText = scriptText.substring(0, scriptText.indexOf('*') + 2);
            String endScriptText = scriptText.substring(scriptText.indexOf('*') + 2);

            String modifiedScriptText = beginScriptText;
            for (int i = 1; i <= Integer.parseInt(System.getProperty(Constants.NO_OF_NETWORK_PARTITION)); i++) {
                modifiedScriptText +=
                        System.getProperty("line.separator") + Constants.NETWORK_PARTITION_DEPLOYMENT_COMMAND_PART1 + i
                                + Constants.NETWORK_PARTITION_DEPLOYMENT_COMMAND_PART2;
            }
            modifiedScriptText += endScriptText;
            File outputDirectory = new File(outputLocation + Constants.DIRECTORY_OUTPUT_SCRIPT_DEPLOY);

            boolean hasCreated = outputDirectory.mkdirs();
            if (!outputDirectory.exists() && !hasCreated) {
                throw new IOException("Error in creating the output directory");
            }
            writer = new FileWriter(new File(outputDirectory.getPath() + Constants.FILE_SOURCE_SCRIPT_DEPLOY), false);
            writer.write(modifiedScriptText);

        } catch (IOException e) {
            log.error("Error in copying scripts directory ", e);
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (writer != null)
                    writer.close();
            } catch (IOException ignore) {
            }
        }
        addIaasScriptDirectories(outputLocation + File.separator + "scripts");
    }

    /**
     * Method to get configuration details
     */
    public static void readInitialConfiguration() {
        try {
            final PropertiesConfiguration propsConfig = new PropertiesConfiguration(System.getProperty("config"));
            SystemConfiguration.setSystemProperties(propsConfig);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config file: " + e);
        }
    }
}