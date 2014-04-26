package org.palladiosimulator.simulizar.interpreter.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.UnsupportedDataTypeException;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.probeframework.calculator.ICalculatorFactory;
import org.palladiosimulator.probeframework.probes.Probe;
import org.palladiosimulator.probeframework.probes.TriggeredProbe;
import org.palladiosimulator.simulizar.access.IModelAccessFactory;
import org.palladiosimulator.simulizar.access.PMSAccess;
import org.palladiosimulator.simulizar.access.PRMAccess;
import org.palladiosimulator.simulizar.metrics.aggregators.ResponseTimeAggregator;
import org.palladiosimulator.simulizar.pms.MeasurementSpecification;
import org.palladiosimulator.simulizar.pms.PerformanceMetricEnum;
import org.palladiosimulator.simulizar.prm.PrmFactory;

import de.uka.ipd.sdq.pcm.core.entity.Entity;
import de.uka.ipd.sdq.pcm.seff.ExternalCallAction;
import de.uka.ipd.sdq.pcm.usagemodel.EntryLevelSystemCall;
import de.uka.ipd.sdq.pcm.usagemodel.UsageScenario;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.probes.TakeCurrentSimulationTimeProbe;

/**
 * Class for listening to interpreter events in order to store collected data
 * using the ProbeFramework
 * 
 * @author snowball
 * 
 */
public class ProbeFrameworkListener extends AbstractInterpreterListener {

    private static final Logger LOG = Logger.getLogger(ProbeFrameworkListener.class);
    private static final int START_PROBE_INDEX = 0;
    private static final int STOP_PROBE_INDEX = 1;

    private final PMSAccess pmsModelAccess;
    private final PRMAccess prmAccess;
    private final ICalculatorFactory calculatorFactory;

    private final Map<EObject, List<TriggeredProbe>> currentTimeProbes = new HashMap<EObject, List<TriggeredProbe>>();
    private final TriggeredProbe reconfTimeProbe;

    /**
     * @param modelAccessFactory Provides access to simulated models
     * @param simuComModel Provides access to the central simulation
     */
    public ProbeFrameworkListener(final IModelAccessFactory modelAccessFactory, final SimuComModel simuComModel) {
        super();
        this.pmsModelAccess = modelAccessFactory.getPMSModelAccess();
        this.prmAccess = modelAccessFactory.getPRMModelAccess();
        this.calculatorFactory = simuComModel.getProbeFrameworkContext().getCalculatorFactory();
        this.reconfTimeProbe = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.upb.pcm.interpreter.interpreter.listener.AbstractInterpreterListener#
     * beginUsageScenarioInterpretation
     * (de.upb.pcm.interpreter.interpreter.listener.ModelElementPassedEvent)
     */
    @Override
    public void beginUsageScenarioInterpretation(final ModelElementPassedEvent<UsageScenario> event) {
        this.startMeasurement(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.upb.pcm.interpreter.interpreter.listener.AbstractInterpreterListener#
     * endUsageScenarioInterpretation
     * (de.upb.pcm.interpreter.interpreter.listener.ModelElementPassedEvent)
     */
    @Override
    public void endUsageScenarioInterpretation(final ModelElementPassedEvent<UsageScenario> event) {
        this.endMeasurement(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.upb.pcm.interpreter.interpreter.listener.AbstractInterpreterListener#
     * beginEntryLevelSystemCallInterpretation
     * (de.upb.pcm.interpreter.interpreter.listener.ModelElementPassedEvent)
     */
    @Override
    public void beginEntryLevelSystemCallInterpretation(final ModelElementPassedEvent<EntryLevelSystemCall> event) {
        this.startMeasurement(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.upb.pcm.interpreter.interpreter.listener.AbstractInterpreterListener#
     * endEntryLevelSystemCallInterpretation
     * (de.upb.pcm.interpreter.interpreter.listener.ModelElementPassedEvent)
     */
    @Override
    public void endEntryLevelSystemCallInterpretation(final ModelElementPassedEvent<EntryLevelSystemCall> event) {
        this.endMeasurement(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.upb.pcm.simulizar.interpreter.listener.AbstractInterpreterListener#
     * beginExternalCallInterpretation
     * (de.upb.pcm.simulizar.interpreter.listener.ModelElementPassedEvent)
     */
    @Override
    public void beginExternalCallInterpretation(final RDSEFFElementPassedEvent<ExternalCallAction> event) {
        this.startMeasurement(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.upb.pcm.simulizar.interpreter.listener.AbstractInterpreterListener#
     * endExternalCallInterpretation
     * (de.upb.pcm.simulizar.interpreter.listener.ModelElementPassedEvent)
     */
    @Override
    public void endExternalCallInterpretation(final RDSEFFElementPassedEvent<ExternalCallAction> event) {
        this.endMeasurement(event);
    }

    /**
     * Initializes response time measurement.
     * 
     */
    private <T extends Entity> void initReponseTimeMeasurement(final ModelElementPassedEvent<T> event) {

        final EObject modelElement = event.getModelElement();
        final SimuComModel simuComModel = event.getThread().getModel();

        final MeasurementSpecification measurementSpecification = this.pmsModelAccess.isMonitored(modelElement,
                PerformanceMetricEnum.RESPONSE_TIME);
        if (elementShouldBeMonitored(measurementSpecification) && !entityIsAlreadyInstrumented(modelElement)) {
            final List<Probe> probeList = createStartAndStopProbe(modelElement, simuComModel);
            
            final String calculatorName = this.getCalculatorName(event);
            final Calculator calculator = calculatorFactory.buildResponseTimeCalculator(calculatorName,probeList);

            try {
                new ResponseTimeAggregator(this.prmAccess, measurementSpecification, calculator, calculatorName,
                        modelElement, PrmFactory.eINSTANCE.createPCMModelElementMeasurement(), simuComModel
                        .getSimulationControl().getCurrentSimulationTime());
            } catch (final UnsupportedDataTypeException e) {
                LOG.error(e);
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Initializes response time measurement.
     * 
     */
    private <T extends Entity> void initReconfTimeMeasurement(final ReconfigurationEvent event) {

        final SimuComModel simuComModel = event.getModel();

        final Probe probe = new TakeCurrentSimulationTimeProbe(simuComModel.getSimulationControl());
        
        final String calculatorName = "Reconfiguration";
        final Calculator calculator = this.calculatorFactory.buildIdentityCalculator(calculatorName,probe);
    }

    /**
     * @param modelElement
     * @param simuComModel
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected List<Probe> createStartAndStopProbe(final EObject modelElement, final SimuComModel simuComModel) {
        final List probeList = new ArrayList<TriggeredProbe>(2);
        probeList.add(new TakeCurrentSimulationTimeProbe(simuComModel.getSimulationControl()));
        probeList.add(new TakeCurrentSimulationTimeProbe(simuComModel.getSimulationControl()));
        currentTimeProbes.put(modelElement, Collections.unmodifiableList(probeList));
        return probeList;
    }

    /**
     * @param measurementSpecification
     * @return
     */
    protected boolean elementShouldBeMonitored(final MeasurementSpecification measurementSpecification) {
        return measurementSpecification != null;
    }

    /**
     * @param modelElement
     * @return
     */
    protected boolean entityIsAlreadyInstrumented(final EObject modelElement) {
        return this.currentTimeProbes.containsKey(modelElement);
    }

    /**
     * @param <T>
     * @param event
     */
    private <T extends Entity> void startMeasurement(final ModelElementPassedEvent<T> event) {
        this.initReponseTimeMeasurement(event);
        if (this.currentTimeProbes.containsKey(event.getModelElement())) {
            this.currentTimeProbes.get(event.getModelElement()).get(START_PROBE_INDEX)
            .takeMeasurement(event.getThread().getRequestContext());
        }
    }

    /**
     * @param event
     */
    private <T extends Entity> void endMeasurement(final ModelElementPassedEvent<T> event) {
        if (this.currentTimeProbes.containsKey(event.getModelElement())) {
            this.currentTimeProbes.get(event.getModelElement()).get(STOP_PROBE_INDEX)
            .takeMeasurement(event.getThread().getRequestContext());
        }
    }

    /**
     * @param event
     * @return
     */
    private <T extends Entity> String getCalculatorName(final ModelElementPassedEvent<T> event) {
        final Entity entity = event.getModelElement();
        final StringBuilder sb = new StringBuilder();

        sb.append(entity.eClass().getName());
        sb.append(" >");
        sb.append(entity.getEntityName());
        sb.append(" (ID: ");
        sb.append(entity.getId());
        if (event instanceof RDSEFFElementPassedEvent) {
            final RDSEFFElementPassedEvent<T> rdseffEvent = (RDSEFFElementPassedEvent<T>) event;
            sb.append(", AssCtx: ");
            sb.append(rdseffEvent.getAssemblyContext().getId());
        }
        sb.append(" )<");

        return sb.toString();
    }

    @Override
    public void reconfigurationInterpretation(ReconfigurationEvent event) {
        if(this.reconfTimeProbe == null) {
            initReconfTimeMeasurement(event);
        } else {
            this.reconfTimeProbe.takeMeasurement();
        }
    }
}