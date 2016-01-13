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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.apache.stratos.cloud.controller.stub.pojo.Persistence;
import org.apache.stratos.cloud.controller.stub.pojo.PortMapping;
import org.apache.stratos.cloud.controller.stub.pojo.Volume;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.application.ComponentBean;
import org.apache.stratos.common.beans.application.SubscribableInfo;
import org.apache.stratos.common.beans.application.domain.mapping.DomainMappingBean;
import org.apache.stratos.common.beans.artifact.repository.ArtifactRepositoryBean;
import org.apache.stratos.common.beans.cartridge.*;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionReferenceBean;
import org.apache.stratos.common.beans.partition.PartitionBean;
import org.apache.stratos.common.beans.partition.PartitionReferenceBean;
import org.apache.stratos.common.beans.policy.autoscale.*;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.wso2.ppaas.tools.artifactmigration.exception.ArtifactLoadingException;
import org.wso2.ppaas.tools.artifactmigration.exception.TransformationException;
import org.wso2.ppaas.tools.artifactmigration.loader.ArtifactLoader400;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforms the artifacts from PPaaS 4.0.0 to 4.1.0
 */
public class Transformer {

    private static final Logger log = Logger.getLogger(Transformer.class);

    /**
     * Method to transform Auto Scale Policies
     */
    public static void transformAutoscalePolicyList() throws TransformationException {

        List<AutoscalePolicy> autoscalePolicy400List;
        AutoscalePolicyBean autoscalePolicy410 = new AutoscalePolicyBean();
        try {
            autoscalePolicy400List = ArtifactLoader400.fetchAutoscalePolicyList();
            log.info("Fetched Auto Scale Policy from PPaaS 4.0.0");

            File directoryName = new File(Constants.ROOT_DIRECTORY + Constants.DIRECTORY_POLICY_AUTOSCALE);

            for (AutoscalePolicy autoscalePolicy400 : autoscalePolicy400List) {

                autoscalePolicy410.setId(autoscalePolicy400.getId());

                //PPaaS 4.1.0 artifacts
                LoadThresholdsBean loadThresholds410 = new LoadThresholdsBean();
                RequestsInFlightThresholdsBean requestsInFlight410 = new RequestsInFlightThresholdsBean();
                MemoryConsumptionThresholdsBean memoryConsumption410 = new MemoryConsumptionThresholdsBean();
                LoadAverageThresholdsBean loadAverage410 = new LoadAverageThresholdsBean();

                requestsInFlight410
                        .setThreshold(autoscalePolicy400.getLoadThresholds().getRequestsInFlight().getUpperLimit());
                memoryConsumption410
                        .setThreshold(autoscalePolicy400.getLoadThresholds().getMemoryConsumption().getUpperLimit());
                loadAverage410.setThreshold(autoscalePolicy400.getLoadThresholds().getLoadAverage().getUpperLimit());
                loadThresholds410.setRequestsInFlight(requestsInFlight410);
                loadThresholds410.setLoadAverage(loadAverage410);
                loadThresholds410.setMemoryConsumption(memoryConsumption410);

                autoscalePolicy410.setLoadThresholds(loadThresholds410);
                JsonWriter.writeFile(directoryName, autoscalePolicy410.getId() + Constants.JSON_EXTENSION,
                        getGson().toJson(autoscalePolicy410));
            }
            log.info("Created Auto Scale Policy 4.1.0 artifacts");
        } catch (JsonSyntaxException e) {
            String msg = "JSON syntax error in retrieving auto scale policies";
            log.error(msg);
            throw new TransformationException(msg, e);
        } catch (ArtifactLoadingException e) {
            String msg = "Artifact Loading error in fetching auto scale policies";
            log.error(msg);
            throw new TransformationException(msg, e);
        }
    }

    /**
     * Method to transform network partitions
     */
    public static void transformNetworkPartitionList() throws TransformationException {

        List<Partition> networkPartition400List;
        NetworkPartitionBean networkPartition410 = new NetworkPartitionBean();
        File directoryName;
        try {
            networkPartition400List = ArtifactLoader400.fetchPartitionList();
            log.info("Fetched Network Partition List from PPaaS 4.0.0");

            int networkPartitionIterator = 1;
            for (Partition networkPartition400 : networkPartition400List) {
                networkPartition410.setId(networkPartition400.getId());
                networkPartition410.setProvider(networkPartition400.getProvider());
                List<PartitionBean> partitionsList410 = new ArrayList<>();
                PartitionBean partition410 = new PartitionBean();
                partition410.setId(Constants.NETWORK_PARTITION_ID+networkPartitionIterator);
                if (networkPartition400.getProperty() != null) {
                    List<org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean> property400List = networkPartition400
                            .getProperty();
                    List<org.apache.stratos.common.beans.PropertyBean> property410List = new ArrayList<>();

                    for (org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean property400 : property400List) {
                        org.apache.stratos.common.beans.PropertyBean property = new org.apache.stratos.common.beans.PropertyBean();
                        property.setName(property400.getName());
                        property.setValue(property400.getValue());
                        property410List.add(property);
                    }
                    partition410.setProperty(property410List);
                    partitionsList410.add(0, partition410);
                    networkPartition410.setPartitions(partitionsList410);
                }
                directoryName = new File(
                        Constants.ROOT_DIRECTORY + Constants.DIRECTORY_NETWORK_PARTITION + File.separator
                                + networkPartition400.getProvider());
                JsonWriter.writeFile(directoryName,
                        Constants.NETWORK_PARTITION_NAME + (networkPartitionIterator++) + Constants.JSON_EXTENSION,
                        getGson().toJson(networkPartition410));

                //Setting network partition ID as a system property to be used for application policies
                System.setProperty("defaultNetworkPartitionId", networkPartition400.getId());
            }
            //Setting number of network partitions as a system property to be used for deploying scripts
            System.setProperty(Constants.NO_OF_NETWORK_PARTITION, (Integer.toString(networkPartitionIterator - 1)));
            log.info("Created Network Partition List 4.1.0 artifacts");
        } catch (JsonSyntaxException e) {
            String msg = "JSON syntax error in retrieving network partition lists";
            log.error(msg);
            throw new TransformationException(msg, e);
        } catch (ArtifactLoadingException e) {
            String msg = "Artifact loading error in fetching network partition lists";
            log.error(msg);
            throw new TransformationException(msg, e);
        }
    }

    /**
     * Method to transform DeploymentPolicy
     */
    public static void transformDeploymentPolicyList() throws TransformationException {

        List<DeploymentPolicy> deploymentPolicy400List;
        DeploymentPolicyBean deploymentPolicy410 = new DeploymentPolicyBean();
        try {
            deploymentPolicy400List = ArtifactLoader400.fetchDeploymentPolicyList();
            log.info("Fetched Deployment Policy from PPaaS 4.0.0");
            File directoryName = new File(Constants.ROOT_DIRECTORY + Constants.DIRECTORY_POLICY_DEPLOYMENT);

            for (DeploymentPolicy deploymentPolicy400 : deploymentPolicy400List) {

                deploymentPolicy410.setId(deploymentPolicy400.getId());
                List<PartitionGroup> partitionGroup400List = deploymentPolicy400.getPartitionGroup();
                List<NetworkPartitionReferenceBean> networkPartitions410List = new ArrayList<>();

                int a = 0;
                for (PartitionGroup partitionGroup : partitionGroup400List) {
                    NetworkPartitionReferenceBean tempNetworkPartition = new NetworkPartitionReferenceBean();
                    tempNetworkPartition.setPartitionAlgo(partitionGroup.getPartitionAlgo());

                    List<Partition> partition400List = partitionGroup.getPartition();
                    List<PartitionReferenceBean> partitions410List = new ArrayList<>();

                    int partitionIterator = 0;
                    for (Partition partition : partition400List) {

                        tempNetworkPartition.setId(partition.getId());
                        PartitionReferenceBean tempPartition = new PartitionReferenceBean();
                        tempPartition.setId(Constants.NETWORK_PARTITION_ID+ (partitionIterator+1));
                        tempPartition.setPartitionMax(partition.getPartitionMax());

                        if (partition.getProperty() != null) {
                            List<PropertyBean> property400List = partition.getProperty();
                            List<org.apache.stratos.common.beans.PropertyBean> property410List = new ArrayList<>();
                            int c = 0;
                            for (PropertyBean propertyBean400 : property400List) {
                                org.apache.stratos.common.beans.PropertyBean tempPropertyBean410 = new org.apache.stratos.common.beans.PropertyBean();

                                tempPropertyBean410.setName(propertyBean400.getName());
                                tempPropertyBean410.setValue(propertyBean400.getValue());
                                property410List.add(c++, tempPropertyBean410);
                            }
                            tempPartition.setProperty(property410List);
                        }
                        partitions410List.add(partitionIterator++, tempPartition);
                    }
                    tempNetworkPartition.setPartitions(partitions410List);
                    networkPartitions410List.add(a, tempNetworkPartition);
                }
                deploymentPolicy410.setNetworkPartitions(networkPartitions410List);

                JsonWriter.writeFile(directoryName, deploymentPolicy410.getId() + Constants.JSON_EXTENSION,
                        getGson().toJson(deploymentPolicy410));
            }
            log.info("Created Deployment Policy 4.1.0 artifacts");
        } catch (JsonSyntaxException e) {
            String msg = "JSON syntax error in retrieving deployment policies";
            log.error(msg);
            throw new TransformationException(msg, e);

        } catch (ArtifactLoadingException e) {
            String msg = "Artifact Loading error in fetching deployment policies";
            log.error(msg);
            throw new TransformationException(msg, e);
        }
    }

    /**
     * Method to transform cartridge list
     */
    public static void transformCartridgeList() throws TransformationException {
        List<Cartridge> cartridge400List;
        List<CartridgeInfoBean> subscription400List;
        ApplicationBean application410 = new ApplicationBean();
        CartridgeBean cartridge410 = new CartridgeBean();

        try {
            cartridge400List = ArtifactLoader400.fetchCartridgeList();
            log.info("Fetched Cartridge List from PPaaS 4.0.0");

            subscription400List = ArtifactLoader400.fetchSubscriptionDataList();
            log.info("Fetched Subscription List");

            File outputDirectoryNameCartridge = new File(Constants.ROOT_DIRECTORY + Constants.DIRECTORY_CARTRIDGE);

            for (Cartridge cartridge : cartridge400List) {

                ComponentBean components = new ComponentBean();
                List<CartridgeReferenceBean> cartridges = new ArrayList<>();
                CartridgeReferenceBean cartridgeReference410 = new CartridgeReferenceBean();
                SubscribableInfo subscribableInfo = new SubscribableInfo();

                //Getting corresponding subscription data
                List<ArtifactRepositoryBean> signup410List = new ArrayList<>();
                List<DomainMappingBean> domainMapping410List = new ArrayList<>();
                int domainMappingIterator = 0;
                int a = 0;
                for (CartridgeInfoBean subscription : subscription400List) {

                    if (cartridge.getCartridgeType().equalsIgnoreCase(subscription.getCartridgeType())) {

                        subscribableInfo.setAutoscalingPolicy(subscription.getAutoscalePolicy());
                        subscribableInfo.setDeploymentPolicy(subscription.getDeploymentPolicy());
                        subscribableInfo.setAlias(subscription.getAlias());

                        ArtifactRepositoryBean artifactRepository = new ArtifactRepositoryBean();

                        artifactRepository.setAlias(subscription.getAlias());
                        artifactRepository.setPrivateRepo(subscription.isPrivateRepo());
                        artifactRepository.setRepoUrl(subscription.getRepoURL());
                        artifactRepository.setRepoUsername(subscription.getRepoUsername());
                        artifactRepository.setRepoPassword(subscription.getRepoPassword());

                        signup410List.add(a++, artifactRepository);

                        List<SubscriptionDomainBean> domainMapping400 = ArtifactLoader400
                                .fetchDomainMappingList(cartridge.getCartridgeType(), subscription.getAlias());

                        for (SubscriptionDomainBean domainMapping : domainMapping400) {
                            DomainMappingBean domainMappingBean = new DomainMappingBean();
                            domainMappingBean.setCartridgeAlias(subscription.getAlias());
                            domainMappingBean.setDomainName(domainMapping.getDomainName());
                            domainMappingBean.setContextPath(domainMapping.getApplicationContext());
                            domainMapping410List.add(domainMappingIterator++, domainMappingBean);
                        }
                    }
                }
                cartridgeReference410.setSubscribableInfo(subscribableInfo);
                cartridgeReference410.setType(cartridge.getCartridgeType());

                cartridges.add(0, cartridgeReference410);
                components.setCartridges(cartridges);
                application410.setComponents(components);
                application410.setAlias(cartridgeReference410.getSubscribableInfo().getAlias());
                application410.setName(cartridge.getDisplayName());
                application410.setApplicationId(cartridge.getDisplayName());
                application410.setDescription(cartridge.getDescription());

                File outputDirectoryNameApp = new File(
                        Constants.ROOT_DIRECTORY + Constants.DIRECTORY_APPLICATION + File.separator + application410
                                .getName() + File.separator + Constants.DIRECTORY_ARTIFACTS);
                JsonWriter.writeFile(outputDirectoryNameApp, application410.getName() + Constants.JSON_EXTENSION,
                        getGson().toJson(application410));
                JsonWriter.writeFile(outputDirectoryNameApp, Constants.FILENAME_APPLICATION_SIGNUP,
                        getGson().toJson(signup410List));

                //Converting domain mapping list string to the standard format
                String domainMappingJsonString = "{\"domainMappings\":" + getGson().toJson(domainMapping410List) + "}";
                JsonWriter
                        .writeFile(outputDirectoryNameApp, Constants.FILENAME_DOMAIN_MAPPING, domainMappingJsonString);

                ConversionTool.addCommonDeployingScript(
                        Constants.ROOT_DIRECTORY + Constants.DIRECTORY_OUTPUT_SCRIPT + File.separator + application410
                                .getName(), subscribableInfo, cartridge.getDisplayName());

                cartridge410.setDisplayName(cartridge.getDisplayName());
                cartridge410.setDescription(cartridge.getDescription());
                cartridge410.setCategory(Constants.CARTRIDGE_CATEGORY);
                cartridge410.setType(cartridge.getCartridgeType());
                cartridge410.setProvider(cartridge.getProvider());
                cartridge410.setVersion(cartridge.getVersion());
                cartridge410.setHost(cartridge.getHostName());
                cartridge410.setMultiTenant(cartridge.isMultiTenant());

                //Setting the port mappings details
                //Use of default values in port mappings
                List<PortMappingBean> portMappingList = new ArrayList<>();
                PortMappingBean portMappingBean = new PortMappingBean();
                portMappingBean.setPort(Integer.parseInt(System.getProperty(Constants.PORT)));
                portMappingBean.setProxyPort(Integer.parseInt(System.getProperty(Constants.PROXY_PORT)));
                portMappingBean.setProtocol(System.getProperty(Constants.PROTOCOL));
                portMappingList.add(0, portMappingBean);

                cartridge410.setPortMapping(portMappingList);

                //Overwrite the default mappings if port mappings exist
                if (cartridge.getPortMappings() != null) {
                    PortMapping[] portMapping400List = cartridge.getPortMappings();
                    List<PortMappingBean> portMapping410List = new ArrayList<>();

                    int b = 0;
                    for (PortMapping portMapping : portMapping400List) {

                        PortMappingBean portMappingBeanTemp = new PortMappingBean();
                        portMappingBeanTemp.setPort(Integer.parseInt(portMapping.getPort()));
                        portMappingBeanTemp.setProtocol(portMapping.getProtocol());
                        portMappingBeanTemp.setProxyPort(Integer.parseInt(portMapping.getProxyPort()));

                        portMapping410List.add(b++, portMappingBeanTemp);
                    }
                    cartridge410.setPortMapping(portMapping410List);
                }

                if (cartridge.getPersistence() != null) {
                    Persistence persistence400 = cartridge.getPersistence();
                    PersistenceBean persistenceBean410 = new PersistenceBean();
                    persistenceBean410.setRequired(persistence400.getPersistanceRequired());

                    Volume[] volume400Array = persistence400.getVolumes();
                    List<VolumeBean> volumeBean410List = new ArrayList<>();

                    int b = 0;
                    for (Volume volume : volume400Array) {

                        VolumeBean volumeBeanTemp = new VolumeBean();
                        volumeBeanTemp.setId(volume.getId());
                        volumeBeanTemp.setSize(String.valueOf(volume.getSize()));
                        volumeBeanTemp.setMappingPath(volume.getMappingPath());
                        volumeBeanTemp.setDevice(volume.getDevice());
                        volumeBeanTemp.setRemoveOnTermination(volume.isRemoveOnterminationSpecified());

                        volumeBean410List.add(b++, volumeBeanTemp);
                    }
                    persistenceBean410.setVolume(volumeBean410List);
                    cartridge410.setPersistence(persistenceBean410);
                }

                //Setting IaaS provider details
                List<IaasProviderBean> iaasProviderList = new ArrayList<>();
                IaasProviderBean iaasProvider = new IaasProviderBean();
                iaasProvider.setType(System.getProperty(Constants.IAAS));
                iaasProvider.setImageId(System.getProperty(Constants.IAAS_IMAGE_ID));

                iaasProviderList.add(0, iaasProvider);
                cartridge410.setIaasProvider(iaasProviderList);

                JsonWriter.writeFile(outputDirectoryNameCartridge,
                        cartridge410.getDisplayName() + Constants.JSON_EXTENSION, getGson().toJson(cartridge410));
            }
            log.info("Created Cartridge List 4.1.0 artifacts");
            log.info("Created Application List 4.1.0 artifacts");

        } catch (JsonSyntaxException e) {
            String msg = "JSON syntax error in retrieving cartridges";
            log.error(msg);
            throw new TransformationException(msg, e);
        } catch (ArtifactLoadingException e) {
            String msg = "Artifact loading error in fetching cartridges";
            log.error(msg);
            throw new TransformationException(msg, e);
        }
    }

    /**
     * Method to add default application policies
     */
    public static void addDefaultApplicationPolicies(String networkPartition) {
        ApplicationPolicyBean applicationPolicyBean = new ApplicationPolicyBean();

        applicationPolicyBean.setId(Constants.APPLICATION_POLICY_ID);
        applicationPolicyBean.setAlgorithm(Constants.APPLICATION_POLICY_ALGO);
        // String networkPartition = Constants.NETWORK_PARTITION_NAME +"1";
        String[] networkPartitions = { networkPartition };
        applicationPolicyBean.setNetworkPartitions(networkPartitions);

        File directoryName = new File(Constants.ROOT_DIRECTORY + Constants.DIRECTORY_POLICY_APPLICATION);
        JsonWriter.writeFile(directoryName, Constants.APPLICATION_POLICY_ID + Constants.JSON_EXTENSION,
                getGson().toJson(applicationPolicyBean));
    }

    /**
     * Method to get Gson
     */
    private static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.setPrettyPrinting().create();
    }
}