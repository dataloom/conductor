package com.openlattice.pods

import com.openlattice.client.RetrofitFactory
import com.openlattice.datastore.services.EdmManager
import com.openlattice.edm.EdmApi
import com.openlattice.edm.EntityDataModel
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import javax.annotation.PostConstruct
import javax.inject.Inject

private const val EDM_SYNC_CONFIGURATION = "edmsync"

/**
 * Syncs the EDM from production if the [EDM_SYNC_CONFIGURATION] profile is active.
 */
@Configuration
class ConductorEdmSyncPod
@Inject constructor(val edmManager: EdmManager, val environment: Environment) {
    companion object {
        private val OL_AUDIT_FQN = FullQualifiedName("OPENLATTICE_AUDIT", "AUDIT")
        private val logger = LoggerFactory.getLogger(ConductorEdmSyncPod::class.java)
    }


    @PostConstruct
    fun syncEdm() {
        if (environment.acceptsProfiles(Profiles.of(EDM_SYNC_CONFIGURATION))) {
            logger.info("Start syncing EDM")
            updateEdm()
            logger.info("Finished syncing EDM")
        }
    }

    /**
     * Import EDM from production environment.
     */
    private fun updateEdm() {
        val prodRetrofit = RetrofitFactory.newClient(RetrofitFactory.Environment.PRODUCTION)
        val prodEdmApi = prodRetrofit.create(EdmApi::class.java)
        // get prod edm model and remove audit types
        val prodEdm = prodEdmApi.entityDataModel
        if(prodEdm == null) {
            logger.error("Received null EntityDataModel from prod. Either prod is down or EntityDataModel changed.")
            return
        }

        val cleanedProdEdm = removeAuditType(prodEdm)
        val edm = EntityDataModel(
                cleanedProdEdm.namespaces,
                cleanedProdEdm.schemas,
                cleanedProdEdm.entityTypes,
                cleanedProdEdm.associationTypes,
                cleanedProdEdm.propertyTypes)

        // get differences between prod and local
        val edmDiff = edmManager.getEntityDataModelDiff(edm)

        // update with differences
        edmManager.entityDataModel = edmDiff.diff
    }

    /**
     * Filter out audit entity sets
     */
    @SuppressFBWarnings(value=["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"],justification = "Weird bug")
    fun removeAuditType(edm: EntityDataModel): EntityDataModel {
        val propertyTypes = edm.propertyTypes.filter { it.type.toString() != OL_AUDIT_FQN.toString() }
        val entityTypes = edm.entityTypes.filter { it.type.toString() != OL_AUDIT_FQN.toString() }
        val associationTypes = edm.associationTypes.filter {
            it.associationEntityType.type.toString() != OL_AUDIT_FQN.toString()
        }

        return EntityDataModel(
                edm.namespaces,
                edm.schemas,
                entityTypes,
                associationTypes,
                propertyTypes)
    }
}