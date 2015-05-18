package org.palladiosimulator.simulizar.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Test;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.MonitorRepositoryFactory;
import org.palladiosimulator.monitorrepository.StatisticalCharacterizationEnum;
import org.palladiosimulator.runtimemeasurement.RuntimeMeasurement;
import org.palladiosimulator.runtimemeasurement.RuntimeMeasurementFactory;
import org.palladiosimulator.simulizar.access.IModelAccess;
import org.palladiosimulator.simulizar.access.ModelAccess;
import org.palladiosimulator.simulizar.reconfiguration.qvto.QVTOExecutor;
import org.palladiosimulator.simulizar.runconfig.SimuLizarWorkflowConfiguration;

import de.uka.ipd.sdq.pcm.allocation.Allocation;
import de.uka.ipd.sdq.pcm.allocation.AllocationContext;
import de.uka.ipd.sdq.pcm.allocation.util.AllocationResourceFactoryImpl;
import de.uka.ipd.sdq.pcm.core.composition.AssemblyConnector;
import de.uka.ipd.sdq.pcm.core.composition.AssemblyContext;
import de.uka.ipd.sdq.pcm.core.composition.Connector;
import de.uka.ipd.sdq.pcm.repository.BasicComponent;
import de.uka.ipd.sdq.pcm.repository.util.RepositoryResourceFactoryImpl;
import de.uka.ipd.sdq.pcm.resourceenvironment.ProcessingResourceSpecification;
import de.uka.ipd.sdq.pcm.resourceenvironment.ResourceContainer;
import de.uka.ipd.sdq.pcm.resourceenvironment.ResourceEnvironment;
import de.uka.ipd.sdq.pcm.resourceenvironment.util.ResourceenvironmentResourceFactoryImpl;
import de.uka.ipd.sdq.pcm.seff.ProbabilisticBranchTransition;
import de.uka.ipd.sdq.pcm.seff.ResourceDemandingSEFF;
import de.uka.ipd.sdq.pcm.seff.ServiceEffectSpecification;
import de.uka.ipd.sdq.pcm.system.util.SystemResourceFactoryImpl;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import de.uka.ipd.sdq.workflow.pcm.blackboard.PCMResourceSetPartition;
import de.uka.ipd.sdq.workflow.pcm.jobs.LoadPCMModelsIntoBlackboardJob;

public class QVToReconfigurationTest {

    private final String REPOSITORY_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.repository";
    private final String RESOURCE_ENVIRONMENT_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.resourceenvironment";
    private final String SYSTEM_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.system";
    private final String ALLOCATION_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.allocation";
    private final String PMS_MODEL_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.pms";
    private final String TRANSFORMATION_RULES_ADD_DUPLICATED_SERVER_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/addClonedServer";
    private final String TRANSFORMATION_RULES_ADD_SERVER_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/addNewServer";
    private final String TRANSFORMATION_RULES_OUTSOURCE_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/outsource";
    private final String TRANSFORMATION_RULES_SCALE_UP_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/scaleUp";
    private final String PMS_CONFIGURATION_KEY = "pmsFile";
    private final String RECONFIGURATION_RULES_CONFIGURATION_KEY = "reconfigurationRulesFolder";
    private final String ALLOCATION_FILE_CONFIGURATION_KEY = "allocationFile";
    private final String REPOSITORY_EXTENSION = "repository";
    private final String RESOURCE_ENVIRONMENT_EXTENSION = "resourceenvironment";
    private final String SYSTEM_EXTENSION = "system";
    private final String ALLOCATION_EXTENSION = "allocation";
    private final String BRANCH_2_ENTITY_NAME = "branch2";
    private final double BRANCH_2_EXPECTED_VALUE_AFTER_OUTSOURCING = 0.1;
    private final double BRANCH_2_EXPECTED_VALUE_BEFORE_OUTSOURCING = 0.0;
    private final double MEASUREMENT_BELOW_THRESHOLD = 1.0;
    private final double MEASUREMENT_OVER_THRESHOLD = 5.0;
    private final String SERVER_RESOURCE_CONTAINER_NAME = "server";
    private final double SERVER_EXPECTED_PROCESSING_RATE_AFTER_SCALING = 1100.0;
    private final double SERVER_EXPECTED_PROCESSING_RATE_BEFORE_SCALING = 1000.0;
    private final int EXPECTED_NUMBER_OF_SERVERS_BEFORE_ADDING = 1;
    private final int EXPECTED_NUMBER_OF_SERVERS_AFTER_ADDING = 2;

    @Test
    public void test() {
        // Deactivating test, because it kills Jenkins
        // reconfigurationTests();
    }

    private void reconfigurationTests() {
        assertEquals("The branch probability was not changed as expected!", BRANCH_2_EXPECTED_VALUE_AFTER_OUTSOURCING,
                outsource(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("The branch probability has not remained as it was expected!",
                BRANCH_2_EXPECTED_VALUE_BEFORE_OUTSOURCING, outsource(MEASUREMENT_BELOW_THRESHOLD), 0.0);

        assertEquals("Processing resources have not scaled as expected!",
                SERVER_EXPECTED_PROCESSING_RATE_AFTER_SCALING, scaleUp(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("Processing resources have not remained as it was expected!",
                SERVER_EXPECTED_PROCESSING_RATE_BEFORE_SCALING, scaleUp(MEASUREMENT_BELOW_THRESHOLD), 0.0);

        assertEquals("The server was not added!", EXPECTED_NUMBER_OF_SERVERS_AFTER_ADDING,
                addNewServer(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("The number of servers is not as expected!", EXPECTED_NUMBER_OF_SERVERS_BEFORE_ADDING,
                addNewServer(MEASUREMENT_BELOW_THRESHOLD), 0.0);

        assertEquals("The server was not added!", EXPECTED_NUMBER_OF_SERVERS_AFTER_ADDING,
                addClonedServer(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("The number of servers is not as expected!", EXPECTED_NUMBER_OF_SERVERS_BEFORE_ADDING,
                addClonedServer(MEASUREMENT_BELOW_THRESHOLD), 0.0);
    }

    private int addNewServer(double m) {
        PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_ADD_SERVER_PATH);

        Allocation allocation = pcmResourceSet.getAllocation();
        int numOfServer1Client = 0, numOfServer2Client = 0;
        for (Connector connector : allocation.getSystem_Allocation().getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {

                AssemblyConnector assemblyConnector = (AssemblyConnector) connector;
                AssemblyContext assemblyContextProviding = assemblyConnector
                        .getProvidingAssemblyContext_AssemblyConnector();
                AssemblyContext assemblyContextRequiring = assemblyConnector
                        .getRequiringAssemblyContext_AssemblyConnector();

                if (assemblyContextProviding.getEncapsulatedComponent__AssemblyContext().getEntityName()
                        .equals("server1")
                        && assemblyContextRequiring.getEncapsulatedComponent__AssemblyContext().getEntityName()
                                .equals("client")) {
                    numOfServer1Client++;
                }

                if (assemblyContextProviding.getEncapsulatedComponent__AssemblyContext().getEntityName()
                        .equals("server2")
                        && assemblyContextRequiring.getEncapsulatedComponent__AssemblyContext().getEntityName()
                                .equals("client")) {
                    numOfServer2Client++;
                }
            }
        }
        return numOfServer1Client + numOfServer2Client;
    }

    private int addClonedServer(double m) {
        PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_ADD_DUPLICATED_SERVER_PATH);

        Allocation allocation = pcmResourceSet.getAllocation();
        int numOfIServerProviders = 0;
        for (Connector connector : allocation.getSystem_Allocation().getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {

                AssemblyConnector assemblyConnector = (AssemblyConnector) connector;
                AssemblyContext assemblyContextProviding = assemblyConnector
                        .getProvidingAssemblyContext_AssemblyConnector();
                AssemblyContext assemblyContextRequiring = assemblyConnector
                        .getRequiringAssemblyContext_AssemblyConnector();

                if (assemblyContextProviding.getEncapsulatedComponent__AssemblyContext().getEntityName()
                        .equals("server1")
                        && assemblyContextRequiring.getEncapsulatedComponent__AssemblyContext().getEntityName()
                                .equals("client")) {
                    numOfIServerProviders++;
                }
            }
        }
        return numOfIServerProviders;
    }

    /**
     * Performs scaling up with the measurement passed as parameter. If the measurement is over the
     * threshold the scaling up should be performed, otherwise not.
     * 
     * @param m
     *            measurement that defines whether the scaling up is performed or not.
     * @return processing resource of the server that is to be scaled up.
     */
    private double scaleUp(double m) {
        PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_SCALE_UP_PATH);

        Allocation allocation = pcmResourceSet.getAllocation();
        ResourceEnvironment resourceEnvironment = allocation.getTargetResourceEnvironment_Allocation();
        Iterator<ResourceContainer> iteratorResourceContainer = resourceEnvironment
                .getResourceContainer_ResourceEnvironment().iterator();
        while (iteratorResourceContainer.hasNext()) {
            ResourceContainer resourceContainer = iteratorResourceContainer.next();
            if (resourceContainer.getEntityName().equals(SERVER_RESOURCE_CONTAINER_NAME)) {
                Iterator<ProcessingResourceSpecification> iteratorProcessingResourceSpecification = resourceContainer
                        .getActiveResourceSpecifications_ResourceContainer().iterator();
                while (iteratorProcessingResourceSpecification.hasNext()) {
                    ProcessingResourceSpecification processingResourceSpecification = iteratorProcessingResourceSpecification
                            .next();
                    return Double.parseDouble(processingResourceSpecification
                            .getProcessingRate_ProcessingResourceSpecification().getSpecification());
                }
            }
        }
        assertTrue("The test reached the end!", false);
        return Double.NaN;
    }

    /**
     * Performs outsourcing with the measurement passed as parameter. If the measurement is over the
     * threshold the outsourcing should be performed, otherwise not.
     * 
     * @param m
     *            measurement that defines whether the outsourcing is performed or not.
     * @return branch probability that is to be increased.
     */
    private double outsource(double m) {
        PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_OUTSOURCE_PATH);
        TreeIterator<EObject> pcmModelIterator = pcmResourceSet.getAllocation().eAllContents();
        /*
         * Iterate over all the elements of the allocation diagram.
         */
        while (pcmModelIterator.hasNext()) {
            EObject root = pcmModelIterator.next();
            /*
             * We are interested in AllocationContexts only because we can get to the server which
             * contains the SEFF that is of our interest.
             */
            if (root instanceof AllocationContext) {
                AllocationContext serverAllocationContext = (AllocationContext) root;
                AssemblyContext serverAssemblyContext = serverAllocationContext.getAssemblyContext_AllocationContext();
                /*
                 * The server that contains our SEFF is of type BasicComponent.
                 */
                if (serverAssemblyContext.getEncapsulatedComponent__AssemblyContext() instanceof BasicComponent) {
                    BasicComponent serverBasicComponent = (BasicComponent) serverAssemblyContext
                            .getEncapsulatedComponent__AssemblyContext();
                    EList<ServiceEffectSpecification> serverSeffs = serverBasicComponent
                            .getServiceEffectSpecifications__BasicComponent();
                    /*
                     * We iterate all the SEFFs within the BasicComponent.
                     */
                    for (ServiceEffectSpecification seff : serverSeffs) {
                        /*
                         * ResourceDemandingSEFF in particular are of our interest.
                         */
                        if (seff instanceof ResourceDemandingSEFF) {
                            TreeIterator<EObject> seffIterator = seff.eAllContents();
                            /*
                             * We now iterate every ResourceDemandingSEFF in attempt to find
                             * ProbabilisticBranchTransition.
                             */
                            while (seffIterator.hasNext()) {
                                EObject seffObject = seffIterator.next();
                                /*
                                 * Once we find our ProbabilisticBranchTransitions we compare their
                                 * values with the expected ones.
                                 */
                                if (seffObject instanceof ProbabilisticBranchTransition) {
                                    ProbabilisticBranchTransition branchTransition = (ProbabilisticBranchTransition) seffObject;
                                    if (branchTransition.getEntityName().equals(BRANCH_2_ENTITY_NAME)) {
                                        return branchTransition.getBranchProbability();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue("The test reached the end!", false);
        return Double.NaN;
    }

    /**
     * Creates a measurement out of parameter "m", reads in the PCM model from the folder
     * "testmodel", performs the QVTo rules that are placed in the folder "testmodel/rules" and
     * returns the resulting PCM model. The PCM model could be changed or not, depending on the
     * parameter "m".
     * 
     * @param m
     *            measurement.
     * @return The PCM model after the rules from "testmodel/rules" have been applied.
     */
    private PCMResourceSetPartition readPcmModelAndApplyTransformationRules(double m,
            String reconfigurationRulesFolderPath) {
        /*
         * Create a measurement.
         */
        MeasurementSpecification measurementSpecification = MonitorRepositoryFactory.eINSTANCE
                .createMeasurementSpecification();
        measurementSpecification.setId("_sEx-cMLAEeSZr8oGpigbHA");
        measurementSpecification.setMetricDescription(MetricDescriptionConstants.RESPONSE_TIME_METRIC);
        measurementSpecification.setStatisticalCharacterization(StatisticalCharacterizationEnum.ARITHMETIC_MEAN);
        measurementSpecification.setTemporalRestriction(null);

        RuntimeMeasurement measurement = RuntimeMeasurementFactory.eINSTANCE.createRuntimeMeasurement();
        measurement.setId("");
        measurement.setMeasuringValue(m);
        measurement.setMeasurementSpecification(measurementSpecification);

        Resource.Factory repositoryFactory = new RepositoryResourceFactoryImpl();
        Resource.Factory resourceEnvironmentFactory = new ResourceenvironmentResourceFactoryImpl();
        Resource.Factory systemFactory = new SystemResourceFactoryImpl();
        Resource.Factory allocationFactory = new AllocationResourceFactoryImpl();
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(REPOSITORY_EXTENSION, repositoryFactory);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(RESOURCE_ENVIRONMENT_EXTENSION,
                resourceEnvironmentFactory);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(SYSTEM_EXTENSION, systemFactory);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(ALLOCATION_EXTENSION, allocationFactory);

        /*
         * Get the URIs of the files needed to create the model
         */
        URI repositoryURI = URI.createPlatformPluginURI(REPOSITORY_PATH, true);
        repositoryURI = CommonPlugin.resolve(repositoryURI);
        URI resourceEnvironmentURI = URI.createPlatformPluginURI(RESOURCE_ENVIRONMENT_PATH, true);
        resourceEnvironmentURI = CommonPlugin.resolve(resourceEnvironmentURI);
        URI systemURI = URI.createPlatformPluginURI(SYSTEM_PATH, true);
        systemURI = CommonPlugin.resolve(systemURI);
        URI allocationURI = URI.createPlatformPluginURI(ALLOCATION_PATH, true);
        allocationURI = CommonPlugin.resolve(allocationURI);
        URI pmsURI = URI.createPlatformPluginURI(PMS_MODEL_PATH, true);
        pmsURI = CommonPlugin.resolve(pmsURI);
        URI reconfRulesURI = URI.createPlatformPluginURI(reconfigurationRulesFolderPath, true);
        reconfRulesURI = CommonPlugin.resolve(reconfRulesURI);

        /*
         * Read in the PCM model.
         */
        PCMResourceSetPartition pcmResourceSet = new PCMResourceSetPartition();
        pcmResourceSet.loadModel(repositoryURI);
        pcmResourceSet.loadModel(resourceEnvironmentURI);
        pcmResourceSet.loadModel(systemURI);
        pcmResourceSet.loadModel(allocationURI);
        TreeIterator<EObject> pcmModelIterator = pcmResourceSet.getRepositories().get(0).eAllContents();
        EObject monitoredElement = null;
        while (pcmModelIterator.hasNext()) {
            EObject element = pcmModelIterator.next();
            EAttribute id = element.eClass().getEIDAttribute();
            Object idAttribute = element.eGet(id);
            if (idAttribute.toString().equals("_1P7G0LwGEeSxGbiYbg6Waw")) {
                monitoredElement = element;
            }
        }

        /*
         * Put the PCM model into the MDSD blackboard.
         */
        MDSDBlackboard blackboard = new MDSDBlackboard();
        blackboard.addPartition(LoadPCMModelsIntoBlackboardJob.PCM_MODELS_PARTITION_ID, pcmResourceSet);
        IModelAccess modelAccess = new ModelAccess(blackboard);
        modelAccess.getRuntimeMeasurementModel().getMeasurements().add(measurement);

        /*
         * Create the configuration for the QVTo executor.
         */
        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put(ALLOCATION_FILE_CONFIGURATION_KEY, Paths.get(allocationURI.path()).toAbsolutePath()
                .toString());
        configuration.put(PMS_CONFIGURATION_KEY, Paths.get(pmsURI.path()).toAbsolutePath().toString());
        configuration.put(RECONFIGURATION_RULES_CONFIGURATION_KEY, Paths.get(reconfRulesURI.path()).toAbsolutePath()
                .toString());

        SimuLizarWorkflowConfiguration swfc = new SimuLizarWorkflowConfiguration(configuration);
        swfc.setMonitorRepositoryFile(configuration.get(PMS_CONFIGURATION_KEY).toString());
        swfc.setReconfigurationRulesFolder(configuration.get(RECONFIGURATION_RULES_CONFIGURATION_KEY).toString());
        QVTOExecutor qvtoExecutor = new QVTOExecutor(modelAccess, swfc);
        qvtoExecutor.executeRules(monitoredElement);
        return pcmResourceSet;
    }

}