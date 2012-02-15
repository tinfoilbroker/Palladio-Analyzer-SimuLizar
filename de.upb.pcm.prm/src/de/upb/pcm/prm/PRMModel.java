/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package de.upb.pcm.prm;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>PRM Model</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Base class representing the prm model.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link de.upb.pcm.prm.PRMModel#getPcmModelElementMeasurements <em>Pcm Model Element Measurements</em>}</li>
 * </ul>
 * </p>
 *
 * @see de.upb.pcm.prm.PrmPackage#getPRMModel()
 * @model
 * @generated
 */
public interface PRMModel extends UniqueElement
{
   /**
    * Returns the value of the '<em><b>Pcm Model Element Measurements</b></em>' containment reference list.
    * The list contents are of type {@link de.upb.pcm.prm.PCMModelElementMeasurement}.
    * <!-- begin-user-doc -->
    * <p>
    * If the meaning of the '<em>Pcm Model Element Measurements</em>' containment reference list isn't clear,
    * there really should be more of a description here...
    * </p>
    * <!-- end-user-doc -->
    * @return the value of the '<em>Pcm Model Element Measurements</em>' containment reference list.
    * @see de.upb.pcm.prm.PrmPackage#getPRMModel_PcmModelElementMeasurements()
    * @model containment="true"
    * @generated
    */
   EList<PCMModelElementMeasurement> getPcmModelElementMeasurements();

} // PRMModel
