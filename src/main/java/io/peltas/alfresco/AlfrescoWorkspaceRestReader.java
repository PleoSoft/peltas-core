/**
 * Copyright 2019 Pleo Soft d.o.o. (pleosoft.com)

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.peltas.alfresco;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.peltas.boot.PeltasProperties;
import io.peltas.core.PeltasEntry;
import io.peltas.core.batch.AbstractPeltasRestReader;
import io.peltas.core.repository.TxDataRepository;
import io.peltas.core.repository.jpa.PeltasTimestamp;

public class AlfrescoWorkspaceRestReader extends AbstractPeltasRestReader<AlfrescoWorkspaceNodes>
		implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoWorkspaceRestReader.class);

	public static final String AUDIT_ID_SEPARATOR = "___";

	private final TxDataRepository auditRepository;
	private final PeltasProperties auditProperties;
	private final AtomicLong txId = new AtomicLong(0);
	private Long skipToNodeId = null;
	private Long skipToTxId = null;
	private boolean retry = true;
	private PeltasTimestamp auditTimeStamp;
	private Long fromTxId;
	private Long toTxId;

	private Long lastFromTxId;
	private Long currentMaxTxId;

	public AlfrescoWorkspaceRestReader(final RestTemplate restTemplate, final PeltasProperties properties,
			TxDataRepository auditRepository, String applicationName) {
		super(applicationName, restTemplate);
		this.auditProperties = properties;
		this.auditRepository = auditRepository;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		auditTimeStamp = auditRepository.readTx(getCurrentApplicationName());
		if (auditTimeStamp != null) {
			String ref = auditTimeStamp.getRef();
			String[] split = ref.split(AUDIT_ID_SEPARATOR);
			String[] tx = split[0].split(";");
			Long currentTxId = Long.valueOf(tx[0]);
			txId.set(currentTxId - 1);

			skipToTxId = currentTxId;
			skipToNodeId = Long.valueOf(tx[1]);
		}
		fromTxId = txId.incrementAndGet();
		toTxId = txId.addAndGet(5);
		
		currentMaxTxId = getCurrentMaxTxnId();
	}

	@Override
	protected void doOpen() throws Exception {
		onOpen();
	}

	@Override
	public void onOpen() {
		lastFromTxId = fromTxId.longValue();
		// setCurrentItemCount(0);
		// setMaxItemCount(lastCount > 0 ? lastCount : 1);
		super.onOpen();
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	protected AlfrescoWorkspaceNodes onResponseReceived(AlfrescoWorkspaceNodes auditEntries) {
		List<AlfrescoNode> notSorted = auditEntries.getNodes();
		if (notSorted.size() > 1) {
			notSorted.sort(new Comparator<AlfrescoNode>() {
				@Override
				public int compare(AlfrescoNode m1, AlfrescoNode m2) {
					if (m1.getTxnId().equals(m2.getTxnId())) {
						return 0;
					}
					return m1.getTxnId() > m2.getTxnId() ? 1 : -1;
				}
			});
		}

		return super.onResponseReceived(auditEntries);
	}

	@Override
	public PeltasEntry read() throws Exception, UnexpectedInputException, ParseException {
		return super.read();
	}

	@BeforeChunk
	public void beforeChunk(ChunkContext context) {
		context.setAttribute("peltasTimestamp", auditTimeStamp);
	}

	@AfterChunk
	public void afterChunk(ChunkContext context) {
		auditTimeStamp = (PeltasTimestamp) context.getAttribute("peltasTimestamp");
		if (auditTimeStamp == null) {
			// before chunk was not executed, it is highly probably that the alfresco server
			// was down
			return;
		}

		String[] tx = auditTimeStamp.getRef().split(";");
		Long currentTxId = Long.valueOf(tx[0]);
		txId.set(currentTxId);
		fromTxId = txId.incrementAndGet();
		toTxId = txId.addAndGet(5);
	}

	@Override
	protected PeltasEntry doRetryRead() throws Exception {
		if (!retry) {
			return null;
		}

		if (lastFromTxId != null && lastFromTxId.equals(fromTxId.longValue())) {
			return null;
		}

		doOpen();
		PeltasEntry read = read();
		return read;
	}

	@Override
	protected List<PeltasEntry> retreiveCollection(AlfrescoWorkspaceNodes response) {

		List<AlfrescoNode> nodes = response.getNodes();
		if (nodes.size() > 0) {
			retry = true;
		} else {
			retry = false;			
			if (currentMaxTxId.longValue() > lastFromTxId.longValue()) {
				retry = true;
				fromTxId = txId.incrementAndGet();
				toTxId = txId.addAndGet(5);
			} else {
				currentMaxTxId = getCurrentMaxTxnId();
				retry = true;
			}
			return Collections.emptyList();
		}

		List<AlfrescoNodeMetadata> deletedNodesMetadata = fetchDeletedNodesMetadata(nodes);

		List<Long> nodesId = new ArrayList<>(nodes.size());
		LinkedMultiValueMap<Long, AlfrescoNode> nodesMap = new LinkedMultiValueMap<>(nodes.size());
		for (AlfrescoNode livedataNode : nodes) {

			if (shouldSkipLivedataNode(livedataNode)) {
				continue;
			}

			Long nodeId = livedataNode.getId();
			nodesId.add(nodeId);
			nodesMap.add(nodeId, livedataNode);
		}

		if (nodesId.size() == 0) {
			if (currentMaxTxId.longValue() > lastFromTxId.longValue()) {
				retry = true;
				fromTxId = txId.incrementAndGet();
				toTxId = txId.addAndGet(5);
			} else {
				currentMaxTxId = getCurrentMaxTxnId();
				retry = true;
			}
			return Collections.emptyList();	
		}

		Map<Object, Object> map = new HashMap<>();
		map.put("nodeIds", nodesId);
		map.put("includeProperties", "true");
		map.put("includeParentAssociations", "false");
		map.put("includeChildIds", "false");
		map.put("includeChildAssocs", "false");
		map.put("includeAclId", "false");
		map.put("includePaths", "false");
		map.put("includeAspects", "true");

		String url = auditProperties.getHost() + "/" + auditProperties.getServiceUrl() + "/metadata";

		LOGGER.trace("retreiveCollection() repository data {}", url);
		ResponseEntity<AlfrescoNodeMetadataList> responseMetadata = getRestTemplate().exchange(url, HttpMethod.POST,
				new HttpEntity<>(map), AlfrescoNodeMetadataList.class);
		AlfrescoNodeMetadataList body = responseMetadata.getBody();
		List<AlfrescoNodeMetadata> nodesMetadata = body.getNodes();
		nodesMetadata.addAll(deletedNodesMetadata);

		if (nodesMetadata.size() == 0) {
			return Collections.emptyList();
		}

		List<PeltasEntry> metadataList = new ArrayList<>(nodesMetadata.size());
		for (AlfrescoNodeMetadata metadata : nodesMetadata) {
			AlfrescoNode livedataNode = nodesMap.get(metadata.getId()).remove(0);
			PeltasEntry auditEntry = convertToAudit(metadata, livedataNode);
			metadataList.add(auditEntry);
		}

		LOGGER.trace("retreiveCollection() repository data received {}", metadataList);

		return metadataList;
	}

	private List<AlfrescoNodeMetadata> fetchDeletedNodesMetadata(List<AlfrescoNode> nodes) {
		List<AlfrescoNodeMetadata> result = new ArrayList<>();
		for (AlfrescoNode node : nodes) {

			if (shouldSkipLivedataNode(node)) {
				continue;
			}

			if ("d".equals(node.getStatus())) {
				AlfrescoNodeMetadata metadata = new AlfrescoNodeMetadata();
				metadata.setNodeRef(node.getNodeRef());
				metadata.setId(node.getId());
				metadata.setTxnId(node.getTxnId());
				result.add(metadata);
			}
		}
		return result;
	}

	private PeltasEntry convertToAudit(AlfrescoNodeMetadata livedataMetadata, AlfrescoNode livedataNode) {
		PeltasEntry auditEntry = new PeltasEntry();

		auditEntry.setId(livedataMetadata.getTxnId() + ";" + livedataMetadata.getId());
		auditEntry.setApplication(getCurrentApplicationName());

		Map<String, Object> liveProperties = livedataMetadata.getProperties();

		List<Map<String, String>> aspects = new ArrayList<>();
		if (livedataMetadata.getAspects() != null) {
			for (String aspect : livedataMetadata.getAspects()) {
				String prefix = aspect;
				String[] split = prefix.split(":", 2);
				Map<String, String> map = new HashMap<>();
				map.put("prefixString", split[0]);
				map.put("localName", split[1]);
				aspects.add(map);
			}
		}

		String status = livedataNode.getStatus();
		String actionStatus = status != null ? status.equals("d") ? "DELETED" : "UPDATED" : "UNKNOWN";

		Map<String, Object> map = new HashMap<>();
		if (liveProperties != null) {
			map.put("/alfresco-workspace/transaction/properties/add", liveProperties);
		}

		if (aspects != null) {
			map.put("/alfresco-workspace/transaction/aspects/add", aspects);
		}

		if (livedataMetadata.getType() != null) {
			map.put("/alfresco-workspace/transaction/type", livedataMetadata.getType());
		}

		if (actionStatus != null) {
			map.put("/alfresco-workspace/transaction/action", "NODE-" + actionStatus);
		}

		if (livedataMetadata.getNodeRef() != null) {
			map.put("/alfresco-workspace/transaction/path", livedataMetadata.getNodeRef());
			map.put("/alfresco-workspace/transaction/nodeRef", livedataMetadata.getNodeRef());

			int lastIndexOf = livedataMetadata.getNodeRef().lastIndexOf("/");
			if (lastIndexOf != -1) {
				String nodeId = livedataMetadata.getNodeRef().substring(lastIndexOf + 1);
				map.put("/alfresco-workspace/transaction/nodeId", nodeId);
			}
		}

		auditEntry.setValues(map);

		// get cm:creator & created
		auditEntry.setUser("UNKNOWN");
		auditEntry.setTime(new Timestamp(new Date().getTime()));
		return auditEntry;
	}

	@Override
	protected String getQueryString() {
		return auditProperties.getHost() + "/" + auditProperties.getServiceUrl() + "/nodes";
	}

	@Override
	protected HttpEntity<?> getHttpEntity() {
		Map<String, Object> map = new HashMap<>();
		map.put("fromTxnId", fromTxId);
		map.put("toTxnId", toTxId);
		return new HttpEntity<>(map);
	}

	@Override
	protected HttpMethod getHttpMethod() {
		return HttpMethod.POST;
	}

	private Boolean shouldSkipLivedataNode(AlfrescoNode livedataNode) {
		String nodeRef = livedataNode.getNodeRef();
		if (!nodeRef.startsWith("workspace://SpacesStore/")) {
			// TODO handle other stores (solr cores)?
			return true;
		}

		if (skipToTxId != null && skipToTxId > livedataNode.getTxnId()) {
			return true;
		}

		if (skipToTxId != null && skipToNodeId != null && skipToTxId.equals(livedataNode.getTxnId())
				&& skipToNodeId >= livedataNode.getId()) {
			return true;
		}
		return false;
	}

	private Long getCurrentMaxTxnId() {
		String url = auditProperties.getHost() + "/" + auditProperties.getServiceUrl() + "/transactions";
		url = UriComponentsBuilder.fromHttpUrl(url).queryParam("minTxnId", 1).queryParam("maxResults", 1).toUriString();

		try {
			LOGGER.trace("Getting MAX transactions from Alfresco/SOLR");
			ResponseEntity<AlfrescoWorkspaceTxnMetadata> response = getRestTemplate().getForEntity(url,
					AlfrescoWorkspaceTxnMetadata.class);
			LOGGER.trace("Received MAX transactions from Alfresco/SOLR");
			AlfrescoWorkspaceTxnMetadata txnMetadata = response.getBody();
			return txnMetadata.getMaxTxnId();
		} catch (RestClientException e) {
			return currentMaxTxId != null ? currentMaxTxId : 1;
		}
	}

}
