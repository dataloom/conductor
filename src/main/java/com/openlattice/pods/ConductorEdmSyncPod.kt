package com.openlattice.pods

import com.openlattice.client.RetrofitFactory
import com.openlattice.datastore.services.EdmManager
import com.openlattice.edm.EdmApi
import com.openlattice.edm.EntityDataModel
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.annotation.PostConstruct
import javax.inject.Inject

private const val EDM_SYNC_CONFIGURATION = "edmsync"

/**
 * Syncs the EDM from production if the [EDM_SYNC_CONFIGURATION] profile is active.
 */
@Configuration
class ConductorEdmSyncPod
@Inject constructor(val edmManager: EdmManager, val environment: Environment) {
    private val OL_AUDIT_FQN = FullQualifiedName("OPENLATTICE_AUDIT", "AUDIT")

    companion object {
        private val logger = LoggerFactory.getLogger(ConductorEdmSyncPod::class.java)
    }


    @PostConstruct
    fun syncEdm() {
        if (environment.acceptsProfiles(EDM_SYNC_CONFIGURATION)) {
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
        val prodEdm = removeAuditType(prodEdmApi.entityDataModel)

        // update version number
        val localVersion = edmManager.currentEntityDataModelVersion

        val edm = EntityDataModel(
                localVersion,
                prodEdm.namespaces,
                prodEdm.schemas,
                prodEdm.entityTypes,
                prodEdm.associationTypes,
                prodEdm.propertyTypes)

        // get differences between prod and local
        val edmDiff = edmManager.getEntityDataModelDiff(edm)

        // update with differences
        edmManager.setEntityDataModel(edmDiff.diff)
    }

    /**
     * Filter out audit entity sets
     */
    fun removeAuditType(edm: EntityDataModel): EntityDataModel {
        val propertyTypes = edm.propertyTypes.filter { it.type.toString() != OL_AUDIT_FQN.toString() }
        val entityTypes = edm.entityTypes.filter { it.type.toString() != OL_AUDIT_FQN.toString() }
        val associationTypes = edm.associationTypes.filter {
            it.associationEntityType.type.toString() != OL_AUDIT_FQN.toString()
        }

        return EntityDataModel(
                edm.version,
                edm.namespaces,
                edm.schemas,
                entityTypes,
                associationTypes,
                propertyTypes)
    }
}