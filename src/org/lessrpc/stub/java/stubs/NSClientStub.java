package org.lessrpc.stub.java.stubs;

import java.io.IOException;
import java.util.List;

import org.lessrpc.stub.java.StubConstants;
import org.lessrpc.stub.java.cache.ServiceProviderCache;
import org.lessrpc.stub.java.errors.NoProviderAvailableException;
import org.lessrpc.common.errors.RPCException;
import org.lessrpc.common.errors.RPCProviderFailureException;
import org.lessrpc.common.errors.ResponseContentTypeCannotBePrasedException;
import org.lessrpc.common.errors.SerializationFormatNotSupported;
import org.lessrpc.common.errors.ServiceProviderNotAvailable;
import org.lessrpc.common.info.SerializationFormat;
import org.lessrpc.common.info.ServiceDescription;
import org.lessrpc.common.info.ServiceInfo;
import org.lessrpc.common.info.ServiceProviderInfo;
import org.lessrpc.common.info.ServiceSupportInfo;
import org.lessrpc.common.info.responses.ServiceResponse;
import org.lessrpc.common.serializer.Serializer;

public class NSClientStub extends ClientStub implements StubConstants {

	private NSClient ns;

	/**
	 * provider cache
	 */
	private final ServiceProviderCache cache;

	private ServiceProviderInfo nsInfo;

	public NSClientStub(ServiceProviderInfo nsInfo, ServiceProviderCache cache, List<Serializer> serializers) {
		super(serializers);
		this.nsInfo = nsInfo;
		this.cache = cache;
		this.ns = new NSClient(nsInfo, serializers);
	}

	/**
	 * executes /service service for given service to given service provider
	 * 
	 * @param info
	 * @param service
	 * @return
	 * @throws Exception
	 */
	public ServiceSupportInfo getServiceSupport(ServiceInfo<?> service) throws Exception {
		return getServiceSupport(getProvider(service).getProvider(), service);
	}

	/**
	 * calls execute service. It uses cached service provider or retrieves an
	 * available provider from the name server
	 * 
	 * @param service
	 * @param info
	 * @param args
	 * @param serializer
	 * @return
	 * @throws ResponseContentTypeCannotBePrasedException
	 * @throws SerializationFormatNotSupported
	 * @throws RPCException
	 * @throws RPCProviderFailureException
	 * @throws IOException
	 * @throws Exception
	 */
	public <T> ServiceResponse<T> call(ServiceDescription<T> service, Object[] args, Serializer serializer)
			throws ResponseContentTypeCannotBePrasedException, SerializationFormatNotSupported, RPCException,
			RPCProviderFailureException, IOException, Exception {

		return this.call(service, args, serializer, null);
	}

	/**
	 * calls execute service. It uses cached service provider or retrieves an
	 * available provider from the name server
	 * 
	 * @param service
	 * @param info
	 * @param args
	 * @param serializer
	 * @return
	 * @throws ResponseContentTypeCannotBePrasedException
	 * @throws SerializationFormatNotSupported
	 * @throws RPCException
	 * @throws RPCProviderFailureException
	 * @throws IOException
	 * @throws Exception
	 */
	public <T> ServiceResponse<T> call(ServiceDescription<T> service, Object[] args, Serializer serializer,
			SerializationFormat[] accept) throws ResponseContentTypeCannotBePrasedException,
					SerializationFormatNotSupported, RPCException, RPCProviderFailureException, IOException, Exception {

		ServiceProviderInfo provider = getProvider(service.getInfo()).getProvider();
		if (provider == null) {
			// no provider existed so throw appropriate Exception
			throw new NoProviderAvailableException(service.getInfo());
		}
		// provider existed so try to connect
		ServiceResponse<T> response = null;
		try {
			response = call(service, provider, args, serializer, accept);
		} catch (ResponseContentTypeCannotBePrasedException | RPCException | SerializationFormatNotSupported e) {
			// none connectivity error happened
			throw e;
		} catch (RPCProviderFailureException | IOException e) {
			// a connectivity error happened. So try to find a new Provider
			// clear cache
			cache.clear(service.getInfo());
			// check if provider still works
			ns.checkProviderStatus(provider);
			// call again and it will ask for a provider from NameServer
			// accordingly
			call(service, args, serializer);
		} catch (Exception e) {
			// other none connectivity errors
			throw e;
		}

		return response;
	}

	/**
	 * Retrieves a provider from cache. If it doesn't exist then it requests one
	 * from database
	 * 
	 * @param service
	 * @return ServiceSupportInfo
	 * @throws Exception
	 * @throws IOException
	 * @throws RPCProviderFailureException
	 * @throws RPCException
	 * @throws SerializationFormatNotSupported
	 * @throws ResponseContentTypeCannotBePrasedException
	 */
	private ServiceSupportInfo getProvider(ServiceInfo<?> service) throws ResponseContentTypeCannotBePrasedException,
			SerializationFormatNotSupported, RPCException, RPCProviderFailureException, IOException, Exception {
		// attempts to get the cached service provider for the given service
		ServiceSupportInfo info = cache.get(service);
		// checks if an info is cached for the given service
		if (info == null) {
			info = ns.getProvider(service);
			cache.cache(info);
		}
		if (info == null) {
			throw new ServiceProviderNotAvailable(service);
		}
		return info;
	}

	public ServiceInfo<?> getServiceInfo(int serviceId) throws ResponseContentTypeCannotBePrasedException,
			SerializationFormatNotSupported, RPCException, RPCProviderFailureException, IOException, Exception {
		return ns.getServiceInfoById(serviceId);
	}

	public ServiceInfo<?> getServiceInfo(String serviceName) throws ResponseContentTypeCannotBePrasedException,
			SerializationFormatNotSupported, RPCException, RPCProviderFailureException, IOException, Exception {
		return ns.getServiceInfoByName(serviceName);
	}

	public ServiceProviderInfo getNsInfo() {
		return nsInfo;
	}

}
