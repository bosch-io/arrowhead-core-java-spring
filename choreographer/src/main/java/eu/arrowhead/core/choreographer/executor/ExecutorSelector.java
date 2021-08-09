/********************************************************************************
 * Copyright (c) 2021 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   AITIA - implementation
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.arrowhead.core.choreographer.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.database.entity.ChoreographerExecutor;
import eu.arrowhead.common.dto.shared.ChoreographerExecutorServiceInfoResponseDTO;
import eu.arrowhead.core.choreographer.database.service.ChoreographerExecutorDBService;
import eu.arrowhead.core.choreographer.service.ChoreographerDriver;

@Component
public class ExecutorSelector {
	
	//=================================================================================================
	// methods
	
	@Autowired
	private ChoreographerExecutorDBService executorDBService;
	
	@Autowired
	private ChoreographerDriver driver;
	
	@Autowired
	private ExecutorPrioritizationStrategy prioritizationStrategy;

	private final Object lock = new Object();
	
	private static final Logger logger = LogManager.getLogger(ExecutorSelector.class);
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ChoreographerExecutor select(final String serviceDefinition, final Integer minVersion, final Integer maxVersion, final List<ChoreographerExecutor> exclusions) {
		//exclusions: when a ChoreographerSessionStep failed due to executor issue, then selection can be repeated but without that executor(s)
		logger.debug("select started...");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is empty");
		
		final int minVersion_ = minVersion == null ? 0 : minVersion;
		final int maxVersion_ = maxVersion == null ? Integer.MAX_VALUE : maxVersion;
		
		List<ChoreographerExecutor> potentials = executorDBService.getExecutorsByServiceDefinitionAndVersion(serviceDefinition, minVersion_, maxVersion_);
		potentials = filterOutLockedAndExcludedExecutors(potentials, exclusions);
		if (potentials.isEmpty()) {
			return null;
		}
		
		final Map<ChoreographerExecutor,ChoreographerExecutorServiceInfoResponseDTO> executorServiceInfos = collectExecutorServiceInfos(potentials, serviceDefinition, minVersion_, maxVersion_);
		potentials = priorizeByStrategy(executorServiceInfos);
		
		return selectVerifyAndInitFirstAvailable(potentials);
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private List<ChoreographerExecutor> filterOutLockedAndExcludedExecutors(final List<ChoreographerExecutor> original, final List<ChoreographerExecutor> exclusions) {
		logger.debug("filterOutLockedAndExcludedExecutors started...");
		
		final Set<Long> excudedIds = exclusions.stream().map(e -> e.getId()).collect(Collectors.toSet());
		
		synchronized (lock) {
			final List<ChoreographerExecutor> filtered = new ArrayList<>(original.size());
			for (final ChoreographerExecutor executor : original) {
				if (!executor.isLocked() && !excudedIds.contains(executor.getId())) {
					filtered.add(executor);
				}
			}			
			return filtered;
		}		
	}
	
	//-------------------------------------------------------------------------------------------------
	private Map<ChoreographerExecutor,ChoreographerExecutorServiceInfoResponseDTO> collectExecutorServiceInfos(final List<ChoreographerExecutor> potentials, final String serviceDefinition,
																							  				   final int minVersion, final int maxVersion) {
		logger.debug("collectExecutorServiceInfos started...");
		
		final Map<ChoreographerExecutor,ChoreographerExecutorServiceInfoResponseDTO> collected = new HashMap<>(potentials.size());
		for (final ChoreographerExecutor executor : potentials) {
			try {
				final ChoreographerExecutorServiceInfoResponseDTO info = driver.getExecutorServiceInfo(executor.getAddress(), executor.getPort(), executor.getBaseUri(),
																									   serviceDefinition, minVersion, maxVersion);
				collected.put(executor, info);
				
			} catch (final Exception ex) {
				logger.debug(ex.getMessage());
			}
		}
		return collected;
	}
	
	//-------------------------------------------------------------------------------------------------
	private List<ChoreographerExecutor> priorizeByStrategy(final Map<ChoreographerExecutor, ChoreographerExecutorServiceInfoResponseDTO> executorServiceInfos) {
		logger.debug("sortByStrategy started...");
		return prioritizationStrategy.priorize(executorServiceInfos);
	}
	
	//-------------------------------------------------------------------------------------------------
	private ChoreographerExecutor selectVerifyAndInitFirstAvailable(final List<ChoreographerExecutor> potentials) {
		logger.debug("selectAndInit started...");
		
		if (potentials.isEmpty()) {
			return null;
		}
		
		synchronized (lock) {
			for (final ChoreographerExecutor potential : potentials) {
				final Optional<ChoreographerExecutor> optional = executorDBService.getExecutorOptionalById(potential.getId()); //refreshing from DB
				if (optional.isEmpty()) {
					continue;
				}
				final ChoreographerExecutor executor = optional.get();
				if (!executor.isLocked()) {
					// TODO Verify the dependencies wit SR (Strategy implementation either did it or didn't)
					// TODO create a ChoreographerSessionStep entry with Initialized state in order to not being deletable 
					return executor;
				}
			}
			return null;
		}
	}
}
