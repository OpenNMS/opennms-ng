package org.opennms.ng.persistence.dao.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;

import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.opennms.netmgt.model.SurveillanceStatus;
import org.opennms.ng.persistence.dao.OnmsNodeDao;
import org.opennms.ng.persistence.entities.OnmsIpInterface;
import org.opennms.ng.persistence.entities.OnmsNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class OnmsNodeJpaDao extends GenericJpaDao<OnmsNode, Integer> implements OnmsNodeDao {

    private static final Logger LOG = LoggerFactory.getLogger(OnmsNodeJpaDao.class);

    public OnmsNodeJpaDao() {
        super(OnmsNode.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnmsNode get(String lookupCriteria) {
        if (lookupCriteria.contains(":")) {
            String[] criteria = lookupCriteria.split(":");
            return findByForeignId(criteria[0], criteria[1]);
        }
        return get(Integer.parseInt(lookupCriteria));
    }

    /**
     * Test the ability to simply retrieve a String object (node label) without
     * having to return a bulky Node object.
     */
    @Override
    public String getLabelForId(Integer id) {
        String label = null;
        label = findObjects(String.class, "select n.label from OnmsNode as n where n.id = ?", id).get(0);
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findNodes(final OnmsDistPoller distPoller) {
        return find("from OnmsNode where distPoller = ?", distPoller);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnmsNode getHierarchy(Integer id) {
        OnmsNode node = findUnique("select distinct n from OnmsNode as n " + "left join fetch n.assetRecord " + "where n.id = ?", id);

        initialize(node.getIpInterfaces());
        for (org.opennms.netmgt.model.OnmsIpInterface i : node.getIpInterfaces()) {
            initialize(i.getMonitoredServices());
        }

        initialize(node.getSnmpInterfaces());
        for (OnmsSnmpInterface i : node.getSnmpInterfaces()) {
            initialize(i.getIpInterfaces());
        }

        return node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findByLabel(String label) {
        return find("from OnmsNode as n where n.label = ?", label);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findAllByVarCharAssetColumn(String columnName, String columnValue) {
        return find("from OnmsNode as n where n.assetRecord." + columnName + " = ?", columnValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findAllByVarCharAssetColumnCategoryList(String columnName, String columnValue, Collection<OnmsCategory> categories) {

        return find("select distinct n from OnmsNode as n " + "join n.categories as c " + "left join fetch n.assetRecord "
            + "left join fetch n.ipInterfaces as ipInterface " + "left join fetch ipInterface.monitoredServices as monSvc "
            + "left join fetch monSvc.serviceType " + "left join fetch monSvc.currentOutages " + "where n.assetRecord." + columnName + " = ? "
            + "and c.name in (" + categoryListToNameList(categories) + ")", columnValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findByCategory(OnmsCategory category) {
        return find("select distinct n from OnmsNode as n " + "join n.categories c " + "left join fetch n.assetRecord "
            + "left join fetch n.ipInterfaces as ipInterface " + "left join fetch ipInterface.monitoredServices as monSvc "
            + "left join fetch monSvc.serviceType " + "left join fetch monSvc.currentOutages " + "where c.name = ?", category.getName());
    }

    private String categoryListToNameList(Collection<OnmsCategory> categories) {
        List<String> categoryNames = new ArrayList<String>();
        for (OnmsCategory category : categories) {
            categoryNames.add(category.getName());
        }
        return StringUtils.collectionToDelimitedString(categoryNames, ", ", "'", "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findAllByCategoryList(Collection<OnmsCategory> categories) {
        return find("select distinct n from OnmsNode as n " + "join n.categories c " + "left join fetch n.assetRecord "
            + "left join fetch n.ipInterfaces as ipInterface " + "left join fetch n.snmpInterfaces as snmpIface"
            + "left join fetch ipInterface.monitoredServices as monSvc " + "left join fetch monSvc.serviceType "
            + "left join fetch monSvc.currentOutages " + "where c.name in (" + categoryListToNameList(categories) + ")" + "and n.type != 'D'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findAllByCategoryLists(final Collection<OnmsCategory> rowCategories, final Collection<OnmsCategory> columnCategories) {

        Query q = em.createQuery(
            "select distinct n from OnmsNode as n " + "join n.categories c1 " + "join n.categories c2 " + "left join fetch n.assetRecord "
                + "left join fetch n.ipInterfaces as iface " + "left join fetch n.snmpInterfaces as snmpIface"
                + "left join fetch iface.monitoredServices as monSvc " + "left join fetch monSvc.serviceType "
                + "left join fetch monSvc.currentOutages " + "where c1 in (:rowCategories) " + "and c2 in (:colCategories) " + "and n.type != 'D'")
                    .setParameter("rowCategories", rowCategories).setParameter("colCategories", columnCategories);

        return q.getResultList();
    }

    @Override
    public SurveillanceStatus findSurveillanceStatusByCategoryLists(final Collection<OnmsCategory> rowCategories,
                                                                    final Collection<OnmsCategory> columnCategories) {

        //TODO - Weird query

        /**
         return getHibernateTemplate().execute(new HibernateCallback<SurveillanceStatus>() {

        @Override public SurveillanceStatus doInHibernate(Session session) throws HibernateException, SQLException {
        return (SimpleSurveillanceStatus)session.createSQLQuery("select" +
        " count(distinct case when outages.outageid is not null and monSvc.status = 'A' then monSvc.id else null end) as svcCount," +
        " count(distinct case when outages.outageid is null and monSvc.status = 'A' then node.nodeid else null end) as upNodeCount," +
        " count(distinct node.nodeid) as nodeCount" +
        " from node" +
        " join category_node cn1 using (nodeid)" +
        " join category_node cn2 using (nodeid)" +
        " left outer join ipinterface ip using (nodeid)" +
        " left outer join ifservices monsvc on (monsvc.ipinterfaceid = ip.id)" +
        " left outer join outages on (outages.ifserviceid = monsvc.id and outages.ifregainedservice is null)" +
        " where nodeType <> 'D'" +
        " and cn1.categoryid in (:rowCategories)" +
        " and cn2.categoryid in (:columnCategories)"
        )
        .setParameterList("rowCategories", rowCategories)
        .setParameterList("columnCategories", columnCategories)
        .setResultTransformer(new ResultTransformer() {
        private static final long serialVersionUID = 5152094813503430377L;

        @Override public Object transformTuple(Object[] tuple, String[] aliases) {
        LOG.debug("tuple length = {}", tuple.length);
        for (int i = 0;i < tuple.length;i++) {
        LOG.debug("{}: {} ({})", i, tuple[i], tuple[i].getClass());
        }
        return new SimpleSurveillanceStatus((Number) tuple[0], (Number) tuple[1], (Number) tuple[2]);
        }

        // Implements Hibernate API
        @SuppressWarnings("rawtypes")
        @Override public List transformList(List collection) {
        return collection;
        }
        })
        .uniqueResult();
        }

        });

         */

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Integer> getForeignIdToNodeIdMap(String foreignSource) {
        List<Object[]> pairs = find("select n.id, n.foreignId from OnmsNode n where n.foreignSource = ?", foreignSource);
        Map<String, Integer> foreignIdMap = new HashMap<String, Integer>();
        for (Object[] pair : pairs) {
            foreignIdMap.put((String) pair[1], (Integer) pair[0]);
        }
        return Collections.unmodifiableMap(foreignIdMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findByForeignSource(String foreignSource) {
        return find("from OnmsNode n where n.foreignSource = ?", foreignSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OnmsNode findByForeignId(String foreignSource, String foreignId) {
        return findUnique("from OnmsNode n where n.foreignSource = ? and n.foreignId = ?", foreignSource, foreignId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsNode> findByForeignSourceAndIpAddress(String foreignSource, String ipAddress) {
        if (foreignSource == null) {
            return find(
                "select distinct n from OnmsNode n join n.ipInterfaces as ipInterface where n.foreignSource is NULL and ipInterface.ipAddress = ?",
                ipAddress);
        } else {
            return find(
                "select distinct n from OnmsNode n join n.ipInterfaces as ipInterface where n.foreignSource = ? and ipInterface" + ".ipAddress = ?",
                foreignSource, ipAddress);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNodeCountForForeignSource(String foreignSource) {
        return queryInt("select count(*) from OnmsNode as n where n.foreignSource = ?", foreignSource);
    }

    /**
     * <p>findAll</p>
     *
     * @return a {@link java.util.List} object.
     */
    @Override
    public List<OnmsNode> findAll() {
        return find("from OnmsNode order by label");
    }

    /**
     * <p>findAllProvisionedNodes</p>
     *
     * @return a {@link java.util.List} object.
     */
    @Override
    public List<OnmsNode> findAllProvisionedNodes() {
        return find("from OnmsNode n where n.foreignSource is not null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OnmsIpInterface> findObsoleteIpInterfaces(Integer nodeId, Date scanStamp) {
        // we exclude the primary interface from the obsolete list since the only way for them to be obsolete is when we have snmp
        return findObjects(OnmsIpInterface.class,
            "from OnmsIpInterface ipInterface where ipInterface.node.id = ? and ipInterface.isSnmpPrimary != 'P' and (ipInterface.ipLastCapsdPoll "
                + "is null or ipInterface.ipLastCapsdPoll < ?)",
            nodeId, scanStamp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteObsoleteInterfaces(Integer nodeId, Date scanStamp) {
        //TODO - Create bulk

        /**

         getHibernateTemplate().bulkUpdate("delete from OnmsIpInterface ipInterface where ipInterface.node.id = ? and ipInterface.isSnmpPrimary !=
         'P' and (ipInterface.ipLastCapsdPoll is null or ipInterface.ipLastCapsdPoll < ?)", new Object[] { nodeId, scanStamp });
         getHibernateTemplate().bulkUpdate("delete from OnmsSnmpInterface snmpInterface where snmpInterface.node.id = ? and (snmpInterface
         .lastCapsdPoll is null or snmpInterface.lastCapsdPoll < ?)", new Object[] { nodeId, scanStamp });
         */

    }

    /**
     * {@inheritDoc}
     */
    public void updateNodeScanStamp(Integer nodeId, Date scanStamp) {
        OnmsNode n = get(nodeId);
        n.setLastCapsdPoll(scanStamp);
        update(n);
    }

    /**
     * <p>getNodeIds</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @Override
    public Collection<Integer> getNodeIds() {
        return findObjects(Integer.class, "select distinct n.id from OnmsNode as n where n.type != 'D'");
    }

    @Override
    public Integer getNextNodeId(Integer nodeId) {
        Integer nextNodeId = null;
        nextNodeId = findObjects(Integer.class, "select n.id from OnmsNode as n where n.id > ? and n.type != 'D' order by n.id asc limit 1", nodeId)
            .get(0);
        return nextNodeId;
    }

    @Override
    public Integer getPreviousNodeId(Integer nodeId) {
        Integer nextNodeId = null;
        nextNodeId = findObjects(Integer.class, "select n.id from OnmsNode as n where n.id < ? and n.type != 'D' order by n.id desc limit 1", nodeId)
            .get(0);
        return nextNodeId;
    }

    public static class SimpleSurveillanceStatus implements SurveillanceStatus {
        private static final Logger LOG = LoggerFactory.getLogger(SimpleSurveillanceStatus.class);
        private int m_serviceOutages;
        private int m_upNodeCount;
        private int m_nodeCount;

        public SimpleSurveillanceStatus(Number serviceOutages, Number upNodeCount, Number nodeCount) {
            LOG.debug("Args: {} ({}), {} ({}), {} ({})", serviceOutages, serviceOutages == null ? null : serviceOutages.getClass(), upNodeCount,
                upNodeCount == null ? null : upNodeCount.getClass(), nodeCount, nodeCount == null ? null : nodeCount.getClass());

            m_serviceOutages = serviceOutages == null ? 0 : serviceOutages.intValue();
            m_upNodeCount = upNodeCount == null ? 0 : upNodeCount.intValue();
            m_nodeCount = nodeCount == null ? 0 : nodeCount.intValue();
        }

        @Override
        public Integer getDownEntityCount() {
            return m_nodeCount - m_upNodeCount;
        }

        @Override
        public Integer getTotalEntityCount() {
            return m_nodeCount;
        }

        @Override
        public String getStatus() {
            switch (m_serviceOutages) {
            case 0:
                return "Normal";
            case 1:
                return "Warning";
            default:
                return "Critical";
            }
        }
    }
}
