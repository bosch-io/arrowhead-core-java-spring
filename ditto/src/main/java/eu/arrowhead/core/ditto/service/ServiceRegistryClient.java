/********************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.arrowhead.core.ditto.service;

import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.http.HttpService;

@Service
public class ServiceRegistryClient {

	//=================================================================================================
	// members

	private static final String SERVICEREGISTRY_REGISTER_URI =
			CommonConstants.SERVICEREGISTRY_URI + CommonConstants.OP_SERVICEREGISTRY_REGISTER_URI;

	private static final String SERVICEREGISTRY_UNREGISTER_URI =
			CommonConstants.SERVICEREGISTRY_URI + CommonConstants.OP_SERVICEREGISTRY_UNREGISTER_URI;

	@Autowired
	private HttpService httpService;

	@Resource(name = CommonConstants.ARROWHEAD_CONTEXT)
	private Map<String,Object> arrowheadContext;

	@Autowired
	protected SSLProperties sslProperties;

	@Value(CoreCommonConstants.$CORE_SYSTEM_NAME)
	private String systemName;

	@Value(CoreCommonConstants.$DOMAIN_NAME)
	private String systemDomainName;

	@Value(CoreCommonConstants.$DOMAIN_PORT)
	private int systemDomainPort;

	@Value(CommonConstants.$SERVICEREGISTRY_ADDRESS_WD)
	private String serviceRegistryAddress;

	@Value(CommonConstants.$SERVICEREGISTRY_PORT_WD)
	private int serviceRegistryPort;



	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ResponseEntity<ServiceRegistryResponseDTO> registerService(
			final String serviceDefinition,
			final String serviceUri,
			final Map<String, String> metadata
		) {
		Assert.notNull(serviceDefinition, "Service definition is null");
		Assert.notNull(serviceUri, "Service URI is null");

		UriComponents uri = getRegisterUri();
		final ServiceRegistryRequestDTO request =
				getServiceRegistryRequest(encodeServiceDefinition(serviceDefinition), serviceUri, metadata);
		return httpService.sendRequest(uri, HttpMethod.POST, ServiceRegistryResponseDTO.class, request);
	}

	// -------------------------------------------------------------------------------------------------
	public ResponseEntity<Void> unregisterService(final String serviceDefinition, final String serviceUri) {
		Assert.notNull(serviceUri, "Service URI is null");
		UriComponents uri = getUnregisterUri(encodeServiceDefinition(serviceDefinition), serviceUri);
		return httpService.sendRequest(uri, HttpMethod.DELETE, Void.class);
	}

	//=================================================================================================
	// assistant methods

	// -------------------------------------------------------------------------------------------------
	private ServiceRegistryRequestDTO getServiceRegistryRequest(
		final String serviceDefinition,
		final String serviceUri,
		final Map<String, String> metadata
	) {
		final ServiceRegistryRequestDTO request = new ServiceRegistryRequestDTO();
		request.setProviderSystem(getSystemDescription());
		request.setServiceDefinition(serviceDefinition);
		request.setServiceUri(serviceUri);
		if (metadata != null) {
			request.setMetadata(metadata);
		}
		request.setSecure(ServiceSecurityType.TOKEN.name());
		request.setInterfaces(List.of(CommonConstants.HTTP_SECURE_JSON));
		return request;
	}

	//-------------------------------------------------------------------------------------------------
	private UriComponents getRegisterUri() {
		return UriComponentsBuilder.newInstance()
				.scheme(sslProperties.isSslEnabled() ? CommonConstants.HTTPS : CommonConstants.HTTP)
				.host(serviceRegistryAddress)
				.port(serviceRegistryPort)
				.path(SERVICEREGISTRY_REGISTER_URI)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private UriComponents getUnregisterUri(final String serviceDefinition, final String serviceUri) {
		return UriComponentsBuilder.newInstance()
				.scheme(sslProperties.isSslEnabled() ? CommonConstants.HTTPS : CommonConstants.HTTP)
				.host(serviceRegistryAddress)
				.queryParam("service_definition", serviceDefinition)
				.queryParam("service_uri", serviceUri)
				.queryParam("system_name", systemName.toLowerCase())
				.queryParam("address", systemDomainName)
				.queryParam("port", systemDomainPort)
				.port(serviceRegistryPort)
				.path(SERVICEREGISTRY_UNREGISTER_URI)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private SystemRequestDTO getSystemDescription() {
		final SystemRequestDTO system = new SystemRequestDTO();
		system.setAddress(systemDomainName);
		system.setPort(systemDomainPort);
		system.setSystemName(systemName);

		if (sslProperties.isSslEnabled()) {
			final PublicKey publicKey = (PublicKey) arrowheadContext.get(CommonConstants.SERVER_PUBLIC_KEY);
			final String authInfo = Base64.getEncoder().encodeToString(publicKey.getEncoded());
			system.setAuthenticationInfo(authInfo);
		}
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	private String encodeServiceDefinition(String serviceDefinition) {
		Base32 base32 = new Base32();
		String encodedServiceDefinition = base32.encodeAsString(serviceDefinition.getBytes());
		encodedServiceDefinition = encodedServiceDefinition.toLowerCase();
		encodedServiceDefinition = encodedServiceDefinition.replace('=', '-');

		encodedServiceDefinition = encodedServiceDefinition + "a";

		return encodedServiceDefinition;
	}
}
